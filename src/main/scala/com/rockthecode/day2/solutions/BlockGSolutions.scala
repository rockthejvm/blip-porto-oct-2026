package com.rockthecode.day2.solutions

import com.rockthecode.day2.*

import java.util.concurrent.{ConcurrentHashMap, TimeUnit, TimeoutException}
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}

/**
 * Day 2, Block G - reference solutions.
 */
object BlockGSolutions extends BlockG {

  import scala.concurrent.ExecutionContext.Implicits.global

  // ---------------------------------------------------------------------------
  // G.1 The one that matters
  // ---------------------------------------------------------------------------

  def sumThree(first: => Future[Int], second: => Future[Int], third: => Future[Int]): Future[Int] = {
    val started1 = first // <- these three lines are the entire exercise
    val started2 = second
    val started3 = third

    for {
      a <- started1
      b <- started2
      c <- started3
    } yield a + b + c
  }

  // The wrong version, which is what everyone writes and what you should put on
  // screen first:
  //
  //   for { a <- first; b <- second; c <- third } yield a + b + c
  //
  // ...takes three times as long, and the reason is that a Future starts when
  // it is CREATED. In the wrong version the expression `second` is not
  // evaluated until `first`'s flatMap runs it, so the second call does not
  // begin until the first has finished. The for-comprehension is not "running
  // them in parallel" and never was - it is sequencing whatever it is given.
  //
  // In the right version all three are already running before the
  // for-comprehension starts, and it just waits for each in turn.
  //
  // The question to leave them with, because it is the whole mental model:
  // AT WHAT MOMENT DOES A FUTURE START? The answer - when you construct it, not
  // when you consume it - explains this bug, explains why you cannot retry a
  // Future (only the function that makes one), and explains why by-name
  // parameters keep showing up around them.
  //
  // Real code hits this with method calls rather than by-name parameters:
  //
  //   for { user <- fetchUser(id); prefs <- fetchPrefs(id) } yield ...
  //
  // ...where the two fetches are independent and it is three lines to fix.

  // ---------------------------------------------------------------------------
  // G.2 Fanning out
  // ---------------------------------------------------------------------------

  def fetchAll[A, B](ids: List[A])(fetch: A => Future[B]): Future[List[B]] =
    Future.traverse(ids)(fetch)

  // The same `traverse` as D.5 and E.5, third container, same name, same shape.
  // Worth saying out loud that this is now the third time - the abstraction
  // they have not been taught is doing visible work.
  //
  // `Future.sequence(ids.map(fetch))` is equivalent and one step longer.

  def fetchAllSettled[A, B](ids: List[A])(fetch: A => Future[B]): Future[List[Either[Throwable, B]]] =
    Future.traverse(ids) { id =>
      fetch(id)
        .map(result => Right(result): Either[Throwable, B])
        .recover { case error => Left(error) }
    }

  // Make each Future incapable of failing FIRST, then combine. Once no
  // individual Future can fail, the combined one cannot either, and the
  // "all-or-nothing" behaviour of traverse becomes irrelevant rather than
  // needing to be worked around.
  //
  // That is a general move worth naming: instead of handling the failure of the
  // combination, remove the possibility of failure from the parts. The same
  // trick turns up in E.5 and it is easier to reason about both times.

  // ---------------------------------------------------------------------------
  // G.3 When it goes wrong
  // ---------------------------------------------------------------------------

  def fetchOrDefault[A](fetch: => Future[A], fallback: A): Future[A] =
    fetch.recover { case _ => fallback }

  def fetchWithBackup[A](primary: => Future[A], backup: => Future[A]): Future[A] =
    primary.recoverWith { case _ => backup }

  // `recover` gives a VALUE, `recoverWith` gives another FUTURE. It is exactly
  // `map` versus `flatMap`, on the failure side, and if somebody notices that
  // out loud the block has done its job.
  //
  // Both take their recovery as a PartialFunction, so you can recover from some
  // failures and let others through:
  //
  //   fetch.recover { case _: TimeoutException => fallback }
  //
  // ...which is usually what you want and almost never what people write.
  // `case _ =>` swallows the OutOfMemoryError too.

  def describeOutcome(future: Future[Int]): Future[String] =
    future.transform {
      case Success(value) => Success(s"ok: $value")
      case Failure(error) => Success(s"failed: ${error.getMessage}")
    }

  // `transform` sees the `Try` and produces a `Try`, so it can do both sides at
  // once and can turn a failure into a success - which is what makes the
  // returned Future one that never fails.
  //
  // The same thing with map + recover is two passes and reads worse:
  //   future.map(v => s"ok: $v").recover { case e => s"failed: ${e.getMessage}" }

  // ---------------------------------------------------------------------------
  // G.4 Promise
  // ---------------------------------------------------------------------------

  def fetchAsFuture(id: Int): Future[String] = {
    val promise = Promise[String]()

    LegacyClient.fetch(
      id,
      result => promise.success(result),
      error => promise.failure(error)
    )

    promise.future
  }

