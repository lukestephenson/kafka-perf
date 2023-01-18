import Common._
import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serde
import zio.{Chunk, Task, ZIO, ZIOAppDefault}

object ZIOKafkaPerf extends ZIOAppDefault {
  override def run = {
    val zioJob: Task[Unit] = withProducer(config) { producer =>

      val message = new ProducerRecord[Array[Byte], Array[Byte]]("escape.heartbeat", "test message".getBytes)
      val publish: Task[List[Task[RecordMetadata]]] = ZIO.foreach((1 to messages).toList) { _ => producer.produceAsync(message, Serde.byteArray, Serde.byteArray) }
      val wait: Task[List[RecordMetadata]] = publish.flatMap(jobs => ZIO.foreach(jobs)(identity))
      val job1 = wait.timed.flatMap(result => zio.Console.printLine(s"Took unchunked ${result._1.toMillis}"))

      val chunks: Chunk[ProducerRecord[Array[Byte], Array[Byte]]] = Chunk.fill(messages)(message)
      val bulkPublish = producer.produceChunkAsync(chunks, Serde.byteArray, Serde.byteArray)
      val bulkWait = bulkPublish.flatten
      val job2 = bulkWait.timed.flatMap(result => zio.Console.printLine(s"Took chunked ${result._1.toMillis}"))
      job2.repeatN(5).flatMap(_ => job1.repeatN(5))
    }

    zioJob
  }

  def withProducer[T](producerConfig: Map[String, Object])(task: Producer => Task[T]): Task[T] = {
    val settings = ProducerSettings(List.empty).withProperties(producerConfig).withSendBufferSize(1024*1024*4)
    val zioTask: Task[T] = ZIO.scoped {
      Producer.make(settings).flatMap { kafkaProducer =>
        task(kafkaProducer)
      }
    }
    zioTask
  }
}
