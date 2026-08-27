package com.rockthecode.day2

import com.rockthecode.day2.solutions.BlockESolutions

import java.util.concurrent.atomic.AtomicInteger
import scala.util.{Failure, Success, Try}

/**
 * Day 2, Block E - the tests.
 *
 *   sbt blockE                                                 <- attendees
 *   sbt "testOnly com.rockthecode.day2.BlockESolutionsSuite"   <- trainer
 */
abstract class BlockESuite(impl: BlockE) extends munit.FunSuite {

  import PricingError.*
  import ValidationError.*
  import LookupError.*

  // --- E.1 -------------------------------------------------------------------

  test("E.1 priceOf finds a price") {
    assertEquals(impl.priceOf("widget"), Success(BigDecimal(24)))
    assertEquals(impl.priceOf("gizmo"), Success(BigDecimal(40)))
  }

  test("E.1 priceOf catches instead of throwing") {
    val result = impl.priceOf("nothing")
    assert(result.isFailure, "an unknown sku must not escape as an exception")
    assert(
      result.failed.get.isInstanceOf[NoSuchElementException],
      s"expected the original exception, got ${result.failed.get}"
    )
  }

  test("E.1 unitCost chains two calls that can each throw") {
    assertEquals(impl.unitCost("widget", 4), Success(BigDecimal(6)))
    assertEquals(impl.unitCost("doohickey", 3), Success(BigDecimal(2)))
  }

  test("E.1 a failure anywhere in the chain short-circuits") {
    assert(impl.unitCost("nothing", 4).isFailure, "unknown sku")
    assert(impl.unitCost("widget", 0).isFailure, "division by zero")
  }

  test("E.1 priceOrDefault decides what to do about the failure") {
    assertEquals(impl.priceOrDefault("widget", BigDecimal(99)), BigDecimal(24))
    assertEquals(impl.priceOrDefault("nothing", BigDecimal(99)), BigDecimal(99))
  }

  // --- E.2 -------------------------------------------------------------------

  test("E.2 priceEither on the happy path") {
    assertEquals(impl.priceEither("widget"), Right(BigDecimal(24)))
  }

  test("E.2 the Throwable becomes one of OUR errors, naming the sku it did not have") {
    assertEquals(impl.priceEither("nothing"), Left(UnknownSku("nothing")))
  }

  test("E.2 unitCostEither turns two exception types into two domain errors") {
    assertEquals(impl.unitCostEither("widget", 4), Right(BigDecimal(6)))
    assertEquals(impl.unitCostEither("nothing", 4), Left(UnknownSku("nothing")))
    assertEquals(impl.unitCostEither("widget", 0), Left(InvalidQuantity(0)))
  }

  test("E.2 the sku error wins when both are wrong, because it happens first") {
    assertEquals(impl.unitCostEither("nothing", 0), Left(UnknownSku("nothing")))
  }

  // --- E.3 -------------------------------------------------------------------

  test("E.3 validateName") {
    assertEquals(impl.validateName("Ana"), Right("Ana"))
    assertEquals(impl.validateName(""), Left(NameIsEmpty))
    assertEquals(impl.validateName("   "), Left(NameIsEmpty), "blank is empty")
  }

  test("E.3 validateEmail") {
    assertEquals(impl.validateEmail("ana@blip.pt"), Right("ana@blip.pt"))
    assertEquals(impl.validateEmail("nope"), Left(EmailIsInvalid("nope")))
    assertEquals(impl.validateEmail(""), Left(EmailIsInvalid("")))
  }

  test("E.3 validateAge tells the two failures apart") {
    assertEquals(impl.validateAge("30"), Right(30))
    assertEquals(impl.validateAge("18"), Right(18), "eighteen is old enough")
    assertEquals(impl.validateAge("12"), Left(AgeIsTooLow(12)))
    assertEquals(impl.validateAge("old"), Left(AgeIsNotANumber("old")))
    assertEquals(impl.validateAge(""), Left(AgeIsNotANumber("")))
  }

  private val goodForm = SignupForm("Ana", "ana@blip.pt", "30")

  test("E.3 validateFailFast on a good form") {
    assertEquals(impl.validateFailFast(goodForm), Right(Signup("Ana", "ana@blip.pt", 30)))
  }

  test("E.3 validateFailFast stops at the first problem and never sees the rest") {
    assertEquals(impl.validateFailFast(SignupForm("", "nope", "12")), Left(NameIsEmpty))
    assertEquals(impl.validateFailFast(SignupForm("Ana", "nope", "12")), Left(EmailIsInvalid("nope")))
    assertEquals(impl.validateFailFast(SignupForm("Ana", "ana@blip.pt", "12")), Left(AgeIsTooLow(12)))
  }

