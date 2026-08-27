package com.rockthecode.day2

import java.util.concurrent.{Executors, ScheduledExecutorService, ThreadFactory}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.Future

/**
 * Day 2, Block G - Futures and concurrency.
 *
 * A `Future[A]` is "an A, later, or a failure". It has `map`, `flatMap` and
 * `filter`, so it has a for-comprehension, so by now you already know how to
 * use it - which is the nicest thing about the design and the reason this block
 * is last.
 *
 * What is genuinely new is that a Future is ALREADY RUNNING. `Option` and
 * `Either` describe a value you have; a Future describes work that started when
 * you wrote it down. Almost every mistake in this block comes from forgetting
 * that, or from forgetting the opposite - that a Future you have not created
 * yet has not started.
 *
 * A note on `Await`. It appears in these tests because a test has to stop and
 * look at the answer. It should not appear in your code. Blocking a thread to
 * wait for a Future gives back everything the Future bought you, and doing it
 * on the same thread pool the Future needs in order to finish is how services
 * deadlock.
 *
 * A note on ExecutionContexts, since the signatures below do not mention them.
 * Real code takes `(using ec: ExecutionContext)` and gets handed a pool chosen
 * on purpose. These exercises use the global one so the signatures stay about
 * Futures. Worth knowing before you leave: `ExecutionContext.global` is a
 * work-stealing pool sized to your CPUs, which is right for computation and
 * wrong for anything that blocks - fill it with sleeping threads and everything
 * else in the process stops, including the work that would have woken them.
 * `scala.concurrent.blocking { ... }` tells it to compensate; a separate pool
 * for blocking work is better.
 *
 * Same rules: no `var`, no `while`, no mutable collection - except where the
 * brief says otherwise, which happens twice and both times for the reason SF1
 * gave.
 *
 * This trait is the exercise brief. Write your answers in `BlockGExercises`.
 */
trait BlockG {

  // ---------------------------------------------------------------------------
  // G.1 The one that matters
  //
  // Three independent calls, each taking about a fifth of a second. Combine
  // their results.
  //
  // The obvious answer takes six tenths of a second. The right answer takes
  // two. They differ by two lines and no cleverness, and this is the single
  // most common Future bug in real Scala codebases - it is invisible in review,
  // invisible in tests that do not measure, and shows up as "why is this
  // endpoint slow".
  //
  // The parameters are by-name (Block F, still earning its keep) precisely so
  // that this mistake is possible here. In real code the same thing happens
  // when the three things are method calls: `for { a <- fetchA(); b <- fetchB() }`.
  // ---------------------------------------------------------------------------

  /**
   * The sum of all three.
   *
   * Write it with a for-comprehension first, run the test, and read the timing
   * assertion when it fails. Then work out why, and fix it. The fix is two
   * lines above the for-comprehension.
   *
   * The question to be able to answer afterwards: at what moment does a Future
   * start running?
   */
  def sumThree(first: => Future[Int], second: => Future[Int], third: => Future[Int]): Future[Int]

  // ---------------------------------------------------------------------------
  // G.2 Fanning out
  // ---------------------------------------------------------------------------

  /**
   * Fetch every id, all at once, and give back the results in the ORIGINAL
   * order - not the order they happened to arrive in.
   *
   * If any one of them fails, the whole thing fails.
   *
   * This is D.5's `traverse` with a different container, and the standard
   * library already has it under exactly that name.
   */
  def fetchAll[A, B](ids: List[A])(fetch: A => Future[B]): Future[List[B]]

  /**
   * The same fan-out, except that one failure must not sink the batch: every id
   * comes back as either its error or its result, in the original order, and
   * the returned Future always succeeds.
   *
   * E.5's `partitionResults` question, now that the answers arrive later. The
   * trick is to make each individual Future incapable of failing before you
   * combine them - once a Future cannot fail, `fetchAll` cannot fail either.
   *
   * "We fetched 9,998 of 10,000 and here are the two" is a good morning.
   */
  def fetchAllSettled[A, B](ids: List[A])(fetch: A => Future[B]): Future[List[Either[Throwable, B]]]

  // ---------------------------------------------------------------------------
  // G.3 When it goes wrong
  // ---------------------------------------------------------------------------

  /** The value, or `fallback` if the Future failed. One combinator. */
  def fetchOrDefault[A](fetch: => Future[A], fallback: A): Future[A]

