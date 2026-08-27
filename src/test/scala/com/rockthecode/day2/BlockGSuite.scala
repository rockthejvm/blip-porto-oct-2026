package com.rockthecode.day2

import com.rockthecode.day2.solutions.BlockGSolutions

import java.util.concurrent.{CountDownLatch, TimeoutException}
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future, blocking}
import scala.util.{Failure, Success, Try}

/**
 * Day 2, Block G - the tests.
 *
 * These measure elapsed time, because a correct-but-sequential answer returns
 * exactly the same value as a correct-and-parallel one. Timing assertions are
 * deliberately loose - they are looking for "three delays instead of one", not
 * for milliseconds.
 *
 *   sbt blockG                                                 <- attendees
 *   sbt "testOnly com.rockthecode.day2.BlockGSolutionsSuite"   <- trainer
 */
abstract class BlockGSuite(impl: BlockG) extends munit.FunSuite {

  import scala.concurrent.ExecutionContext.Implicits.global

  override val munitTimeout: FiniteDuration = 30.seconds

  private def await[A](future: Future[A]): A = Await.result(future, 10.seconds)

  private def elapsed[A](body: => A): (A, Long) = {
    val startedAt = System.nanoTime()
    val result = body
    (result, (System.nanoTime() - startedAt) / 1000000)
  }

  /** A Future that takes its time, without starving the pool while it does. */
  private def slow[A](millis: Long)(value: => A): Future[A] =
    Future(blocking { Thread.sleep(millis); value })

  private def failing[A](message: String): Future[A] =
    Future.failed(new RuntimeException(message))

  // --- G.1 -------------------------------------------------------------------

  test("G.1 sumThree adds them up") {
    assertEquals(await(impl.sumThree(Future.successful(1), Future.successful(2), Future.successful(3))), 6)
  }

  test("G.1 sumThree runs them at the same time, not one after another") {
    val (total, took) = elapsed {
      await(impl.sumThree(slow(300)(1), slow(300)(2), slow(300)(3)))
    }

    assertEquals(total, 6)
    assert(
      took < 700,
      s"took ${took}ms for three 300ms calls - that is one after another. When does a Future start running?"
    )
  }

  test("G.1 sumThree fails if any of them fails") {
    val result = Try(await(impl.sumThree(Future.successful(1), failing("boom"), Future.successful(3))))
    assert(result.isFailure)
  }

  // --- G.2 -------------------------------------------------------------------

  test("G.2 fetchAll keeps the ORIGINAL order, not the order they finished in") {
    val (results, _) = elapsed {
      // deliberately finishing backwards
      await(impl.fetchAll(List(1, 2, 3))(id => slow((4 - id) * 100L)(s"item-$id")))
    }
    assertEquals(results, List("item-1", "item-2", "item-3"))
  }

  test("G.2 fetchAll fans out rather than queueing") {
    val (results, took) = elapsed {
      await(impl.fetchAll((1 to 8).toList)(id => slow(200)(id * 10)))
    }
    assertEquals(results, (1 to 8).toList.map(_ * 10))
    assert(took < 800, s"took ${took}ms for eight 200ms calls run together")
  }

  test("G.2 fetchAll fails if any one of them fails") {
    val result = Try(await(impl.fetchAll(List(1, 2, 3))(id => if (id == 2) failing("boom") else Future.successful(id))))
    assert(result.isFailure)
  }

  test("G.2 fetchAll of nothing") {
    assertEquals(await(impl.fetchAll(List.empty[Int])(id => Future.successful(id))), Nil)
  }

  test("G.2 fetchAllSettled survives a failure and reports it in place") {
    val results = await(
      impl.fetchAllSettled(List(1, 2, 3))(id => if (id == 2) failing("boom") else Future.successful(s"ok-$id"))
    )

    assertEquals(results.size, 3)
    assertEquals(results(0), Right("ok-1"))
    assertEquals(results(2), Right("ok-3"))
    assert(results(1).isLeft, "the middle one should be a Left, not a lost batch")
    assertEquals(results(1).left.map(_.getMessage), Left("boom"))
  }

  test("G.2 fetchAllSettled never fails, even when everything does") {
    val results = await(impl.fetchAllSettled(List(1, 2))(_ => failing[String]("all broken")))
    assertEquals(results.count(_.isLeft), 2)
  }

  // --- G.3 -------------------------------------------------------------------

