package projection.to.kafka

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.ActorContext
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.kafka.ConsumerMessage.TransactionalMessage
import akka.kafka.{ ProducerMessage, ProducerSettings, Subscriptions }
import akka.kafka.scaladsl.{ Consumer, SendProducer, Transactional }
import akka.stream.scaladsl.Sink
import example.bet.grpc.BetService
import example.betting.{ Bet, Market }
import example.betting.Bet.Command
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

import scala.collection.mutable.HashMap
import scala.concurrent.{ ExecutionContext, Future }
import com.google.protobuf.any.{ Any => PbAny }

object BetResultKafkaService {
  val log = LoggerFactory.getLogger(this.getClass)
  val PROTOTYPE_KEY = "prototype-key"
  val consumers =
    new HashMap[String, BetResultTransactionalConsumer]()

  def init(
      implicit producer: SendProducer[String, Array[Byte]],
      adminClient: AdminClient,
      sharding: ClusterSharding,
      system: ActorSystem[Nothing],
      ec: ExecutionContext): Unit = {
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
    consumer.init(marketId, marketResult)
    consumers.put(marketId, consumer)
  }

  def deleteConsumer(marketId: String): Unit = {
    consumers.remove(marketId)
  }

  private def getPrototype(): BetResultTransactionalConsumer = {
    consumers.get(PROTOTYPE_KEY).get
  }

  def sendEvent(event: Bet.Opened): Future[Done] = {
    val prototype = getPrototype()
    implicit val producer = prototype.getProducer
    implicit val ec = prototype.getEc
    val topic = s"bet-result-${event.marketId}"
    log.debug(
      s"sending bet result event [$event] to topic [${topic}]}")

    val serializedEvent = serialize(event, topic)
    if (!serializedEvent.isEmpty) {
      val record =
        new ProducerRecord(topic, event.betId, serializedEvent)
      producer.send(record).map { _ =>
        log.debug(s"published event [$event] to topic [$topic]}")
        Done
      }
      Future.successful(Done)
    } else {
      Future.successful(Done)
    }
  }

  private def serialize(
      event: Bet.Opened,
      topic: String): Array[Byte] = {
    val proto = example.bet.grpc.Bet(
      event.betId,
      event.walletId,
      event.marketId,
      event.odds,
      event.stake,
      event.result)
    PbAny.pack(proto, topic).toByteArray
  }
}