  /**
   * The primary result, or - if it failed - whatever the backup gives, which
   * might itself fail.
   *
   * The difference from `fetchOrDefault` is the difference between `map` and
   * `flatMap`, in the same place and for the same reason. And note the by-name
   * parameter again: the backup call must not be made unless it is needed.
   */
  def fetchWithBackup[A](primary: => Future[A], backup: => Future[A]): Future[A]

  /**
   * Describe how it went, as a Future that always succeeds.
   *
   * `"ok: 42"` for a success, `"failed: boom"` for a failure carrying the
   * message "boom".
   *
   * `recover` handles the failure side; `map` handles the success side;
   * `transform` handles both at once, which is what this wants.
   */
  def describeOutcome(future: Future[Int]): Future[String]

  // ---------------------------------------------------------------------------
  // G.4 Promise - where Futures come from
  //
  // Everything so far consumed Futures somebody else made. A `Promise[A]` is
  // the writing end: you hand out `promise.future` and complete it later,
  // from wherever the answer eventually turns up.
  //
  // This is how a Future gets made out of something that is not one - which,
  // in a codebase with any Java in it, is most things.
  // ---------------------------------------------------------------------------

  /**
   * Wrap `LegacyClient.fetch` - a Java-shaped API that returns immediately and
   * calls one of two callbacks later, on a thread you do not control - in a
   * Future.
   *
   * Ids below zero fail. The Future must fail with the same exception the
   * callback was given, not a new one.
   *
   * This is the single most directly applicable exercise of the day for a
   * codebase with Java in it. Every callback API, every `CompletableFuture`,
   * every listener interface becomes a Future through this exact shape, and it
   * is about five lines.
   */
  def fetchAsFuture(id: Int): Future[String]

  /**
   * A Future that completes with `value` after `delay`.
   *
   * The obvious answer is `Future { Thread.sleep(delay.toMillis); value }`. It
   * works, and it occupies an entire pool thread doing nothing for the whole
   * delay. Set enough timeouts that way and the pool fills up with sleepers -
   * including, eventually, the threads that the work you are waiting for needs
   * in order to finish. Everything then times out, so you create more timeouts.
   *
   * `Clock.scheduler` (bottom of this file) will run a `Runnable` for you after
   * a delay, on a thread of its own, and costs nothing while it waits. Your job
   * is the bridge between that and a Future - which is the second Promise
   * exercise, and the same five lines as the first.
   *
   * If evaluating `value` throws, the Future must fail with that exception. A
   * scheduler thread swallowing an exception in silence is a bad afternoon.
   */
  def delayed[A](delay: FiniteDuration)(value: => A): Future[A]

  /**
   * Give a Future a deadline: the same result if it arrives in time, otherwise
   * a failed Future carrying a `java.util.concurrent.TimeoutException`.
   *
   * Built from `delayed` - the one you just wrote - plus
   * `Future.firstCompletedOf`. Two things racing, and the loser is ignored.
   *
   * Say what this does NOT do: the original Future keeps running. There is no
   * cancellation in `scala.concurrent`. You have stopped waiting, which is not
   * the same as having stopped the work, and the difference matters when the
   * work is holding a database connection.
   */
  def withTimeout[A](future: Future[A], after: FiniteDuration): Future[A]

  // ---------------------------------------------------------------------------
  // G.5 Not all at once
  //
  // `fetchAll` starts everything simultaneously, which is exactly what you want
  // for three calls and exactly what gets you rate-limited, or thrown off a
  // database connection pool, at three thousand.
  // ---------------------------------------------------------------------------

  /**
   * Fetch every id in batches of `batchSize`: everything within a batch runs
   * together, and a batch does not start until the previous one has finished.
   * Results come back in the original order.
   *
   * `foldLeft` over a `Future`, with `grouped` doing the splitting.
   *
   * Note what the name promises and what it does not. Batching BOUNDS the
   * number of calls in flight - a test checks that you never exceed
   * `batchSize` - but it does not KEEP that many in flight: a batch whose
   * slowest call takes a second leaves the other slots idle for most of that
   * second while it waits for the straggler.
   *
   * A true concurrency limiter starts a new call the moment any one finishes,
   * and needs a queue and a semaphore rather than ten lines. Batching is the
   * version you can write from memory and is usually enough; knowing precisely
   * which of the two you have built is the point of the exercise.
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
   * `Future.firstCompletedOf` takes the first to FINISH, which is a different
   * thing: a source that fails in one millisecond beats one that succeeds in
   * ten, and your fallback logic never runs. That distinction is the exercise.
   *
   * If every one of them fails, fail with the last failure to arrive.
   *
   * A `Promise` and a counter. The counter has to be shared and mutated, which
   * is SF1's lesson again - contained inside the function, invisible to the
   * caller - except that now two threads really can touch it at the same
   * moment, so an ordinary `Int` will not do. `AtomicInteger` will.
   */
  def firstSuccessOf[A](futures: List[Future[A]]): Future[A]

