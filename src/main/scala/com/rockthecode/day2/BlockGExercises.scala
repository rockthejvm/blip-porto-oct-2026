package com.rockthecode.day2

import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.duration.FiniteDuration
import scala.util.{Try, Success, Failure}
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/** Day 2, Block G
  */
object BlockGExercises extends BlockG {

  // import scala.concurrent.ExecutionContext.Implicits.global

  val threadPool: ExecutorService = Executors.newFixedThreadPool(8)
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(threadPool)

  val aFuture: Future[Int] = Future {
    Thread.sleep(1000)
    42
  } // we allocate a thread RIGHT HERE

  // is the thread alive AT THE MOMENT OF THE CALL?
  // YES
  val fallbackv1 = fetchOrDefault(aFuture, 21)
  // NO - will be spawned INSIDE the function (at the evaluation time)
  val fallbackv2 = fetchOrDefault(
    Future {
      Thread.sleep(1000)
      42
    },
    21
  )

  // G.1
  def sumThree(first: => Future[Int], second: => Future[Int], third: => Future[Int]): Future[Int] =
    for {
      a <- first
      b <- second
      c <- third
    } yield a + b + c

  // G.2
  def fetchAll[A, B](ids: List[A])(fetch: A => Future[B]): Future[List[B]] =
    ids.foldRight(Future.successful(List())) { (e, acc) =>
      for {
        r <- fetch(e)
        list <- acc
      } yield r :: list
    }

  def fetchAllSettled[A, B](ids: List[A])(fetch: A => Future[B]): Future[List[Either[Throwable, B]]] =
    ids.foldRight(Future.successful(List())) { (e, acc) =>
      for {
        r <- fetch(e).map(success => Right(success)).recover(ex => Left(ex)) // Future[Either[Throwable, B]]
        list <- acc
      } yield r :: list
    }

  def fetchAllSettledv2[A, B](ids: List[A])(fetch: A => Future[B]): Future[List[Either[Throwable, B]]] =
    fetchAll(ids)(e => fetch(e).map(success => Right(success)).recover(ex => Left(ex)))

  // G.3
  def fetchOrDefault[A](fetch: => Future[A], fallback: A): Future[A] = {
    // assume you're using fetch multiple times
    // CBN + lazy val = call by need
    lazy val theFuture = fetch
    theFuture.recover { case _ =>
      fallback
    }
  }

  def fetchWithBackup[A](primary: => Future[A], backup: => Future[A]): Future[A] =
    primary.recoverWith { case _ => backup }

  def describeOutcome(future: Future[Int]): Future[String] =
    future.transform {
      case Success(value) => Success(value.toString)
      case Failure(_) => Success("failure: boom")
    }

  def describeOutcomeCheat(future: Future[Int]): Future[String] =
    fetchOrDefault(future.map(_.toString), "failure: boom")

  // G.4
  def fetchAsFuture(id: Int): Future[String] = {
    val promise = Promise[String]()
    // in the meantime, make sure the promise gets filled with something
    LegacyClient.fetch(id, onSuccess = result => promise.success(result), onFailure = ex => promise.failure(ex))

    promise.future
  }

  // careful that `=> A` is an expression that might throw!
  def delayed[A](delay: FiniteDuration)(value: => A): Future[A] = {
    val promise = Promise[A]()
    Clock.scheduler.schedule(() => promise.complete(Try(value)), delay.toMillis, TimeUnit.MILLISECONDS)
    promise.future
  }

  def withTimeout[A](future: Future[A], after: FiniteDuration): Future[A] = {
    val promise = Promise[A]()

    future.onComplete(result => promise.tryComplete(result))
    Clock.scheduler.schedule(
      () => promise.tryComplete(Failure(new TimeoutException)),
      after.toMillis,
      TimeUnit.MILLISECONDS
    )

    promise.future
  }

  def first[A, B](fa: Future[A], fb: Future[B]): Future[Either[A, B]] = {
    val promise = Promise[Either[A, B]]()

    fa.onComplete(result => promise.tryComplete(result.map(Left(_))))
    fb.onComplete(result => promise.tryComplete(result.map(Right(_))))

    promise.future
  }

  def first[A](lfa: List[Future[A]]): Future[A] = {
    val promise = Promise[A]()
    lfa.foreach { fa =>
      fa.onComplete(result => promise.tryComplete(result))
    }
    promise.future
  }

  // G.5
  def batched[A, B](ids: List[A], batchSize: Int)(fetch: A => Future[B]): Future[List[B]] = ???

  // ===========================================================================
  //
  //  STRETCH SECTION - only if you finished the block above.
  //
  // ===========================================================================

  def firstSuccessOf[A](futures: List[Future[A]]): Future[A] = ???

  def retryWithBackoff[A](delays: LazyList[FiniteDuration], attempts: Int)(action: () => Future[A]): Future[A] = ???

  def foldSequentially[A, B](items: List[A], zero: B)(step: (B, A) => Future[B]): Future[B] = ???

  def memoizeFuture[A, B](compute: A => Future[B]): A => Future[B] = ???
}
