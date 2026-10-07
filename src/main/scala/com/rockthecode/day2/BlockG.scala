package com.rockthecode.day2

import java.util.concurrent.{Executors, ScheduledExecutorService, ThreadFactory}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.Future

/**
 * Day 2, Block G - Futures and concurrency.
 *
 */
trait BlockG {

  /**
   * G.1 The sum of all three.
   */
  def sumThree(first: => Future[Int], second: => Future[Int], third: => Future[Int]): Future[Int]

  // ---------------------------------------------------------------------------
  // G.2 Fanning out
  // ---------------------------------------------------------------------------

  /**
   * Fetch every id, all at once, and give back the results in the ORIGINAL
   * order.
   *
   * If any one of them fails, the whole thing fails.
   *
   */
  def fetchAll[A, B](ids: List[A])(fetch: A => Future[B]): Future[List[B]]

  /**
   * The same fan-out, except that one failure must not sink the batch: every id
   * comes back as either its error or its result, in the original order, and
   * the returned Future always succeeds.
   */
  def fetchAllSettled[A, B](ids: List[A])(fetch: A => Future[B]): Future[List[Either[Throwable, B]]]

  // ---------------------------------------------------------------------------
  // G.3 Dealing with failure
  // ---------------------------------------------------------------------------

  /** The value, or `fallback` if the Future failed.  */
  def fetchOrDefault[A](fetch: => Future[A], fallback: A): Future[A]

  /**
   * The primary result, or, if it failedr, whatever the backup gives, which
   * might also fail.
   *
   */
  def fetchWithBackup[A](primary: => Future[A], backup: => Future[A]): Future[A]

  /**
   * Describe a Future, as a Future that always succeeds.
   *
   * `"ok: 42"` for a success, `"failed: boom"` for a failure with the
   * message "boom".
   */
  def describeOutcome(future: Future[Int]): Future[String]

  // ---------------------------------------------------------------------------
  // G.4 Promise 
  // ---------------------------------------------------------------------------

  /**
   * Wrap `LegacyClient.fetch` - a Java-shaped API that returns immediately and
   * calls one of two callbacks later, on a thread you do not control - in a
   * Future.
   *
   * Ids below zero fail. The Future must fail with the same exception the
   * callback was given, not a new one.
   */
  def fetchAsFuture(id: Int): Future[String]

  /**
   * A Future that completes with `value` after `delay`.
   *
   */
  def delayed[A](delay: FiniteDuration)(value: => A): Future[A]

  /**
   * Give a Future a deadline: the same result if it arrives in time, otherwise
   * a failed Future carrying a `java.util.concurrent.TimeoutException`.
   *
   */
  def withTimeout[A](future: Future[A], after: FiniteDuration): Future[A]

  // ---------------------------------------------------------------------------
  // G.5 Not all at once
  // ---------------------------------------------------------------------------

  /**
   * Fetch every id in batches of `batchSize`: everything within a batch runs
   * together, and a batch does not start until the previous one has finished.
   * Results come back in the original order.
   *
   */
  def batched[A, B](ids: List[A], batchSize: Int)(fetch: A => Future[B]): Future[List[B]]

  // ===========================================================================
  //
  //  STRETCH SECTION - only if you finished the block above.
  //
  // ===========================================================================

  /**
   * SG1. Race several already-running Futures and take the first SUCCESS.
   *
   */
  def firstSuccessOf[A](futures: List[Future[A]]): Future[A]

  /**
   * SG2. Retry an action, waiting between attempts.
   *
   */
  def retryWithBackoff[A](delays: LazyList[FiniteDuration], attempts: Int)(action: () => Future[A]): Future[A]

  /**
   * SG3. Process the items strictly one at a time, each step seeing the result
   * of the one before it.
   *
   */
  def foldSequentially[A, B](items: List[A], zero: B)(step: (B, A) => Future[B]): Future[B]

  /**
   * SG4. F.SF1's `memoize`, for something asynchronous - and now the
   * thread-safety caveat from that exercise actually matters.
   *
   */
  def memoizeFuture[A, B](compute: A => Future[B]): A => Future[B]
}

// -----------------------------------------------------------------------------
// Types
// -----------------------------------------------------------------------------

private object DaemonThreads extends ThreadFactory {
  def newThread(runnable: Runnable): Thread = {
    val thread = new Thread(runnable, "day2-block-g")
    thread.setDaemon(true)
    thread
  }
}

/**
 * A Java-like asynchronous API: it returns immediately and calls one
 * of the two callbacks later, on some thread .
 */
object LegacyClient {

  private val pool = Executors.newCachedThreadPool(DaemonThreads)

  def fetch(id: Int, onSuccess: String => Unit, onFailure: Throwable => Unit): Unit =
    pool.execute { () =>
      Thread.sleep(50)
      if (id >= 0) onSuccess(s"record-$id")
      else onFailure(new IllegalArgumentException(s"bad id: $id"))
    }
}

object Clock {

  val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(DaemonThreads)
}
