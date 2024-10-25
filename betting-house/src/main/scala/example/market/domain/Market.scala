package example.betting

import akka.actor.typed.scaladsl.{
  ActorContext,
  Behaviors,
  TimerScheduler
}
import akka.actor.typed.{ ActorRef, Behavior, SupervisorStrategy }
import akka.cluster.sharding.typed.scaladsl.{
  ClusterSharding,
  EntityTypeKey
}
import akka.persistence.typed.scaladsl.{
  Effect,
  EventSourcedBehavior,
  ReplyEffect,
  RetentionCriteria
}
import akka.persistence.typed.PersistenceId
import akka.util.Timeout
import example.betting.Bet.{
  oddsDoMatch,
  Close,
  Command,
  Fail,
  Failed,
  FailedState,
  HandleValidationsPassed,
  MarketStatusAvailable
}
import example.betting.Market.{
  ClosedState,
  Command,
  CreateResultsConsumer,
  State
}
import projection.to.kafka.BetResultKafkaService

import scala.concurrent.duration._
import java.time.{ OffsetDateTime, ZoneId }
import scala.concurrent.Future
import scala.util.{ Failure, Success }

/**
 *
 */
object Market {

  val typeKey = EntityTypeKey[Command]("market")
  implicit val timeout: Timeout = 6.seconds

  final case class Fixture(
      id: String,
      homeTeam: String,
      awayTeam: String)
      extends CborSerializable
  final case class Odds(
      winHome: Double,
      winAway: Double,
      draw: Double)
      extends CborSerializable

  sealed trait Command extends CborSerializable {
    def replyTo: ActorRef[Response]
  }
  final case class Open(
      fixture: Fixture,
      odds: Odds,
      opensAt: Long,
      replyTo: ActorRef[Response])
      extends Command

  final case class Update(
      odds: Option[Odds],
      opensAt: Option[Long],
      replyTo: ActorRef[Response])
      extends Command

  final case class CreateResultsConsumer(
      result: Int, //0 =  winHome, 1 = winAway, 2 = draw
      replyTo: ActorRef[Response])
      extends Command

  private final case class ConsumerCreationSuccess(
      replyTo: ActorRef[Response])
      extends Command

  private final case class MarketCloseTimeout(
      seconds: Int,
      replyTo: ActorRef[Response])
      extends Command

  private final case class ConsumerCreationFailure(
      reason: String,
      replyTo: ActorRef[Response])
      extends Command

  final case class AllMessagesConsumed(replyTo: ActorRef[Response])
      extends Command

  private final case class AllMessagesConsumedTimeout(
      seconds: Int,
      replyTo: ActorRef[Response])
      extends Command

  final case class Close(
      result: Int, //0 =  winHome, 1 = winAway, 2 = draw
      replyTo: ActorRef[Response])
      extends Command

  final case class Cancel(reason: String, replyTo: ActorRef[Response])
      extends Command
  final case class GetState(replyTo: ActorRef[Response])
      extends Command

  sealed trait Response extends CborSerializable
  final case object Accepted extends Response
  final case class CurrentState(status: Status) extends Response
  final case class RequestUnaccepted(reason: String) extends Response

  sealed trait State extends CborSerializable {
    def status: Status;
  }
  final case class Status(
      marketId: String,
      fixture: Fixture,
      odds: Odds,
      result: Int,
      open: Boolean,
      opensAt: Long)
      extends CborSerializable
  object Status {
    def empty(marketId: String) =
      Status(
        marketId,
        Fixture("", "", ""),
        Odds(-1, -1, -1),
        0,
        true,
        0)
  }
  final case class UninitializedState(status: Status) extends State
  final case class OpenState(status: Status) extends State
  final case class ClosedState(status: Status) extends State

  final case class CancelledState(status: Status) extends State
  final case class FailedState(status: Status, reason: String)
      extends State

