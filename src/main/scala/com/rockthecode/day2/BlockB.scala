package com.rockthecode.day2

/**
 * Day 2, Block B - Collections and folds.
 *
 * Block 0 was the standard library's names. This block is what is underneath
 * them: every one of `map`, `filter`, `reverse`, `groupBy`, `partition` is a
 * fold wearing a hat. Once you can see that, you can write the operation the
 * standard library did not give you - which is most of what day 3 turns out
 * to be.
 *
 * Same rules: no `var`, no `while`, no mutable collection. And in B.1 and B.2,
 * no list methods at all beyond `foldLeft` and `foldRight` - that is the point.
 *
 * This trait is the exercise brief. Write your answers in `BlockBExercises`.
 */
trait BlockB {

  // ---------------------------------------------------------------------------
  // B.1 Folds are everything
  //
  // Reimplement four familiar methods. The ONLY list operations you may use are
  // `foldLeft` and `foldRight`, plus `::` to build. No recursion of your own, no
  // `map`, no `reverse`, no `length`.
  //
  // Do them in this order - each one teaches the next. The thing to notice is
  // direction: `foldLeft` naturally builds the answer backwards, `foldRight`
  // naturally builds it in order. Same fold, opposite grain.
  //
  // The tests run these on twenty thousand elements, so an answer that works on
  // three elements and dies on twenty thousand is not an answer.
  // ---------------------------------------------------------------------------

  def myLength[A](list: List[A]): Int

  def myReverse[A](list: List[A]): List[A]

  def myMap[A, B](list: List[A])(f: A => B): List[B]

  def myFilter[A](list: List[A])(predicate: A => Boolean): List[A]

  // ---------------------------------------------------------------------------
  // B.2 The three traps
  //
  // Short ones. Each exists because of a mistake that is invisible at n = 6 and
  // expensive at n = 6 million.
  // ---------------------------------------------------------------------------

  /**
   * The sum of a list of ints. Zero for the empty list.
   *
   * Use `fold`, and then try it with `reduce` and see what happens. `reduce`
   * cannot invent an answer out of nothing, and it cannot change the type on
   * the way through. `fold` is given a seed, so it can do both.
   */
  def safeSum(list: List[Int]): Int

  /**
   * The largest element, or None if there is nothing to compare.
   *
   * Same lesson, and the reason `maxOption` exists in the standard library
   * alongside `max`.
   */
  def safeMax(list: List[Int]): Option[Int]

  /**
   * Your own `foldRight`, and it must survive a list of two hundred thousand
   * elements.
   *
   * Write the obvious recursive version first:
   *
   *   case Nil => seed
   *   case head :: tail => op(head, myFoldRight(tail, seed)(op))
   *
   * ...and find out where it dies. (It is around five thousand.) Then run the
   * standard library's `List.foldRight` on half a million elements and watch it
   * not die.
   *
   * The lesson is not "avoid foldRight". It is that your recursion consumes a
   * stack and the library's does not - because the library does not recurse. It
   * turns the problem into one it already knows how to do safely. Do the same.
   */
  def myFoldRight[A, B](list: List[A], seed: B)(op: (A, B) => B): B

  // ---------------------------------------------------------------------------
  // B.3 Run-length encoding
  // ---------------------------------------------------------------------------

  /**
   * Collapse consecutive equal elements into (element, how many).
   *
   * `encode(List('a', 'a', 'a', 'b', 'c', 'c')) == List(('a', 3), ('b', 1), ('c', 2))`
   *
   * One fold. The accumulator has to carry the runs finished so far AND the run
   * currently being built - and there is a direction of travel that makes that
   * much less painful than the other one. Try both.
   */
  def encode[A](list: List[A]): List[(A, Int)]

