package example.bet.grpc

import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.cluster.sharding.typed.scaladsl.{
  ClusterSharding,
  Entity
}
import akka.util.Timeout

import java.time.{ Instant, OffsetDateTime, ZoneId }
import example.betting.Bet
import org.slf4j.LoggerFactory

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

class BetServiceImplSharding(sharding: ClusterSharding)
    extends BetService {

  implicit val timeout: Timeout = 6.seconds
  implicit val executionContext: ExecutionContext =
    ExecutionContext.global

  implicit val log = LoggerFactory.getLogger(this.getClass)

  sharding.init(
    Entity(Bet.typeKey)(entityContext => Bet(entityContext.entityId)))

  def cancel(in: example.bet.grpc.CancelMessage)
      : scala.concurrent.Future[example.bet.grpc.BetResponse] = {
    val bet = sharding.entityRefFor(Bet.typeKey, in.betId)

    def auxCancel(reason: String)(replyTo: ActorRef[Bet.Response]) =
      Bet.Cancel(reason, replyTo)

    bet.ask(auxCancel(in.reason)).mapTo[Bet.Response].map {
      response =>
        response match {
          case Bet.Accepted =>
            example.bet.grpc.BetResponse("initialized")
          case Bet.RequestUnaccepted(reason) =>
            example.bet.grpc
              .BetResponse(s"Bet NOT cancelled because [$reason]")
        }
    }
  }
  def open(in: example.bet.grpc.Bet)
      : scala.concurrent.Future[example.bet.grpc.BetResponse] = {
    val bet = sharding.entityRefFor(Bet.typeKey, in.betId)

    def auxOpen(
        walletId: String,
        marketId: String,
        marketName: String,
        odds: Double,
        stake: Int,
        result: Int)(replyTo: ActorRef[Bet.Response]) =
      Bet.Open(
        walletId,
        marketId,
        marketName,
        odds,
        stake,
        result,
        replyTo)

    bet
      .ask(
        auxOpen(
          in.walletId,
          in.marketId,
          in.marketName,
          in.odds,
          in.stake,
          in.result))
      .mapTo[Bet.Response]
      .map { response =>
        response match {
          case Bet.Accepted =>
            example.bet.grpc.BetResponse("initialized")
          case Bet.RequestUnaccepted(reason) =>
            val message =
              s"Bet [${in.betId}] NOT opened because [$reason]"
            log.error(message)
            example.bet.grpc
              .BetResponse(message)
        }
      }
  }

  def settle(in: example.bet.grpc.SettleMessage)
      : scala.concurrent.Future[example.bet.grpc.BetResponse] = {
    val bet = sharding.entityRefFor(Bet.typeKey, in.betId)

    def auxSettle(result: Int)(replyTo: ActorRef[Bet.Response]) =
      Bet.Settle(result, replyTo)

    bet.ask(auxSettle(in.result)).mapTo[Bet.Response].map {
      response =>
        response match {
          case Bet.Accepted =>
            example.bet.grpc.BetResponse("initialized")
          case Bet.RequestUnaccepted(reason) =>
            example.bet.grpc
              .BetResponse(s"Bet NOT settled because [$reason]")
        }
    }
  }

  def getState(in: example.bet.grpc.BetId)
      : scala.concurrent.Future[example.bet.grpc.Bet] = {
    val bet = sharding.entityRefFor(Bet.typeKey, in.betId)

    bet.ask(Bet.GetState).mapTo[Bet.Response].map { response =>
      response match {
        case Bet.CurrentState(state) =>
          val status = state.status
          example.bet.grpc.Bet(
            status.betId,
            status.walletId,
            status.marketId,
            status.marketName,
            status.odds,
            status.stake,
            status.result,
            status.marketConfirmed,
            status.fundsConfirmed)
      }
    }
  }

}
