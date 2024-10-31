package projection.to.kafka

import akka.{ Done, NotUsed }
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.kafka.scaladsl.Consumer.{ Control, DrainingControl }
import akka.kafka.scaladsl.{ Consumer, SendProducer, Transactional }
import akka.kafka.{ ProducerMessage, Subscriptions }
import akka.stream.{
  ActorMaterializer,
  KillSwitches,
  RestartSettings,
  UniqueKillSwitch
}
import akka.stream.scaladsl.{
  Flow,
  Keep,
  RestartSource,
  Sink,
  Source
}
import example.bet.grpc.{ BetService, SettleMessage }
import example.betting.{ Bet, Market }
import example.betting.Bet.{ Command }
import org.apache.kafka.clients.admin.{ AdminClient, NewTopic }
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

import java.util.stream.Collectors
import scala.collection.mutable.HashMap
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

class BetResultTransactionalConsumer(
    implicit producer: SendProducer[String, Array[Byte]],
    adminClient: AdminClient,
    sharding: ClusterSharding,
    system: ActorSystem[Nothing],
    ec: ExecutionContext) {

  def getProducer: SendProducer[String, Array[Byte]] = producer
  def getAdminClient: AdminClient = adminClient
  def getSharding: ClusterSharding = sharding
  def getSystem: ActorSystem[Nothing] = system
  def getEc: ExecutionContext = ec

  val log =
    LoggerFactory.getLogger(classOf[BetResultTransactionalConsumer])

  def init(marketId: String, marketResult: Int): UniqueKillSwitch = {
    val topic = s"bet-result-${marketId}"
    val groupId = topic
    val newTopic = new NewTopic(topic, 3, 3: Short)
    adminClient.createTopics(java.util.Arrays.asList(newTopic))
    val consumerSettings =
      BetResultConsumerSettings(producer.settings, groupId, system)
    val (killSwitch: UniqueKillSwitch, streamDone: Future[Done]) =
      RestartSource
        .onFailuresWithBackoff(
          RestartSettings(
            minBackoff = 1.second,
            maxBackoff = 10.seconds,
            randomFactor = 0.1)
          /*.withMaxRestarts(2, 120.minutes)*/ ) { () =>
          Transactional
            .source(
              consumerSettings.kafkaConsumerSettings(),
              Subscriptions.topics(topic))
            .map { msg =>
              log.warn(s"Got message - ${msg.record.value}")
              val betProto = {
                example.bet.grpc.Bet.parseFrom(msg.record.value)
              }
              Bet
                .requestBetSettlement(
                  betProto.betId,
                  marketResult,
                  sharding)
                .map { response =>
                  response match {
                    case Bet.Accepted =>
                      log.warn(s"stake settled [${betProto.betId}]")
                    case Bet.RequestUnaccepted(reason) =>
                      val message =
                        s"stake not settled [${betProto.betId}]. Reason [${reason}]"
                      log.error(message)
                  }
                }
              log.debug(
                s"Settle message: betId - ${betProto.betId}, marketId = ${marketId}, marketResult - ${marketResult}")
            }
        }
        .viaMat(KillSwitches.single)(
          Keep.right
        ) // Attach the KillSwitch
        .toMat(Sink.ignore)(
          Keep.both
        ) // Keep both the KillSwitch and the stream result (streamDone)
        .run()

    streamDone.onComplete((_) =>
      Market
        .requestAllMessagesConsumed(marketId, sharding)
        .map { response =>
          response match {
            case Market.Accepted =>
              log.warn(
                s"All messages have been consumed for topic ${topic}")
            case Market.RequestUnaccepted(reason) =>
              val message =
                s"All messages consumed failure for market [${marketId}]. Reason [${reason}]"
              log.error(message)
          }
        })

    killSwitch
  }

}
