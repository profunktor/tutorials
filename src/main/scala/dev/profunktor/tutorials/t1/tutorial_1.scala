package dev.profunktor.tutorials.t1

import cats.effect._
import cats.effect.concurrent.Deferred
import cats.implicits._
import scala.concurrent.duration._
import scala.util.Random
import scala.util.control.NoStackTrace
import java.util.concurrent.TimeUnit

object tutorial_1 extends IOApp {

  case class Fail(value: String) extends NoStackTrace {
    override def toString(): String = s"Fail($value)"
  }

  def putStrLn[A](a: A): IO[Unit] = IO.sleep(150.millis) *> IO(println(a))

  // Simulates expensive computations
  def randomPutStrLn[A](a: A): IO[Unit] =
    IO(Random.nextInt(10)).flatMap {
      case n if n % 2 == 0 => IO.sleep(Duration(n.toLong, TimeUnit.SECONDS)) *> putStrLn(a)
      case n => IO.sleep(Duration(n.toLong, TimeUnit.SECONDS)) *> IO.raiseError(Fail(s"n=$n, a=$a"))
    }

  val list = List.range(1, 11)

  def handler[A]: PartialFunction[Throwable, IO[A]] = {
    case e: Fail => putStrLn(e) *> IO.raiseError(e)
  }

  def parFailFast[A](gfa: List[IO[A]]): IO[List[A]] =
    gfa.parTraverse(_.recoverWith(handler).uncancelable)

  def parFailFastAlt[A](gfa: List[IO[A]]): IO[List[A]] =
    gfa.parTraverse { fa =>
      Deferred[IO, Either[Throwable, A]].flatMap { d =>
        fa.recoverWith(handler).attempt.flatMap(d.complete).start *> d.get.rethrow
      }
    }

  def run(args: List[String]): IO[ExitCode] =
    parFailFast(list.map(randomPutStrLn))
      .handleErrorWith {
        case e: Fail => putStrLn(s"First failure: $e")
      }
      .as(ExitCode.Success)

}