  /**
   * Expand the runs back out again.
   *
   * `decode(encode(xs)) == xs`, always. The test states exactly that, which is
   * a far stronger claim than any list of examples.
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
   *
   * This is the one that bites. Three things go wrong, in this order:
   *   - the last run never gets compared, because the list ended before you
   *     noticed the run ended
   *   - the run comes out backwards
   *   - measuring "which is longer" with `.length` inside the fold quietly makes
   *     the whole thing quadratic - see B.2
   *
   * The accumulator carries four things. Give it a name.
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
   * Ties are broken alphabetically. That is not decoration: without a
   * tie-break the answer depends on hash order and is not reproducible, which
   * is a real bug and one that only shows up in production.
   *
   * Two things to get right. Splitting on `[^a-z]+` will quietly mangle
   * "não" and "coração" - this training is happening in Portugal, so use a
   * definition of "letter" that knows about the rest of the alphabet.
   * And sorting by two keys at once is one expression, not two passes.
   *
   * The whole thing is one pipeline, top to bottom. Write it as one.
   */
  def topWords(text: String, n: Int): List[(String, Int)]

  // ===========================================================================
  //
  //  STRETCH SECTION - only if you finished the block above.
  //
  //  B said every list operation is a fold. These four take that seriously:
  //  rebuilding the library's own methods, deriving one fold from the other,
  //  and then turning the whole thing around and folding *outwards*.
  //
  //  Nothing later in the day depends on any of these.
  //
  // ===========================================================================

  /**
   * SB1. `foldLeft`, written using nothing but `foldRight`.
   *
   * This is the hard one, and it is hard in an interesting way rather than a
   * fiddly way. Take five minutes to convince yourself it is impossible before
   * reading the hint.
   *
   * Hint, when you want it: `foldRight` can only build up a value from the
   * right. So do not build a value. Build a FUNCTION from the right - each step
   * wrapping the last - and then apply the whole assembled function to the seed
   * at the very end.
   *
   * It is five lines. When it clicks, it will be the best five lines of your
   * day.
   */
  def foldLeftViaFoldRight[A, B](list: List[A], seed: B)(op: (B, A) => B): B

  /**
   * SB2. `groupBy`, from scratch, with a fold.
   *
   * Same contract as the real one: elements keep their original relative order
   * inside each group.
   *
   * Watch how you accumulate each group - B.2's third trap is waiting here,
   * and this time it is hiding inside a Map.
   */
  def myGroupBy[A, K](list: List[A])(key: A => K): Map[K, List[A]]

  /**
   * SB3. `partition`, from scratch, with a fold.
   *
   * Both halves keep their original order. One direction of fold gives you that
   * for free and the other makes you work for it.
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
   *
   * A fold with a list as a stack. The genuinely interesting part: once you
   * have seen `([)]` you already know the answer, but a fold has no way to
   * stop. What do you carry forward instead? (Whatever you choose, notice that
   * you are inventing a way to say "this went wrong". That is Block E, and you
   * are about to want it.)
   */
  def isBalanced(text: String): Boolean

  /**
   * SB5. `unfold` - the mirror image of `fold`.
   *
   * A fold consumes a list and produces one value. `unfold` takes one value and
   * produces a list: at each step, `step` either hands back an element and the
   * next seed, or says None and the list ends there.
   *
   * `unfold(1)(n => if (n > 5) None else Some((n, n + 1))) == List(1, 2, 3, 4, 5)`
   *
   * It must be stack-safe - the tests generate a hundred thousand elements with
   * it. You already know the technique from day 1: `@tailrec` with an
   * accumulator.
   */
  def unfold[S, A](seed: S)(step: S => Option[(A, S)]): List[A]

  /**
   * SB6. The first `count` Fibonacci numbers, built with `unfold`.
   *
   * `fibonacci(7) == List(0, 1, 1, 2, 3, 5, 8)`, and `fibonacci(0) == Nil`.
   *
   * The seed has to carry everything the next step needs - which is more than
   * just the last number. BigInt, because this gets big fast, and the test goes
   * to 200 where a Long would have silently wrapped around into nonsense.
   *
   * When you meet `LazyList` in Block F, notice that it is this, made infinite.
   */
  def fibonacci(count: Int): List[BigInt]
}
