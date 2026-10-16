package com.rockthecode.day2.solutions

import com.rockthecode.day2.BlockF

import scala.concurrent.duration.FiniteDuration

/** Day 2, Block F - reference solutions.
  */
object BlockFSolutions extends BlockF {

  // ---------------------------------------------------------------------------
  // F.1 By-name parameters
  // ---------------------------------------------------------------------------

  def timed[A](block: => A): (A, Long) = {
    val startedAt = System.nanoTime()
    val result = block
    (result, (System.nanoTime() - startedAt) / 1000000)
  }

  // Delete the `=>` from the signature and run the test: the block executes at
  // the call site, `timed` receives a finished value, and it reports 0ms.

  def slowQueryLog(elapsedMillis: Long, thresholdMillis: Long, message: => String): Option[String] =
    if (elapsedMillis >= thresholdMillis) Some(message) else None

  // Every logging API in every language has this signature, and the ones in
  // languages without by-name parameters make you write the guard by hand at
  // every call site:
  //
  //   if (log.isDebugEnabled) log.debug(expensiveString)

  def once[A](compute: => A): () => A = {
    lazy val value = compute
    () => value
  }

  //   () => compute            recomputes every call. A by-name parameter is
  //                            the EXPRESSION, substituted at each use.
  //   val value = compute      computes right now, whether or not anyone ever
  //                            calls the result.
  //
  // `lazy val` is the only one that is both deferred and remembered, and that
  // is the row of the table people reach past:
  //
  //   val        once, now
  //   lazy val   once, on first use, then remembered
  //   def / =>   every single use

  // ---------------------------------------------------------------------------
  // F.2 Views
  // ---------------------------------------------------------------------------

  def firstMatching[A, B](items: List[A])(f: A => Option[B]): Option[B] =
    items.view.flatMap(f).headOption

  def firstPositive(items: List[Int], count: Int)(transform: Int => Int): List[Int] =
    items.view.map(transform).filter(_ > 0).take(count).toList

  // ---------------------------------------------------------------------------
  // F.3 LazyList
  // ---------------------------------------------------------------------------

  def naturals: LazyList[Int] = LazyList.from(0)

  def fibonacci: LazyList[BigInt] = {
    def from(current: BigInt, next: BigInt): LazyList[BigInt] =
      current #:: from(next, current + next)

    from(0, 1)
  }

  // WHY THIS IS NOT INFINITE RECURSION, because it certainly looks like it.
  //
  // `#::` ends in a colon, so it is right-associative: `current #:: from(...)`
  // means `from(...).#::(current)`, and the tail is the RECEIVER. Evaluating a
  // receiver in order to call a method on it would indeed never return.
  //
  // What saves it is that there is no `#::` method on LazyList at all. There is
  // an implicit conversion:
  //
  //   implicit def toDeferrer[A](l: => LazyList[A]): Deferrer[A]
  //
  // ...and it takes the tail BY NAME. So the recursive call is captured
  // unevaluated on its way to becoming the receiver, and nothing recurses until
  // somebody asks for the tail. The head is by-name too, which is what SF4
  // relies on.

  def primes: LazyList[Int] = {
    def sieve(candidates: LazyList[Int]): LazyList[Int] =
      candidates.head #:: sieve(candidates.tail.filter(_ % candidates.head != 0))

    sieve(LazyList.from(2))
  }

  // "the first candidate is prime, and the rest of
  // the primes are the sieve of every later candidate it does not divide."

  // ---------------------------------------------------------------------------
  // F.4 Building your own
  // ---------------------------------------------------------------------------

  def unfoldLazy[S, A](seed: S)(step: S => Option[(A, S)]): LazyList[A] =
    step(seed) match {
      case Some((value, nextSeed)) => value #:: unfoldLazy(nextSeed)(step)
      case None => LazyList.empty
    }

  def collatz(start: Int): LazyList[Int] =
    unfoldLazy(Option(start)) {
      case None => None
      case Some(1) => Some((1, None))
      case Some(value) => Some((value, Some(if (value % 2 == 0) value / 2 else 3 * value + 1)))
    }

  // ---------------------------------------------------------------------------
  // F.5 Backoff
  // ---------------------------------------------------------------------------

  def backoffSchedule(initial: FiniteDuration, factor: Int, cap: FiniteDuration): LazyList[FiniteDuration] =
    LazyList.iterate(initial.min(cap))(previous => (previous * factor).min(cap))

  // ===========================================================================
  //
  //  STRETCH SECTION
  //
  // ===========================================================================

  def memoize[A, B](compute: A => B): A => B = {
    val cache = scala.collection.mutable.Map.empty[A, B]
    argument => cache.getOrElseUpdate(argument, compute(argument))
  }

  def pascal: LazyList[List[Int]] =
    LazyList.iterate(List(1))(row => (0 +: row).zip(row :+ 0).map(_ + _))

  def iterateUntilStable[A](start: A)(next: A => A): A = {
    val steps = LazyList.iterate(start)(next)

    steps.zip(steps.tail).collectFirst { case (current, following) if current == following => current }.get
  }

  def hamming: LazyList[Int] = {
    def merge(left: LazyList[Int], right: LazyList[Int]): LazyList[Int] =
      if (left.head < right.head) left.head #:: merge(left.tail, right)
      else if (left.head > right.head) right.head #:: merge(left, right.tail)
      else left.head #:: merge(left.tail, right.tail) // equal: emit once, drop both

    lazy val values: LazyList[Int] =
      1 #:: merge(values.map(_ * 2), merge(values.map(_ * 3), values.map(_ * 5)))

    values
  }

}
