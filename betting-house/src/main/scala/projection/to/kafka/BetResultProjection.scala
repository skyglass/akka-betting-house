package betting.house.projection

import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings
import akka.projection.{ProjectionBehavior, ProjectionId}
import akka.projection.scaladsl.{AtLeastOnceProjection, ExactlyOnceProjection, SourceProvider}
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.jdbc.scaladsl.JdbcProjection
import akka.persistence.query.Offset
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import example.repository.scalike.ScalikeJdbcSession
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.SendProducer
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import example.betting.Market
import example.betting.Bet

//Bet results grouped for market and processed asynchronously with kafka consumer-producer when market closes with result
object BetResultProjection { //BPM

  def init(system: ActorSystem[_]): Unit = {
    val producer = createProducer(system)
    val topic =
      system.settings.config
        .getString("kafka.bet-projection.topic")

    ShardedDaemonProcess(system).init(
      name = "BetProjection",
      Market.tags.size,
      index =>
        ProjectionBehavior(
          createProjection(system, topic, producer, index)),
      ShardedDaemonProcessSettings(system),
      Some(ProjectionBehavior.Stop))
  }

  def createProducer(
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

  private def createProjection(
                                system: ActorSystem[_],
                                topic: String,
                                producer: SendProducer[String, Array[Byte]],
                                index: Int)
  : ExactlyOnceProjection[Offset, EventEnvelope[Bet.Event]] = {
    val tag = Market.tags(index)
    val sourceProvider
    : SourceProvider[Offset, EventEnvelope[Bet.Event]] =
      EventSourcedProvider.eventsByTag[Bet.Event](
        system = system,
        readJournalPluginId = JdbcReadJournal.Identifier,
        tag = tag)

    JdbcProjection.atLeastOnceAsync(
      projectionId = ProjectionId("MarketProjection", tag),
      sourceProvider = sourceProvider,
      handler =
        () => new MarketProjectionHandler(system, topic, producer),
      sessionFactory = () => new ScalikeJdbcSession())(system)
  }
}
