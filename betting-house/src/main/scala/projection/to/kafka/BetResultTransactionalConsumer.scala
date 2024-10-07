package projection.to.kafka

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.kafka.scaladsl.Consumer.{ Control, DrainingControl }
import akka.kafka.scaladsl.{ Consumer, SendProducer, Transactional }
import akka.kafka.{ ProducerMessage, Subscriptions }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Flow, Keep, RestartSource, Sink }
import example.bet.grpc.BetService
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import projection.to.kafka.BetResultConsumer.log

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

object BetResultTransactionalConsumer {

  val log = LoggerFactory.getLogger(this.getClass)

  def init(
      implicit consumerSettings: BetResultConsumerSettings,
      producer: SendProducer[String, Array[Byte]],
      adminClient: AdminClient,
      transactionalId: String,
      betService: BetService,
      system: ActorSystem[Nothing],
      ec: ExecutionContext) = {
    // #transactionalFailureRetry
    var innerControl: Control = null

    val stream = RestartSource.onFailuresWithBackoff(
      minBackoff = 1.seconds,
      maxBackoff = 30.seconds,
      randomFactor = 0.2) { () =>
      Transactional
        .source(
          consumerSettings.kafkaConsumerSettings(),
          Subscriptions.topics(consumerSettings.topic))
        .via(business)
        .map { msg =>
          log.warn(s"Got message - ${msg.record.value()}")
        /*ProducerMessage.Message(
            new ProducerRecord[String, Array[Byte]](
              "sink-topic",
              msg.record.value),
            msg.partitionOffset)*/
        }
        .mapMaterializedValue(innerControl = _)
    }

    stream.runWith(Sink.ignore)

    // Add shutdown hook to respond to SIGTERM and gracefully shutdown stream
    /*sys.ShutdownHookThread {
      Await.result(innerControl.shutdown(), 10.seconds)
    }
    // #transactionalFailureRetry */

    terminateWhenDone(
      system,
      ec,
      adminClient,
      consumerSettings,
      innerControl.shutdown())
  }

  def business[T] = Flow[T]

  def terminateWhenDone(
      implicit system: ActorSystem[Nothing],
      ec: ExecutionContext,
      adminClient: AdminClient,
      consumerSettings: BetResultConsumerSettings,
      result: Future[Done]): Unit =
    result.onComplete {
      case Failure(e) =>
        log.error(e.getMessage)
        adminClient.deleteTopics(
          java.util.Arrays.asList(consumerSettings.topic))
      case Success(_) =>
        log.warn("consumed all messages")
        adminClient.deleteTopics(
          java.util.Arrays.asList(consumerSettings.topic))
    }
}
