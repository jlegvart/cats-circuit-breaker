package circuitbreaker.model.error

final case class ServiceUnavailable(source: Throwable) extends Exception
