package example.betting

import akka.Done
import akka.actor.{ Address, CoordinatedShutdown }
import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.actor.typed.scaladsl.Behaviors
import akka.management.scaladsl.AkkaManagement
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.typed.Cluster

import scala.concurrent.{ ExecutionContext, Future }
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.SendProducer
import org.apache.kafka.clients.admin.{
  AdminClient,
  AdminClientConfig,
  NewTopic
}
import example.bet.grpc.{ BetServiceImplSharding, BetServiceServer }

import scala.io.StdIn
import example.bet.akka.http.WalletServiceServer
import example.repository.scalike.{
  BetRepositoryImpl,
  ScalikeJdbcSetup,
  WalletRepositoryImpl
}
import betting.house.projection.{
  BetProjection,
  BetProjectionServer,
  MarketProjection
}
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

import scala.util.control.NonFatal
import org.apache.kafka.common.serialization.{
  ByteArraySerializer,
  StringSerializer
}
import projection.to.kafka
import projection.to.kafka.{
  BetResultConsumerSettings,
  BetResultKafkaService,
  BetResultTransactionalConsumer
}

import scala.collection.immutable.List

object Main {

  val log = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {
    implicit val system =
      ActorSystem[Nothing](Behaviors.empty, "betting-house")
    try {

      val sharding = ClusterSharding(system)
      implicit val ec: ExecutionContext = system.executionContext

      AkkaManagement(system).start()
      ClusterBootstrap(system).start()
      ScalikeJdbcSetup.init(system)

      val betRepository = new BetRepositoryImpl()

      BetServiceServer.init(system, sharding)
      MarketServiceServer.init(system, sharding, ec)
      WalletServiceServer.init(system, sharding, ec)

      val cluster = Cluster(system)

      val producer = createProducer(system)
      val adminClient = createKafkaAdminClient(system)
      BetProjectionServer.init(betRepository)
      BetProjection.init(system, betRepository)
      MarketProjection.init(system, producer)
      log.warn("prepare consumer")
      BetResultKafkaService.init(
        producer,
        adminClient,
        sharding,
        system,
        ec)
      log.warn("prepared consumer")
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

  private def createKafkaAdminClient(
      system: ActorSystem[_]): AdminClient = {

    val producerSettings =
      ProducerSettings( //they look up on creation at "akka.kafka.producer" in .conf
        system,
        new StringSerializer,
        new ByteArraySerializer)
    val config = new java.util.Properties
    config.putAll(producerSettings.getProperties)
    val admin = AdminClient.create(config)
    CoordinatedShutdown(system).addTask(
      CoordinatedShutdown.PhaseBeforeActorSystemTerminate,
      "closing kafka admin client") { () =>
      admin.close()
      Future.successful(Done)
    }
    admin
  }

}
