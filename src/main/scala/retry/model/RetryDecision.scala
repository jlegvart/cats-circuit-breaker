package retry.model

import scala.concurrent.duration.Duration

sealed trait RetryDecision extends Product with Serializable

object RetryDecision {

  case object GiveUp                   extends RetryDecision
  case class TryAgain(delay: Duration) extends RetryDecision

}
