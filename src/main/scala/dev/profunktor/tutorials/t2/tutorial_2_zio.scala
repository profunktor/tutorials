package dev.profunktor.tutorials.t2

import java.util.concurrent.TimeUnit
import scalaz.zio._
import scalaz.zio.duration.Duration

object tutorial_2_zio extends App {

  def consumeWork[A](worker: A => UIO[Unit])(workerNo: Int): UIO[(Int, Queue[A])] =
    for {
      queue <- Queue.bounded[A](5)
      _     <- queue.take.flatMap(worker).forever.fork
    } yield (workerNo, queue)

  def produceWork[A](idOf: A => Int)(queues: Map[Int, Queue[A]]): A => UIO[Boolean] =
    (a: A) => queues(idOf(a) % queues.size).offer(a)

  def partition[A](noOfShards: Int, idOf: A => Int)(worker: A => UIO[Unit]): UIO[A => UIO[Boolean]] =
    ZIO.foreach(0 until noOfShards)(consumeWork(worker)).map(_.toMap).map(produceWork(idOf))

  val numbers: List[Int] =
    List.range(1, 11)

  def printNumberAndFiber(n: Int): UIO[Unit] =
    UIO.descriptorWith(desc => console.putStrLn(s"Fiber: ${desc.id}, n = $n").provide(console.Console.Live))

  def run(args: List[String]): ZIO[Environment, Nothing, Int] =
    partition[Int](5, _ % 5)(printNumberAndFiber)
      .flatMap(f => UIO.foreach(numbers)(f))
      .delay(Duration(500, TimeUnit.MILLISECONDS)) // Wait for workers to get a chance to run
      .map(_ => 0) // Exit as normal

}