  def apply(marketId: String): Behavior[Command] =
    Behaviors.withTimers { timers =>
      Behaviors
        .setup[Command] { context =>
          val sharding = ClusterSharding(context.system)
          EventSourcedBehavior[Command, Event, State](
            PersistenceId(typeKey.name, marketId),
            UninitializedState(Status.empty(marketId)),
            commandHandler = (state, command) =>
              handleCommands(
                state,
                command,
                sharding,
                context,
                timers),
            eventHandler = handleEvents)
            .withTagger {
              case _ => Set(calculateTag(marketId, tags))
            }
            .withRetention(
              RetentionCriteria
                .snapshotEvery(
                  numberOfEvents = 100,
                  keepNSnapshots = 2))
            .onPersistFailure(
              SupervisorStrategy.restartWithBackoff(
                minBackoff = 10.seconds,
                maxBackoff = 60.seconds,
                randomFactor = 0.1))
        }
    }

  private def handleCommands(
      state: State,
      command: Command,
      sharding: ClusterSharding,
      context: ActorContext[Command],
      timer: TimerScheduler[Command]): ReplyEffect[Event, State] =
    (state, command) match {
      case (state: UninitializedState, command: Open) =>
        open(state, command)
      case (state: OpenState, command: Update) =>
        update(state, command)
      case (state: OpenState, command: Close) =>
        close(state, command, context, sharding, timer)
      case (state: ClosedState, command: CreateResultsConsumer) =>
        createResultsConsumer(state, command)
      case (state: ClosedState, command: ConsumerCreationSuccess) =>
        consumerCreationSuccess(state, command)
      case (state: ClosedState, command: AllMessagesConsumed) =>
        allMessagesConsumed(state, command)
      case (state: ClosedState, command: MarketCloseTimeout) =>
        marketCloseTimeout(state, command)
      case (state: ClosedState, command: ConsumerCreationFailure) =>
        marketFailure(state, command.reason, command)
      case (_, command: Cancel)   => cancel(state, command)
      case (_, command: GetState) => tell(state, command)
      case _                      => invalid(state, command)
    }

  sealed trait Event extends CborSerializable {
    def marketId: String
  }
  final case class Opened(
      marketId: String,
      fixture: Fixture,
      odds: Odds,
      opensAt: Long)
      extends Event
  final case class Updated(
      marketId: String,
      odds: Option[Odds],
      opensAt: Option[Long])
      extends Event

  final case class Closed(
      marketId: String,
      result: Int,
      at: OffsetDateTime)
      extends Event
  final case class Cancelled(marketId: String, reason: String)
      extends Event

  final case class Failed(marketId: String, reason: String)
      extends Event

  final case class ConsumerCreated(marketId: String) extends Event

  private def handleEvents(state: State, event: Event): State = {
    (state, event) match {
      case (_, Opened(marketId, fixture, odds, opensAt)) =>
        OpenState(Status(marketId, fixture, odds, 0, true, opensAt))
      case (state: OpenState, Updated(_, odds, opensAt)) =>
        state.copy(status = Status(
          state.status.marketId,
          state.status.fixture,
          odds.getOrElse(state.status.odds),
          state.status.result,
          state.status.open,
          opensAt.getOrElse(state.status.opensAt)))
      case (state: OpenState, Closed(_, result, _)) =>
        ClosedState(state.status.copy(result = result, open = false))
      case (state: ClosedState, ConsumerCreated(_)) =>
        state
      case (_, Cancelled(_, _)) =>
        CancelledState(state.status)
      case (_, Failed(_, reason)) =>
        FailedState(state.status, reason)
    }
  }

  private def open(
      state: State,
      command: Open): ReplyEffect[Opened, State] = {
    val opened =
      Opened(
        state.status.marketId,
        command.fixture,
        command.odds,
        command.opensAt)
    Effect
      .persist(opened)
      .thenReply(command.replyTo)(_ => Accepted)
  }

  private def update(
      state: State,
      command: Update): ReplyEffect[Updated, State] = {
    val updated =
      Updated(state.status.marketId, command.odds, command.opensAt)
    Effect
      .persist(updated)
      .thenReply(command.replyTo)(_ => Accepted)
  }

