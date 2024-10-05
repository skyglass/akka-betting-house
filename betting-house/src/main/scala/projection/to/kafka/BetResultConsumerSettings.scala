package projection.to.kafka

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.kafka.{ConsumerSettings, ProducerSettings}
import akka.util.Timeout
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}

import scala.concurrent.duration._
import example.betting.Bet

case object BetResultConsumerSettings {
  def apply(
      configLocation: String,
      producerSettings: ProducerSettings[String, Array[Byte]],
      topic: String,
      system: ActorSystem[Nothing]): BetResultConsumerSettings = {
    val config = system.settings.config.getConfig(configLocation)
    new BetResultConsumerSettings(
      producerSettings.getProperty("bootstrap.servers"),
      topic,
      config.getString("group-id"),
      Timeout.create(config.getDuration("ask-timeout")),
      system: ActorSystem[Nothing])
  }
}

final class BetResultConsumerSettings(
    val bootstrapServers: String,
    val topic: String,
    val groupId: String,
    val askTimeout: Timeout,
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
