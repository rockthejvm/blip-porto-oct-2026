package com.rockthecode.day2

import com.rockthecode.day2.solutions.BlockFSolutions

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration.*

/**
 * Day 2, Block F - the tests.
 *
 * Many of these count evaluations rather than checking values, because a lazy
 * answer and a wasteful one are the same answer. A failure on a count means the
 * code is correct and does far too much.
 *
 *   sbt blockF                                                 <- attendees
 *   sbt "testOnly com.rockthecode.day2.BlockFSolutionsSuite"   <- trainer
 */
abstract class BlockFSuite(impl: BlockF) extends munit.FunSuite {

  // --- F.1 -------------------------------------------------------------------

  test("F.1 timed returns the result of the block") {
    assertEquals(impl.timed(21 * 2)._1, 42)
    assertEquals(impl.timed("hello")._1, "hello")
  }

  test("F.1 timed actually measures the work, which needs the block to run inside it") {
    // With a by-value parameter the sleep happens at the call site and `timed`
    // reports 0ms for work it never saw.
    val (result, elapsed) = impl.timed { Thread.sleep(50); "done" }

    assertEquals(result, "done")
    assert(elapsed >= 40, s"measured ${elapsed}ms for 50ms of work - was the block evaluated before timed was entered?")
  }

  test("F.1 timed runs the block exactly once") {
    val calls = new AtomicInteger(0)
    impl.timed(calls.incrementAndGet())
    assertEquals(calls.get(), 1)
  }

  test("F.1 slowQueryLog reports only the slow ones") {
    assertEquals(impl.slowQueryLog(120, 100, "slow!"), Some("slow!"))
    assertEquals(impl.slowQueryLog(100, 100, "on the line"), Some("on the line"))
    assertEquals(impl.slowQueryLog(20, 100, "fast"), None)
  }

  test("F.1 slowQueryLog does not build the message for a query that was fast") {
    val calls = new AtomicInteger(0)
    def message: String = { calls.incrementAndGet(); "expensively rendered query plan" }

    assertEquals(impl.slowQueryLog(20, 100, message), None)
    assertEquals(calls.get(), 0, "rendered a log line nobody asked for - on every fast query, forever")

    assertEquals(impl.slowQueryLog(500, 100, message), Some("expensively rendered query plan"))
    assertEquals(calls.get(), 1)
  }

  test("F.1 once does not compute anything until it is called") {
    val calls = new AtomicInteger(0)
    val deferred = impl.once { calls.incrementAndGet(); "the pool" }

    assertEquals(calls.get(), 0, "opened a connection this process may never need")
  }

  test("F.1 once computes on first call and never again") {
    val calls = new AtomicInteger(0)
    val deferred = impl.once { calls.incrementAndGet(); "the pool" }

    assertEquals(deferred(), "the pool")
    assertEquals(calls.get(), 1)

    assertEquals(deferred(), "the pool")
    assertEquals(deferred(), "the pool")
    assertEquals(calls.get(), 1, "`() => compute` recomputes every time - a by-name param is the expression")
  }

  test("F.1 two separate deferrals are separate") {
    val calls = new AtomicInteger(0)
    val first = impl.once(calls.incrementAndGet())
    val second = impl.once(calls.incrementAndGet())

    first()
    first()
    second()
    assertEquals(calls.get(), 2)
  }

  // --- F.2 -------------------------------------------------------------------

  test("F.2 firstMatching finds the first match") {
    assertEquals(impl.firstMatching(List(1, 2, 3, 4))(n => if (n % 2 == 0) Some(n * 10) else None), Some(20))
    assertEquals(impl.firstMatching(List(1, 3))(n => if (n % 2 == 0) Some(n) else None), None)
    assertEquals(impl.firstMatching(List.empty[Int])(Some(_)), None)
  }

  test("F.2 firstMatching stops looking as soon as it has found something") {
    val calls = new AtomicInteger(0)
    val hundred = (1 to 100).toList

    val found = impl.firstMatching(hundred) { n =>
      calls.incrementAndGet()
      if (n >= 5) Some(n) else None
    }

    assertEquals(found, Some(5))
    assertEquals(calls.get(), 5, "imagine each of those was an HTTP call")
  }

