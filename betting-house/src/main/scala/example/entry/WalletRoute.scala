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
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.scaladsl.Sink

class WalletService(
    implicit sharding: ClusterSharding,
    system: ActorSystem[_]) {

  implicit val executionContext = ExecutionContext.global

  implicit val timeout: Timeout = 5.seconds

  // implicit val cargoFormat: RootJsonFormat[Wallet.UpdatedResponse] =
  // jsonFormat1(Wallet.UpdatedResponse)
  implicit val currentBalanceFormat
      : RootJsonFormat[Wallet.CurrentBalance] = jsonFormat1(
    Wallet.CurrentBalance)
  implicit val walletDataFormat: RootJsonFormat[WalletData] =
    jsonFormat2(WalletData)
  implicit val walletListFormat: RootJsonFormat[WalletList] =
    jsonFormat1(WalletList)

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

                val wallet =
                  sharding.entityRefFor(Wallet.typeKey, walletId)
                def auxAddFundsRequest(requestId: String, funds: Int)(
                    replyTo: ActorRef[Wallet.UpdatedResponse]) =
                  Wallet.AddFundsRequest(requestId, funds, replyTo)

                val response =
                  wallet
                    .ask(auxAddFundsRequest(requestId, funds))
                    .mapTo[Wallet.UpdatedResponse]
                    .map {
                      case Wallet.Accepted => StatusCodes.Accepted
                    }

                complete(
                  response
                ) //FIXME The request has been accepted for processing, but the processing has not been completed
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

                val wallet =
                  sharding.entityRefFor(Wallet.typeKey, walletId)

                def auxReserveFundsRequest(
                    requestId: String,
                    funds: Int)(
                    replyTo: ActorRef[Wallet.UpdatedResponse]) =
                  Wallet
                    .ReserveFundsRequest(requestId, funds, replyTo)

                val response: Future[HttpResponse] =
                  wallet
                    .ask(auxReserveFundsRequest(requestId, funds))
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
        },
        get {
          parameters("walletId".as[String]) {
            entityId => //FIXME avoid param, id is assumed
              val container =
                sharding.entityRefFor(Wallet.typeKey, entityId)

              val response: Future[WalletData] =
                container
                  .ask(Wallet.CheckFunds)
                  .mapTo[Wallet.CurrentBalance]
                  .map(balance =>
                    WalletData(entityId, balance.amount))

              complete(response)
          }
        },
        path("all") {
          get {
            val readJournal = PersistenceQuery(system)
              .readJournalFor[JdbcReadJournal](
                JdbcReadJournal.Identifier)
            val walletIdsFuture: Future[Seq[String]] =
              readJournal
                .currentPersistenceIds()
                .runWith(Sink.seq)
                .map(
                  _.filter(_.startsWith(Wallet.typeKey.name + "|")))

            val response: Future[WalletList] =
              walletIdsFuture.flatMap {
                walletIds =>
                  val walletFutures: Seq[Future[WalletData]] =
                    walletIds.map {
                      walletId =>
                        val sanitizedWalletId = walletId
                          .replace(Wallet.typeKey.name + "|", "")
                        val entityRef = sharding.entityRefFor(
                          Wallet.typeKey,
                          sanitizedWalletId)
                        entityRef
                          .ask(Wallet.CheckFunds)
                          .mapTo[Wallet.CurrentBalance]
                          .map(balance =>
                            WalletData(
                              sanitizedWalletId,
                              balance.amount))
                    }
                  Future.sequence(walletFutures).map(WalletList)
              }

            onSuccess(response) { walletList =>
              complete(walletList)
            }
          }
        })
    }

}

case class WalletData(walletId: String, balance: Int)
case class WalletList(wallets: Seq[WalletData])
