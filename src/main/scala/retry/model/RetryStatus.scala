package retry.model

import scala.concurrent.duration._

case class RetryStatus(totalRetries: Int, totalDelay: Duration) {

  def add(delay: Duration): RetryStatus =
    RetryStatus(totalRetries + 1, totalDelay + delay)

  override def toString(): String =
    s"total retries: $totalRetries, totalDelay: $totalDelay"
}
