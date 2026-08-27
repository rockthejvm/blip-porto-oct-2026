package com.rockthecode.day2.solutions

import com.rockthecode.day2.BlockB

import scala.annotation.tailrec

/**
 * Day 2, Block B - reference solutions.
 */
object BlockBSolutions extends BlockB {

  // ---------------------------------------------------------------------------
  // B.1 Folds are everything
  // ---------------------------------------------------------------------------

  def myLength[A](list: List[A]): Int =
    list.foldLeft(0)((count, _) => count + 1)

  def myReverse[A](list: List[A]): List[A] =
    list.foldLeft(List.empty[A])((reversed, elem) => elem :: reversed)

  // foldLeft prepends, and prepending reverses. So `myReverse` is the SHAPE of
  // foldLeft with nothing added - which is the cleanest possible statement of
  // what foldLeft does to order.

  def myMap[A, B](list: List[A])(f: A => B): List[B] =
    list.foldRight(List.empty[B])((elem, mapped) => f(elem) :: mapped)

  def myFilter[A](list: List[A])(predicate: A => Boolean): List[A] =
    list.foldRight(List.empty[A])((elem, kept) => if (predicate(elem)) elem :: kept else kept)

  // Both of these work with foldLeft too - and then you have to reverse at the
  // end, because foldLeft built them backwards:
  //
  //   list.foldLeft(List.empty[B])((mapped, elem) => f(elem) :: mapped).reverse
  //
  // Same answer, one extra pass, slightly more to explain. Worth writing both
  // on the board: the choice of fold IS the choice of whether you need that
  // reverse. Notice what you must NOT do to avoid it: `mapped :+ f(elem)`.
  // See B.2.

  // ---------------------------------------------------------------------------
  // B.2 The three traps
  // ---------------------------------------------------------------------------

  def safeSum(list: List[Int]): Int =
    list.fold(0)(_ + _)

  // `list.reduce(_ + _)` throws on the empty list, because reduce has no seed
  // and therefore nothing to return. It also cannot change type: reduce is
  // (A, A) => A, while fold is (B, A) => B. That type difference is the whole
  // reason `foldLeft` can turn a List[Order] into a Map, and reduce cannot.

  def safeMax(list: List[Int]): Option[Int] =
    list.reduceOption(_ max _)

  // ...or `list.maxOption`, which is what you would actually write. The point
  // of the exercise is that the `Option` in the name is not politeness - it is
  // the type system refusing to pretend that "the largest of nothing" exists.

  def myFoldRight[A, B](list: List[A], seed: B)(op: (A, B) => B): B =
    list.reverse.foldLeft(seed)((accumulator, elem) => op(elem, accumulator))

  // This is, near enough, what the standard library does. No recursion, so no
  // stack to run out of.
  //
  // Measured on this machine, Scala 3.8.4:
  //   hand-written recursive foldRight   dies between 4,000 and 5,000
  //   this version                       fine at 500,000
  //   stdlib List.foldRight              fine at 500,000
  //
  // Worth demonstrating live, because "foldRight blows the stack" is folklore
  // that has been false for List since 2.13 and people still repeat it. What is
  // true is that YOUR recursion blows the stack.

  // ---------------------------------------------------------------------------
  // B.3 Run-length encoding
  // ---------------------------------------------------------------------------

  def encode[A](list: List[A]): List[(A, Int)] =
    list.foldRight(List.empty[(A, Int)]) { (elem, runs) =>
      runs match {
        case (value, count) :: rest if value == elem => (value, count + 1) :: rest
        case _                                      => (elem, 1) :: runs
      }
    }

  // Going right-to-left, the run you are extending is always the HEAD of what
  // you have built so far - so you can pattern match on it, bump the count, and
  // put it back. No "current run" to carry, no flush at the end.
  //
  // The foldLeft version has to carry the in-progress run separately and
  // remember to flush it when the list ends. It is maybe twice the size. Show
  // both: the lesson of B.3 is that direction of travel is a design decision,
  // not a coin toss.

