package betting.house.projection

import akka.NotUsed
import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import akka.projection.ProjectionId
import akka.projection.scaladsl.{
  AtLeastOnceProjection,
  ExactlyOnceProjection
}
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.ProjectionBehavior
import akka.projection.jdbc.scaladsl.{ JdbcHandler, JdbcProjection }
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.SendProducer
import akka.persistence.query.Offset
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import example.repository.scalike.{
  BetRepository,
  ScalikeJdbcSession,
  WalletRepository
}
import example.betting.{ Bet, Wallet }
import org.apache.kafka.common.serialization.{
  ByteArraySerializer,
  StringSerializer
}

object WalletProjection {

  def init(
      system: ActorSystem[_],
      repository: WalletRepository): Unit = {

    ShardedDaemonProcess(system).init(
      name = "bet-projection",
      Wallet.tags.size,
      index =>
        ProjectionBehavior(
          createProjection(system, repository, index)),
      ShardedDaemonProcessSettings(system),
      Some(ProjectionBehavior.Stop))
  }

  def createProjection(
      system: ActorSystem[_],
      repository: WalletRepository,
      index: Int)
      : AtLeastOnceProjection[Offset, EventEnvelope[Wallet.Event]] = {

    val tag = Wallet.tags(index)

    val sourceProvider =
      EventSourcedProvider.eventsByTag[Wallet.Event](
        system = system,
        readJournalPluginId = JdbcReadJournal.Identifier,
        tag = tag)

    JdbcProjection.atLeastOnce(
      projectionId = ProjectionId("WalletProjection", tag),
      sourceProvider = sourceProvider,
      handler = () => new WalletProjectionHandler(repository),
      sessionFactory = () => new ScalikeJdbcSession())(system)
  }
}
