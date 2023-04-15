package circuitbreaker.model

import scala.concurrent.duration.Duration

final case class CircuitBreakerPolicy(failureThreshold: Int, successThreshold: Int, timeout: Duration)
