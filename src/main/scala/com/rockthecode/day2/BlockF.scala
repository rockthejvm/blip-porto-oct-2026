package com.rockthecode.day2

import scala.concurrent.duration.FiniteDuration

/**
 * Day 2, Block F - Laziness.
 */
trait BlockF {

  // ---------------------------------------------------------------------------
  // F.1 By-name parameters
  // ---------------------------------------------------------------------------

  /**
   * Run a block, and report both its result and how long it took in
   * milliseconds.
   *
   * `timed { expensiveThing() }` gives `(result, 1503)`.
   *
   */
  def timed[A](block: => A): (A, Long)

  /**
   * Build a slow-query log line, but only for queries that were actually slow.
   *
   * `Some(message)` when `elapsedMillis >= thresholdMillis`, `None` otherwise -
   * and when it is None the message must never have been BUILT.
   *
   */
  def slowQueryLog(elapsedMillis: Long, thresholdMillis: Long, message: => String): Option[String]

  /**
   * Defer an expensive computation, and do it at most once.
   *
   * `val connect = once(openConnectionPool())` - nothing happens yet. The first
   * `connect()` opens the pool; every `connect()` after that returns the same
   * one without opening anything.
   *
   */
  def once[A](compute: => A): () => A

  // ---------------------------------------------------------------------------
  // F.2 Views - laziness on collections you already have
  //
  // ---------------------------------------------------------------------------

  /**
   * The first item for which `f` produces something.
   *
   * `f` must be called as few times as possible.
   */
  def firstMatching[A, B](items: List[A])(f: A => Option[B]): Option[B]

  /**
   * Apply `transform` to each item, keep the results that are strictly
   * positive, and take the first `count` of them.
   *
   */
  def firstPositive(items: List[Int], count: Int)(transform: Int => Int): List[Int]

  // ---------------------------------------------------------------------------
  // F.3 LazyLists
  //
  // ---------------------------------------------------------------------------

  /** 0, 1, 2, 3, ...  */
  def naturals: LazyList[Int]

  /**
   * 0, 1, 1, 2, 3, 5, 8, .... etc.
   *
   */
  def fibonacci: LazyList[BigInt]

  /**
   * 2, 3, 5, 7, 11, ... .
   *
   */
  def primes: LazyList[Int]

  // ---------------------------------------------------------------------------
  // F.4 Your own primitives
  // ---------------------------------------------------------------------------

  /**
   * `unfold`, but for LazyList
   *
   * `unfoldLazy(1)(n => Some((n, n + 1)))` is the naturals from 1, forever.
   * `unfoldLazy(1)(n => if (n > 3) None else Some((n, n + 1))).toList == List(1, 2, 3)`
   *
   */
  def unfoldLazy[S, A](seed: S)(step: S => Option[(A, S)]): LazyList[A]

  /**
   * The Collatz sequence from `start`, ending WITH 1.
   *
   * Halve it if it is even, otherwise triple it and add 1, and stop when you
   * reach 1.
   *
   * `collatz(6).toList == List(6, 3, 10, 5, 16, 8, 4, 2, 1)`
   * `collatz(1).toList == List(1)`
   *
   */
  def collatz(start: Int): LazyList[Int]

  // ---------------------------------------------------------------------------
  // F.5 Realistic primitives
  // ---------------------------------------------------------------------------

  /**
   * A retry schedule: how long to wait before each attempt.
   *
   * The first wait is `initial`; each one after is the previous multiplied by
   * `factor`, and none of them ever exceeds `cap`. 
   *
   * `backoffSchedule(100.millis, 2, 1.second).take(6).toList`
   *   `== List(100.millis, 200.millis, 400.millis, 800.millis, 1.second, 1.second)`
   *
   */
  def backoffSchedule(initial: FiniteDuration, factor: Int, cap: FiniteDuration): LazyList[FiniteDuration]

  // ===========================================================================
  //
  //  STRETCH SECTION - only if you finished the block above.
  //
  // ===========================================================================

  /**
   * SF1. Memoise a function: the same argument is computed once, ever.
   *
   * `val fast = memoize(slow)` - the first `fast(5)` calls `slow`, and every
   * later `fast(5)` calls nothing at all.
   */
  def memoize[A, B](compute: A => B): A => B

  /**
   * SF2. Pascal's triangle, one row at a time.
   *
   * `pascal.take(4).toList == List(List(1), List(1, 1), List(1, 2, 1), List(1, 3, 3, 1))`
   *
   */
  def pascal: LazyList[List[Int]]

  /**
   * SF3. Apply `next` over and over until the value stops changing, and return
   * it.
   *
   * `iterateUntilStable(100)(_ / 2) == 0`
   *
   * Assume it does converge; if it does not, this runs forever, which is ok.
   */
  def iterateUntilStable[A](start: A)(next: A => A): A

  /**
   * SF4. The Hamming numbers, in order, without duplicates: every positive
   * integer whose only prime factors are 2, 3 and 5.
   *
   * `hamming.take(10).toList == List(1, 2, 3, 4, 5, 6, 8, 9, 10, 12)`
   *
   */
  def hamming: LazyList[Int]
}
