package com.rockthecode.day2

/**
 * Day 2, Block B - Collections and folds.
 *
 * Same rules: no `var`, no `while`, no mutable collection.
 */
trait BlockB {

  // ---------------------------------------------------------------------------
  // B.1 Folds are everything
  //
  // Reimplement familiar methods.
  // ---------------------------------------------------------------------------

  def myLength[A](list: List[A]): Int

  def myReverse[A](list: List[A]): List[A]

  def myMap[A, B](list: List[A])(f: A => B): List[B]

  def myFilter[A](list: List[A])(predicate: A => Boolean): List[A]

  // ---------------------------------------------------------------------------
  // B.2 Traps
  //
  // Short ones.
  // ---------------------------------------------------------------------------

  /**
   * The sum of a list of ints. Zero for the empty list.
   */
  def safeSum(list: List[Int]): Int

  /**
   * The largest element, or None if there is nothing to compare.
   */
  def safeMax(list: List[Int]): Option[Int]

  /**
   * Your own `foldRight`, and it must survive a list of two hundred thousand
   * elements.
   */
  def myFoldRight[A, B](list: List[A], seed: B)(op: (A, B) => B): B

  // ---------------------------------------------------------------------------
  // B.3 Run-length encoding
  // ---------------------------------------------------------------------------

  /**
   * Collapse consecutive equal elements into (element, how many).
   *
   * `encode(List('a', 'a', 'a', 'b', 'c', 'c')) == List(('a', 3), ('b', 1), ('c', 2))`
   */
  def encode[A](list: List[A]): List[(A, Int)]

  /**
   * Expand the runs back out again.
   *
   * Invariant: `decode(encode(xs)) == xs`
   */
  def decode[A](runs: List[(A, Int)]): List[A]

  // ---------------------------------------------------------------------------
  // B.4 The longest increasing run
  // ---------------------------------------------------------------------------

  /**
   * The longest run of strictly increasing consecutive elements - the run
   * itself, not its length.
   *
   * `longestIncreasingRun(List(1, 2, 1, 2, 3, 1)) == List(1, 2, 3)`
   * `longestIncreasingRun(List(5, 4, 3))          == List(5)`   // every single element is a run of one
   * `longestIncreasingRun(Nil)                    == Nil`
   *
   * On a tie, the earliest run wins.
   */
  def longestIncreasingRun(numbers: List[Int]): List[Int]

  // ---------------------------------------------------------------------------
  // B.5 Word frequency
  // ---------------------------------------------------------------------------

  /**
   * The `n` most frequent words in a piece of text, most frequent first.
   *
   * Words are runs of letters; everything else separates them. Case is
   * irrelevant - "The" and "the" are the same word.
   *
   * Ties are broken alphabetically. 
   */
  def topWords(text: String, n: Int): List[(String, Int)]

  // ===========================================================================
  //
  //  STRETCH SECTION - only if you finished the block above.
  //
  // ===========================================================================

  /**
   * SB1. `foldLeft`, using only `foldRight`.
   *
   * This is the hard one, and it is hard in an interesting way rather than a
   */
  def foldLeftViaFoldRight[A, B](list: List[A], seed: B)(op: (B, A) => B): B

  /**
   * SB2. `groupBy`
   */
  def myGroupBy[A, K](list: List[A])(key: A => K): Map[K, List[A]]

  /**
   * SB3. `partition`
   */
  def myPartition[A](list: List[A])(predicate: A => Boolean): (List[A], List[A])

  /**
   * SB4. Are the brackets balanced?
   *
   * `()`, `[]` and `{}` must match and nest properly. Anything that is not a
   * bracket is ignored.
   *
   *   "a(b[c]{d})e" -> true
   *   "([)]"        -> false
   *   ")("          -> false
   *   ""            -> true
   */
  def isBalanced(text: String): Boolean

  /**
   * SB5. `unfold` - the mirror image of `fold`.
   *
   * It must be stack-safe. The tests generate 100000 elements with
   * it.
   */
  def unfold[S, A](seed: S)(step: S => Option[(A, S)]): List[A]

  /**
   * SB6. The first `count` Fibonacci numbers, built with `unfold`.
   *
   * `fibonacci(7) == List(0, 1, 1, 2, 3, 5, 8)`, and `fibonacci(0) == Nil`.
   */
  def fibonacci(count: Int): List[BigInt]
}
