package monix

import cats.effect.Resource
import monix.eval.Task
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.Serializer

import java.util.Properties

object KafkaProducerResource {

  def apply[K, V](
                   producerProps: Properties,
                   keySerializer: Serializer[K],
                   valueSerializer: Serializer[V]): Resource[Task, KafkaProducer[K, V]] = {
    Resource.make {
      Task(new KafkaProducer[K, V](producerProps, keySerializer, valueSerializer))
    }(producer => Task(producer.close()).executeAsync)
  }
}