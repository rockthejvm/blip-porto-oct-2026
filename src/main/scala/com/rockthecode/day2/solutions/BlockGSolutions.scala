package com.rockthecode.day2.solutions

import com.rockthecode.day2.*

import java.util.concurrent.{ConcurrentHashMap, TimeUnit, TimeoutException}
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}

/** Day 2, Block G - reference solutions.
  */
object BlockGSolutions extends BlockG {

  import scala.concurrent.ExecutionContext.Implicits.global

  // ---------------------------------------------------------------------------
  // G.1
  // ---------------------------------------------------------------------------

  def sumThree(first: => Future[Int], second: => Future[Int], third: => Future[Int]): Future[Int] = {
    val started1 = first
    val started2 = second
    val started3 = third

    for {
      a <- started1
      b <- started2
      c <- started3
    } yield a + b + c
  }

  // ---------------------------------------------------------------------------
  // G.2 Fanning out
  // ---------------------------------------------------------------------------

  def fetchAll[A, B](ids: List[A])(fetch: A => Future[B]): Future[List[B]] =
    ids.foldRight(Future.successful(List())) { (e, acc) =>
      for {
        r <- fetch(e)
        list <- acc
      } yield r :: list
    }

  // `Future.sequence(ids.map(fetch))` is equivalent and one step longer.

  def fetchAllSettled[A, B](ids: List[A])(fetch: A => Future[B]): Future[List[Either[Throwable, B]]] =
    Future.traverse(ids) { id =>
      fetch(id)
        .map(result => Right(result): Either[Throwable, B])
        .recover { case error => Left(error) }
    }

  // ---------------------------------------------------------------------------
  // G.3 When it goes wrong
  // ---------------------------------------------------------------------------

  def fetchOrDefault[A](fetch: => Future[A], fallback: A): Future[A] =
    fetch.recover { case _ => fallback }

  def fetchWithBackup[A](primary: => Future[A], backup: => Future[A]): Future[A] =
    primary.recoverWith { case _ => backup }

  // `recover` gives a VALUE, `recoverWith` gives another FUTURE. It is exactly
  // `map` versus `flatMap`, on the failure side

  def describeOutcome(future: Future[Int]): Future[String] =
    future.transform {
      case Success(value) => Success(s"ok: $value")
      case Failure(error) => Success(s"failed: ${error.getMessage}")
    }

  // `transform` sees the `Try` and produces a `Try`, so it can do both sides at
  // once and can turn a failure into a success - which is what makes the
  // returned Future one that never fails.

  // ---------------------------------------------------------------------------
  // G.4 Promise
  // ---------------------------------------------------------------------------

  def fetchAsFuture(id: Int): Future[String] = {
    val promise = Promise[String]()

    LegacyClient.fetch(
      id,
      result => promise.success(result),
      error => promise.failure(error)
    )

    promise.future
  }

  // Five lines unlock an entire legacy codebase.

  def delayed[A](delay: FiniteDuration)(value: => A): Future[A] = {
    val promise = Promise[A]()
    val task: Runnable = () => promise.complete(Try(value))

    Clock.scheduler.schedule(task, delay.toMillis, TimeUnit.MILLISECONDS)

    promise.future
  }

  def withTimeout[A](future: Future[A], after: FiniteDuration): Future[A] =
    Future.firstCompletedOf(
      List(
        future,
        delayed(after)(throw new TimeoutException(s"timed out after $after"))
      )
    )

  // ---------------------------------------------------------------------------
  // G.5 Not all at once
  // ---------------------------------------------------------------------------

  def batched[A, B](ids: List[A], batchSize: Int)(fetch: A => Future[B]): Future[List[B]] =
    ids
      .grouped(math.max(batchSize, 1))
      .foldLeft(Future.successful(List.empty[B])) { (accumulated, group) =>
        for {
          done <- accumulated // wait for the previous group...
          next <- Future.traverse(group)(fetch) // ...then run this one, all together
        } yield done ++ next
      }

  // ===========================================================================
  //
  //  STRETCH SECTION
  //
  // ===========================================================================

  def firstSuccessOf[A](futures: List[Future[A]]): Future[A] = {
    val promise = Promise[A]()
    val remaining = new AtomicInteger(futures.size)

    if (futures.isEmpty) promise.failure(new NoSuchElementException("no futures to race"))
    else
      futures.foreach { future =>
        future.onComplete {
          case Success(value) => promise.trySuccess(value)
          case Failure(error) => if (remaining.decrementAndGet() == 0) promise.tryFailure(error)
        }
      }

    promise.future
  }

  def retryWithBackoff[A](delays: LazyList[FiniteDuration], attempts: Int)(action: () => Future[A]): Future[A] =
    action().recoverWith {
      case error if attempts <= 1 => Future.failed(error)
      case _ =>
        delayed(delays.head)(())
          .flatMap(_ => retryWithBackoff(delays.tail, attempts - 1)(action))
    }

  def foldSequentially[A, B](items: List[A], zero: B)(step: (B, A) => Future[B]): Future[B] =
    items.foldLeft(Future.successful(zero)) { (accumulated, item) =>
      accumulated.flatMap(previous => step(previous, item))
    }

  def memoizeFuture[A, B](compute: A => Future[B]): A => Future[B] = {
    val cache = new ConcurrentHashMap[A, Future[B]]()

    key => cache.computeIfAbsent(key, _ => compute(key))
  }

}
