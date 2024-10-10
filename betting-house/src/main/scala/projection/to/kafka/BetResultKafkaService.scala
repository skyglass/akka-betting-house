package projection.to.kafka

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.ActorContext
import akka.cluster.sharding.typed.scaladsl.{
  ClusterSharding,
  Entity
}
import akka.kafka.ConsumerMessage.TransactionalMessage
import akka.kafka.{ ProducerMessage, ProducerSettings, Subscriptions }
import akka.kafka.scaladsl.{ Consumer, SendProducer, Transactional }
import akka.stream.UniqueKillSwitch
import akka.stream.scaladsl.Sink
import example.bet.grpc.BetService
import example.betting.{ Bet, Market }
import example.betting.Bet.{
  Command,
  OpenState,
  ValidationsPassedState
}
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

import scala.collection.mutable.HashMap
import scala.concurrent.{ ExecutionContext, Future }

object BetResultKafkaService {
  val log = LoggerFactory.getLogger(this.getClass)
  val PROTOTYPE_KEY = "prototype-key"

  private val consumers =
    new HashMap[String, BetResultTransactionalConsumer]()

  private val killSwitches =
    new HashMap[String, UniqueKillSwitch]()

  def init(
      implicit producer: SendProducer[String, Array[Byte]],
      adminClient: AdminClient,
      sharding: ClusterSharding,
      system: ActorSystem[Nothing],
      ec: ExecutionContext): Unit = {
    sharding.init(Entity(Bet.typeKey)(entityContext =>
      Bet(entityContext.entityId)))
    val prototype = new BetResultTransactionalConsumer()
    BetResultKafkaService.consumers.put(PROTOTYPE_KEY, prototype)
  }

  def createConsumer(marketId: String, marketResult: Int): Unit = {
    val prototype = getPrototype()
    implicit val producer = prototype.getProducer
    implicit val adminClient = prototype.getAdminClient
    implicit val sharding = prototype.getSharding
    implicit val system = prototype.getSystem
    implicit val ec = prototype.getEc
    val consumer = new BetResultTransactionalConsumer()
    val killSwitch = consumer.init(marketId, marketResult)
    addConsumer(marketId, consumer, killSwitch)
  }

  private def addConsumer(
      marketId: String,
      consumer: BetResultTransactionalConsumer,
      killSwitch: UniqueKillSwitch): Unit = {
    consumers.put(marketId, consumer)
    killSwitches.put(marketId, killSwitch)
  }

  def shutdownConsumer(marketId: String): Unit = {
    val killSwitch = killSwitches.get(marketId).get
    killSwitch.shutdown()
    killSwitches.remove(marketId)
  }

  def deleteTopic(marketId: String): Unit = {
    val topic = s"bet-result-${marketId}"
    val prototype = getPrototype()
    val adminClient = prototype.getAdminClient
    adminClient.deleteTopics(java.util.Arrays.asList(topic))
    consumers.remove(marketId)
    log.warn(
      s"shut down consumer and deleted topic for marketId [$marketId]}")
  }

  def deleteTopicOnComplete(marketId: String): Unit = {
    val topic = s"bet-result-${marketId}"
    val prototype = getPrototype()
    val adminClient = prototype.getAdminClient
    adminClient.deleteTopics(java.util.Arrays.asList(topic))
    consumers.remove(marketId)
    log.warn(
      s"on complete shut down consumer and deleted topic for marketId [$marketId]}")
  }

  private def getPrototype(): BetResultTransactionalConsumer = {
    consumers.get(PROTOTYPE_KEY).get
  }

  def sendEvent(state: ValidationsPassedState): Future[Done] = {
    val prototype = getPrototype()
    implicit val producer = prototype.getProducer
    implicit val ec = prototype.getEc
    val topic = s"bet-result-${state.status.marketId}"
    log.debug(
      s"sending bet result event with betId [${state.status.betId}] to topic [${topic}]}")

    val serializedEvent = serialize(state.status.betId, state)
    if (!serializedEvent.isEmpty) {
      val record =
        new ProducerRecord(topic, state.status.betId, serializedEvent)
      producer.send(record).map { _ =>
        log.debug(
          s"published event with betId [${state.status.betId}] to topic [$topic]}")
        Done
      }
      Future.successful(Done)
    } else {
      Future.successful(Done)
    }
  }

  def sendAllMessagesConsumedPoisonPill(
      poisonPillId: String,
      state: ValidationsPassedState): Future[Done] = {
    val prototype = getPrototype()
    implicit val producer = prototype.getProducer
    implicit val ec = prototype.getEc
    val topic = s"bet-result-${state.status.marketId}"
    log.debug(
      s"sending poison pill message telling that all messages consumed for topic [${topic}]}")

    val serializedEvent = serialize(poisonPillId, state)
    if (!serializedEvent.isEmpty) {
      val record =
        new ProducerRecord(topic, poisonPillId, serializedEvent)
      producer.send(record).map { _ =>
        log.debug(
          s"published poison pill event with poisonPillId [${poisonPillId}] to topic [$topic]}")
        Done
      }
      Future.successful(Done)
    } else {
      Future.successful(Done)
    }
  }

  private def serialize(
      betId: String,
      state: Bet.ValidationsPassedState): Array[Byte] = {
    val proto = example.bet.grpc.Bet(
      betId,
      state.status.walletId,
      state.status.marketId,
      state.status.odds,
      state.status.stake,
      state.status.result)
    proto.toByteArray
  }

}