  // Five lines, and this is the shape that unlocks an entire legacy codebase.
  // Every callback API, every listener, every `CompletableFuture`, every
  // `onComplete(result, error)` in the JDK becomes composable through exactly
  // this: make a Promise, hand its `future` to the caller, complete it from the
  // callback.
  //
  // Two things to point at:
  //
  //   - `promise.success` throws if called twice. `trySuccess` returns a
  //     Boolean instead. Use `try*` whenever more than one thing might complete
  //     the promise - which is every race, including SG1 and `withTimeout`.
  //   - `promise.future` is handed out BEFORE anything completes it, and that is
  //     the point. The caller gets a handle to an answer that does not exist
  //     yet.
  //
  // The condensed version, once they have seen the long one:
  //   LegacyClient.fetch(id, promise.success, promise.failure)

  def delayed[A](delay: FiniteDuration)(value: => A): Future[A] = {
    val promise = Promise[A]()
    val task: Runnable = () => promise.complete(Try(value))

    Clock.scheduler.schedule(task, delay.toMillis, TimeUnit.MILLISECONDS)

    promise.future
  }

  // The same five lines as `fetchAsFuture`, and that is worth pointing out
  // explicitly: a callback API and a scheduler are the same problem. Something
  // will produce an answer later and gives you no way to compose with it; a
  // Promise is the adapter, every time.
  //
  // `promise.complete(Try(value))` rather than `promise.success(value)`: if
  // evaluating `value` throws, `Try` catches it and the Future fails with it.
  // Write `success` instead and the exception escapes on the scheduler thread,
  // where nobody is listening, and the Future simply never completes - so the
  // caller waits forever for a failure that already happened. That is a
  // genuinely horrible afternoon and it is one word away.
  //
  // Note `value` is by-name and is evaluated inside the task, not at the call
  // site. `delayed(1.second)(expensiveThing())` should not compute anything for
  // a second. Block F, one more time.

  def withTimeout[A](future: Future[A], after: FiniteDuration): Future[A] =
    Future.firstCompletedOf(
      List(
        future,
        delayed(after)(throw new TimeoutException(s"timed out after $after"))
      )
    )

  // Two things racing; the loser is ignored. Because `delayed` evaluates its
  // value lazily, the exception is constructed and thrown inside the scheduled
  // task, and `Try` inside `delayed` turns it into a failed Future.
  //
  // The naive timeout - `Future { Thread.sleep(after.toMillis); throw ... }` -
  // works and burns a pool thread for the full duration of every timeout you
  // set. On the global EC, enough concurrent timeouts starve the pool that the
  // work being timed needs in order to finish, so everything times out, so you
  // set more timeouts. That is a real incident shape and it is why `delayed`
  // was an exercise rather than a fixture.
  //
  // Say clearly what this does NOT do: the original Future keeps running. There
  // is no cancellation in `scala.concurrent`. You have stopped waiting, which
  // is not the same as having stopped the work.

  // ---------------------------------------------------------------------------
  // G.5 Not all at once
  // ---------------------------------------------------------------------------

  def batched[A, B](ids: List[A], batchSize: Int)(fetch: A => Future[B]): Future[List[B]] =
    ids
      .grouped(math.max(batchSize, 1))
      .foldLeft(Future.successful(List.empty[B])) { (accumulated, group) =>
        for {
          done <- accumulated          // wait for the previous group...
          next <- Future.traverse(group)(fetch) // ...then run this one, all together
        } yield done ++ next
      }

  // Sequential between groups, parallel within a group. The `foldLeft` over a
  // Future is the same move as A.2's fold over an account - the accumulator is
  // just a Future now, and `flatMap` is what makes each step wait for the last.
  //
  // Note `Future.traverse(group)` is INSIDE the for-comprehension, so it is not
  // constructed until the previous group has finished. In G.1 that was the bug;
  // here it is precisely the point. Same mechanism, opposite intent - which is
  // the strongest possible argument that you have to understand when a Future
  // starts rather than memorising a rule.
  //
  // Be precise about what this is, because the obvious name for it would be a
  // lie. It BOUNDS concurrency - never more than `batchSize` in flight - but it
  // does not MAINTAIN it: a batch of three whose slowest call takes a second
  // leaves two slots idle for most of that second, waiting for the straggler.
  //
  // A true concurrency limiter starts a new call the moment any one finishes,
  // and wants a queue and a `Semaphore` rather than ten lines. Batching is the
  // version you can write from memory and it is usually enough. Knowing exactly
  // which of the two you have built - and being able to say so in a code review
  // - is the actual skill here.

  // ===========================================================================
  //
  //  STRETCH SECTION
  //
  // ===========================================================================

  def firstSuccessOf[A](futures: List[Future[A]]): Future[A] = {
    val promise = Promise[A]()
    val remaining = new AtomicInteger(futures.size)

    if (futures.isEmpty) promise.failure(new NoSuchElementException("no futures to race"))
    else
      futures.foreach { future =>
        future.onComplete {
          case Success(value) => promise.trySuccess(value)
          case Failure(error) => if (remaining.decrementAndGet() == 0) promise.tryFailure(error)
        }
      }

    promise.future
  }