  private def close(
      state: State,
      command: Close,
      context: ActorContext[Command],
      sharding: ClusterSharding,
      timer: TimerScheduler[Command]): ReplyEffect[Closed, State] = {
    //TODO: find out why timer doesn't work
    /*timer.startSingleTimer(
      "lifespan",
      MarketCloseTimeout(
        10,
        command.replyTo
      ), // this would read from configuration
      10.seconds)*/
    val closed = Closed(
      state.status.marketId,
      command.result,
      OffsetDateTime.now(ZoneId.of("UTC")))
    Effect
      .persist(closed)
      .thenRun((_: State) => {
        def auxCreateResultsConsumer(result: Int)(
            replyTo: ActorRef[Market.Response])
            : CreateResultsConsumer =
          CreateResultsConsumer(result, replyTo)
        val marketRef =
          sharding.entityRefFor(Market.typeKey, state.status.marketId)

        context
          .ask(marketRef, auxCreateResultsConsumer(command.result)) {
            case Success(_) =>
              val message =
                s"Consumer creation success. Market [${state.status.marketId}]"
              context.log.error(message)
              ConsumerCreationSuccess(command.replyTo)
            case Failure(ex) =>
              val message =
                s"Consumer creation failure. Reason [${ex.getMessage}]"
              context.log.error(message)
              ConsumerCreationFailure(message, command.replyTo)
          }
      })
      .thenReply(command.replyTo)(_ => Accepted)
  }

  private def consumerCreationSuccess(
      state: ClosedState,
      command: ConsumerCreationSuccess): ReplyEffect[Event, State] = {
    Effect
      .persist(ConsumerCreated(state.status.marketId))
      .thenReply(command.replyTo)(_ => Accepted)
  }

  private def marketCloseTimeout(
      state: ClosedState,
      command: MarketCloseTimeout): ReplyEffect[Event, State] = {
    marketFailure(state, s"market close timeout[${state}]", command)
  }

  private def marketFailure(
      state: ClosedState,
      reason: String,
      command: Command): ReplyEffect[Event, State] = {
    Effect
      .persist(Failed(state.status.marketId, reason))
      .thenReply(command.replyTo)(_ => Accepted)
  }

  private def createResultsConsumer(
      state: ClosedState,
      command: CreateResultsConsumer): ReplyEffect[Event, State] = {
    Effect.none
      .thenRun((_: State) =>
        BetResultKafkaService
          .createConsumer(state.status.marketId, command.result))
      .thenReply(command.replyTo)(_ => Accepted)
  }

  def requestAllMessagesConsumed(
      marketId: String,
      sharding: ClusterSharding): Future[Market.Response] = {
    val marketRef = sharding.entityRefFor(Market.typeKey, marketId)
    marketRef.ask(Market.AllMessagesConsumed).mapTo[Market.Response]
  }

  private def allMessagesConsumed(
      state: ClosedState,
      command: AllMessagesConsumed): ReplyEffect[Closed, State] = {
    Effect.none
      .thenRun((_: State) =>
        BetResultKafkaService.deleteTopic(state.status.marketId))
      .thenReply(command.replyTo)(_ => Accepted)
  }

  private def cancel(
      state: State,
      command: Cancel): ReplyEffect[Cancelled, State] = {
    val cancelled = Cancelled(state.status.marketId, command.reason)
    Effect
      .persist(cancelled)
      .thenReply(command.replyTo)((_: State) => Accepted)
  }

  private def tell(
      state: State,
      command: GetState): ReplyEffect[Event, State] = {
    Effect.none.thenReply(command.replyTo)(_ =>
      CurrentState(state.status))
  }

  private def invalid(
      state: State,
      command: Command): ReplyEffect[Event, State] = {
    Effect.none.thenReply(command.replyTo)(
      _ =>
        RequestUnaccepted(
          s"[$command] is not allowed upon state [$state]"))
  }

  //TODO read 3 from properties
  val tags = Vector.tabulate(3)(i => s"market-tag-$i")

  private def calculateTag(
      entityId: String,
      tags: Vector[String] = tags): String = {
    val tagIndex =
      math.abs(entityId.hashCode % tags.size)
    tags(tagIndex)
  }

}
