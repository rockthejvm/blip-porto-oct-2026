package com.rockthecode.day2.solutions

import com.rockthecode.day2.BlockF

import scala.concurrent.duration.FiniteDuration

/**
 * Day 2, Block F - reference solutions.
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
  // the call site, `timed` receives a finished value, and it reports 0ms with a
  // completely straight face. That is the demo - a measuring function that
  // cannot measure.
  //
  // Note also that `block` is used exactly once, which is why no `lazy val` is
  // needed here and is needed in `once`.

  def slowQueryLog(elapsedMillis: Long, thresholdMillis: Long, message: => String): Option[String] =
    if (elapsedMillis >= thresholdMillis) Some(message) else None

  // Every logging API in every language has this signature, and the ones in
  // languages without by-name parameters make you write the guard by hand at
  // every call site:
  //
  //   if (log.isDebugEnabled) log.debug(expensiveString)
  //
  // ...which people forget, and which is why "just add some debug logging"
  // sometimes shows up in a flame graph.

  def once[A](compute: => A): () => A = {
    lazy val value = compute
    () => value
  }

  // Three lines, and both wrong answers are instructive enough to write on the
  // board next to it:
  //
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
  //
  // Nearly every "why is this being called twice" in a code review is somebody
  // who wanted the middle row and wrote the bottom one.
  //
  // (Scala initialises `lazy val` under a lock, so this is safe if two threads
  // call it at once - worth knowing before Block G, and worth knowing that it
  // is not free.)

  // ---------------------------------------------------------------------------
  // F.2 Views
  // ---------------------------------------------------------------------------

  def firstMatching[A, B](items: List[A])(f: A => Option[B]): Option[B] =
    items.view.flatMap(f).headOption

  // Exactly D.1's `firstAvailable`, with `.view` added - and D.1's solution
  // said this was coming. Worth putting the two on screen together: identical
  // code, one word apart, and the difference is 5 lookups against 100.

  def firstPositive(items: List[Int], count: Int)(transform: Int => Int): List[Int] =
    items.view.map(transform).filter(_ > 0).take(count).toList

  // The eager version is the same line without `.view`, and it transforms all
  // hundred items to keep three.
  //
  // The mental model that makes views click: without a view, each stage builds
  // a whole intermediate collection and hands it on. With a view, nothing is
  // built and nothing runs; `toList` at the end pulls one element through the
  // entire pipeline, then the next, and stops pulling when `take` has enough.
  //
  // Two practical notes worth thirty seconds:
  //   - the `.toList` is not optional. A view is a description; without forcing
  //     it you have handed your caller a promise rather than an answer.
  //   - views do NOT memoise. Force one twice and everything runs twice. That
  //     is the difference from LazyList, and it is the next section.

  // ---------------------------------------------------------------------------
  // F.3 LazyList
  // ---------------------------------------------------------------------------

  def naturals: LazyList[Int] = LazyList.from(0)

  // ...or, spelled out so the shape is visible:
  //   def from(n: Int): LazyList[Int] = n #:: from(n + 1)
  // which looks like infinite recursion and is not, because the tail of `#::`
  // is by-name. Worth writing on the board even though the library has it.

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
  //
  // Measured, by counting how many times the body of `from` is entered:
  //
  //   from(0, 1), taking nothing      1
  //   .take(5)                        5
  //   .take(10) on the same list     10   (memoised - only 5 new)
  //
  // Worth putting a counter in it live. It is the one place in the block where
  // the mechanism is genuinely surprising, and somebody will ask.
  //
  // Compare with B.SB6, which had to be told how many numbers to make. This one
  // is not told, and the caller decides with `take`. The description and the
  // consumption have come apart, which is the entire idea of the block.
  //
  // The self-referential one-liner is a classic and worth showing as a curio:
  //   lazy val fibs: LazyList[BigInt] = 0 #:: 1 #:: fibs.zip(fibs.tail).map(_ + _)

  def primes: LazyList[Int] = {
    def sieve(candidates: LazyList[Int]): LazyList[Int] =
      candidates.head #:: sieve(candidates.tail.filter(_ % candidates.head != 0))

    sieve(LazyList.from(2))
  }

  // Read it out loud as English: "the first candidate is prime, and the rest of
  // the primes are the sieve of every later candidate it does not divide."
  // The code is that sentence.
  //
  // It is defined in terms of itself and it terminates, because nothing is
  // computed until something asks. In an eager language this line is an
  // infinite loop; here it is a definition.
  //
  // Honest footnote: it gets slow, because element n has n stacked filters to
  // pass through. It is a beautiful definition, not a fast sieve, and knowing
  // the difference is part of the lesson.

  // ---------------------------------------------------------------------------
  // F.4 Building your own
  // ---------------------------------------------------------------------------

  def unfoldLazy[S, A](seed: S)(step: S => Option[(A, S)]): LazyList[A] =
    step(seed) match {
      case Some((value, nextSeed)) => value #:: unfoldLazy(nextSeed)(step)
      case None                    => LazyList.empty
    }

  // Put this beside B.SB5's List version. That one needed @tailrec, an
  // accumulator and a reverse, and could only ever finish. This one needs none
  // of them, is shorter, and can run forever.
  //
  // It is not tail recursive and does not need to be: the recursive call is the
  // by-name tail of `#::`, so it does not happen until somebody asks for it,
  // and there is no stack frame waiting around in the meantime.

  def collatz(start: Int): LazyList[Int] =
    unfoldLazy(Option(start)) {
      case None        => None
      case Some(1)     => Some((1, None))
      case Some(value) => Some((value, Some(if (value % 2 == 0) value / 2 else 3 * value + 1)))
    }

  // The `Option` in the seed is the "and stop AFTER emitting 1" bookkeeping:
  // reaching 1 emits it and sets the seed to None, and the next step ends the
  // list. Worth a moment, because "produce this element and then stop" is
  // genuinely fiddly in every unfold-shaped API and this is the standard trick.

  // ---------------------------------------------------------------------------
  // F.5 Backoff
  // ---------------------------------------------------------------------------

  def backoffSchedule(initial: FiniteDuration, factor: Int, cap: FiniteDuration): LazyList[FiniteDuration] =
    LazyList.iterate(initial.min(cap))(previous => (previous * factor).min(cap))

  // `initial.min(cap)` on the seed too, so a caller who passes an initial delay
  // larger than the cap gets the cap rather than one rogue first wait. Small,
  // and the kind of thing that only shows up in production at 3am.
  //
  // The point worth making: the POLICY is now a value. You can print it, test
  // it, put it in config, or hand a different one to the same retry loop. Sixty
  // seconds on why that beats `delay = delay * 2` inside a while loop is time
  // well spent - it is the same argument as pure functions, one level up.

  // ===========================================================================
  //
  //  STRETCH SECTION
  //
  // ===========================================================================

  def memoize[A, B](compute: A => B): A => B = {
    val cache = scala.collection.mutable.Map.empty[A, B]
    argument => cache.getOrElseUpdate(argument, compute(argument))
  }

  // Two lines, and the interesting one is the first: `val`, outside the lambda.
  // Move it inside and every call builds a fresh empty Map, nothing is ever
  // remembered, and the test catches it immediately. Same trap as every other
  // caching bug.
  //
  // `getOrElseUpdate(key, value)` takes its second argument by-name, which is
  // the only reason this works: `compute(argument)` is not evaluated on a cache
  // hit. Block F, quietly doing its job inside a standard library method.
  //
  // Worth spending a minute on the mutation, because this is the last exercise
  // of a day that banned it:
  //
  //   The rule was never "never mutate". It is "mutate where nobody can see
  //   it". This returns an ordinary `A => B`. The caller cannot reach the Map,
  //   cannot corrupt it, and cannot observe it except in the speed. Referential
  //   transparency is intact - `memoize(f)(x)` is still `f(x)`, every time.
  //
  // Contrast with a `var` in the middle of domain logic, which every caller can
  // see the effects of and nobody can reason about. Same keyword, entirely
  // different thing, and being able to tell them apart is worth more than
  // either rule.
  //
  // Two honest caveats:
  //
  //   - not thread-safe. Two threads can miss at the same instant and both
  //     compute. For a pure `compute` that is wasted work rather than a wrong
  //     answer; make `compute` impure and it is a real bug. A concurrent map or
  //     a lock fixes it, and costs something. Block G.
  //   - it never evicts. This is a memory leak with good manners. Real caches
  //     have a bound and a policy.
  //
  // Footnote worth thirty seconds, since it is the block we are in: a LazyList
  // memoises too, and `LazyList.from(0).map(compute)` is a cache indexed by
  // position with no mutation anywhere. It also computes every entry below the
  // one you asked for, which is why it is a curiosity here and a Map is the
  // answer.

  def pascal: LazyList[List[Int]] =
    LazyList.iterate(List(1))(row => (0 +: row).zip(row :+ 0).map(_ + _))

  // Pad with a zero at each end, zip the two shifted copies, add. The rows are
  // produced on demand, so `pascal` is a definition of the whole triangle and
  // `pascal.take(4)` is four rows of work.

  def iterateUntilStable[A](start: A)(next: A => A): A = {
    val steps = LazyList.iterate(start)(next)

    steps.zip(steps.tail).collectFirst { case (current, following) if current == following => current }.get
  }

  // "The sequence of steps, zipped with itself shifted by one, and the first
  // place where the two agree." That is the specification, and it is the code.
  //
  // The `.get` is the one place today where it is defensible: the brief says
  // assume convergence, and if it does not converge this never returns rather
  // than returning None. Say that out loud rather than hiding it - a `.get` you
  // have thought about and can justify is different from the ones Block D
  // banned, and pretending otherwise teaches cargo cult.
  //
  // `steps.tail` shares the memoised `steps`, so nothing is computed twice.
  // With a view instead of a LazyList, everything would be.

  def hamming: LazyList[Int] = {
    def merge(left: LazyList[Int], right: LazyList[Int]): LazyList[Int] =
      if (left.head < right.head) left.head #:: merge(left.tail, right)
      else if (left.head > right.head) right.head #:: merge(left, right.tail)
      else left.head #:: merge(left.tail, right.tail) // equal: emit once, drop both

    lazy val values: LazyList[Int] =
      1 #:: merge(values.map(_ * 2), merge(values.map(_ * 3), values.map(_ * 5)))

    values
  }

  // The definition reads as its own specification: one is a Hamming number, and
  // every other one is some Hamming number times two, three or five - so the
  // sequence is 1 followed by the merge of itself times each.
  //
  // It is circular. `lazy val` is what makes a circular definition legal, and
  // the by-name tail of `#::` is what makes it terminate: by the time anything
  // asks for `values.map(_ * 2).head`, `values.head` is already 1.
  //
  // Three things worth pointing at:
  //   - the third branch of `merge` is the deduplication. 6 arrives from both
  //     the times-two and the times-three streams, and without that branch it
  //     appears twice.
  //   - `values` is memoised, so `values.map(_ * 2)` walks a list that is
  //     already computed. Try the same shape with a view and it recomputes the
  //     whole sequence three times per element, exponentially.
  //   - the obvious alternative - test each integer for 2-3-5-smoothness - is
  //     correct and unusable: the 1,000th Hamming number is 51,200,000, so you
  //     would test fifty million candidates to find a thousand answers. The
  //     test's time limit makes that point without anybody having to say it.
}
