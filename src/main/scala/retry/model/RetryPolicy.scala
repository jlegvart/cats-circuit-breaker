package retry.model

import retry.model.RetryDecision.TryAgain
import retry.model.RetryDecision.GiveUp
import scala.concurrent.duration._

final case class RetryPolicy private (f: RetryStatus => RetryDecision) {

  def next(status: RetryStatus): RetryDecision =
    f(status)

}

object RetryPolicy {

  def withMaxRetries(maxRetries: Int, fixDelay: Duration): RetryPolicy = RetryPolicy { status =>
    status match {
      case _ @RetryStatus(retries, _) if retries == maxRetries => GiveUp
      case _                                                   => TryAgain(fixDelay)
    }
  }

  def withExponentialBackoff(maxRetries: Int, maxBackOffDelay: Duration = 3.minutes): RetryPolicy =
    RetryPolicy { status =>
      status match {
        case RetryStatus(retries, _) if retries == maxRetries => GiveUp
        case RetryStatus(totalRetries, totalDelay) =>
          TryAgain((Math.pow(2, totalRetries).millis) min maxBackOffDelay)
      }
    }

}
