package betting.house.projection

import akka.Done
import akka.kafka.scaladsl.SendProducer
import org.slf4j.LoggerFactory
import akka.projection.eventsourced.EventEnvelope
import akka.projection.jdbc.scaladsl.JdbcHandler
import betting.house.projection
import com.google.protobuf.empty.Empty
import example.betting.Bet.{
  OpenState,
  State,
  Status,
  ValidationsPassedState
}
import example.repository.scalike.{
  BetRepository,
  ScalikeJdbcSession
}
import example.betting.{ Bet, Market }
import org.apache.kafka.clients.producer.ProducerRecord
import com.google.protobuf.any.{ Any => PbAny }

import scala.concurrent.{ ExecutionContext, Future }

class BetProjectionHandler(topic: String, repository: BetRepository)
    extends JdbcHandler[EventEnvelope[Bet.Event], ScalikeJdbcSession] {

  val log = LoggerFactory.getLogger(classOf[BetProjectionHandler])
  implicit val ec = ExecutionContext.global

  override def process(
      session: ScalikeJdbcSession,
      envelope: EventEnvelope[Bet.Event]): Unit = {
    log.debug(s"processing bet event [$envelope]")

    //TODO: skip for now but the code is kept for demo purposes
    /* val event = envelope.event
    val serializedEvent = serialize(event)
    if (!serializedEvent.isEmpty) {
      val record =
        new ProducerRecord(topic, event.betId, serializedEvent)
      producer.send(record).map { _ =>
        log.debug(s"published event [$event] to topic [$topic]}")
        Done
      }
    }*/
    envelope.event match {
      case b: Bet.Opened =>
        repository.addBet(
          b.betId,
          b.walletId,
          b.marketId,
          b.marketName,
          b.odds,
          b.stake,
          b.result,
          session)
      case x =>
        log.debug("ignoring event {} in projection", x)

    }
  }

  def serialize(event: Bet.Event): Array[Byte] = {
    val proto = event match {
      case Bet.MarketConfirmed(
          _,
          OpenState(
            Status(
              betId,
              walletId,
              marketId,
              marketName,
              odds,
              stake,
              result,
              marketConfirmed,
              fundsConfirmed),
            fundReservationRetryCount,
            fundReservationMaxRetries,
            marketConfirmationRetryCount,
            marketConfirmationMaxRetries)) =>
        val status = projection.proto.Status(
          betId,
          walletId,
          marketId,
          marketName,
          odds,
          stake,
          result,
          marketConfirmed,
          fundsConfirmed)
        val openState = projection.proto.OpenState(
          Some(status),
          fundReservationRetryCount,
          fundReservationMaxRetries,
          marketConfirmationRetryCount,
          marketConfirmationMaxRetries)
        projection.proto.MarketConfirmed(betId, Some(openState))

      case Bet.FundsGranted(
          _,
          OpenState(
            Status(
              betId,
              walletId,
              marketId,
              marketName,
              odds,
              stake,
              result,
              marketConfirmed,
              fundsConfirmed),
            fundReservationRetryCount,
            fundReservationMaxRetries,
            marketConfirmationRetryCount,
            marketConfirmationMaxRetries)) =>
        val status = projection.proto.Status(
          betId,
          walletId,
          marketId,
          marketName,
          odds,
          stake,
          result,
          marketConfirmed,
          fundsConfirmed)
        val openState = projection.proto.OpenState(
          Some(status),
          fundReservationRetryCount,
          fundReservationMaxRetries,
          marketConfirmationRetryCount,
          marketConfirmationMaxRetries)
        projection.proto.FundsGranted(betId, Some(openState))

      case Bet.ValidationsPassed(
          _,
          OpenState(
            Status(
              betId,
              walletId,
              marketId,
              marketName,
              odds,
              stake,
              result,
              marketConfirmed,
              fundsConfirmed),
            fundReservationRetryCount,
            fundReservationMaxRetries,
            marketConfirmationRetryCount,
            marketConfirmationMaxRetries)) =>
        val status = projection.proto.Status(
          betId,
          walletId,
          marketId,
          marketName,
          odds,
          stake,
          result,
          marketConfirmed,
          fundsConfirmed)
        val state = projection.proto.OpenState(
          Some(status),
          fundReservationRetryCount,
          fundReservationMaxRetries,
          marketConfirmationRetryCount,
          marketConfirmationMaxRetries)
        projection.proto.ValidationsPassed(betId, Some(state))

      case Bet.BetSettled(
          _,
          ValidationsPassedState(
            Status(
              betId,
              walletId,
              marketId,
              marketName,
              odds,
              stake,
              result,
              marketConfirmed,
              fundsConfirmed))) =>
        val status = projection.proto.Status(
          betId,
          walletId,
          marketId,
          marketName,
          odds,
          stake,
          result,
          marketConfirmed,
          fundsConfirmed)
        val state =
          projection.proto.ValidationsPassedState(Some(status))
        projection.proto.BetSettled(betId, Some(state))

      case Bet.Opened(
          betId,
          walletId,
          marketId,
          marketName,
          odds,
          stake,
          result) =>
        projection.proto.Opened(
          betId,
          walletId,
          marketId,
          marketName,
          odds,
          stake,
          result)

      case Bet.Cancelled(betId, reason) =>
        projection.proto.Cancelled(betId, reason)

      case Bet.MarketValidationFailed(betId, reason) =>
        projection.proto.MarketValidationFailed(betId, reason)

      case Bet.MarketConfirmationDenied(
          _,
          reason,
          OpenState(
            Status(
              betId,
              walletId,
              marketId,
              marketName,
              odds,
              stake,
              result,
              marketConfirmed,
              fundsConfirmed),
            fundReservationRetryCount,
            fundReservationMaxRetries,
            marketConfirmationRetryCount,
            marketConfirmationMaxRetries)) =>
        val status = projection.proto.Status(
          betId,
          walletId,
          marketId,
          marketName,
          odds,
          stake,
          result,
          marketConfirmed,
          fundsConfirmed)
        val openState = projection.proto.OpenState(
          Some(status),
          fundReservationRetryCount,
          fundReservationMaxRetries,
          marketConfirmationRetryCount,
          marketConfirmationMaxRetries)
        projection.proto.MarketConfirmationDenied(
          betId,
          reason,
          Some(openState))

      case Bet.FundReservationDenied(
          _,
          reason,
          OpenState(
            Status(
              betId,
              walletId,
              marketId,
              marketName,
              odds,
              stake,
              result,
              marketConfirmed,
              fundsConfirmed),
            fundReservationRetryCount,
            fundReservationMaxRetries,
            marketConfirmationRetryCount,
            marketConfirmationMaxRetries)) =>
        val status = projection.proto.Status(
          betId,
          walletId,
          marketId,
          marketName,
          odds,
          stake,
          result,
          marketConfirmed,
          fundsConfirmed)
        val openState = projection.proto.OpenState(
          Some(status),
          fundReservationRetryCount,
          fundReservationMaxRetries,
          marketConfirmationRetryCount,
          marketConfirmationMaxRetries)
        projection.proto.FundReservationDenied(
          betId,
          reason,
          Some(openState))

      case Bet.Failed(betId, reason) =>
        projection.proto.Failed(betId, reason)

      case Bet.Closed(betId) =>
        projection.proto.Closed(betId)

      case x =>
        log.error(s"ignoring event $x in projection")
        Empty.defaultInstance
    }
    PbAny.pack(proto, topic).toByteArray
  }
}
