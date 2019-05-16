package dev.profunktor.tutorials

import java.util.concurrent.TimeUnit
import scalaz.zio._
import scalaz.zio.clock.Clock
import scalaz.zio.duration._
import scala.util.Random

object tutorial_1_zio extends App {

  case class Fail(value: String)

  def putStrLn[A](a: A): ZIO[Clock, Nothing, Unit] =
    ZIO.sleep(150.millis) *> ZIO.effectTotal(println(a))

  // Simulates expensive computations
  def randomPutStrLn[A](a: A): ZIO[Clock, Fail, Unit] =
    ZIO.effectTotal(Random.nextInt(10)).flatMap {
      case n if n % 2 == 0 => ZIO.sleep(Duration(n.toLong, TimeUnit.SECONDS)) *> putStrLn(a)
      case n => ZIO.sleep(Duration(n.toLong, TimeUnit.SECONDS)) *> ZIO.fail(Fail(s"n=$n, a=$a"))
    }

  val list = List.range(1, 11)

  def handler[A]: PartialFunction[Fail, ZIO[Clock, Fail, A]] = {
    case e: Fail => putStrLn(e) *> IO.fail(e)
  }

  // does not behave like the cats effect equivalent
  def parFailFastAlt[A](gfa: List[ZIO[Clock, Fail, A]]): ZIO[Clock, Fail, List[A]] =
    ZIO.foreachPar(gfa)(_.catchSome(handler).uninterruptible)

  def parFailFast[A](gfa: List[ZIO[Clock, Fail, A]]): ZIO[Clock, Fail, List[A]] =
    ZIO.foreachPar(gfa) { fa =>
      ZIO.accessM { _ =>
        Promise.make[Nothing, Either[Fail, A]].flatMap { promise =>
          fa.catchSome(handler).either.flatMap(promise.succeed).fork *>
            promise.await.flatMap(e => ZIO.accessM(_ => ZIO.fromEither(e)))
        }
      }
    }

  def run(args: List[String]): ZIO[Environment, Nothing, Int] =
    parFailFastAlt(list.map(randomPutStrLn))
      .catchAll(e => putStrLn(s"First failure: $e"))
      .map(_ => 0)

}
