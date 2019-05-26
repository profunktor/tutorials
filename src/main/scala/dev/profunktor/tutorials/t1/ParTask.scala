package dev.profunktor.tutorials.t1

import cats.Traverse
import cats.effect._
import cats.effect.concurrent.Deferred
import cats.effect.implicits._
import cats.implicits._
import cats.temp.par._

object ParTask {

  /**
    * Runs N computations concurrently and either waits for all of them to complete or just return as
    * soon as any of them fail while keeping the remaining computations running in the background
    * just for their effects.
    * */
  def parFailFast[F[_]: Concurrent: Par, G[_]: Traverse, A](gfa: G[F[A]]): F[G[A]] = {
    val handler: PartialFunction[Throwable, F[A]] = {
      case t: Throwable => Sync[F].delay(println(s"Error: $t")) *> Sync[F].raiseError(t)
    }
    parFailFastWithHandler[F, G, A](gfa, handler)
  }

  private def parFailFastWithHandler[F[_]: Concurrent: Par, G[_]: Traverse, A](
      gfa: G[F[A]],
      handler: PartialFunction[Throwable, F[A]]
  ): F[G[A]] =
    gfa.parTraverse { fa =>
      Deferred[F, Either[Throwable, A]].flatMap { d =>
        fa.recoverWith(handler).attempt.flatMap(d.complete).start *> d.get.rethrow
      }
    }

  def parFailFastWithHandlerAlt[F[_]: Concurrent: Par, G[_]: Traverse, A](
      gfa: G[F[A]],
      handler: PartialFunction[Throwable, F[A]]
  ): F[G[A]] =
    gfa.parTraverse { fa =>
      fa.recoverWith(handler).uncancelable
    }

}
