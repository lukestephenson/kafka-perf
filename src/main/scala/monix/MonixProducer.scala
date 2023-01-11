package monix

import monix.eval.Task
import org.apache.kafka.clients.producer.{Callback, Producer, ProducerRecord, RecordMetadata}

import scala.concurrent.{Promise, blocking}

/**
 * This helper exposes publishing to Kafka as a `Task[Task[RecordMetadata]]`. Here is a breakdown of that signature:
 *
 * - '''Task['''Task[RecordMetadata]''']''' - The outer `Task` represents the side effect of handing the `ProducerRecord` to the `KafkaProducer`. This is a side effect as it can potentially block and the `KafkaProducer` buffers messages together. But it indicates that the `ProducerRecord` has been given to the `KafkaProducer` (which is the important bit for ordering to be preserved).
 * - Task['''Task'''[RecordMetadata''']'''] - The inner `Task` actually waits for the completion of the publishing to Kafka. If there was a failure on the brokers, it would be conveyed on this `Task`.
 * - Task[Task['''RecordMetadata''']] - The `RecordMetadata` is that returned by the `KafkaProducer` indicating the offset of the published message (and other metadata).
 *
 * If you do not care about the distinction between handing off to the `KafkaProducer` and the receipt of the messages on the Kafka brokers, use `sendAndAwait` which returns a Task[RecordMetadata]`
 *
 * Inspired by the fs2-kafka implementation.
 * https://github.com/fd4s/fs2-kafka/blob/904dddcfbb6e4706df7576ea4adbb65549d43c8a/modules/core/src/main/scala/fs2/kafka/KafkaProducer.scala#L180-L201
 */
object MonixProducer {
  def send[K, V](producer: Producer[K, V], record: ProducerRecord[K, V]): Task[Task[RecordMetadata]] = {
    Task.delay(Promise[RecordMetadata]()).map { promise =>
      blocking {
        producer.send(
          record,
          new Callback {
            override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
              if (exception == null) promise.success(metadata)
              else promise.failure(exception)

            }
          }
        )
        Task.deferFuture(promise.future)
      }
    }
  }
}