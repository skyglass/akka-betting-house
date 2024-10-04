package betting.house.projection

import akka.NotUsed
import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import akka.projection.ProjectionId
import akka.projection.scaladsl.ExactlyOnceProjection
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.ProjectionBehavior
import akka.projection.jdbc.scaladsl.{ JdbcHandler, JdbcProjection }
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.kafka.{ ProducerSettings }
import akka.kafka.scaladsl.SendProducer
import akka.persistence.query.Offset
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import example.repository.scalike.{
  BetRepository,
  ScalikeJdbcSession
}
import example.betting.Bet
import org.apache.kafka.common.serialization.{
  ByteArraySerializer,
  StringSerializer
}

object BetProjection {

  def init(
      system: ActorSystem[_],
      repository: BetRepository): Unit = {
    //val producer = createProducer(system)

    ShardedDaemonProcess(system).init(
      name = "bet-projection",
      Bet.tags.size,
      index =>
        ProjectionBehavior(
          createProjection(system, repository, index)),
      ShardedDaemonProcessSettings(system),
      Some(ProjectionBehavior.Stop))
  }

  def createProjection(
      system: ActorSystem[_],
      repository: BetRepository,
      index: Int)
      : ExactlyOnceProjection[Offset, EventEnvelope[Bet.Event]] = {

    val tag = Bet.tags(index)

    val sourceProvider =
      EventSourcedProvider.eventsByTag[Bet.Event](
        system = system,
        readJournalPluginId = JdbcReadJournal.Identifier,
        tag = tag)

    JdbcProjection.exactlyOnce(
      projectionId = ProjectionId("BetProjection", tag),
      sourceProvider = sourceProvider,
      handler = () => new BetProjectionHandler(repository),
      sessionFactory = () => new ScalikeJdbcSession())(system)
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
}