  // --- E.4 -------------------------------------------------------------------

  test("E.4 validateAll on a good form") {
    assertEquals(impl.validateAll(goodForm), Right(Signup("Ana", "ana@blip.pt", 30)))
  }

  test("E.4 validateAll reports EVERY problem, in field order") {
    // The for-comprehension version passes the test above and fails this one.
    assertEquals(
      impl.validateAll(SignupForm("", "nope", "12")),
      Left(List(NameIsEmpty, EmailIsInvalid("nope"), AgeIsTooLow(12)))
    )
  }

  test("E.4 validateAll with two of three wrong") {
    assertEquals(
      impl.validateAll(SignupForm("", "ana@blip.pt", "old")),
      Left(List(NameIsEmpty, AgeIsNotANumber("old")))
    )
    assertEquals(
      impl.validateAll(SignupForm("Ana", "nope", "12")),
      Left(List(EmailIsInvalid("nope"), AgeIsTooLow(12)))
    )
  }

  test("E.4 one problem is still a list of one") {
    assertEquals(impl.validateAll(SignupForm("Ana", "ana@blip.pt", "12")), Left(List(AgeIsTooLow(12))))
  }

  // --- E.5 -------------------------------------------------------------------

  test("E.5 sequence is all-or-nothing") {
    assertEquals(impl.sequence(List(Right(1), Right(2))), Right(List(1, 2)))
    assertEquals(impl.sequence(List[Either[String, Int]](Right(1), Left("bad"), Right(3))), Left("bad"))
    assertEquals(impl.sequence(List.empty[Either[String, Int]]), Right(Nil))
  }

  test("E.5 sequence keeps the FIRST error") {
    assertEquals(
      impl.sequence(List[Either[String, Int]](Left("first"), Left("second"))),
      Left("first")
    )
  }

  test("E.5 traverse converts as it goes") {
    assertEquals(impl.traverse(List("1", "2"))(_.toIntOption.toRight("nope")), Right(List(1, 2)))
    assertEquals(impl.traverse(List("1", "x"))(_.toIntOption.toRight("nope")), Left("nope"))
    assertEquals(impl.traverse(List.empty[String])(_.toIntOption.toRight("nope")), Right(Nil))
  }

  test("E.5 partitionResults keeps both sides, in order") {
    assertEquals(
      impl.partitionResults(List[Either[String, Int]](Right(1), Left("bad"), Right(3), Left("worse"))),
      (List("bad", "worse"), List(1, 3))
    )
    assertEquals(impl.partitionResults(List.empty[Either[String, Int]]), (Nil, Nil))
  }

  test("E.5 sequence and partitionResults answer different questions about the same input") {
    val batch = List[Either[String, Int]](Right(1), Left("bad"), Right(3))
    assertEquals(impl.sequence(batch), Left("bad"), "all or nothing")
    assertEquals(impl.partitionResults(batch), (List("bad"), List(1, 3)), "and here is what we salvaged")
  }

  // --- E.6 -------------------------------------------------------------------

  private val directory: Map[String, Colleague] =
    Map(
      "ana" -> Colleague("ana", Some("bruno"), Some("ana@blip.pt")),
      "bruno" -> Colleague("bruno", Some("carla"), Some("bruno@blip.pt")),
      "carla" -> Colleague("carla", None, Some("carla@blip.pt")),
      "dora" -> Colleague("dora", Some("ghost"), Some("dora@blip.pt")),
      "eva" -> Colleague("eva", Some("frank"), Some("eva@blip.pt")),
      "frank" -> Colleague("frank", None, None)
    )

  test("E.6 the happy path is unchanged from D.6") {
    assertEquals(impl.managerEmailEither(directory, "ana"), Right("bruno@blip.pt"))
  }

  test("E.6 the four Nones of D.6 are now four different answers") {
    assertEquals(impl.managerEmailEither(directory, "nobody"), Left(NoSuchPerson("nobody")))
    assertEquals(impl.managerEmailEither(directory, "carla"), Left(NoManagerRecorded("carla")))
    assertEquals(impl.managerEmailEither(directory, "dora"), Left(NoSuchPerson("ghost")))
    assertEquals(impl.managerEmailEither(directory, "eva"), Left(NoEmailOnRecord("frank")))
  }

  test("E.6 an error names the entity it is actually about") {
    // "dora" exists; it is her MANAGER who does not. An error that said
    // NoSuchPerson("dora") would send somebody to look in the wrong place.
    assertEquals(impl.managerEmailEither(directory, "dora"), Left(NoSuchPerson("ghost")))
    assertEquals(impl.managerEmailEither(directory, "eva"), Left(NoEmailOnRecord("frank")))
  }

