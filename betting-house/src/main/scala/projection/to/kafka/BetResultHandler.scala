package betting.house.projection

import akka.Done
import akka.actor.typed.ActorSystem
import akka.projection.eventsourced.EventEnvelope
import akka.projection.scaladsl.Handler
import akka.kafka.scaladsl.SendProducer
import org.slf4j.LoggerFactory
import org.apache.kafka.clients.producer.ProducerRecord
import com.google.protobuf.any.{Any => PbAny}
import com.google.protobuf.empty.Empty

import scala.concurrent.{ExecutionContext, Future}
import example.betting.Market
import example.betting.Bet
import betting.house.projection
import example.betting.Bet.OpenState

class BetResultHandler(
                               system: ActorSystem[_],
                               topic: String,
                               producer: SendProducer[String, Array[Byte]])
  extends Handler[EventEnvelope[Bet.Event]] {

  val log = LoggerFactory.getLogger(classOf[BetResultHandler])
  implicit val ec = ExecutionContext.global

  override def process(
                        envelope: EventEnvelope[Bet.Event]): Future[Done] = {
    log.debug(
      s"processing market event [$envelope] to topic [$topic]}")

    val event = envelope.event
    val serializedEvent = serialize(event)
    if (!serializedEvent.isEmpty) {
      val record =
        new ProducerRecord(topic, event.betId, serializedEvent)
      producer.send(record).map { _ =>
        log.debug(s"published event [$event] to topic [$topic]}")
        Done
      }
    } else {
      Future.successful(Done)
    }
  }

  def serialize(event: Bet.Event): Array[Byte] = {
    val proto = event match {
      case Bet.MarketConfirmed(event.betId, state: OpenState) =>
        projection.proto.MarketClosed(state.status.betId, state.status.result)
      case x =>
        log.info(s"ignoring event $x in projection")
        Empty.defaultInstance
    }
    PbAny.pack(proto, "bet-projection").toByteArray
  }

  def sendRecord(event: Bet.Event): Array[Byte] = {
    val proto = event match {
      case Bet.MarketConfirmed(event.betId, state: OpenState) =>
        projection.proto.MarketClosed(state.status.betId, state.status.result)
      case x =>
        log.info(s"ignoring event $x in projection")
        Empty.defaultInstance
    }
    PbAny.pack(proto, "bet-projection").toByteArray
  }

}
