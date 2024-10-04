package example.betting

import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.management.scaladsl.AkkaManagement
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.cluster.sharding.typed.scaladsl.ClusterSharding

import scala.concurrent.{ ExecutionContext, Future }
import akka.http.scaladsl.{ Http, HttpConnectionContext }
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.SendProducer
import example.market.grpc.{
  MarketServiceHandler,
  MarketServiceImplSharding
}
import example.bet.grpc.BetServiceServer

import scala.io.StdIn
import example.bet.akka.http.WalletServiceServer
import example.repository.scalike.BetRepositoryImpl
import betting.house.projection.{
  BetProjection,
  BetProjectionServer,
  MarketProjection
}
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

import scala.util.control.NonFatal
import example.repository.scalike.ScalikeJdbcSetup
import org.apache.kafka.common.serialization.{
  ByteArraySerializer,
  StringSerializer
}

object Main {

  val log = LoggerFactory.getLogger(Main + "")

  def main(args: Array[String]): Unit = {
    implicit val system =
      ActorSystem[Nothing](Behaviors.empty, "betting-house")
    try {

      val sharding = ClusterSharding(system)
      implicit val ec: ExecutionContext = system.executionContext

      AkkaManagement(system).start()
      ClusterBootstrap(system).start()
      ScalikeJdbcSetup.init(system)

      BetServiceServer.init(system, sharding, ec)
      MarketServiceServer.init(system, sharding, ec)
      WalletServiceServer.init(system, sharding, ec)

      val betRepository = new BetRepositoryImpl()
      val producer = createProducer(system)
      BetProjectionServer.init(betRepository)
      BetProjection.init(system, betRepository, producer)
      MarketProjection.init(system, producer)
    } catch {
      case NonFatal(ex) =>
        log.error(
          s"Terminating Betting App. Reason [${ex.getMessage}]")
        system.terminate
    }

  }

  private def createProducer(
      system: ActorSystem[_]): SendProducer[String, Array[Byte]] = {

    val producerSettings =
      ProducerSettings( //they look up on creation at "akka.kafka.producer" in .conf
        system,
        new StringSerializer,
        new ByteArraySerializer)
    val sendProducer = SendProducer(producerSettings)(system)
    CoordinatedShutdown(system).addTask(
      CoordinatedShutdown.PhaseBeforeActorSystemTerminate,
      "closing send producer") { () =>
      sendProducer.close()
    } //otherwise trying to restart the application you would probably get [WARN] [org.apache.kafka.common.utils.AppInfoParser] [] [betting-house-akka.kafka.default-dispatcher-X] - Error registering AppInfo mbean javax.management.InstanceAlreadyExistsException: kafka.producer:type=app-info,id=producer-
    sendProducer
  }

}
