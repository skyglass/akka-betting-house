package projection.to.kafka

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.kafka.{ ConsumerSettings, ProducerSettings }
import akka.util.Timeout
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{
  ByteArrayDeserializer,
  StringDeserializer
}

import scala.concurrent.duration._
import example.betting.Bet

case object BetResultConsumerSettings {
  def apply(
      producerSettings: ProducerSettings[String, Array[Byte]],
      groupId: String,
      system: ActorSystem[Nothing]): BetResultConsumerSettings = {
    new BetResultConsumerSettings(
      producerSettings.getProperty("bootstrap.servers"),
      groupId,
      system: ActorSystem[Nothing])
  }
}

final class BetResultConsumerSettings(
    val bootstrapServers: String,
    val groupId: String,
    val system: ActorSystem[Nothing]) {
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
}
