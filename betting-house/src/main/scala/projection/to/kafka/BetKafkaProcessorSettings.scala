package projection.to.kafka

import akka.actor.ActorSystem
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.kafka.ConsumerSettings
import akka.util.Timeout
import com.typesafe.config.Config
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}

import scala.jdk.CollectionConverters._
import scala.concurrent.duration._
import example.betting.Bet

case object BetKafkaProcessorSettings {
  def apply(
      config: Config,
      topics: List[String],
      groupId: String,
      askTimeout: java.time.Duration,
      system: ActorSystem): BetKafkaProcessorSettings = {
    new BetKafkaProcessorSettings(
      config.getString("bootstrap-servers"),
      topics,
      groupId,
      Timeout.create(askTimeout),
      system: ActorSystem)
  }
}

final class BetKafkaProcessorSettings(
    val bootstrapServers: String,
    val topics: List[String],
    val groupId: String,
    val askTimeout: Timeout,
    val system: ActorSystem) {
  def kafkaConsumerSettings()
      : ConsumerSettings[String, Array[Byte]] = {
    ConsumerSettings(
      system,
      new StringDeserializer,
      new ByteArrayDeserializer)
      .withBootstrapServers(bootstrapServers)
      .withGroupId(groupId)
      .withProperty(
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
        "earliest")
      .withStopTimeout(0.seconds)
  }

  /**
   * By using the same consumer group id as our entity type key name we can setup multiple consumer groups and connect
   * each with a different sharded entity coordinator.
   */
  val entityTypeKey: EntityTypeKey[Bet.Command] = EntityTypeKey(
    groupId)
}