  test("F.2 firstPositive returns the right answers") {
    assertEquals(impl.firstPositive((1 to 10).toList, 3)(n => n - 5), List(1, 2, 3))
    assertEquals(impl.firstPositive((1 to 10).toList, 3)(_ => -1), Nil)
    assertEquals(impl.firstPositive(Nil, 3)(identity), Nil)
  }

  test("F.2 firstPositive does not transform items it did not need") {
    val calls = new AtomicInteger(0)
    val hundred = (1 to 100).toList

    val result = impl.firstPositive(hundred, 3) { n => calls.incrementAndGet(); n }

    assertEquals(result, List(1, 2, 3))
    assertEquals(calls.get(), 3, "a hundred expensive calls to keep three")
  }

  test("F.2 firstPositive still works when it has to look a long way") {
    val calls = new AtomicInteger(0)
    val result = impl.firstPositive((1 to 100).toList, 2) { n => calls.incrementAndGet(); n - 50 }

    assertEquals(result, List(1, 2))
    assertEquals(calls.get(), 52)
  }

  // --- F.3 -------------------------------------------------------------------

  test("F.3 naturals goes on forever, and taking from it terminates") {
    assertEquals(impl.naturals.take(5).toList, List(0, 1, 2, 3, 4))
    assertEquals(impl.naturals.drop(1000).head, 1000)
  }

  test("F.3 fibonacci") {
    assertEquals(impl.fibonacci.take(10).toList, List(0, 1, 1, 2, 3, 5, 8, 13, 21, 34).map(BigInt(_)))
  }

  test("F.3 fibonacci at 200, where nothing smaller than a BigInt would do") {
    assertEquals(impl.fibonacci(200), BigInt("280571172992510140037611932413038677189525"))
  }

  test("F.3 primes") {
    assertEquals(impl.primes.take(10).toList, List(2, 3, 5, 7, 11, 13, 17, 19, 23, 29))
    assertEquals(impl.primes(24), 97, "the 25th prime")
  }

  test("F.3 nothing is computed until it is asked for") {
    // If any of these three did their work eagerly, this test would never
    // return rather than fail - which is itself the demonstration.
    assertEquals(impl.naturals.take(3).toList.size, 3)
    assertEquals(impl.fibonacci.take(3).toList.size, 3)
    assertEquals(impl.primes.take(3).toList.size, 3)
  }

  // --- F.4 -------------------------------------------------------------------

  test("F.4 unfoldLazy builds a finite list when the step stops") {
    assertEquals(impl.unfoldLazy(1)(n => if (n > 3) None else Some((n, n + 1))).toList, List(1, 2, 3))
    assertEquals(impl.unfoldLazy(1)(_ => None).toList, Nil)
  }

  test("F.4 unfoldLazy builds an infinite one when the step never stops") {
    assertEquals(impl.unfoldLazy(1)(n => Some((n, n + 1))).take(5).toList, List(1, 2, 3, 4, 5))
  }

  test("F.4 unfoldLazy goes deeper than a recursion could") {
    assertEquals(impl.unfoldLazy(0)(n => Some((n, n + 1))).drop(200000).head, 200000)
  }

  test("F.4 collatz") {
    assertEquals(impl.collatz(6).toList, List(6, 3, 10, 5, 16, 8, 4, 2, 1))
    assertEquals(impl.collatz(1).toList, List(1))
    assertEquals(impl.collatz(2).toList, List(2, 1))
    assertEquals(impl.collatz(7).toList.last, 1)
    assertEquals(impl.collatz(27).toList.size, 112)
  }

  // --- F.5 -------------------------------------------------------------------

  test("F.5 backoffSchedule doubles until it hits the cap") {
    assertEquals(
      impl.backoffSchedule(100.millis, 2, 1.second).take(6).toList,
      List(100.millis, 200.millis, 400.millis, 800.millis, 1.second, 1.second)
    )
  }

  test("F.5 backoffSchedule stays at the cap forever") {
    assertEquals(impl.backoffSchedule(100.millis, 2, 1.second)(50), 1.second)
  }

  test("F.5 a factor of one never grows") {
    assertEquals(
      impl.backoffSchedule(250.millis, 1, 10.seconds).take(3).toList,
      List(250.millis, 250.millis, 250.millis)
    )
  }

