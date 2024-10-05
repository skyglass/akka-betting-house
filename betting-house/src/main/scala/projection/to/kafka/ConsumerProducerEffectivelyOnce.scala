package projection.to.kafka

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.kafka.ConsumerMessage.TransactionalMessage
import akka.kafka.scaladsl.{ Consumer, Transactional }
import akka.kafka.{
  ConsumerSettings,
  ProducerMessage,
  ProducerSettings,
  Subscriptions
}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{
  StringDeserializer,
  StringSerializer
}

import scala.concurrent.ExecutionContext
import scala.io.StdIn

object ConsumerProducerEffectivelyOnce {

  implicit val system = ActorSystem(Behaviors.empty, "producerOne")

  implicit val ec = ExecutionContext.Implicits.global

  def createConsumerProducer(
      groupId: String,
      transactionalId: String,
      subscriptionTopic: String) = {

    val bootstrapServers = "127.0.0.1:9092"

    val consumerConfig =
      system.settings.config.getConfig("akka.kafka.consumer")

    val consumerSettings: ConsumerSettings[String, String] =
      ConsumerSettings(
        consumerConfig,
        new StringDeserializer(),
        new StringDeserializer())
        .withBootstrapServers(bootstrapServers)
        .withGroupId(groupId)
        .withProperty(
          ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
          "earliest")

    val producerConfig =
      system.settings.config.getConfig("akka.kafka.producer")

    val producerSettings = ProducerSettings(
      producerConfig,
      new StringSerializer(),
      new StringSerializer())
      .withBootstrapServers(bootstrapServers)

    val drainingControl: Consumer.DrainingControl[_] =
      Transactional
        .source(
          consumerSettings,
          Subscriptions.topics(subscriptionTopic)
        ) //we don't need committer settings
        .map { msg: TransactionalMessage[String, String] =>
          ProducerMessage.single(
            new ProducerRecord[String, String](
              subscriptionTopic,
              msg.record.key,
              msg.record.value),
            msg.partitionOffset)
        }
        .toMat(Transactional.sink(producerSettings, transactionalId))(
          Consumer.DrainingControl.apply)
        .run()

    StdIn.readLine("Consumer started \n Press ENTER to stop")
    val future = drainingControl.drainAndShutdown
    future.onComplete(_ => system.terminate)

  }
}
