package projection.to.kafka

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.kafka.scaladsl.Consumer.{ Control, DrainingControl }
import akka.kafka.scaladsl.{ Consumer, SendProducer, Transactional }
import akka.kafka.{ ProducerMessage, Subscriptions }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Flow, Keep, RestartSource, Sink }
import example.bet.grpc.{ BetService, SettleMessage }
import example.betting.{ Bet, Market }
import example.betting.Bet.Command
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

import java.util.stream.Collectors
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

  def init(marketId: String, marketResult: Int): Unit = {
    // #transactionalFailureRetry
    val topic = s"bet-result-${marketId}"
    val groupId = topic
    val consumerSettings =
      BetResultConsumerSettings(producer.settings, groupId, system)

    val stream = RestartSource.onFailuresWithBackoff(
      minBackoff = 1.seconds,
      maxBackoff = 30.seconds,
      randomFactor = 0.2) { () =>
      Transactional
        .source(
          consumerSettings.kafkaConsumerSettings(),
          Subscriptions.topics(topic))
        .map { msg =>
          log.warn(s"Got message - ${msg.record.value()}")
          val betProto =
            example.bet.grpc.Bet.parseFrom(msg.record.value)
          Bet
            .requestBetSettlement(
              betProto.betId,
              marketResult,
              sharding)
            .map { response =>
              response match {
                case Bet.Accepted =>
                  log.warn(s"stake settled [$betProto]")
                case Bet.RequestUnaccepted(reason) =>
                  val message =
                    s"stake not settled [$betProto]. Reason [${reason}]"
                  log.error(message)
              }
            }
          log.warn(
            s"Settle message: betId - ${betProto.betId}, marketResult - ${marketResult}")
        }
    }

    stream.runWith(Sink.ignore)

  }

}
