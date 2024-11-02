package betting.house.projection

import akka.Done
import akka.kafka.scaladsl.SendProducer
import org.slf4j.LoggerFactory
import akka.projection.eventsourced.EventEnvelope
import akka.projection.jdbc.scaladsl.JdbcHandler
import example.repository.scalike.{
  BetRepository,
  ScalikeJdbcSession,
  WalletRepository
}
import example.betting.{ Bet, Market, Wallet }

import scala.concurrent.{ ExecutionContext, Future }

class WalletProjectionHandler(repository: WalletRepository)
    extends JdbcHandler[
      EventEnvelope[Wallet.Event],
      ScalikeJdbcSession] {

  val log = LoggerFactory.getLogger(classOf[WalletProjectionHandler])
  implicit val ec = ExecutionContext.global

  override def process(
      session: ScalikeJdbcSession,
      envelope: EventEnvelope[Wallet.Event]): Unit = {
    log.debug(s"processing wallet event [$envelope]")

    envelope.event match {
      case b: Wallet.FundsRequested =>
        repository.addWalletRequest(b.requestId, session)
      case x =>
        log.debug("ignoring event {} in projection", x)

    }
  }

}
