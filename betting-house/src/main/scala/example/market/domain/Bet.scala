package example.betting

import akka.Done
import akka.actor.typed.{ ActorRef, Behavior, SupervisorStrategy }
import akka.actor.typed.scaladsl.{
  ActorContext,
  Behaviors,
  TimerScheduler
}
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

import scala.concurrent.duration._
import scala.util.{ Failure, Success }
import akka.util.Timeout
import org.slf4j.LoggerFactory
import projection.to.kafka.BetResultKafkaService

import scala.concurrent.{ ExecutionContext, Future }

object Bet {

  val logger = LoggerFactory.getLogger(Bet.getClass())

  val typeKey = EntityTypeKey[Command]("bet")

  implicit val timeout: Timeout = 6.seconds

  implicit val executionContext: ExecutionContext =
    ExecutionContext.global

  sealed trait Command extends CborSerializable
  trait ReplyCommand extends Command {
    def replyTo: ActorRef[Response]
  }
  final case class Open(
      walletId: String,
      marketId: String,
      marketName: String,
      odds: Double,
      stake: Int,
      result: Int, //0 winHome, 1 winAway, 2 draw
      replyTo: ActorRef[Response])
      extends ReplyCommand
  //probably want a local class not to depend on Market? see Market.State below
  final case class Settle(result: Int, replyTo: ActorRef[Response])
      extends ReplyCommand
  final case class Cancel(reason: String, replyTo: ActorRef[Response])
      extends ReplyCommand
  final case class GetState(replyTo: ActorRef[Response])
      extends ReplyCommand

  private final case class ValidationsPassedRequest(
      replyTo: ActorRef[Response])
      extends ReplyCommand

  private final case class ValidationsPassedResponse(betId: String)
      extends Command

  private final case class RetryFundReservationRequest(seconds: Int)
      extends Command

  private final case class RetryMarketConfirmationRequest(
      seconds: Int)
      extends Command

  private final case class MarketStatusAvailable(
      open: Boolean,
      oddsAvailable: Boolean,
      marketOdds: Option[Double])
      extends Command
  private final case class WalletFundsReservationResponded(
      response: Wallet.UpdatedResponse)
      extends Command

  private final case class WalletRefundGranted(
      reason: String,
      response: Wallet.UpdatedResponse)
      extends Command

  private final case class ValidationsTimedOut(seconds: Int)
      extends Command
  private final case class Fail(reason: String) extends Command
  private final case class Close(reason: String) extends Command

  sealed trait Response extends CborSerializable
  final case object Accepted extends Response
  final case class RequestUnaccepted(reason: String) extends Response
  final case class CurrentState(state: State) extends Response

  //how do I know I bet to the winner or the looser or draw??
  final case class Status(
      betId: String,
      walletId: String,
      marketId: String,
      marketName: String,
      odds: Double,
      stake: Int,
      result: Int,
      marketConfirmed: Boolean,
      fundsConfirmed: Boolean)
      extends CborSerializable
  object Status {
    def empty(betId: String) =
      Status(
        betId,
        "uninitialized",
        "uninitialized",
        "uninitialized",
        -1,
        -1,
        0,
        false,
        false)
  }

  sealed trait StatusState extends CborSerializable {
    def status: Status
  }

  sealed class State(override val status: Status) extends StatusState

  final case class UninitializedState(override val status: Status)
      extends State(status)
  final case class OpenState(
      override val status: Status,
      fundReservationRetryCount: Int = 1,
      fundReservationMaxRetries: Int = 10,
      marketConfirmationRetryCount: Int = 1,
      marketConfirmationMaxRetries: Int = 10)
      extends State(
        status
      ) // the ask user when market no longer available

  final case class ValidationsPassedState(override val status: Status)
      extends State(status)
  final case class BetSettledState(override val status: Status)
      extends State(status)
  final case class CancelledState(override val status: Status)
      extends State(status)
  final case class FailedState(
      override val status: Status,
      reason: String)
      extends State(status)

  final case class ClosedState(override val status: Status)
      extends State(status)

