package dev.profunktor.tutorials.t2

import cats.effect._
import cats.implicits._
import fs2._
import fs2.concurrent.Queue

object tutorial_2 extends IOApp {

  def putStrLn[A](a: A): IO[Unit] = IO(println(a))

  val src = Stream.range[IO](1, 11)

  def sharded(shards: Int, action: Int => Int => IO[Unit])(source: Stream[IO, Int]): Stream[IO, Unit] =
    Stream.eval(Queue.bounded[IO, Int](100).replicateA(shards).map(_.zipWithIndex.map(_.swap).toMap)).flatMap { kvs =>
      source.flatMap { n =>
        val q = kvs(n % shards)
        Stream.eval(q.enqueue1(n)).concurrently(q.dequeue.evalMap(action(n % shards)))
      }
    }

  val showShardAndValue: Int => Int => IO[Unit] =
    s => v => putStrLn(s"Shard: $s, Value: $v")

  def run(args: List[String]): IO[ExitCode] =
    sharded(3, showShardAndValue)(src).compile.drain.as(ExitCode.Success)

}
