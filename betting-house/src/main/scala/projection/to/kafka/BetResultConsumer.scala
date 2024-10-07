package projection.to.kafka

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.{
  ClusterSharding,
  Entity
}
import akka.kafka.ConsumerMessage.TransactionalMessage
import akka.kafka.scaladsl.{ Consumer, SendProducer, Transactional }
import akka.kafka.{
  ConsumerSettings,
  ProducerMessage,
  ProducerSettings,
  Subscriptions
}
import akka.stream.scaladsl.Sink
import akka.util.Timeout
import example.bet.grpc.{ BetService, SettleMessage }
import example.betting.Bet
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{
  StringDeserializer,
  StringSerializer
}
import org.slf4j.LoggerFactory

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ ExecutionContext, Future }
import scala.io.StdIn

object BetResultConsumer {

  val log = LoggerFactory.getLogger(this.getClass)

  val timeout: Timeout = 6.seconds

  def init(
      implicit consumerSettings: BetResultConsumerSettings,
      producer: SendProducer[String, Array[Byte]],
      transactionalId: String,
      betService: BetService,
      system: ActorSystem[Nothing],
      ec: ExecutionContext) = {

    log.warn("Consumer starting")

    val drainingControl: Consumer.DrainingControl[_] =
      Transactional
        .source(
          consumerSettings.kafkaConsumerSettings(),
          Subscriptions.topics(consumerSettings.topic)
        ) //we don't need committer settings
        .map { msg: TransactionalMessage[String, Array[Byte]] =>
          /*val betProto =
            example.bet.grpc.Bet.parseFrom(msg.record.value)
          sendEvent(msg.record.key, msg.record.value, producer)
          betService.settle(
            SettleMessage(betProto.betId, betProto.result))*/
          log.warn(s"Got message - ${msg.record.value()}")
          ProducerMessage.single(
            new ProducerRecord[String, Array[Byte]](
              "sink-topic",
              msg.record.key,
              msg.record.value),
            msg.partitionOffset)
        }
        .toMat(
          Transactional.sink(producer.settings, transactionalId))(
          Consumer.DrainingControl.apply)
        .run()

    val future = drainingControl.drainAndShutdown
    //future.onComplete(_ => system.terminate)

    log.warn("Consumer started")

  }

  /*private def sendEvent(
      key: String,
      serializedEvent: Array[Byte],
      producer: SendProducer[String, Array[Byte]]): Future[Done] = {
    val topic = "consumed"

    if (!serializedEvent.isEmpty) {
      val record =
        new ProducerRecord(topic, key, serializedEvent)
      producer.send(record).map { _ =>
        log.debug(s"published event [$key] to topic [$topic]}")
        Done
      }
      Future.successful(Done)
    } else {
      Future.successful(Done)
    }
  }*/
}
