object Common {
  val messages = 1_000_000

  val config = Map(
    "bootstrap.servers" -> "kafka.docker:9092",
    "compression.type" -> "zstd",
    "batch.size" -> "200000",
    "linger.ms" -> "5",
    "retries" -> "30",
    "retry.backoff.ms" -> "1000",
    "max.request.size" -> "10000000",
    "enable.idempotence" -> "true",
    "acks" -> "all")
}
