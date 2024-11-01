package betting.house.projection

import org.slf4j.LoggerFactory
import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings
import akka.projection.{ ProjectionBehavior, ProjectionId }
import akka.projection.scaladsl.{
  AtLeastOnceProjection,
  SourceProvider
}
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.jdbc.scaladsl.JdbcProjection
import akka.persistence.query.Offset
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import example.repository.scalike.ScalikeJdbcSession
import akka.kafka.scaladsl.SendProducer
import example.betting.Market

//Bets grouped per Market and Wallet
object MarketProjection { //BPM

  def init(
      system: ActorSystem[_],
      producer: SendProducer[String, Array[Byte]]): Unit = {
    val topic =
      system.settings.config
        .getString("kafka.market-projection.topic")

    ShardedDaemonProcess(system).init(
      name = "MarketProjection",
      Market.tags.size,
      index =>
        ProjectionBehavior(
          createProjection(system, topic, producer, index)),
      ShardedDaemonProcessSettings(system),
      Some(ProjectionBehavior.Stop))
  }

  private def createProjection(
      system: ActorSystem[_],
      topic: String,
      producer: SendProducer[String, Array[Byte]],
      index: Int)
      : AtLeastOnceProjection[Offset, EventEnvelope[Market.Event]] = {
    val tag = Market.tags(index)
    val sourceProvider
        : SourceProvider[Offset, EventEnvelope[Market.Event]] =
      EventSourcedProvider.eventsByTag[Market.Event](
        system = system,
        readJournalPluginId = JdbcReadJournal.Identifier,
        tag = tag)

    JdbcProjection.atLeastOnceAsync(
      projectionId = ProjectionId("MarketProjection", tag),
      sourceProvider = sourceProvider,
      handler = () => new MarketProjectionHandler(topic, producer),
      sessionFactory = () => new ScalikeJdbcSession())(system)
  }
}