  // ===========================================================================
  //
  //  STRETCH SECTION
  //
  // ===========================================================================

  // --- SE1 -------------------------------------------------------------------

  test("SE1 map2Accumulating combines two successes") {
    assertEquals(
      impl.map2Accumulating(Right(2): Either[List[String], Int], Right(3): Either[List[String], Int])(_ + _),
      Right(5)
    )
  }

  test("SE1 map2Accumulating keeps errors from BOTH sides") {
    assertEquals(
      impl.map2Accumulating(
        Left(List("a")): Either[List[String], Int],
        Left(List("b", "c")): Either[List[String], Int]
      )(_ + _),
      Left(List("a", "b", "c"))
    )
  }

  test("SE1 map2Accumulating with one side wrong") {
    assertEquals(
      impl.map2Accumulating(Left(List("a")): Either[List[String], Int], Right(3): Either[List[String], Int])(_ + _),
      Left(List("a"))
    )
    assertEquals(
      impl.map2Accumulating(Right(2): Either[List[String], Int], Left(List("b")): Either[List[String], Int])(_ + _),
      Left(List("b"))
    )
  }

  // --- SE2 -------------------------------------------------------------------

  test("SE2 retry stops as soon as it works") {
    val calls = new AtomicInteger(0)
    val action = () => { calls.incrementAndGet(); Try(42) }

    assertEquals(impl.retry(3)(action), Success(42))
    assertEquals(calls.get(), 1, "it worked the first time - do not call it again")
  }

  test("SE2 retry keeps trying until it works") {
    val calls = new AtomicInteger(0)
    val action = () => {
      val attempt = calls.incrementAndGet()
      if (attempt < 3) Failure(new RuntimeException(s"attempt $attempt")) else Success(attempt)
    }

    assertEquals(impl.retry(5)(action), Success(3))
    assertEquals(calls.get(), 3)
  }

  test("SE2 retry gives up after the right number of attempts, with the LAST failure") {
    val calls = new AtomicInteger(0)
    val action = () => Failure(new RuntimeException(s"attempt ${calls.incrementAndGet()}"))

    val result = impl.retry(3)(action)
    assertEquals(calls.get(), 3)
    assertEquals(result.failed.get.getMessage, "attempt 3", "the last failure describes where we ended up")
  }

  test("SE2 retry always tries at least once") {
    val calls = new AtomicInteger(0)
    val action = () => { calls.incrementAndGet(); Try(1) }

    assertEquals(impl.retry(0)(action), Success(1))
    assertEquals(calls.get(), 1)
  }

  // --- SE3 -------------------------------------------------------------------

  test("SE3 mapError translates the left and leaves the right alone") {
    assertEquals(impl.mapError(Left(UnknownSku("x")): Either[PricingError, Int])(e => s"pricing: $e"), Left("pricing: UnknownSku(x)"))
    assertEquals(impl.mapError(Right(7): Either[PricingError, Int])(e => s"pricing: $e"), Right(7))
  }

  // --- SE4 -------------------------------------------------------------------

  test("SE4 validateBatch on rows that are all fine") {
    val forms = List(goodForm, SignupForm("Bruno", "bruno@blip.pt", "41"))
    assertEquals(
      impl.validateBatch(forms),
      Right(List(Signup("Ana", "ana@blip.pt", 30), Signup("Bruno", "bruno@blip.pt", 41)))
    )
    assertEquals(impl.validateBatch(Nil), Right(Nil))
  }

  test("SE4 validateBatch tags every error with the row it came from") {
    val forms = List(goodForm, SignupForm("", "nope", "12"), SignupForm("Carla", "carla@blip.pt", "old"))
    assertEquals(
      impl.validateBatch(forms),
      Left(
        List(
          (1, NameIsEmpty),
          (1, EmailIsInvalid("nope")),
          (1, AgeIsTooLow(12)),
          (2, AgeIsNotANumber("old"))
        )
      )
    )
  }

  test("SE4 one bad row rejects the whole batch - which is a choice, not an accident") {
    val forms = List(goodForm, goodForm, SignupForm("Zed", "zed@blip.pt", "9"), goodForm)
    assertEquals(impl.validateBatch(forms), Left(List((2, AgeIsTooLow(9)))))
  }
}

/** Red until the attendees make it green. That is the point. */
class BlockEExercisesSuite extends BlockESuite(BlockEExercises)

/** Green, always. If this one goes red, the reference solutions are wrong. */
class BlockESolutionsSuite extends BlockESuite(BlockESolutions)
