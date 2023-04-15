package retry

import cats.syntax.all._
import retry.model.RetryPolicy
import cats.Monad
import cats.effect.kernel.Temporal
import retry.model.RetryStatus
import scala.concurrent.duration._
import retry.model.RetryDecision.TryAgain
import retry.model.RetryDecision.GiveUp
import cats.MonadThrow
import retry.model.error.MaxAttemptsReached
import org.typelevel.log4cats.Logger

trait Retry[F[_]] {

  def retry[A](f: F[A], policy: RetryPolicy): F[A]

}

object Retry {

  def instance[F[_]: MonadThrow: Temporal: Logger]: Retry[F] =
    new Retry[F] {

      override def retry[A](f: F[A], policy: RetryPolicy): F[A] = {
        def doRetry(status: RetryStatus): F[A] =
          Logger[F].info(s"$status") >> MonadThrow[F].attempt(f).flatMap {
            case Right(value) => value.pure[F]
            case Left(_)      => onError(status)
          }

        def onError(status: RetryStatus): F[A] =
          policy.next(status) match
            case _: GiveUp.type => MonadThrow[F].raiseError(MaxAttemptsReached(status.totalRetries))
            case TryAgain(delay) =>
              Temporal[F].sleep(delay) >> doRetry(status.add(delay))

        doRetry(RetryStatus(0, Duration.Zero))
      }

    }

}
