package dev.profunktor.tutorials.t2

import cats.effect._
import cats.implicits._
import fs2.concurrent.Queue

object tutorial_2_cats extends IOApp {

  def putStrLn[A](a: A): IO[Unit] = IO(println(a))

  def consumer[A](worker: A => IO[Unit])(workerNo: Int): IO[(Int, Queue[IO, A])] =
    Queue.bounded[IO, A](500).flatMap { queue =>
      queue.dequeue1.flatMap(worker).foreverM.start.as(workerNo -> queue)
    }

  def producer[A](idOf: A => Int)(queues: Map[Int, Queue[IO, A]]): A => IO[Unit] =
    (a: A) => queues(idOf(a) % queues.size).enqueue1(a)

  def partition[A](noOfShards: Int, idOf: A => Int)(worker: Int => A => IO[Unit]): IO[A => IO[Unit]] =
    List.range(0, noOfShards).traverse(s => consumer(worker(s))(s)).map(_.toMap).map(producer(idOf))

  val numbers: List[Int] =
    List.range(1, 11)

  def showShardAndValue: Int => Int => IO[Unit] =
    s => v => putStrLn(s"Shard: $s, Value: $v")

  def run(args: List[String]): IO[ExitCode] =
    partition[Int](5, _ % 5)(s => x => putStrLn(s"Shard: $s, Value: $x"))
      .flatMap(numbers.traverse(_))
      .as(ExitCode.Success)

}
