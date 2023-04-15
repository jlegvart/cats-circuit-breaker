package circuitbreaker

import cats.syntax.all._
import circuitbreaker.model.CircuitBreakerStatus
import cats.Monad
import circuitbreaker.model.CircuitBreakerPolicy
import cats.effect.kernel.Ref
import circuitbreaker.model.CircuitBreakerStatus.Closed
import circuitbreaker.model.CircuitBreakerStatus.HalfOpen
import circuitbreaker.model.CircuitBreakerStatus.Open
import cats.MonadThrow
import java.time.LocalDateTime
import cats.effect.kernel.Sync
import circuitbreaker.model.error.ServiceUnavailable

trait CircuitBreaker[F[_]] {

  def run[A](call: => F[A]): F[A]
  def status: F[CircuitBreakerStatus]

}

object CircuitBreaker {

  def instance[F[_]: Sync](
    policy: CircuitBreakerPolicy
  ): F[CircuitBreaker[F]] =
    Ref.of[F, CircuitBreakerStatus](CircuitBreakerStatus.init).map { state =>
      new CircuitBreaker[F] {

        override def status: F[CircuitBreakerStatus] = state.get

        override def run[A](call: => F[A]): F[A] =
          status.flatMap {
            case open: Open         => handleOpen(call, open)
            case closed: Closed     => handleClosed(call, closed)
            case halfOpen: HalfOpen => handleHalfOpen(call, halfOpen)
          }

        private def handleOpen[A](call: => F[A], open: Open): F[A] =
          for {
            currentTime <- getTime
            result <-
              if (open.timeoutExpired(currentTime))
                state.set(HalfOpen.init) >> run(call)
              else
                MonadThrow[F].raiseError[A](ServiceUnavailable(open.err))
          } yield result

        private def handleClosed[A](call: => F[A], closed: Closed): F[A] =
          call.handleErrorWith { e =>
            for {
              currentTime <- getTime
              result      <- setStatusAndRaiseUnavailable[A](closed.actionFailed(currentTime, policy, e), e)
            } yield result
          }

        private def handleHalfOpen[A](call: => F[A], halfOpen: HalfOpen): F[A] =
          call
            .flatMap { result =>
              state.set(halfOpen.actionSucceeded(policy)) >> result.pure[F]
            }
            .handleErrorWith { e =>
              for {
                currentTime <- getTime
                result      <- setStatusAndRaiseUnavailable[A](halfOpen.actionFailed(currentTime, policy, e), e)
              } yield result
            }

        private def setStatusAndRaiseUnavailable[A](
          nextStatus: CircuitBreakerStatus,
          e: Throwable
        ): F[A] =
          state.set(nextStatus) >> MonadThrow[F].raiseError[A](ServiceUnavailable(e))

        private def getTime: F[LocalDateTime] =
          Sync[F].delay(LocalDateTime.now())
      }

    }

}