  def decode[A](runs: List[(A, Int)]): List[A] =
    runs.flatMap((value, count) => List.fill(count)(value))

  // ---------------------------------------------------------------------------
  // B.4 The longest increasing run
  // ---------------------------------------------------------------------------

  /**
   * Four things to carry, so it gets a name rather than becoming a tuple.
   *
   * The lengths are carried explicitly. Calling `.length` on the runs inside
   * the fold would be the correct answer to the wrong question - on an already
   * sorted list of n elements that is 1 + 2 + ... + n comparisons, which is
   * B.2's trap in yet another costume.
   */
  private case class RunScan(best: List[Int], bestLength: Int, current: List[Int], currentLength: Int)

  def longestIncreasingRun(numbers: List[Int]): List[Int] = {
    val scan =
      numbers.foldLeft(RunScan(Nil, 0, Nil, 0)) { (runs, number) =>
        // runs.current is held REVERSED, so its head is the previous element
        val (current, currentLength) =
          runs.current match {
            case previous :: _ if number > previous => (number :: runs.current, runs.currentLength + 1)
            case _                                  => (List(number), 1)
          }

        // strictly greater, so on a tie the EARLIER run is the one we keep
        if (currentLength > runs.bestLength) RunScan(current, currentLength, current, currentLength)
        else RunScan(runs.best, runs.bestLength, current, currentLength)
      }

    scan.best.reverse
  }

  // Note there is no "flush the last run" step, and that is deliberate. Because
  // best is updated on every element rather than when a run ENDS, a run that is
  // still going when the list runs out has already been counted. Most people
  // write the version that updates on run-end, and most of those forget the
  // flush - which is exactly the bug the `List(1, 2, 3)` test catches.

  // ---------------------------------------------------------------------------
  // B.5 Word frequency
  // ---------------------------------------------------------------------------

  def topWords(text: String, n: Int): List[(String, Int)] =
    text.toLowerCase
      .split("[^\\p{L}]+")           // \p{L} is "any Unicode letter" - keeps ç, ã, é
      .toList
      .filter(_.nonEmpty)            // a leading separator produces an empty first piece
      .groupMapReduce(identity)(_ => 1)(_ + _)
      .toList
      .sortBy((word, count) => (-count, word))  // count descending, then word ascending
      .take(n)

  // Three things people get wrong here, all of them quietly:
  //
  //   `[^a-z]+`      turns "coração" into "cora" and "o". In Porto. In front of
  //                  the people who wrote the word.
  //   no tie-break   the answer then depends on Map iteration order, so it is
  //                  reproducible on your laptop and not on CI.
  //   -count         sorting descending by negating is fine for Int, but reach
  //                  for `.sorted(Ordering.by(...).reverse)` on anything where
  //                  negation is not meaningful.

  // ===========================================================================
  //
  //  STRETCH SECTION
  //
  // ===========================================================================

  // ---------------------------------------------------------------------------
  // SB1. foldLeft from foldRight
  // ---------------------------------------------------------------------------

  def foldLeftViaFoldRight[A, B](list: List[A], seed: B)(op: (B, A) => B): B = {
    val assembled: B => B =
      list.foldRight((accumulator: B) => accumulator) { (elem, continue) => accumulator =>
        continue(op(accumulator, elem))
      }

    assembled(seed)
  }

  // How to talk through it: foldRight can only build from the right, so we build
  // the only thing that can defer its own work - a function. Each step wraps the
  // one to its right: "do my bit to the accumulator, then hand it on". Fold the
  // whole list into one big function, then feed it the seed and the work
  // unwinds left-to-right.
  //
  // Trace [1, 2] with op = (b, a) => b - a and seed = 10:
  //   step(2, id)    = acc => id(acc - 2)          = acc => acc - 2
  //   step(1, that)  = acc => that(acc - 1)        = acc => (acc - 1) - 2
  //   applied to 10                                = 7
  // ...which is exactly (10 - 1) - 2. Left-to-right, out of a right fold.

