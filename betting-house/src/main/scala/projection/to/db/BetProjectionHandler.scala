package betting.house.projection

import akka.Done
import akka.kafka.scaladsl.SendProducer
import org.slf4j.LoggerFactory
import akka.projection.eventsourced.EventEnvelope
import akka.projection.jdbc.scaladsl.JdbcHandler
import com.google.protobuf.any.{ Any => PbAny }
import com.google.protobuf.empty.Empty
import example.repository.scalike.{
  BetRepository,
  ScalikeJdbcSession
}
import example.betting.{ Bet, Market }
import org.apache.kafka.clients.producer.ProducerRecord

import scala.concurrent.{ ExecutionContext, Future }

class BetProjectionHandler(repository: BetRepository)
    extends JdbcHandler[EventEnvelope[Bet.Event], ScalikeJdbcSession] {

  val log = LoggerFactory.getLogger(classOf[BetProjectionHandler])
  implicit val ec = ExecutionContext.global

  override def process(
      session: ScalikeJdbcSession,
      envelope: EventEnvelope[Bet.Event]): Unit = {
    envelope.event match {
      case b: Bet.Opened =>
        // sendEvent(b)
        repository.addBet(
          b.betId,
          b.walletId,
          b.marketId,
          b.odds,
          b.stake,
          b.result,
          session)
      case x =>
        log.debug("ignoring event {} in projection", x)

    }
  }

  /*private def sendEvent(event: Bet.Opened): Future[Done] = {
    //val topic = s"bet-result-${event.marketId}"
    val topic = "bet-result"
    log.debug(
      s"sending bet result event [$event] to topic [${topic}]}")

    val serializedEvent = serialize(event, topic)
    if (!serializedEvent.isEmpty) {
      val record =
        new ProducerRecord(topic, event.betId, serializedEvent)
      //producer.send(record).map { _ =>
      //  log.debug(s"published event [$event] to topic [$topic]}")
      //  Done
      //}
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
  }*/
}
