package circuitbreaker.model

import circuitbreaker.CircuitBreaker
import java.time.LocalDateTime
import circuitbreaker.model.CircuitBreakerStatus.Closed
import circuitbreaker.model.CircuitBreakerStatus.HalfOpen
import circuitbreaker.model.CircuitBreakerStatus.Open
import java.time.temporal.ChronoUnit

sealed trait CircuitBreakerStatus extends Product with Serializable

object CircuitBreakerStatus {

  final case class Closed(failed: Int) extends CircuitBreakerStatus {

    def actionFailed(current: LocalDateTime, policy: CircuitBreakerPolicy, err: Throwable): CircuitBreakerStatus =
      if (failed >= policy.failureThreshold)
        Open.withCurrentTime(current, policy, err)
      else
        Closed(failed + 1)

  }

  final case class HalfOpen(succeeded: Int) extends CircuitBreakerStatus {

    def actionSucceeded(policy: CircuitBreakerPolicy): CircuitBreakerStatus =
      if (succeeded >= policy.successThreshold)
        Closed(0)
      else
        HalfOpen(succeeded + 1)

    def actionFailed(current: LocalDateTime, policy: CircuitBreakerPolicy, err: Throwable): CircuitBreakerStatus =
      Open.withCurrentTime(current, policy, err)

  }

  final case class Open(timeoutEnd: LocalDateTime, err: Throwable) extends CircuitBreakerStatus {

    def timeoutExpired(current: LocalDateTime): Boolean =
      current.isAfter(timeoutEnd)

  }

  def init: CircuitBreakerStatus = Closed(0)

  object Open {

    def withCurrentTime(current: LocalDateTime, policy: CircuitBreakerPolicy, err: Throwable): Open =
      new Open(current.plus(policy.timeout.toMillis, ChronoUnit.MILLIS), err)

  }

  object HalfOpen {

    def init: CircuitBreakerStatus =
      HalfOpen(0)

  }
}
