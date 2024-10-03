package projection.to.kafka

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try
import akka.Done
import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.{ActorSystem => TypedActorSystem}
import akka.kafka.scaladsl.Committer
import akka.kafka.scaladsl.Consumer
import akka.kafka.CommitterSettings
import akka.kafka.Subscriptions
import akka.kafka.cluster.sharding.KafkaClusterSharding
import akka.pattern.retry
import org.slf4j.LoggerFactory
import example.betting.Bet

object BetKafkaProcessor {

  sealed trait Command

  private case class KafkaConsumerStopped(reason: Try[Any])
      extends Command

  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply(
      shardRegion: ActorRef[Bet.Command],
      processorSettings: BetKafkaProcessorSettings)
      : Behavior[Nothing] = {
    Behaviors
      .setup[Command] { ctx =>
        implicit val sys: TypedActorSystem[_] = ctx.system
        val result =
          startConsumingFromTopic(shardRegion, processorSettings)

        ctx.pipeToSelf(result) { result =>
          KafkaConsumerStopped(result)
        }

        Behaviors.receiveMessage[Command] {
          case KafkaConsumerStopped(reason) =>
            ctx.log.info("Consumer stopped {}", reason)
            Behaviors.stopped
        }
      }
      .narrow
  }

  private def startConsumingFromTopic(
      shardRegion: ActorRef[Bet.Command],
      processorSettings: BetKafkaProcessorSettings)(
      implicit actorSystem: TypedActorSystem[_]): Future[Done] = {

    implicit val ec: ExecutionContext = actorSystem.executionContext
    implicit val scheduler: Scheduler =
      actorSystem.toClassic.scheduler
    val classic = actorSystem.toClassic

    val rebalanceListener = KafkaClusterSharding(classic)
      .rebalanceListener(processorSettings.entityTypeKey)

    val subscription = Subscriptions
      .topics(processorSettings.topics: _*)
      .withRebalanceListener(rebalanceListener.toClassic)

    Consumer
      .sourceWithOffsetContext(
        processorSettings.kafkaConsumerSettings(),
        subscription)
      // MapAsync and Retries can be replaced by reliable delivery
      .mapAsync(20) { record =>
        logger.info(
          s"user id consumed kafka partition ${record.key()}->${record.partition()}")
        retry(
          () =>
            shardRegion.ask[Bet.Response](replyTo => {
              val betProto = example.bet.grpc.Bet.parseFrom(record.value())
              Bet.Settle(betProto.result, replyTo)
            })(processorSettings.askTimeout, actorSystem.scheduler),
          attempts = 5,
          delay = 1.second)
      }
      .runWith(Committer.sinkWithOffsetContext(
        CommitterSettings(classic)))
  }
}
