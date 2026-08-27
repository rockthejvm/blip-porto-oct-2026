package com.rockthecode.day2

import scala.concurrent.duration.FiniteDuration

/**
 * Day 2, Block F - your workspace.
 *
 * Briefs are the scaladoc on `BlockF`.
 *
 * Most of these tests count how many times something was evaluated, because a
 * lazy answer and an eager one return the same value. If a test fails on a
 * count rather than a value, your code is right and does too much.
 *
 *   sbt blockF
 */
object BlockFExercises extends BlockF {

  // F.1 By-name parameters
  def timed[A](block: => A): (A, Long) = ???

  def slowQueryLog(elapsedMillis: Long, thresholdMillis: Long, message: => String): Option[String] = ???

  def once[A](compute: => A): () => A = ???

  // F.2 Views
  def firstMatching[A, B](items: List[A])(f: A => Option[B]): Option[B] = ???

  def firstPositive(items: List[Int], count: Int)(transform: Int => Int): List[Int] = ???

  // F.3 LazyList
  def naturals: LazyList[Int] = ???

  def fibonacci: LazyList[BigInt] = ???

  def primes: LazyList[Int] = ???

  // F.4 Building your own
  def unfoldLazy[S, A](seed: S)(step: S => Option[(A, S)]): LazyList[A] = ???

  def collatz(start: Int): LazyList[Int] = ???

  // F.5 Something you would actually ship
  def backoffSchedule(initial: FiniteDuration, factor: Int, cap: FiniteDuration): LazyList[FiniteDuration] = ???

  // ===========================================================================
  //
  //  STRETCH SECTION - only if you finished the block above.
  //
  // ===========================================================================

  def memoize[A, B](compute: A => B): A => B = ???

  def pascal: LazyList[List[Int]] = ???

  def iterateUntilStable[A](start: A)(next: A => A): A = ???

  def hamming: LazyList[Int] = ???
}
