import Common._
import cats.effect.ExitCode
import monix.eval.{Task, TaskApp}
import monix.{KafkaProducerResource, MonixProducer}
import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.Serdes.ByteArraySerde

import java.util.Properties

object MonixKafkaPerf extends TaskApp {
  override def run(args: List[String]): Task[ExitCode] = {
    monixProduce().as(ExitCode.Success)
  }

  def monixProduce(): Task[Unit] = {
    KafkaProducerResource(producerProps, new ByteArraySerde().serializer(), new ByteArraySerde().serializer()).use { producer =>
      val message = new ProducerRecord[Array[Byte], Array[Byte]]("escape.heartbeat", "test message".getBytes)
      val publish: Task[List[Task[RecordMetadata]]] = Task.traverse((1 to messages).toList) { _ => MonixProducer.send(producer, message) }
      val wait: Task[List[RecordMetadata]] = publish.flatMap(jobs => Task.traverse(jobs)(identity))
      val job1 = wait.timed.flatMap(result => Task(System.out.println(s"Task Took unchunked ${result._1.toMillis}")))

      job1 >> job1 >> job1 >> job1 >> job1
    }
  }

  private def producerProps: Properties = {
    val props = new Properties()
    config.foreach { case (key, value) =>
      props.put(key, value)
    }
    props
  }
}