  test("F.5 an initial delay above the cap is capped straight away") {
    assertEquals(impl.backoffSchedule(5.seconds, 2, 1.second).take(3).toList, List.fill(3)(1.second))
  }

  // ===========================================================================
  //
  //  STRETCH SECTION
  //
  // ===========================================================================

  // --- SF1 -------------------------------------------------------------------

  test("SF1 memoize returns the same answers as the function it wraps") {
    val fast = impl.memoize[Int, Int](n => n * n)
    assertEquals(fast(0), 0)
    assertEquals(fast(5), 25)
    assertEquals(fast(12), 144)
  }

  test("SF1 memoize computes each argument exactly once, and only the ones asked for") {
    val calls = new AtomicInteger(0)
    val fast = impl.memoize[Int, Int] { n => calls.incrementAndGet(); n * n }

    assertEquals(fast(5), 25)
    assertEquals(calls.get(), 1, "just the one that was asked for")

    assertEquals(fast(5), 25)
    assertEquals(calls.get(), 1, "a hit computes nothing")

    assertEquals(fast(3), 9)
    assertEquals(calls.get(), 2, "a miss computes exactly one thing")
  }

  test("SF1 memoize builds its cache once, not once per call") {
    // Building the Map inside the returned function is the trap: every call
    // gets a fresh empty one and nothing is ever remembered.
    val calls = new AtomicInteger(0)
    val fast = impl.memoize[Int, Int] { n => calls.incrementAndGet(); n }

    (1 to 10).foreach(_ => fast(4))
    assertEquals(calls.get(), 1, "ten lookups of the same argument, one computation")
  }

  test("SF1 memoize works on any argument type, not just integers") {
    val calls = new AtomicInteger(0)
    val lengthOf = impl.memoize[String, Int] { s => calls.incrementAndGet(); s.length }

    assertEquals(lengthOf("hello"), 5)
    assertEquals(lengthOf("hello"), 5)
    assertEquals(lengthOf("hi"), 2)
    assertEquals(calls.get(), 2)
  }

  // --- SF2 -------------------------------------------------------------------

  test("SF2 pascal") {
    assertEquals(
      impl.pascal.take(5).toList,
      List(List(1), List(1, 1), List(1, 2, 1), List(1, 3, 3, 1), List(1, 4, 6, 4, 1))
    )
  }

  test("SF2 pascal keeps going") {
    assertEquals(impl.pascal(10), List(1, 10, 45, 120, 210, 252, 210, 120, 45, 10, 1))
    assertEquals(impl.pascal(20).sum, 1048576, "row n sums to 2^n")
  }

  // --- SF3 -------------------------------------------------------------------

  test("SF3 iterateUntilStable") {
    assertEquals(impl.iterateUntilStable(100)(_ / 2), 0)
    assertEquals(impl.iterateUntilStable(7)(n => if (n > 3) n - 1 else n), 3)
    assertEquals(impl.iterateUntilStable("hello")(identity), "hello", "already stable")
  }

  test("SF3 iterateUntilStable works on anything with equality") {
    assertEquals(impl.iterateUntilStable(List(1, 2, 3))(_.drop(1)), Nil)
  }

  // --- SF4 -------------------------------------------------------------------

  test("SF4 hamming starts correctly and has no duplicates") {
    assertEquals(impl.hamming.take(10).toList, List(1, 2, 3, 4, 5, 6, 8, 9, 10, 12))
    assertEquals(impl.hamming.take(20).toList, impl.hamming.take(20).toList.distinct)
  }

  test("SF4 hamming is sorted") {
    val first = impl.hamming.take(50).toList
    assertEquals(first, first.sorted)
  }

  test("SF4 hamming reaches the thousandth without testing fifty million candidates") {
    // The 1000th Hamming number is 51,200,000. Checking each integer in turn is
    // correct and will not finish.
    assertEquals(impl.hamming(999), 51200000)
  }
}

/** Red until the attendees make it green. That is the point. */
class BlockFExercisesSuite extends BlockFSuite(BlockFExercises)

/** Green, always. If this one goes red, the reference solutions are wrong. */
class BlockFSolutionsSuite extends BlockFSuite(BlockFSolutions)
