package projection.to.kafka

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.kafka.ConsumerMessage.TransactionalMessage
import akka.kafka.scaladsl.{Consumer, SendProducer, Transactional}
import akka.kafka.{ConsumerSettings, ProducerMessage, ProducerSettings, Subscriptions}
import example.betting.Bet
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.slf4j.LoggerFactory
import projection.to.kafka.BetKafkaProcessor.{logger, sendEvent}

import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn

object BetResultConsumer {

  implicit val system = ActorSystem(Behaviors.empty, "producerOne")

  implicit val ec = ExecutionContext.Implicits.global

  private val log = LoggerFactory.getLogger(this.getClass)

  def apply(
      sharding: ClusterSharding,
      consumerSettings: BetResultConsumerSettings,
      producer: SendProducer[String, Array[Byte]],
      transactionalId: String) = {

    val drainingControl: Consumer.DrainingControl[_] =
      Transactional
        .source(
          consumerSettings.kafkaConsumerSettings(),
          Subscriptions.topics(consumerSettings.topic)
        ) //we don't need committer settings
        .map { msg: TransactionalMessage[String, Array[Byte]] =>
          ProducerMessage.single(
            new ProducerRecord[String, Array[Byte]](
              consumerSettings.topic,
              msg.record.key,
              msg.record.value),
            msg.partitionOffset)
          val betProto =
            example.bet.grpc.Bet.parseFrom(msg.record.value)
          sendEvent(msg.record.key, msg.record.value, producer)
          Bet.Settle(betProto.result, replyTo)
        }
        .toMat(Transactional.sink(producer.settings, transactionalId))(
          Consumer.DrainingControl.apply)
        .run()

    StdIn.readLine("Consumer started \n Press ENTER to stop")
    val future = drainingControl.drainAndShutdown
    future.onComplete(_ => system.terminate)

  }

  private def sendEvent(key: String, serializedEvent: Array[Byte], producer: SendProducer[String, Array[Byte]]): Future[Done] = {
    val topic = s"consumed"

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
  }
}
