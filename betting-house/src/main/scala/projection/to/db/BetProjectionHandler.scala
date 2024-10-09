package betting.house.projection

import akka.Done
import akka.kafka.scaladsl.SendProducer
import org.slf4j.LoggerFactory
import akka.projection.eventsourced.EventEnvelope
import akka.projection.jdbc.scaladsl.JdbcHandler
import com.google.protobuf.empty.Empty
import example.repository.scalike.{
  BetRepository,
  ScalikeJdbcSession
}
import example.betting.{ Bet, Market }
import org.apache.kafka.clients.producer.ProducerRecord

import scala.concurrent.{ ExecutionContext, Future }

class BetProjectionHandler(
    repository: BetRepository,
    producer: SendProducer[String, Array[Byte]])
    extends JdbcHandler[EventEnvelope[Bet.Event], ScalikeJdbcSession] {

  val log = LoggerFactory.getLogger(classOf[BetProjectionHandler])
  implicit val ec = ExecutionContext.global

  override def process(
      session: ScalikeJdbcSession,
      envelope: EventEnvelope[Bet.Event]): Unit = {
    envelope.event match {
      case b: Bet.Opened =>
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
}