  test("G.3 fetchOrDefault") {
    assertEquals(await(impl.fetchOrDefault(Future.successful("real"), "fallback")), "real")
    assertEquals(await(impl.fetchOrDefault(failing[String]("boom"), "fallback")), "fallback")
  }

  test("G.3 fetchWithBackup uses the backup only when it has to") {
    assertEquals(await(impl.fetchWithBackup(Future.successful("primary"), Future.successful("backup"))), "primary")
    assertEquals(await(impl.fetchWithBackup(failing[String]("boom"), Future.successful("backup"))), "backup")
  }

  test("G.3 fetchWithBackup does not even call the backup when the primary works") {
    val calls = new AtomicInteger(0)
    // `def`, not `val` - a `val` would evaluate right here and the test would
    // be measuring nothing. Block F.1, biting in a test file.
    def backup: Future[String] = { calls.incrementAndGet(); Future.successful("backup") }

    assertEquals(await(impl.fetchWithBackup(Future.successful("primary"), backup)), "primary")
    assertEquals(calls.get(), 0, "the backup call was made for nothing")
  }

  test("G.3 fetchWithBackup can still fail, if the backup does too") {
    assert(Try(await(impl.fetchWithBackup(failing[String]("first"), failing[String]("second")))).isFailure)
  }

  test("G.3 describeOutcome handles both sides and never fails") {
    assertEquals(await(impl.describeOutcome(Future.successful(42))), "ok: 42")
    assertEquals(await(impl.describeOutcome(failing[Int]("boom"))), "failed: boom")
  }

  // --- G.4 -------------------------------------------------------------------

  test("G.4 fetchAsFuture turns a callback into something composable") {
    assertEquals(await(impl.fetchAsFuture(7)), "record-7")
  }

  test("G.4 fetchAsFuture fails with the exception the callback was handed") {
    val result = Try(await(impl.fetchAsFuture(-1)))
    assert(result.isFailure)
    result match {
      case Failure(error) =>
        assert(error.isInstanceOf[IllegalArgumentException], s"expected the original exception, got $error")
        assertEquals(error.getMessage, "bad id: -1")
      case Success(value) => fail(s"expected a failure, got $value")
    }
  }

  test("G.4 fetchAsFuture composes, which was the whole point") {
    assertEquals(await(impl.fetchAll(List(1, 2, 3))(impl.fetchAsFuture)), List("record-1", "record-2", "record-3"))
  }

  test("G.4 delayed completes with the value") {
    assertEquals(await(impl.delayed(50.millis)("later")), "later")
  }

  test("G.4 delayed actually waits") {
    val (value, took) = elapsed(await(impl.delayed(300.millis)(42)))
    assertEquals(value, 42)
    assert(took >= 250, s"came back after ${took}ms for a 300ms delay")
  }

  test("G.4 delayed does not evaluate the value until the delay is up") {
    val calls = new AtomicInteger(0)
    val future = impl.delayed(300.millis) { calls.incrementAndGet(); "value" }

    assertEquals(calls.get(), 0, "the value was computed at the call site, not when the delay expired")
    assertEquals(await(future), "value")
    assertEquals(calls.get(), 1)
  }

  test("G.4 delayed fails the Future if evaluating the value throws") {
    // `promise.success(value)` lets the exception escape on a scheduler thread
    // where nobody is listening, and the Future then never completes at all -
    // so the caller waits forever for a failure that already happened.
    val result = Try(Await.result(impl.delayed[Int](50.millis)(throw new IllegalStateException("boom")), 2.seconds))

    result match {
      case Failure(_: TimeoutException) =>
        fail("the Future never completed - the exception escaped on the scheduler thread instead of failing it")
      case Failure(error) => assertEquals(error.getMessage, "boom")
      case Success(value) => fail(s"expected a failure, got $value")
    }
  }

  test("G.4 delayed does not tie up a thread per delay") {
    // A hundred concurrent delays. With a scheduler this is one wait; with
    // `Future { Thread.sleep(...) }` it is a hundred sleeping pool threads.
    val (results, took) = elapsed {
      await(Future.sequence((1 to 100).toList.map(n => impl.delayed(200.millis)(n))))
    }

    assertEquals(results.sum, 5050)
    assert(took < 1500, s"took ${took}ms for a hundred concurrent 200ms delays")
  }

