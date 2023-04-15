package retry

import cats.effect.IO
import retry.model.RetryPolicy

object RetryOps {
  extension [F[_], A](op: F[A]) {
    def retry(policy: RetryPolicy)(using retry: Retry[F]): F[A] =
      retry.retry(op, policy)
  }
}
