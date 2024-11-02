package example.betting

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
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
import akka.projection.jdbc.JdbcSession
import example.repository.scalike.{
  ScalikeJdbcSession,
  WalletRepository,
  WalletRepositoryImpl
}
import scalikejdbc.DB

import scala.concurrent.duration._

object Wallet {

  val typeKey = EntityTypeKey[Command]("wallet")

  sealed trait Command extends CborSerializable
  final case class ReserveFunds(
      amount: Int,
      replyTo: ActorRef[UpdatedResponse])
      extends Command

  final case class ReserveFundsRequest(
      requestId: String,
      amount: Int,
      replyTo: ActorRef[UpdatedResponse])
      extends Command

  final case class AddFunds(
      amount: Int,
      replyTo: ActorRef[UpdatedResponse])
      extends Command

  final case class AddFundsRequest(
      requestId: String,
      amount: Int,
      replyTo: ActorRef[UpdatedResponse])
      extends Command

  final case class CheckFunds(replyTo: ActorRef[Response])
      extends Command

  sealed trait Event extends CborSerializable

  final case class FundsReservationRequested(
      requestId: String,
      amount: Int)
      extends Event

  final case class FundsAdditionRequested(
      requestId: String,
      amount: Int)
      extends Event

  final case class FundsReserved(amount: Int) extends Event

  final case class FundsAdded(amount: Int) extends Event
  final case class FundsReservationDenied(amount: Int) extends Event

  sealed trait Response extends CborSerializable
  trait UpdatedResponse extends Response
  final case object Accepted extends UpdatedResponse
  final case object Rejected extends UpdatedResponse
  final case class CurrentBalance(amount: Int) extends Response

  final case class State(balance: Int) extends CborSerializable

  def apply(walletId: String): Behavior[Command] =
    Behaviors
      .supervise(
        Behaviors
          .setup[Command] { context =>
            val sharding = ClusterSharding(context.system)
            val walletRepository = new WalletRepositoryImpl()
            EventSourcedBehavior[Command, Event, State](
              PersistenceId(typeKey.name, walletId),
              State(0),
              commandHandler = (state, command) =>
                handleCommands(
                  state,
                  command,
                  sharding,
                  context,
                  walletRepository),
              eventHandler = handleEvents)
              .withTagger {
                case _ => Set(calculateTag(walletId, tags))
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

  def handleCommands(
      state: State,
      command: Command,
      sharding: ClusterSharding,
      context: ActorContext[Command],
      walletRepository: WalletRepository)
      : ReplyEffect[Event, State] = {
    command match {
      case ReserveFunds(amount, replyTo) =>
        //it might need to check with an external service to
        // prove the customer is not betting such it can be considered addiction.
        if (amount <= state.balance)
          Effect
            .persist(FundsReserved(amount))
            .thenReply(replyTo)(state => Accepted)
        else {
          context.log.error(
            s"funds reservation denied exception: amount [{$amount}]")
          Effect
            .persist(FundsReservationDenied(amount))
            .thenReply(replyTo)(state => Rejected)
        }
      case ReserveFundsRequest(requestId, amount, replyTo) =>
        if (walletRequestExists(requestId, walletRepository)) {
          context.log.error(
            s"funds reservation rejected as duplicate request with the same id: [{$requestId}]")
          Effect.none
            .thenReply(replyTo)(state => Rejected)
        } else if (amount <= state.balance) {
          Effect
            .persist(FundsReservationRequested(requestId, amount))
            .thenRun((_: State) =>
              addWalletRequest(requestId, walletRepository))
            .thenReply(replyTo)(state => Accepted)
        } else {
          context.log.error(
            s"funds reservation denied exception: amount [{$amount}]")
          Effect
            .persist(FundsReservationDenied(amount))
            .thenReply(replyTo)(state => Rejected)
        }
      case AddFunds(amount, replyTo) =>
        Effect
          .persist(FundsAdded(amount))
          .thenReply(replyTo)(state => Accepted)
      case AddFundsRequest(requestId, amount, replyTo) =>
        if (walletRequestExists(requestId, walletRepository)) {
          context.log.error(
            s"add funds rejected as duplicate request with the same id: [{$requestId}]")
          Effect.none
            .thenReply(replyTo)(state => Rejected)
        } else {
          Effect
            .persist(FundsAdditionRequested(requestId, amount))
            .thenRun((_: State) =>
              addWalletRequest(requestId, walletRepository))
            .thenReply(replyTo)(state => Accepted)
        }
      case CheckFunds(replyTo) =>
        Effect.reply(replyTo)(CurrentBalance(state.balance))
    }
  }

  def handleEvents(state: State, event: Event): State = event match {
    case FundsReserved(amount) =>
      State(state.balance - amount)
    case FundsReservationRequested(requestId, amount) =>
      State(state.balance - amount)
    case FundsAdded(amount) =>
      State(state.balance + amount)
    case FundsAdditionRequested(requestId, amount) =>
      State(state.balance + amount)
    case FundsReservationDenied(_) =>
      state
  }

  val tags = Vector.tabulate(3)(i => s"wallet-tag-$i")

  private def calculateTag(
      entityId: String,
      tags: Vector[String] = tags): String = {
    val tagIndex =
      math.abs(entityId.hashCode % tags.size)
    tags(tagIndex)
  }

  private def walletRequestExists(
      requestId: String,
      walletRepository: WalletRepository): Boolean = {
    ScalikeJdbcSession.withSession { session =>
      walletRepository
        .walletRequestExists(requestId, session)
    }
  }

  private def addWalletRequest(
      requestId: String,
      walletRepository: WalletRepository): Unit = {
    ScalikeJdbcSession.withSession(session =>
      walletRepository.addWalletRequest(requestId, session))
  }
}
