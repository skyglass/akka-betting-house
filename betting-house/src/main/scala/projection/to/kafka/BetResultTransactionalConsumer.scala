package projection.to.kafka

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.kafka.scaladsl.Consumer.Control
import akka.kafka.scaladsl.{SendProducer, Transactional}
import akka.kafka.{ProducerMessage, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, RestartSource, Sink}
import example.bet.grpc.BetService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object BetResultTransactionalConsumer {

  val log = LoggerFactory.getLogger(this.getClass)

  def init(
      implicit consumerSettings: BetResultConsumerSettings,
      producer: SendProducer[String, Array[Byte]],
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
          ProducerMessage.Message(
            new ProducerRecord[String, Array[Byte]](
              "sink-topic",
              msg.record.value),
            msg.partitionOffset)
        }
        // side effect out the `Control` materialized value because it can't be propagated through the `RestartSource`
        .mapMaterializedValue(innerControl = _)
        .via(Transactional.flow(producer.settings, transactionalId))
    }

    stream.runWith(Sink.ignore)

    // Add shutdown hook to respond to SIGTERM and gracefully shutdown stream
    sys.ShutdownHookThread {
      Await.result(innerControl.shutdown(), 10.seconds)
    }
    // #transactionalFailureRetry

    terminateWhenDone(system, ec, innerControl.shutdown())
  }

  def business[T] = Flow[T]

  def terminateWhenDone(implicit system: ActorSystem[Nothing], ec: ExecutionContext, result: Future[Done]): Unit =
    result.onComplete {
      case Failure(e) =>
        system.log.error(e.getMessage)
        system.terminate()
      case Success(_) => system.terminate()
    }
}