  def apply(betId: String): Behavior[Command] = {
    Behaviors.withTimers { timers =>
      Behaviors
        .supervise(
          Behaviors
            .setup[Command] { context =>
              val sharding = ClusterSharding(context.system)
              EventSourcedBehavior[Command, Event, State](
                PersistenceId(typeKey.name, betId),
                UninitializedState(Status.empty(betId)),
                commandHandler = (state, command) =>
                  handleCommands(
                    state,
                    command,
                    sharding,
                    context,
                    timers),
                eventHandler = handleEvents)
                .withTagger {
                  case _ => Set(calculateTag(betId, tags))
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
            })
        .onFailure[IllegalStateException](
          SupervisorStrategy
            .restartWithBackoff(
              minBackoff = 1.second,
              maxBackoff = 10.seconds,
              randomFactor = 0.1)
            .withMaxRestarts(10))
    }
  }

  def handleCommands(
      state: State,
      command: Command,
      sharding: ClusterSharding,
      context: ActorContext[Command],
      timer: TimerScheduler[Command]): Effect[Event, State] = {
    (state, command) match {
      case (state: UninitializedState, command: Open) =>
        open(state, command, sharding, context, timer)
      case (state: OpenState, command: MarketStatusAvailable) =>
        validateMarket(state, command, sharding, context, timer)
      case (
          state: OpenState,
          command: WalletFundsReservationResponded) =>
        validateFunds(state, command, sharding, context, timer)
      case (
          state: FailedState,
          command: WalletFundsReservationResponded) =>
        requestWalletRefund(state, command, sharding, context)
      case (state: OpenState, command: ValidationsTimedOut) =>
        checkValidationsAfterTimeout(state, sharding, context)
      case (state: OpenState, command: RetryFundReservationRequest) =>
        retryFundReservation(state, command, sharding, context)
      case (
          state: OpenState,
          command: RetryMarketConfirmationRequest) =>
        retryMarketConfirmation(state, command, sharding, context)
      case (
          state: ValidationsPassedState,
          command: ValidationsPassedRequest) =>
        validationsPassed(state, command)
      case (state: ValidationsPassedState, command: Settle) =>
        settle(state, command, sharding, context)
      case (state: BetSettledState, command: Close) =>
        finish(state, command)
      case (state: State, command: GetState) =>
        getState(state, command.replyTo)
      case (_, command: ValidationsTimedOut)       => Effect.none
      case (_, command: ValidationsPassedResponse) => Effect.none
      case (_, command: WalletRefundGranted)       => Effect.none
      case (_, command: Cancel)                    => cancel(state, command)
      case (_, command: ReplyCommand)              => reject(state, command)
      case (_, command: Fail)                      => fail(state, command)
      case _                                       => invalid(state, command, context)
    }
  }

  sealed trait Event extends CborSerializable {
    def betId: String
  }
  final case class MarketConfirmed(betId: String, state: OpenState)
      extends Event
  final case class FundsGranted(betId: String, state: OpenState)
      extends Event
  final case class ValidationsPassed(betId: String, state: OpenState)
      extends Event
  final case class BetSettled(
      betId: String,
      state: ValidationsPassedState)
      extends Event

  final case class Opened(
      betId: String,
      walletId: String,
      marketId: String,
      marketName: String,
      odds: Double,
      stake: Int,
      result: Int)
      extends Event
  final case class Cancelled(betId: String, reason: String)
      extends Event
  final case class MarketValidationFailed(
      betId: String,
      reason: String)
      extends Event

  final case class FundReservationDenied(
      betId: String,
      reason: String,
      state: OpenState)
      extends Event

  final case class MarketConfirmationDenied(
      betId: String,
      reason: String,
      state: OpenState)
      extends Event
  final case class Failed(betId: String, reason: String) extends Event
  final case class Closed(betId: String) extends Event

  def handleEvents(state: State, event: Event): State =
    event match {
      case Opened(
          betId,
          walletId,
          marketId,
          marketName,
          odds,
          stake,
          result) =>
        OpenState(
          Status(
            betId,
            walletId,
            marketId,
            marketName,
            odds,
            stake,
            result,
            false,
            false),
          1,
          10,
          1,
          10)
      case MarketConfirmed(betId, state) =>
        state.copy(status = state.status.copy(marketConfirmed = true))
      case FundsGranted(betId, state) =>
        state.copy(status = state.status.copy(fundsConfirmed = true))
      case FundReservationDenied(betId, reason, state) =>
        state.copy(fundReservationRetryCount =
          state.fundReservationRetryCount + 1)
      case MarketConfirmationDenied(betId, reason, state) =>
        state.copy(marketConfirmationRetryCount =
          state.marketConfirmationRetryCount + 1)
      case ValidationsPassed(betId, state) =>
        ValidationsPassedState(status = state.status
          .copy(marketConfirmed = true, fundsConfirmed = true))
      case BetSettled(betId, state) =>
        BetSettledState(state.status)
      case Closed(betId) =>
        ClosedState(state.status)
      case Cancelled(betId, reason) =>
        CancelledState(state.status)
      case MarketValidationFailed(_, reason) =>
        FailedState(state.status, reason)
      case Failed(_, reason) =>
        FailedState(state.status, reason)
    }

  private def open(
      state: UninitializedState,
      command: Open,
      sharding: ClusterSharding,
      context: ActorContext[Command],
      timer: TimerScheduler[Command]): ReplyEffect[Opened, State] = {
    timer.startSingleTimer(
      "lifespan",
      ValidationsTimedOut(180), // this would read from configuration
      180.seconds)
    val open = Opened(
      state.status.betId,
      command.walletId,
      command.marketId,
      command.marketName,
      command.odds,
      command.stake,
      command.result)
    Effect
      .persist(open)
      .thenRun(
        (_: State) =>
          requestMarketStatus(
            command.marketId,
            command.odds,
            command.result,
            sharding,
            context))
      .thenRun(
        (_: State) =>
          requestFundsReservation(
            command.stake,
            command.walletId,
            sharding,
            context))
      .thenReply(command.replyTo)(_ => Accepted)
  }

  private def validateMarket(
      state: OpenState,
      command: MarketStatusAvailable,
      sharding: ClusterSharding,
      context: ActorContext[Command],
      timer: TimerScheduler[Command]): Effect[Event, State] = {
    if (!command.open) {
      val message =
        s"market [${state.status.marketId}] is closed, no more bets allowed"
      context.log.error(message)
      marketValidationFailed(state, message, sharding, context)
    } else if (command.oddsAvailable) {
      if (state.status.fundsConfirmed) {
        requestValidationsPassed(state, sharding, context)
      } else {
        Effect.persist(MarketConfirmed(state.status.betId, state))
      }
    } else {
      if (state.marketConfirmationRetryCount <= state.marketConfirmationMaxRetries) {
        retryMarketConfirmation(state, timer)
      } else {
        val message =
          s"market odds [${command.marketOdds}] are less than the bet odds"
        context.log.error(message)
        marketValidationFailed(state, message, sharding, context)
      }
    }
  }

  private def validateFunds(
      state: OpenState,
      command: WalletFundsReservationResponded,
      sharding: ClusterSharding,
      context: ActorContext[Command],
      timer: TimerScheduler[Command]): Effect[Event, State] = {
    command.response match {
      case Wallet.Accepted =>
        if (state.status.marketConfirmed) {
          requestValidationsPassed(state, sharding, context)
        } else {
          Effect.persist(FundsGranted(state.status.betId, state))
        }
      case Wallet.Rejected =>
        if (state.fundReservationRetryCount <= state.fundReservationMaxRetries) {
          retryFundReservation(state, timer)
        } else {
          fundReservationFailed(
            state,
            s"fund reservation failed [${state}]")
        }
    }
  }

  private def retryFundReservation(
      state: OpenState,
      timer: TimerScheduler[Command]): Effect[Event, State] = {
    timer.startSingleTimer(
      s"retry-fund-reservation",
      RetryFundReservationRequest(3),
      3.seconds)
    Effect
      .persist(
        FundReservationDenied(
          state.status.betId,
          "funds not available",
          state))
  }

  private def retryFundReservation(
      state: OpenState,
      command: RetryFundReservationRequest,
      sharding: ClusterSharding,
      context: ActorContext[Command]): Effect[Event, State] = {
    Effect.none
      .thenRun((_: State) => {
        requestFundsReservation(
          state.status.stake,
          state.status.walletId,
          sharding,
          context)
      })
  }

  private def retryMarketConfirmation(
      state: OpenState,
      timer: TimerScheduler[Command]): Effect[Event, State] = {
    timer.startSingleTimer(
      s"retry-market-confirmation",
      RetryMarketConfirmationRequest(3),
      3.seconds)
    Effect
      .persist(
        MarketConfirmationDenied(
          state.status.betId,
          "market confirmation denied",
          state))
  }

  private def retryMarketConfirmation(
      state: OpenState,
      command: RetryMarketConfirmationRequest,
      sharding: ClusterSharding,
      context: ActorContext[Command]): Effect[Event, State] = {
    Effect.none
      .thenRun((_: State) => {
        requestMarketStatus(
          state.status.marketId,
          state.status.odds,
          state.status.result,
          sharding,
          context)
      })
  }

  //market changes very fast even if our system haven't register the
  //change we need to take this decision quickly. If the Market is not available
  // we fail fast.
  private def requestMarketStatus(
      marketId: String,
      odds: Double,
      result: Int,
      sharding: ClusterSharding,
      context: ActorContext[Command]): Unit = {

    val marketRef =
      sharding.entityRefFor(Market.typeKey, marketId)

    implicit val timeout
        : Timeout = Timeout(6, SECONDS) //TODO read from properties
    context.ask(marketRef, Market.GetState) {
      case Success(Market.CurrentState(marketState)) =>
        val matched = oddsDoMatch(marketState, odds, result)
        MarketStatusAvailable(
          marketState.open,
          matched.doMatch,
          Option(matched.marketOdds))
      case Failure(ex) =>
        context.log.error(ex.getMessage())
        MarketStatusAvailable(false, false, None)
    }
  }

  //if I already have asks why do I need a global time out?
  // you use that global time out and then indirectly let the Wallet grant the Bet otherwise will be cancelled.
  // you can tell to funds in case the bet might need thirds party calls or
  // the wallet might need to do so. In general multiple asks chained are a bad practice.

  private def requestFundsReservation(
      stake: Int,
      walletId: String,
      sharding: ClusterSharding,
      context: ActorContext[Command]): Unit = {
    val walletRef =
      sharding.entityRefFor(Wallet.typeKey, walletId)
    val walletResponseMapper: ActorRef[Wallet.UpdatedResponse] =
      context.messageAdapter(rsp =>
        WalletFundsReservationResponded(rsp))

    walletRef ! Wallet.ReserveFunds(stake, walletResponseMapper)
  }

  def requestBetSettlement(
      betId: String,
      result: Int,
      sharding: ClusterSharding): Future[Bet.Response] = {
    def auxSettle(result: Int)(
        replyTo: ActorRef[Bet.Response]): Bet.Settle =
      Bet.Settle(result, replyTo)
    val betRef = sharding.entityRefFor(Bet.typeKey, betId)
    betRef.ask(auxSettle(result)).mapTo[Bet.Response]
  }

  private def marketValidationFailed(
      state: OpenState,
      reason: String,
      sharding: ClusterSharding,
      context: ActorContext[Command]): Effect[Event, State] = {
    Effect
      .persist(MarketValidationFailed(state.status.betId, reason))
      .thenRun((_: State) =>
        checkRequestWalletRefund(state, sharding, context))
  }

  private def fundReservationFailed(
      state: OpenState,
      reason: String): Effect[Event, State] = {
    Effect
      .persist(Failed(state.status.betId, reason))
  }

  private def checkRequestWalletRefund(
      state: OpenState,
      sharding: ClusterSharding,
      context: ActorContext[Command]): Effect[Event, State] = {
    (state.status.fundsConfirmed) match {
      case (true) =>
        requestWalletRefund(state, sharding, context)
      case (_) =>
        Effect.none
    }
  }

  private def requestWalletRefund(
      state: State,
      command: WalletFundsReservationResponded,
      sharding: ClusterSharding,
      context: ActorContext[Command]): Effect[Event, State] = {
    command.response match {
      case Wallet.Accepted =>
        requestWalletRefund(state, sharding, context)
      case Wallet.Rejected =>
        Effect.none
    }
    Effect.none
  }

  private def requestWalletRefund(
      state: State,
      sharding: ClusterSharding,
      context: ActorContext[Command]): Effect[Event, State] = {
    val reason =
      s"market validations didn't pass and refund should be issued [${state}]"
    val walletRef =
      sharding.entityRefFor(Wallet.typeKey, state.status.walletId)
    val walletResponseMapper: ActorRef[Wallet.UpdatedResponse] =
      context.messageAdapter(rsp => WalletRefundGranted(reason, rsp))

    walletRef ! Wallet.AddFunds(
      state.status.stake,
      walletResponseMapper)

    Effect.none
  }

  private def checkValidationsAfterTimeout(
      state: OpenState,
      sharding: ClusterSharding,
      context: ActorContext[Command]): Effect[Event, State] = {
    (state.status.marketConfirmed, state.status.fundsConfirmed) match {
      case (true, true) =>
        requestValidationsPassed(state, sharding, context)
      case (_, true) =>
        marketValidationFailed(
          state,
          s"market validation failed (timeout) [${state}]",
          sharding,
          context)
      case (true, _) =>
        fundReservationFailed(
          state,
          s"fund reservation failed (timeout) [${state}]")
      case _ =>
        Effect.persist(
          Failed(
            state.status.betId,
            s"validations didn't pass (timeout) [${state}]"))
    }
  }

  private def validationsPassed(
      state: ValidationsPassedState,
      command: ValidationsPassedRequest): Effect[Event, State] = {
    Effect.none
      .thenRun((_: State) => BetResultKafkaService.sendEvent(state))
      .thenReply(command.replyTo)(_ => Accepted)
  }

  private def requestValidationsPassed(
      state: OpenState,
      sharding: ClusterSharding,
      context: ActorContext[Command]): Effect[Event, State] = {
    Effect
      .persist(ValidationsPassed(state.status.betId, state))
      .thenRun((_: State) => {
        val betRef =
          sharding.entityRefFor(Bet.typeKey, state.status.betId)
        context.ask(betRef, ValidationsPassedRequest) {
          case Success(_) =>
            ValidationsPassedResponse(state.status.betId)
          case Failure(ex) =>
            context.log.error(ex.getMessage())
            Fail(ex.getMessage())
        }
      })
  }

  private final case class Match(doMatch: Boolean, marketOdds: Double)

  private def oddsDoMatch(
      marketStatus: Market.Status,
      odds: Double,
      result: Int): Match = {
    // if better odds are available the betting house it takes the bet
    // for a lesser benefit to the betting customer. This is why compares
    // with gt
    logger.debug(
      s"checking marketStatus $marketStatus matches requested odds ${odds}")
    result match {
      case 0 =>
        Match(
          marketStatus.odds.winHome >= odds,
          marketStatus.odds.winHome)
      case 1 =>
        Match(
          marketStatus.odds.winAway >= odds,
          marketStatus.odds.winAway)
      case 2 =>
        Match(marketStatus.odds.draw >= odds, marketStatus.odds.draw)
    }
  }

  private def isWinner(
      state: State,
      resultFromMarket: Int): Boolean = {
    state.status.result == resultFromMarket
  }

  //one way to avoid adding funds twice is asking
  private def settle(
      state: ValidationsPassedState,
      command: Settle,
      sharding: ClusterSharding,
      context: ActorContext[Command]): Effect[Event, State] = {

    Effect
      .persist(BetSettled(state.status.betId, state))
      .thenRun((_: State) => {
        implicit val timeout = Timeout(10, SECONDS)
        def auxCreateRequest(stake: Int)(
            replyTo: ActorRef[Wallet.Response]): Wallet.AddFunds =
          Wallet.AddFunds(stake, replyTo)

        if (isWinner(state, command.result)) {
          val walletRef =
            sharding
              .entityRefFor(Wallet.typeKey, state.status.walletId)
          context
            .ask(walletRef, auxCreateRequest(state.status.stake)) {
              case Success(_) =>
                Close(s"stake reimbursed to wallet [$walletRef]")
              case Failure(ex) => //I rather retry
                val message =
                  s"stake NOT reimbursed to wallet [$walletRef]. Reason [${ex.getMessage}]"
                context.log.error(message)
                Fail(message)
            }
        } else {
          val message =
            s"stake NOT reimbursed because bet result [${state.status.result}] is not equal to market result [${command.result}]"
          context.log.debug(message)
        }

      })
      .thenReply(command.replyTo)(_ => Accepted)
  }

  private def fail(
      state: State,
      command: Command): Effect[Event, State] = //FIXME
    Effect.persist(Failed(
      state.status.betId,
      s"Reimbursment unsuccessfull. For wallet [${state.status.walletId}]"))

  private def finish(
      state: State,
      command: Close): Effect[Event, State] =
    Effect.persist(Closed(state.status.betId))

  private def cancel(
      state: State,
      command: Command): Effect[Event, State] = {
    command match {
      case ValidationsTimedOut(time) =>
        Effect.persist(Cancelled(
          state.status.betId,
          s"validation in process when life span expired after [$time] seconds"))
    }
  }

  private def getState(
      state: State,
      replyTo: ActorRef[Response]): Effect[Event, State] = {
    Effect.none.thenReply(replyTo)(_ => CurrentState(state))
  }

  private def reject(
      state: State,
      command: ReplyCommand): Effect[Event, State] = {
    Effect.none.thenReply(command.replyTo)(_ =>
      RequestUnaccepted(
        s"[$command] has been rejected upon the current state [$state]"))
  }

  private def invalid(
      state: State,
      command: Command,
      context: ActorContext[Command]): Effect[Event, State] = {
    context.log.error(
      s"Invalid command [$command] in state [$state]  ")
    Effect.none
  }

  //TODO read 3 from properties
  val tags = Vector.tabulate(3)(i => s"bet-tag-$i")

  private def calculateTag(
      entityId: String,
      tags: Vector[String] = tags): String = {
    val tagIndex =
      math.abs(entityId.hashCode % tags.size)
    tags(tagIndex)
  }

}
