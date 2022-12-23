import akka.actor
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.{Producer, SendProducer}
import akka.stream.scaladsl.Source
import com.fasterxml.jackson.databind.JsonNode
import io.temporal.activity.{ActivityInterface, ActivityMethod}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

@ActivityInterface
trait KafkaActivities {
  @ActivityMethod
  def produceNode(token: String, jsonNode: JsonNode): Unit

  @ActivityMethod
  def produceBatchNode(token: String, jsonNode: JsonNode): Unit
}

class KafkaActivitiesImpl extends KafkaActivities {
  implicit val system: ActorSystem = actor.ActorSystem()

  override def produceNode(token: String, jsonNode: JsonNode): Unit = {
    val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers("localhost:9092")
    val producer = SendProducer(producerSettings)

    val message = new ProducerRecord[String, String]("inbound", token, jsonNode.toString)

    producer.send(message)
  }

  override def produceBatchNode(token: String, jsonNode: JsonNode): Unit = {
    val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers("localhost:9092")

    Await.result(Source(jsonNode.elements().asScala.toList)
      .map(node => new ProducerRecord[String, String]("inbound", token, node.toString))
      .runWith(Producer.plainSink(producerSettings)), 10.minutes)
  }
}
