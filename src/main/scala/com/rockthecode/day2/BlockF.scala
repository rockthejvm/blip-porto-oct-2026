package com.rockthecode.day2

import scala.concurrent.duration.FiniteDuration

/**
 * Day 2, Block F - Laziness.
 *
 * Everything so far has been eager: you wrote an expression, it ran. This block
 * is about the other option - describing work now and doing it later, or never.
 *
 * Three mechanisms, in increasing order of how much they change your code:
 *
 *   by-name parameters   `x: => A`. The argument is not evaluated at the call;
 *                        the EXPRESSION is passed and evaluated wherever the
 *                        parameter is used - every time it is used.
 *   lazy val             evaluated on first use, then remembered.
 *   LazyList             a list whose tail is computed on demand and then
 *                        remembered. Which is what makes an infinite one
 *                        possible.
 *
 * Two things earlier in the day pointed here and get paid off:
 *
 *   Block D, D.1   `firstAvailable` looked up every name and then took the
 *                  first answer. Free for a Map; not free for an HTTP call.
 *   Block B, SB5   `unfold` built a List from a seed. The same function with a
 *                  LazyList result can run forever, usefully.
 *
 * Testing laziness means testing what did NOT happen, so most of the tests in
 * this block count evaluations. If a test says "called 3 times" and yours calls
 * 100, your code is correct and does far too much - which is exactly the class
 * of bug this block is about.
 *
 * Same rules: no `var`, no `while`, no mutable collection.
 *
 * This trait is the exercise brief. Write your answers in `BlockFExercises`.
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
   * By-name is not a stylistic choice here, it is the difference between the
   * function working and not working. If the parameter were an ordinary value,
   * the block would run at the CALL SITE, before `timed` is even entered, and
   * you would faithfully measure the zero milliseconds it takes to hand over an
   * already-computed value. A test sleeps for 50ms and checks you measured it.
   *
   * This is also the general shape of every helper you have ever wanted:
   * `withResource`, `retry`, `withTransaction`, `assertThat`. Once arguments
   * can be by-name, control flow stops being the language's private business.
   */
  def timed[A](block: => A): (A, Long)

  /**
   * Build a slow-query log line, but only for queries that were actually slow.
   *
   * `Some(message)` when `elapsedMillis >= thresholdMillis`, `None` otherwise -
   * and when it is None the message must never have been BUILT.
   *
   * This is the one that pays for itself in production. A line like
   *
   *     log.debug(s"query $sql took $elapsed ms, plan: ${plan.render}")
   *
   * builds that string on every call whether or not anybody logs it, unless the
   * parameter is by-name. On a hot path that is a string concatenation, a
   * `plan.render`, and some garbage, per request, forever, for a line nobody
   * reads. It is one of the most common sources of "why is this service slow"
   * that profiling finds and code review does not.
   */
  def slowQueryLog(elapsedMillis: Long, thresholdMillis: Long, message: => String): Option[String]

  /**
   * Defer an expensive computation, and do it at most once.
   *
   * `val connect = once(openConnectionPool())` - nothing happens yet. The first
   * `connect()` opens the pool; every `connect()` after that returns the same
   * one without opening anything.
   *
   * Two ways to get this wrong, and the tests check both:
   *
   *   `() => compute`        recomputes on every call, because a by-name
   *                          parameter IS the expression, substituted wherever
   *                          the name appears. This is the single most common
   *                          surprise about `=>`.
   *   `val value = compute`  computes immediately, even if nobody ever calls
   *                          the result - which defeats the whole purpose when
   *                          the expensive thing is a connection to a database
   *                          this process may never need.
   *
   * Exactly one keyword does both jobs. Use it.
   */
  def once[A](compute: => A): () => A

  // ---------------------------------------------------------------------------
  // F.2 Views - laziness on collections you already have
  //
  // `.view` makes an ordinary collection lazy. The pipeline you write is the
  // same; what changes is that nothing runs until something asks for a result,
  // and then only as much as that result needs.
  //
  // Both of these are testable only by counting, because a correct answer and a
  // wasteful answer return exactly the same value.
  // ---------------------------------------------------------------------------

  /**
   * The first item for which `f` produces something.
   *
   * `f` must be called as few times as possible - once per item examined, and
   * not one item further. This is D.1's `firstAvailable`, done properly now
   * that you have the tool.
   */
  def firstMatching[A, B](items: List[A])(f: A => Option[B]): Option[B]

  /**
   * Apply `transform` to each item, keep the results that are strictly
   * positive, and take the first `count` of them.
   *
   * `transform` is expensive - imagine a network call - so it must be applied
   * only to as many items as are needed to produce `count` answers.
   *
   * Write the eager version first and look at it: `items.map(transform)` on a
   * hundred items to keep three. Then add one word.
   */
  def firstPositive(items: List[Int], count: Int)(transform: Int => Int): List[Int]

  // ---------------------------------------------------------------------------
  // F.3 LazyList - lists that do not end
  //
  // A LazyList computes its tail only when asked, and remembers the answer. So
  // an infinite one is not a trick: you describe the whole thing, and only the
  // part you look at ever exists.
  //
  // `#::` builds one, the way `::` builds a List - except the tail is by-name.
  // ---------------------------------------------------------------------------

  /** 0, 1, 2, 3, ... forever. */
  def naturals: LazyList[Int]

  /**
   * 0, 1, 1, 2, 3, 5, 8, ... forever.
   *
   * You wrote this in B.SB6 with `unfold`, which had to be told how many to
   * produce. This one is not told, because it does not need to be - the caller
   * decides by how much they take. That difference is the whole point of the
   * block.
   *
   * BigInt, because the test goes to element 200.
   *
   * A fair objection to raise before you start: `#::` ends in a colon, so it is
   * RIGHT-associative, which means `current #:: rest` is really `rest.#::(current)`
   * and the tail is the RECEIVER. Evaluating a receiver to dispatch a method
   * call on it would be infinite recursion. Work out why it is not, and check
   * your answer against the solution - the mechanism is genuinely not obvious
   * and it is the thing that makes this whole section possible.
   */
  def fibonacci: LazyList[BigInt]

  /**
   * 2, 3, 5, 7, 11, ... forever.
   *
   * The sieve of Eratosthenes falls out of LazyList almost as prose: the first
   * candidate is prime, and the rest of the primes are the sieve of everything
   * that is not divisible by it.
   *
   * Write that sentence as two lines of code. It is one of the genuinely
   * beautiful things in the language, and it does not work at all without
   * laziness, because the sentence is defined in terms of itself.
   */
  def primes: LazyList[Int]

  // ---------------------------------------------------------------------------
  // F.4 Building your own
  // ---------------------------------------------------------------------------

  /**
   * B.SB5's `unfold`, producing a LazyList instead of a List.
   *
   * `unfoldLazy(1)(n => Some((n, n + 1)))` is the naturals from 1, forever.
   * `unfoldLazy(1)(n => if (n > 3) None else Some((n, n + 1))).toList == List(1, 2, 3)`
   *
   * Compare the two implementations side by side afterwards. The List version
   * needed `@tailrec` and an accumulator so it would not blow the stack, and it
   * could only ever produce finite results. This one needs neither, and can
   * produce infinite ones. It is also shorter.
   */
  def unfoldLazy[S, A](seed: S)(step: S => Option[(A, S)]): LazyList[A]

  /**
   * The Collatz sequence from `start`, ending WITH 1.
   *
   * Halve it if it is even, otherwise treble it and add one, and stop when you
   * reach 1.
   *
   * `collatz(6).toList == List(6, 3, 10, 5, 16, 8, 4, 2, 1)`
   * `collatz(1).toList == List(1)`
   *
   * Whether this terminates for every start value is famously unproven, which
   * is a nice thing to be relying on. Build it with `unfoldLazy`.
   */
  def collatz(start: Int): LazyList[Int]

  // ---------------------------------------------------------------------------
  // F.5 Something you would actually ship
  // ---------------------------------------------------------------------------

  /**
   * A retry schedule: how long to wait before each attempt.
   *
   * The first wait is `initial`; each one after is the previous multiplied by
   * `factor`, and none of them ever exceeds `cap`. Infinite, because the caller
   * decides how many attempts they are willing to make.
   *
   * `backoffSchedule(100.millis, 2, 1.second).take(6).toList`
   *   `== List(100.millis, 200.millis, 400.millis, 800.millis, 1.second, 1.second)`
   *
   * `LazyList.iterate` is the whole implementation.
   *
   * Notice what separating the schedule from the retrying buys you: the policy
   * is now a value you can test, log, configure and pass around, rather than
   * arithmetic buried inside a loop. E.SE2's `retry` would take one of these as
   * an argument, and that is a better `retry`.
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
   *
   * THIS IS THE ONE EXERCISE TODAY WHERE A MUTABLE COLLECTION IS THE RIGHT
   * ANSWER, and the reason is worth more than the exercise.
   *
   * A cache is state that outlives a single call and is shared between calls.
   * Behind this signature there is no way to express that immutably - you would
   * have to hand the updated cache back to the caller, and then it is not a
   * cache, it is an accumulator, and every call site has to thread it through.
   *
   * So the rule was never "never mutate". It is "mutate in one place, behind a
   * boundary, where nobody outside can see it". `memoize` hands back a plain
   * `A => B`: the caller cannot tell there is a Map in there, cannot reach it,
   * cannot corrupt it, and cannot observe the difference except in the speed.
   * That is contained mutation and it is completely fine. Recognising the
   * difference between this and a `var` in the middle of your domain logic is a
   * more useful skill than either rule on its own.
   *
   * `getOrElseUpdate` on a `mutable.Map` is the whole implementation.
   *
   * The trap is the same one as always: build the Map OUTSIDE the returned
   * function. Build it inside and every call gets a fresh one.
   *
   * One thing this is NOT is thread-safe: two threads can miss at the same
   * moment and both compute. For a pure function that is wasteful rather than
   * wrong, which is exactly the kind of distinction Block G is about.
   */
  def memoize[A, B](compute: A => B): A => B

  /**
   * SF2. Pascal's triangle, one row at a time, forever.
   *
   * `pascal.take(4).toList == List(List(1), List(1, 1), List(1, 2, 1), List(1, 3, 3, 1))`
   *
   * Each row comes from the one before it: pad it with a zero on each side and
   * add the two shifted copies together. `LazyList.iterate` again.
   */
  def pascal: LazyList[List[Int]]

  /**
   * SF3. Apply `next` over and over until the value stops changing, and return
   * it.
   *
   * `iterateUntilStable(100)(_ / 2) == 0`
   *
   * Assume it does converge; if it does not, this runs forever, which is the
   * honest behaviour rather than a wrong answer.
   *
   * Doing it with a recursive function is fine and takes four lines. Doing it
   * with `LazyList.iterate` and then looking for the first place where a value
   * equals the one after it takes one, reads as the specification, and is the
   * reason to have learned this.
   */
  def iterateUntilStable[A](start: A)(next: A => A): A

  /**
   * SF4. The Hamming numbers, in order, without duplicates: every positive
   * integer whose only prime factors are 2, 3 and 5.
   *
   * `hamming.take(10).toList == List(1, 2, 3, 4, 5, 6, 8, 9, 10, 12)`
   *
   * The hardest thing in day 2, and worth it. Testing each number in turn works
   * and is hopeless past a few thousand. The good solution defines the sequence
   * in terms of ITSELF: it starts with 1, and everything after is the sorted
   * merge of the whole sequence times two, times three, and times five.
   *
   * That definition is circular, which is why it needs a `lazy val` and a
   * LazyList and could not be written at all in an eager language. You will
   * also need a merge that drops duplicates, since 6 arrives twice.
   *
   * The test goes to the 1,000th element, so an implementation that tests
   * candidates one at a time will be caught by the clock.
   */
  def hamming: LazyList[Int]
}
