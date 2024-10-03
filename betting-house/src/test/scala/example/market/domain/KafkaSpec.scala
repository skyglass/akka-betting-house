package example.market.domain

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.clients.producer.ProducerRecord
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.kafka.{ ProducerMessage, ProducerSettings }
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.Source
import akka.Done
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._

object KafkaSpec {
  val config = ConfigFactory.parseString(
    """
      kafka {
        test.topic = "test1"
      }

      close-timeout = 20s

      kafka-connection-settings {
        bootstrap.servers = "my-cluster-kafka-bootstrap:9092"
      }

      akka.kafka.producer {
        close-timeout = 20s
        close-on-producer-stop = true
        parallelism = 100
        use-dispatcher = "akka.kafka.default-dispatcher"
        eos-commit-interval = 100ms
        kafka-clients {
          bootstrap.servers = "my-cluster-kafka-bootstrap:9092"
        }
      }
      """)

}

class KafkaSpec
    extends AnyFlatSpec
    with Matchers
    with BeforeAndAfterAll {

  implicit val system = ActorSystem(Behaviors.empty, "producerOne")

  "a producer" should "write to Kafka" in {

    val config =
      KafkaSpec.config.getConfig("akka.kafka.producer").resolve()

    val topicDest = KafkaSpec.config.getString("kafka.test.topic")

    val producerSettings = ProducerSettings(
      config,
      new StringSerializer(),
      new StringSerializer()).withBootstrapServers("127.0.0.1:9092")

    val done: Future[Done] = Source(1 to 10)
      .map(_.toString)
      .map(
        elem => new ProducerRecord[String, String](topicDest, elem)
      ) //we can also pass partition and key
      .runWith(Producer.plainSink(producerSettings))

    Await.ready(done, 3.second)

  }

  override def afterAll() =
    system.terminate()
}
