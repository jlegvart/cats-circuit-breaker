package retry.model.error

final case class MaxAttemptsReached(retries: Int) extends Exception
