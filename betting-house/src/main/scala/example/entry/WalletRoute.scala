package example.betting

import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ Behavior, PostStop }
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding

import scala.io.StdIn
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import akka.util.Timeout
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.AskPattern.schedulerFromActorSystem
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.{
  HttpResponse,
  StatusCode,
  StatusCodes
}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import spray.json.{ JsonFormat, RootJsonFormat }
import akka.cluster.sharding.typed.scaladsl.{
  ClusterSharding,
  Entity
}
import akka.cluster.sharding.typed.ShardingEnvelope
import betting.house.projection.proto.{ SumStake, SumStakes }
import example.repository.scalike.{
  ScalikeJdbcSession,
  WalletRepository
}

class WalletService(
    implicit sharding: ClusterSharding,
    walletRepository: WalletRepository) {

  implicit val executionContext = ExecutionContext.global

  implicit val timeout: Timeout = 5.seconds

  // implicit val cargoFormat: RootJsonFormat[Wallet.UpdatedResponse] =
  // jsonFormat1(Wallet.UpdatedResponse)
  implicit val currentBalanceFormat
      : RootJsonFormat[Wallet.CurrentBalance] =
    jsonFormat1(Wallet.CurrentBalance)

  sharding.init(Entity(Wallet.typeKey)(entityContext =>
    Wallet(entityContext.entityId)))

  val route: Route =
    pathPrefix("wallet") {
      concat(
        path("add") {
          post {
            parameters(
              "walletId".as[String],
              "requestId".as[String],
              "funds".as[Int]) {
              (walletId, requestId, funds) =>

                val requestExists = ScalikeJdbcSession.withSession {
                  session =>
                    walletRepository
                      .walletRequestExists(requestId, session)
                }

                if (requestExists) {
                  complete(Future.successful(StatusCodes.Accepted))
                } else {
                  val wallet =
                    sharding.entityRefFor(Wallet.typeKey, walletId)
                  def auxAddFundRequest(
                      requestId: String,
                      funds: Int)(
                      replyTo: ActorRef[Wallet.UpdatedResponse]) =
                    Wallet.AddFundRequest(requestId, funds, replyTo)

                  val response =
                    wallet
                      .ask(auxAddFundRequest(requestId, funds))
                      .mapTo[Wallet.UpdatedResponse]
                      .map {
                        case Wallet.Accepted => StatusCodes.Accepted
                      }

                  complete(
                    response
                  ) //FIXME The request has been accepted for processing, but the processing has not been completed
                }

            }
          }
        },
        path("remove") {
          post {
            parameters(
              "walletId".as[String],
              "requestId".as[String],
              "funds".as[Int]) {
              (walletId, requestId, funds) =>

                val requestExists = ScalikeJdbcSession.withSession {
                  session =>
                    walletRepository
                      .walletRequestExists(requestId, session)
                }

                if (requestExists) {
                  complete(Future.successful(StatusCodes.Accepted))
                } else {

                  val wallet =
                    sharding.entityRefFor(Wallet.typeKey, walletId)

                  def auxReserveFunds(funds: Int)(
                      replyTo: ActorRef[Wallet.UpdatedResponse]) =
                    Wallet.ReserveFunds(funds, replyTo)

                  val response: Future[HttpResponse] =
                    wallet
                      .ask(auxReserveFunds(funds))
                      .mapTo[Wallet.UpdatedResponse]
                      .map {
                        case Wallet.Accepted =>
                          HttpResponse(StatusCodes.Accepted)
                        case Wallet.Rejected =>
                          HttpResponse(
                            StatusCodes.BadRequest,
                            entity = "not enough funds in the wallet")
                      }

                  complete(
                    response
                  ) //FIXME The request has been accepted for processing, but the processing has not been completed
                }
            }
          }
        },
        get {
          parameters("walletId".as[String]) {
            entityId => //FIXME avoid param, id is assumed
              val container =
                sharding.entityRefFor(Wallet.typeKey, entityId)

              val response: Future[Wallet.CurrentBalance] =
                container
                  .ask(Wallet.CheckFunds)
                  .mapTo[Wallet.CurrentBalance]

              complete(response)
          }
        })
    }

}
