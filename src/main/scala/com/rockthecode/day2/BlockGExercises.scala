package com.rockthecode.day2

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

/**
 * Day 2, Block G 
 */
object BlockGExercises extends BlockG {

  import scala.concurrent.ExecutionContext.Implicits.global

  // G.1 
  def sumThree(first: => Future[Int], second: => Future[Int], third: => Future[Int]): Future[Int] = ???

  // G.2 
  def fetchAll[A, B](ids: List[A])(fetch: A => Future[B]): Future[List[B]] = ???

  def fetchAllSettled[A, B](ids: List[A])(fetch: A => Future[B]): Future[List[Either[Throwable, B]]] = ???

  // G.3 
  def fetchOrDefault[A](fetch: => Future[A], fallback: A): Future[A] = ???

  def fetchWithBackup[A](primary: => Future[A], backup: => Future[A]): Future[A] = ???

  def describeOutcome(future: Future[Int]): Future[String] = ???

  // G.4 
  def fetchAsFuture(id: Int): Future[String] = ???

  def delayed[A](delay: FiniteDuration)(value: => A): Future[A] = ???

  def withTimeout[A](future: Future[A], after: FiniteDuration): Future[A] = ???

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
