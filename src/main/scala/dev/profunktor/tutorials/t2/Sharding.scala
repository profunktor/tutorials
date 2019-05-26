package dev.profunktor.tutorials.t2

import cats.effect._
import cats.implicits._
import fs2._
import fs2.concurrent.Queue

object Sharding {

  def putStrLn[F[_]: Sync, A](a: A): F[Unit] = Sync[F].delay(println(a))

  def sharded[F[_]: Concurrent, A](shards: Int, idOf: A => Int, action: Int => A => F[Unit])(
      source: Stream[F, A]
  ): Stream[F, Unit] =
    Stream.eval(Queue.bounded[F, A](500).replicateA(shards)).map(qs => List.range(0, shards).zip(qs).toMap).flatMap {
      kvs =>
        source.flatMap { n =>
          val q = kvs(idOf(n) % shards)
          Stream.eval(q.enqueue1(n)).concurrently(q.dequeue.evalMap(action(idOf(n) % shards)))
        }
    }

}