  test("G.4 withTimeout lets a fast enough answer through") {
    assertEquals(await(impl.withTimeout(slow(50)("in time"), 2.seconds)), "in time")
  }

  test("G.4 withTimeout gives up on a slow one") {
    val result = Try(await(impl.withTimeout(slow(5000)("far too slow"), 200.millis)))
    assert(result.isFailure)
    assert(
      result.failed.get.isInstanceOf[TimeoutException],
      s"expected a TimeoutException, got ${result.failed.get}"
    )
  }

  test("G.4 withTimeout passes a failure through rather than turning it into a timeout") {
    val result = Try(await(impl.withTimeout(failing[String]("boom"), 2.seconds)))
    assertEquals(result.failed.get.getMessage, "boom")
  }

  // --- G.5 -------------------------------------------------------------------

  /** Records how many calls were ever running at the same moment. */
  private class ConcurrencyProbe {
    private val running = new AtomicInteger(0)
    private val peak = new AtomicInteger(0)

    def track[A](body: => A): Future[A] =
      Future {
        val now = running.incrementAndGet()
        peak.updateAndGet(highest => math.max(highest, now))
        val result = blocking { Thread.sleep(100); body }
        running.decrementAndGet()
        result
      }

    def highWaterMark: Int = peak.get()
  }

  test("G.5 batched returns everything, in order") {
    val probe = new ConcurrencyProbe
    assertEquals(await(impl.batched((1 to 9).toList, 3)(id => probe.track(id * 10))), (1 to 9).toList.map(_ * 10))
  }

  test("G.5 batched never exceeds the limit") {
    val probe = new ConcurrencyProbe
    await(impl.batched((1 to 12).toList, 3)(id => probe.track(id)))
    assert(probe.highWaterMark <= 3, s"${probe.highWaterMark} calls were in flight at once, the batch size was 3")
  }

  test("G.5 batched still does run them in parallel, up to the limit") {
    val probe = new ConcurrencyProbe
    val (_, took) = elapsed(await(impl.batched((1 to 12).toList, 4)(id => probe.track(id))))

    assert(probe.highWaterMark > 1, "a batch of 4 should not run one at a time")
    assert(took < 1200, s"took ${took}ms for twelve 100ms calls, four at a time")
  }

  test("G.5 batched of nothing") {
    assertEquals(await(impl.batched(List.empty[Int], 3)(id => Future.successful(id))), Nil)
  }

  // ===========================================================================
  //
  //  STRETCH SECTION
  //
  // ===========================================================================

  // --- SG1 -------------------------------------------------------------------

  test("SG1 firstSuccessOf takes the first that WORKS, not the first that finishes") {
    // firstCompletedOf would hand back the fast failure and never try the slow success.
    val result = await(impl.firstSuccessOf(List(failing[String]("fast failure"), slow(200)("slow success"))))
    assertEquals(result, "slow success")
  }

  test("SG1 firstSuccessOf takes the fastest success when there are several") {
    val result = await(impl.firstSuccessOf(List(slow(500)("slow"), slow(50)("quick"), slow(900)("slowest"))))
    assertEquals(result, "quick")
  }

  test("SG1 firstSuccessOf fails only when everything has failed") {
    val result = Try(await(impl.firstSuccessOf(List(failing[String]("one"), slow(100)(throw new RuntimeException("two"))))))
    assert(result.isFailure)
  }

  // --- SG2 -------------------------------------------------------------------

  private val delays: LazyList[FiniteDuration] = LazyList.continually(50.millis)

  test("SG2 retryWithBackoff stops as soon as it works") {
    val calls = new AtomicInteger(0)
    val action = () => { calls.incrementAndGet(); Future.successful("fine") }

    assertEquals(await(impl.retryWithBackoff(delays, 3)(action)), "fine")
    assertEquals(calls.get(), 1)
  }

  test("SG2 retryWithBackoff keeps going until it works") {
    val calls = new AtomicInteger(0)
    val action = () => {
      val attempt = calls.incrementAndGet()
      if (attempt < 3) Future.failed(new RuntimeException(s"attempt $attempt")) else Future.successful(attempt)
    }

    assertEquals(await(impl.retryWithBackoff(delays, 5)(action)), 3)
    assertEquals(calls.get(), 3)
  }

