package com.rockthecode.day2

import scala.util.Try

/**
 * Day 2, Block E - Try, Either, and modelling errors.
 *
 * This trait is the exercise brief. Write your answers in `BlockEExercises`.
 */
trait BlockE {

  // ---------------------------------------------------------------------------
  // E.1 Try
  //
  // `LegacyPricing` (bottom of the file) is the API you have been given.
  // ---------------------------------------------------------------------------

  /**
   * The price of one unit, or the failure.
   *
   * `priceOf("widget")  == Success(25)`
   * `priceOf("nothing")` is a `Failure(NoSuchElementException)`
   */
  def priceOf(sku: String): Try[BigDecimal]

  /**
   * The cost of one unit when you buy `quantity` of them together - so, the
   * total for the batch divided by the quantity.
   */
  def unitCost(sku: String, quantity: Int): Try[BigDecimal]

  /**
   * The price, or a fallback if anything at all went wrong.
   *
   * `priceOrDefault("nothing", 99) == 99`
   *
   */
  def priceOrDefault(sku: String, fallback: BigDecimal): BigDecimal

  // ---------------------------------------------------------------------------
  // E.2 Crossing the boundary: Try in, Either out
  //
  // ---------------------------------------------------------------------------

  /**
   * The price, as a domain result.
   *
   * `priceEither("widget")  == Right(25)`
   * `priceEither("nothing") == Left(PricingError.UnknownSku("nothing"))`
   *
   */
  def priceEither(sku: String): Either[PricingError, BigDecimal]

  /**
   * The same for `unitCost`. Two different exceptions become two different
   * domain errors - the unknown sku, and the impossible quantity.
   *
   * `unitCostEither("widget", 0) == Left(PricingError.InvalidQuantity(0))`
   */
  def unitCostEither(sku: String, quantity: Int): Either[PricingError, BigDecimal]

  // ---------------------------------------------------------------------------
  // E.3 Validation, one failure at a time
  //
  // The rules:
  //   name   must not be blank
  //   email  must contain an "@"
  //   age    must be a whole number, and at least 18
  // ---------------------------------------------------------------------------

  def validateName(raw: String): Either[ValidationError, String]

  def validateEmail(raw: String): Either[ValidationError, String]

  def validateAge(raw: String): Either[ValidationError, Int]

  /**
   * Validate a whole form, stopping at the first problem.
   *
   */
  def validateFailFast(form: SignupForm): Either[ValidationError, Signup]

  // ---------------------------------------------------------------------------
  // E.4 Validation, all the failures at once
  //
  // ---------------------------------------------------------------------------

  /**
   * `validateAll(SignupForm("", "nope", "12"))`
   *   `== Left(List(NameIsEmpty, EmailIsInvalid("nope"), AgeIsTooLow(12)))`
   *
   * Errors come back in field order: name, then email, then age.
   */
  def validateAll(form: SignupForm): Either[List[ValidationError], Signup]

  // ---------------------------------------------------------------------------
  // E.5 Lists of results
  //
  // ---------------------------------------------------------------------------

  /** All-or-nothing, keeping the FIRST error. `sequence(Nil) == Right(Nil)`. */
  def sequence[E, A](results: List[Either[E, A]]): Either[E, List[A]]

  /** The same, converting as it goes. */
  def traverse[E, A, B](items: List[A])(f: A => Either[E, B]): Either[E, List[B]]

  /**
   * Best effort: the errors on the left, the successes on the right, both in
   * their original order.
   *
   * `partitionResults(List(Right(1), Left("bad"), Right(3))) == (List("bad"), List(1, 3))`
   *
   */
  def partitionResults[E, A](results: List[Either[E, A]]): (List[E], List[A])

  // ---------------------------------------------------------------------------
  // E.6 D.6, answered
  //
  // Block D's `managerEmail` returned None four different ways and left the
  // ---------------------------------------------------------------------------

  /**
   * `managerEmailEither(directory, "carla") == Left(NoManagerRecorded("carla"))`
   * `managerEmailEither(directory, "dora")  == Left(NoSuchPerson("ghost"))`
   *
   */
  def managerEmailEither(directory: Map[String, Colleague], name: String): Either[LookupError, String]

  // ===========================================================================
  //
  //  STRETCH SECTION - only if you finished the block above.
  //
  // ===========================================================================

  /**
   * SE1. E.4, generalised.
   *
   * Combine two results that each carry a LIST of errors: if both are Right,
   * combine the values; if either is Left, come back with every error from both
   * sides, left to right.
   *
   * `map2Accumulating(Left(List("a")), Left(List("b")))(_ + _) == Left(List("a", "b"))`
   *
   */
  def map2Accumulating[E, A, B, C](
      first: Either[List[E], A],
      second: Either[List[E], B]
  )(combine: (A, B) => C): Either[List[E], C]

  /**
   * SE2. Try something up to `times` times, and take the first success.
   *
   * `retry(3)(action)` calls `action` once, and only calls it again if it
   * failed. On total failure it comes back with the LAST failure, because that
   * is the one that describes the state the world is actually in.
   *
   * `times` below 1 still means one attempt.
   *
   */
  def retry[A](times: Int)(action: () => Try[A]): Try[A]

  /**
   * SE3. Translate an error.
   *
   * `mapError(Left(UnknownSku("x")))(e => s"pricing: $e") == Left("pricing: ...")`
   * A `Right` passes through untouched.
   */
  def mapError[E, F, A](result: Either[E, A])(f: E => F): Either[F, A]

  /**
   * SE4. Validate a whole batch of forms, reporting every error in every row,
   * tagged with the row it came from.
   *
   * `validateBatch(List(good, bad))` gives `Left(List((1, ...), (1, ...)))` -
   * row index first, zero-based, and a row contributes as many entries as it
   * has problems.
   *
   * If every row is fine, `Right` of all the signups, in order.
   *
   */
  def validateBatch(forms: List[SignupForm]): Either[List[(Int, ValidationError)], List[Signup]]
}

// -----------------------------------------------------------------------------
// Types
// -----------------------------------------------------------------------------

/**
 * The API you have been given. It can throw, and the signature doesn't show it.
 */
object LegacyPricing {

  private val catalogue: Map[String, BigDecimal] =
    Map("widget" -> BigDecimal(24), "gizmo" -> BigDecimal(40), "doohickey" -> BigDecimal(6))

  /** Throws NoSuchElementException if the sku is not in the catalogue. */
  def unitPrice(sku: String): BigDecimal = catalogue(sku)

  /** Throws ArithmeticException if quantity is zero. */
  def perUnit(total: BigDecimal, quantity: Int): BigDecimal = total / quantity
}

enum PricingError {
  case UnknownSku(sku: String)
  case InvalidQuantity(quantity: Int)
  case Unexpected(message: String)
}

case class SignupForm(name: String, email: String, age: String)

case class Signup(name: String, email: String, age: Int)

enum ValidationError {
  case NameIsEmpty
  case EmailIsInvalid(value: String)
  case AgeIsNotANumber(value: String)
  case AgeIsTooLow(age: Int)
}

enum LookupError {
  case NoSuchPerson(name: String)
  case NoManagerRecorded(name: String)
  case NoEmailOnRecord(name: String)
}
