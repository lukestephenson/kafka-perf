import cats.effect.ExitCode
import com.zendesk.gourmand.resources.KafkaProducerResource
import com.zendesk.gourmand.{GourmandConfig, Producer}
import monix.eval.{Task, TaskApp}
import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.Serdes.ByteArraySerde
import Common._

object MonixKafkaPerf extends TaskApp {
  override def run(args: List[String]): Task[ExitCode] = {
    monixProduce().as(ExitCode.Success)
  }

  def monixProduce(): Task[Unit] = {
    KafkaProducerResource(GourmandConfig.apply(config), new ByteArraySerde().serializer(), new ByteArraySerde().serializer()).use { producer =>
      val message = new ProducerRecord[Array[Byte], Array[Byte]]("escape.heartbeat", "test message".getBytes)
      val publish: Task[List[Task[RecordMetadata]]] = Task.traverse((1 to messages).toList) { _ => Producer.send(producer, message) }
      val wait: Task[List[RecordMetadata]] = publish.flatMap(jobs => Task.traverse(jobs)(identity))
      val job1 = wait.timed.flatMap(result => Task(System.out.println(s"Task Took unchunked ${result._1.toMillis}")))

      job1 >> job1 >> job1 >> job1 >> job1
    }
  }
}
