package com.rockthecode.day2

/** Day 2, Block B
  */
object BlockBExercises extends BlockB {

  // B.1 Folds are everything
  def myLength[A](list: List[A]): Int =
    list.foldLeft(0)((acc, _) => acc + 1)

  def myReverse[A](list: List[A]): List[A] =
    list.foldLeft(List())((acc, e) => e :: acc)

  def myMap[A, B](list: List[A])(f: A => B): List[B] =
    list.foldRight(List())((e, acc) => f(e) :: acc)

  def myFilter[A](list: List[A])(predicate: A => Boolean): List[A] =
    list.foldRight(List())((e, acc) => if (predicate(e)) e :: acc else acc)

  // B.2 Traps
  def safeSum(list: List[Int]): Int = ???

  def safeMax(list: List[Int]): Option[Int] = ???

  def myFoldRight[A, B](list: List[A], seed: B)(op: (A, B) => B): B = ???

  // B.3 Run-length encoding
  def encode[A](list: List[A]): List[(A, Int)] =
    list.foldRight(List()) { (e, acc) =>
      acc match {
        case Nil => List((e, 1))
        case pair :: rest =>
          if (pair._1 == e) (e, pair._2 + 1) :: rest
          else (e, 1) :: acc
      }
    }

  def decode[A](runs: List[(A, Int)]): List[A] =
    runs.flatMap { case (elem, count) =>
      List.fill(count)(elem)
    }

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

  def main(a: Array[String]) = {
    val numbers = List(1, 2, 3, 4, 5, 6, 7)
    println(myLength(numbers))
    println(myReverse(numbers))
    println(myMap(numbers)(_ * 10))
    println(myFilter(numbers)(_ % 2 == 0))

  }
}
