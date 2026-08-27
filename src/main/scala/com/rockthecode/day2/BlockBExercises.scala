package com.rockthecode.day2

/**
 * Day 2, Block B - your workspace.
 *
 * Briefs are the scaladoc on `BlockB`.
 *
 *   sbt blockB
 */
object BlockBExercises extends BlockB {

  // B.1 Folds are everything - foldLeft and foldRight only
  def myLength[A](list: List[A]): Int = ???

  def myReverse[A](list: List[A]): List[A] = ???

  def myMap[A, B](list: List[A])(f: A => B): List[B] = ???

  def myFilter[A](list: List[A])(predicate: A => Boolean): List[A] = ???

  // B.2 The three traps
  def safeSum(list: List[Int]): Int = ???

  def safeMax(list: List[Int]): Option[Int] = ???

  def myFoldRight[A, B](list: List[A], seed: B)(op: (A, B) => B): B = ???

  // B.3 Run-length encoding
  def encode[A](list: List[A]): List[(A, Int)] = ???

  def decode[A](runs: List[(A, Int)]): List[A] = ???

  // B.4 The longest increasing run
  def longestIncreasingRun(numbers: List[Int]): List[Int] = ???

  // B.5 Word frequency
  def topWords(text: String, n: Int): List[(String, Int)] = ???

  // ===========================================================================
  //
  //  STRETCH SECTION - only if you finished the block above.
  //
  // ===========================================================================

  def foldLeftViaFoldRight[A, B](list: List[A], seed: B)(op: (B, A) => B): B = ???

  def myGroupBy[A, K](list: List[A])(key: A => K): Map[K, List[A]] = ???

  def myPartition[A](list: List[A])(predicate: A => Boolean): (List[A], List[A]) = ???

  def isBalanced(text: String): Boolean = ???

  def unfold[S, A](seed: S)(step: S => Option[(A, S)]): List[A] = ???

  def fibonacci(count: Int): List[BigInt] = ???
}
