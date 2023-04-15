import cats.effect._
import cats.effect.kernel.Resource
import cats.syntax.all._
import circuitbreaker.CircuitBreaker
import circuitbreaker.model.CircuitBreakerPolicy
import circuitbreaker.model.error.ServiceUnavailable
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import retry.model.RetryPolicy
import retry.Retry
import retry.RetryOps._

import java.time.LocalDateTime
import scala.concurrent.duration._
import cats.effect.std.Random

object Main extends IOApp.Simple {

  given Logger[IO]     = Slf4jLogger.getLogger[IO]
  given IO[Random[IO]] = Random.scalaUtilRandom[IO]

  def hello(implicit client: Client[IO]): IO[Unit] =
    client
      .expect[String]("http://localhost:3000/hello")
      .flatMap(IO.println)

  def printHello(circuitBreaker: CircuitBreaker[IO])(using client: Client[IO]): IO[Unit] =
    (circuitBreaker.status >>= IO.println) >>
      circuitBreaker
        .run(hello)
        .handleErrorWith { case e: ServiceUnavailable =>
          IO.println(s"Service unavailable: ${e.source.getMessage()}")
        }

  def repeatHello(time: LocalDateTime, circuitBreaker: CircuitBreaker[IO])(using client: Client[IO]): IO[Unit] =
    (for {
      _ <- printHello(circuitBreaker)
      _ <- IO.sleep(1.second)
    } yield ()).untilM_(IO(LocalDateTime.now().isAfter(time.plusMinutes(1))))

  val run: IO[Unit] = clientResource().use { c =>
    val policy = CircuitBreakerPolicy(
      failureThreshold = 5,
      successThreshold = 3,
      timeout = Duration("30 seconds")
    )

    given Client[IO] = c
    given Retry[IO]  = Retry.instance[IO]

    for {
      circuitBreaker <- CircuitBreaker.instance[IO](policy)
      currentTime    <- IO(LocalDateTime.now())
      retryPolicy = RetryPolicy.withExponentialBackoff(50)
      // _ <- repeatHello(currentTime, circuitBreaker)
      _ <- hello.retry(retryPolicy)
    } yield ()
  }

  def clientResource(): Resource[IO, Client[IO]] = EmberClientBuilder
    .default[IO]
    .build

}