  test("SG2 retryWithBackoff gives up after the right number of attempts, with the last failure") {
    val calls = new AtomicInteger(0)
    val action = () => Future.failed[Int](new RuntimeException(s"attempt ${calls.incrementAndGet()}"))

    val result = Try(await(impl.retryWithBackoff(delays, 3)(action)))
    assertEquals(calls.get(), 3)
    assertEquals(result.failed.get.getMessage, "attempt 3")
  }

  test("SG2 retryWithBackoff actually waits between attempts") {
    val calls = new AtomicInteger(0)
    val action = () => Future.failed[Int](new RuntimeException(s"attempt ${calls.incrementAndGet()}"))

    // three attempts means two waits of 200ms
    val (_, took) = elapsed(Try(await(impl.retryWithBackoff(LazyList.continually(200.millis), 3)(action))))
    assert(took >= 350, s"only took ${took}ms - it did not wait between attempts")
  }

  // --- SG3 -------------------------------------------------------------------

  test("SG3 foldSequentially threads the accumulator through") {
    val total = await(impl.foldSequentially(List(1, 2, 3, 4), 0)((sum, n) => Future.successful(sum + n)))
    assertEquals(total, 10)
    assertEquals(await(impl.foldSequentially(List.empty[Int], 99)((sum, n) => Future.successful(sum + n))), 99)
  }

  test("SG3 foldSequentially runs strictly one at a time") {
    val probe = new ConcurrencyProbe
    val order = await(
      impl.foldSequentially(List(1, 2, 3), List.empty[Int])((seen, n) => probe.track(seen :+ n))
    )

    assertEquals(order, List(1, 2, 3), "and in order")
    assertEquals(probe.highWaterMark, 1, "two steps overlapped - each step must wait for the one before it")
  }

  test("SG3 foldSequentially stops at the first failure") {
    val calls = new AtomicInteger(0)
    val result = Try(
      await(
        impl.foldSequentially(List(1, 2, 3), 0) { (sum, n) =>
          calls.incrementAndGet()
          if (n == 2) Future.failed(new RuntimeException("boom")) else Future.successful(sum + n)
        }
      )
    )

    assert(result.isFailure)
    assertEquals(calls.get(), 2, "the third step should never have run")
  }

  // --- SG4 -------------------------------------------------------------------

  test("SG4 memoizeFuture remembers a completed answer") {
    val calls = new AtomicInteger(0)
    val cached = impl.memoizeFuture[Int, Int] { n => calls.incrementAndGet(); Future.successful(n * n) }

    assertEquals(await(cached(5)), 25)
    assertEquals(await(cached(5)), 25)
    assertEquals(calls.get(), 1)

    assertEquals(await(cached(3)), 9)
    assertEquals(calls.get(), 2)
  }

  test("SG4 memoizeFuture survives the thundering herd") {
    // Twenty REAL threads, released at the same instant, all asking for the
    // same uncached key. `Future.traverse` would not do here - it applies the
    // function on one thread, one after another, and there would be no race to
    // lose.
    val calls = new AtomicInteger(0)
    val cached = impl.memoizeFuture[String, Int] { _ =>
      calls.incrementAndGet()
      // slow to CALL, not just slow to complete - this is the window in which a
      // check-then-insert cache lets everybody through
      blocking(Thread.sleep(200))
      Future.successful(42)
    }

    val gate = new CountDownLatch(1)
    val results = new Array[Future[Int]](20)
    val callers = (0 until 20).map { index =>
      new Thread(() => {
        gate.await()
        results(index) = cached("hot key")
      })
    }

    callers.foreach(_.start())
    gate.countDown()
    callers.foreach(_.join())

    assertEquals(results.toList.map(await), List.fill(20)(42))
    assertEquals(calls.get(), 1, s"${calls.get()} identical queries hit the database instead of one")
  }

  test("SG4 different keys are different entries") {
    val calls = new AtomicInteger(0)
    val cached = impl.memoizeFuture[Int, Int] { n => calls.incrementAndGet(); Future.successful(n) }

    await(Future.traverse((1 to 5).toList)(cached))
    await(Future.traverse((1 to 5).toList)(cached))
    assertEquals(calls.get(), 5)
  }
}

/** Red until the attendees make it green. That is the point. */
class BlockGExercisesSuite extends BlockGSuite(BlockGExercises)

/** Green, always. If this one goes red, the reference solutions are wrong. */
class BlockGSolutionsSuite extends BlockGSuite(BlockGSolutions)