  // ---------------------------------------------------------------------------
  // SB2 / SB3. Rebuilding the library
  // ---------------------------------------------------------------------------

  def myGroupBy[A, K](list: List[A])(key: A => K): Map[K, List[A]] =
    list
      .foldLeft(Map.empty[K, List[A]]) { (groups, elem) =>
        val k = key(elem)
        groups.updated(k, elem :: groups.getOrElse(k, Nil)) // prepend: O(1)
      }
      .map((k, members) => (k, members.reverse)) // ...and pay for the order once, at the end

  // `groups.updated(k, groups.getOrElse(k, Nil) :+ elem)` is the version that
  // reads better and is quadratic in the size of each group. B.2's trap, hidden
  // one level down inside a Map. This is why it is worth measuring once.

  def myPartition[A](list: List[A])(predicate: A => Boolean): (List[A], List[A]) =
    list.foldRight((List.empty[A], List.empty[A])) { case (elem, (yes, no)) =>
      if (predicate(elem)) (elem :: yes, no) else (yes, elem :: no)
    }

  // foldRight, so both halves come out in the original order with no reversing.
  // The foldLeft version needs two reverses at the end. Same choice as B.1.

  // ---------------------------------------------------------------------------
  // SB4. Balanced brackets
  // ---------------------------------------------------------------------------

  private val closerToOpener: Map[Char, Char] = Map(')' -> '(', ']' -> '[', '}' -> '{')
  private val openers: Set[Char] = closerToOpener.values.toSet

  def isBalanced(text: String): Boolean = {
    val outcome =
      text.foldLeft(Option(List.empty[Char])) { (maybeStack, char) =>
        maybeStack.flatMap { stack =>
          if (openers.contains(char)) Some(char :: stack)
          else
            closerToOpener.get(char) match {
              case None => Some(stack) // not a bracket at all: ignore it
              case Some(opener) =>
                stack match {
                  case `opener` :: rest => Some(rest) // matched: pop
                  case _                => None       // mismatched or nothing to close: dead
                }
            }
        }
      }

    outcome.contains(Nil) // survived, and closed everything it opened
  }

  // The interesting bit is the `Option`, and it is worth stopping on. A fold
  // cannot stop early, so "this input is already doomed" has to be carried
  // forward as a value - and once it is None, `flatMap` skips every remaining
  // step for us.
  //
  // What we have just invented, badly, is `Either[Error, Stack]` with the error
  // thrown away. Ask the room what they would want to know when this returns
  // false - "which bracket, at which position?" - and then say: that is Block E,
  // and it is one type away.

  // ---------------------------------------------------------------------------
  // SB5 / SB6. Unfolding
  // ---------------------------------------------------------------------------

  def unfold[S, A](seed: S)(step: S => Option[(A, S)]): List[A] = {
    @tailrec
    def loop(current: S, produced: List[A]): List[A] =
      step(current) match {
        case Some((value, next)) => loop(next, value :: produced)
        case None                => produced.reverse
      }

    loop(seed, Nil)
  }

  // The naive version - `value :: unfold(next)(step)` - is not tail recursive
  // and dies exactly where the hand-written foldRight died in B.2. Accumulate
  // and reverse, same as everywhere else today.

  def fibonacci(count: Int): List[BigInt] =
    unfold((0, BigInt(0), BigInt(1))) { (produced, current, next) =>
      if (produced >= count) None
      else Some((current, (produced + 1, next, current + next)))
    }

  // The seed carries three things: how many we have emitted, and the two
  // numbers needed to make the next one. A seed that carries more than "the
  // last value" is the whole idea of unfold - and it is precisely how a
  // LazyList is defined in Block F.
}