  // `trySuccess`, not `success`: several of them may finish successfully and
  // only the first one wins. With `success` the second would throw, on a thread
  // nobody is watching, and the exception would vanish.
  //
  // The counter is shared and mutated across threads, which is SF1's contained
  // mutation with a new problem: `remaining - 1` from two threads at once can
  // lose an update, and then the promise is never completed and the caller
  // waits forever. `AtomicInteger.decrementAndGet` is one indivisible step.
  // This is the smallest honest example of why "just use a var" stops working
  // the moment there are two threads.
  //
  // Contrast with `Future.firstCompletedOf`, which is one line and answers a
  // different question: first to FINISH, success or not. A source that fails in
  // 1ms beats one that succeeds in 10ms, your fallback never runs, and the bug
  // only appears under the conditions that made you add a fallback.

  def retryWithBackoff[A](delays: LazyList[FiniteDuration], attempts: Int)(action: () => Future[A]): Future[A] =
    action().recoverWith {
      case error if attempts <= 1 => Future.failed(error)
      case _ =>
        delayed(delays.head)(())
          .flatMap(_ => retryWithBackoff(delays.tail, attempts - 1)(action))
    }

  // Three blocks arriving at once and none of them knowing about the others:
  // F.5 produced the schedule as a lazy infinite value, E.SE2 established that
  // the last failure is the one to report, and G.4's `delayed` - which they
  // wrote themselves twenty minutes ago - waits without occupying a thread. `delays.tail` walks the schedule as the retries walk it -
  // the LazyList is doing exactly the job it was built for.
  //
  // `recoverWith` with a guard is doing the "have we run out of attempts"
  // branch inside the pattern match, which is neater than an `if` around the
  // whole thing and reads in the order you would say it.
  //
  // Note `action()` and not `action` - the whole reason it is a function is
  // that a Future cannot be retried. Retrying is re-running the thing that
  // MAKES the Future. Somebody always asks.

  def foldSequentially[A, B](items: List[A], zero: B)(step: (B, A) => Future[B]): Future[B] =
    items.foldLeft(Future.successful(zero)) { (accumulated, item) =>
      accumulated.flatMap(previous => step(previous, item))
    }

  // The most literal possible translation of A.2's `applyAll`, with a Future
  // wrapped around the state. It is sequential for a reason that is not an
  // implementation detail: `flatMap` cannot construct the next step without the
  // previous value, so it cannot start it either.
  //
  // That is the same property that made E.4 unable to accumulate errors. One
  // mechanism, two consequences, and being able to see that they are the same
  // thing is roughly the point of the whole day.

  def memoizeFuture[A, B](compute: A => Future[B]): A => Future[B] = {
    val cache = new ConcurrentHashMap[A, Future[B]]()

    key => cache.computeIfAbsent(key, _ => compute(key))
  }

  // The insight that makes this easy: CACHE THE FUTURE, NOT THE VALUE. A Future
  // for work already in progress is a perfectly good thing to hand the second
  // caller - they wanted the answer when it exists, and that is exactly what
  // they get.
  //
  // Everything after that is making "check, then insert" indivisible.
  // `ConcurrentHashMap.computeIfAbsent` runs its function at most once per key
  // no matter how many threads arrive together, which is the entire guarantee
  // needed - and it is a class everybody in the room already knows from Java.
  //
  // Why not `getOrElseUpdate` on a plain mutable Map, as in F.SF1? Because it
  // is check-then-act with a gap in the middle: two threads both look, both
  // find nothing, both compute. In F.SF1 that was wasted work. Here it is a
  // cache stampede - a popular entry expires, four hundred requests miss
  // together, and four hundred identical queries hit a database sized for one.
  // The test demonstrates exactly that with twenty real threads.
  //
  // One honest caveat about `computeIfAbsent`, worth raising because somebody
  // will hit it: the mapping function runs while the map holds a lock on that
  // bin, so a `compute` that is slow to RETURN (as opposed to slow to complete
  // its Future) blocks other writers. Usually `compute` just constructs a
  // Future and returns immediately, and none of this matters. When it does, the
  // version that holds no lock is:
  //
  //   val promise = Promise[B]()
  //   val existing = cache.putIfAbsent(key, promise.future)
  //   if (existing != null) existing
  //   else { promise.completeWith(compute(key)); promise.future }
  //
  // ...which allocates a Promise on every call, including hits. Pick your
  // trade-off deliberately; both are correct.
  //
  // And the note to end the day on: a FAILED future stays in this cache
  // forever, so one transient error is remembered permanently. Fixing that
  // means evicting on failure, and then thinking about the callers still
  // holding the failed one. Caches are easy to write and hard to write well,
  // which is a fair summary of concurrency in general.
}