  /**
   * SG2. Retry an action, waiting between attempts.
   *
   * `delays` is the schedule from F.5 - infinite, so `attempts` decides when to
   * stop. `attempts` is the total number of tries, so `retryWithBackoff(d, 3)`
   * calls the action at most three times and waits twice.
   *
   * On total failure, come back with the LAST failure, as in E.SE2.
   *
   * The waiting must not block a thread, so use the `delayed` you wrote in
   * G.4. This is the exercise where three separate blocks arrive at once - F.5
   * built the schedule, E.SE2 built the retry, G.4 built the non-blocking
   * delay - and they compose without any of them knowing about the others.
   */
  def retryWithBackoff[A](delays: LazyList[FiniteDuration], attempts: Int)(action: () => Future[A]): Future[A]

  /**
   * SG3. Process the items strictly one at a time, each step seeing the result
   * of the one before it.
   *
   * The opposite of G.2, and sometimes exactly what you need: ordered writes, a
   * paginated API where each page gives you the cursor for the next, anything
   * behind a strict rate limit.
   *
   * A test asserts that no two steps were ever running at the same moment.
   *
   * `foldLeft` over a `Future` is the whole implementation, and the reason it
   * is sequential is worth stating out loud: `flatMap` cannot start the next
   * step until it has the previous value, which is the same property that made
   * E.4 unable to accumulate errors. One mechanism, two consequences.
   */
  def foldSequentially[A, B](items: List[A], zero: B)(step: (B, A) => Future[B]): Future[B]

  /**
   * SG4. F.SF1's `memoize`, for something asynchronous - and now the
   * thread-safety caveat from that exercise actually matters.
   *
   * Ten callers asking for the same uncached key at the same instant must
   * produce ONE call to `compute`, and all ten must get its result. A test does
   * exactly that and counts.
   *
   * This is the "thundering herd" or "cache stampede" problem, and it is a real
   * outage shape: a popular cache entry expires, four hundred requests all miss
   * simultaneously, and four hundred identical queries hit a database sized for
   * one.
   *
   * The insight that makes it easy: cache the FUTURE, not the value. A Future
   * for work already in progress is a perfectly good thing to hand a second
   * caller - they wanted the answer when it exists, and that is what they get.
   *
   * Then the only hard part is making "check, then insert" a single indivisible
   * step. `getOrElseUpdate` on an ordinary mutable Map is not: two threads can
   * both look, both find nothing, and both compute. A
   * `java.util.concurrent.ConcurrentHashMap` is, and you already know it from
   * Java - `computeIfAbsent` runs its function at most once per key, however
   * many threads arrive together.
   */
  def memoizeFuture[A, B](compute: A => Future[B]): A => Future[B]
}

// -----------------------------------------------------------------------------
// Fixtures
// -----------------------------------------------------------------------------

private object DaemonThreads extends ThreadFactory {
  def newThread(runnable: Runnable): Thread = {
    val thread = new Thread(runnable, "day2-block-g")
    thread.setDaemon(true)
    thread
  }
}

/**
 * A Java-shaped asynchronous API: it returns immediately and calls exactly one
 * of the two callbacks later, on some thread of its own choosing.
 *
 * Note what its signature does NOT give you: no return value to compose with,
 * no way to say "and then", and no way to wait. Every callback API in every
 * language looks like this, and G.4 is how you stop having to live with it.
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

/**
 * A scheduler, and nothing more: hand it a `Runnable` and a delay and it will
 * run it later, on a thread of its own, without anybody blocking meanwhile.
 *
 * Turning this into something that composes is G.4's `delayed`.
 */
object Clock {

  val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(DaemonThreads)
}
