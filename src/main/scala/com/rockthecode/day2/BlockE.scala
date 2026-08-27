package com.rockthecode.day2

import scala.util.Try

/**
 * Day 2, Block E - Try, Either, and modelling errors.
 *
 * You have been building up to this all day, mostly by running into its
 * absence:
 *
 *   Block A, SA3   rejection reasons as bare Strings, because there was
 *                  nothing better yet
 *   Block B, SB4   a bracket matcher that knows the input is broken and cannot
 *                  say which bracket, or where
 *   Block C, C.2   `TransitionResult.Moved | Rejected` - a type you designed by
 *                  hand, holding either an answer or a reason
 *   Block D, D.4   `map2` reporting one absence when two things were missing
 *   Block D, D.6   four different failures arriving as the same `None`
 *
 * C.2 is the one to put on screen first, because `Either[L, R]` IS that type,
 * already written, with map and flatMap and a for-comprehension attached. And
 * `Option` is `Either` with the left-hand side thrown away - which is exactly
 * why D.6 could not tell you what went wrong.
 *
 * Three tools, three jobs, and mixing them up is most of the confusion:
 *
 *   exceptions   invisible in the type, unwind the stack. Fine at the very
 *                edges of a program; a menace in the middle of one.
 *   Try[A]       "this either worked or something was thrown". Carries a
 *                Throwable you did not choose. Its job is the BOUNDARY with
 *                code that throws.
 *   Either[E, A] "this either worked or E happened", where E is a type YOU
 *                designed. Its job is everywhere else.
 *
 * The move this block is really teaching: catch at the boundary, classify
 * immediately, and never let a Throwable travel through your own code.
 *
 * Same rules: no `var`, no `while`, no mutable collection. And - as in Block D -
 * no `.get`, on `Try` or on `Option`.
 *
 * This trait is the exercise brief. Write your answers in `BlockEExercises`.
 */
trait BlockE {

  // ---------------------------------------------------------------------------
  // E.1 Try, at the boundary with code that throws
  //
  // `LegacyPricing` (bottom of this file) is the API you have been handed. It
  // throws. Its signatures say `BigDecimal` and mean "BigDecimal, or an
  // exception, good luck".
  // ---------------------------------------------------------------------------

  /**
   * The price of one unit, or the failure.
   *
   * `priceOf("widget")  == Success(25)`
   * `priceOf("nothing")` is a `Failure(NoSuchElementException)`
   *
   * `Try(...)` catches, the same way `Option(...)` checked for null in D.1.
   * Same idea, one layer up.
   */
  def priceOf(sku: String): Try[BigDecimal]

  /**
   * The cost of one unit when you buy `quantity` of them together - so, the
   * total for the batch divided by the quantity.
   *
   * Two calls that can each throw, chained. A `Failure` anywhere short-circuits
   * the rest, and you do not write a single line to make that happen: that is
   * what `flatMap` on `Try` is for, and therefore what a for-comprehension over
   * `Try` gives you.
   *
   * `unitCost("widget", 0)` fails, because dividing by zero throws.
   */
  def unitCost(sku: String, quantity: Int): Try[BigDecimal]

  /**
   * The price, or a fallback if anything at all went wrong.
   *
   * `priceOrDefault("nothing", 99) == 99`
   *
   * This is where a failure stops being a failure and becomes a decision - and
   * as in D.1, notice how far this is from where the exception was thrown.
   */
  def priceOrDefault(sku: String, fallback: BigDecimal): BigDecimal

  // ---------------------------------------------------------------------------
  // E.2 Crossing the boundary: Try in, Either out
  //
  // `Try` did its job - it stopped the exception. But a `Throwable` is a
  // terrible thing to pass around: it can be anything, the compiler will not
  // help you handle it, and it drags a stack trace from a library you do not
  // control through your own domain.
  //
  // So classify it, once, at the edge. After this line, the failures are yours.
  // ---------------------------------------------------------------------------

  /**
   * The price, as a domain result.
   *
   * `priceEither("widget")  == Right(25)`
   * `priceEither("nothing") == Left(PricingError.UnknownSku("nothing"))`
   *
   * `Try` has `.toEither`, which gives you `Either[Throwable, A]`. Then
   * `.left.map` to translate the Throwable into one of YOUR errors - matching
   * on the exception type, with a catch-all for the ones you did not expect.
   *
   * Note that you know the sku here and the exception does not. Context is
   * always richer at the point of the call than it is inside the failure, which
   * is exactly why translating early beats translating late.
   */
  def priceEither(sku: String): Either[PricingError, BigDecimal]

  /**
   * The same for `unitCost`. Two different exceptions become two different
   * domain errors - the unknown sku, and the impossible quantity.
   *
   * `unitCostEither("widget", 0) == Left(PricingError.InvalidQuantity(0))`
   *
   * A for-comprehension over `Either` works exactly like one over `Option` or
   * `Try`, which by now should be unsurprising and is the whole point.
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

  /** Two things can go wrong here, and they are different errors. */
  def validateAge(raw: String): Either[ValidationError, Int]

  /**
   * Validate a whole form, stopping at the first problem.
   *
   * A for-comprehension. `flatMap` on Either short-circuits on a `Left`, so the
   * first failure wins and nothing after it even runs.
   *
   * That is the right behaviour surprisingly often - a pipeline where step two
   * makes no sense if step one failed. It is the wrong behaviour for a form,
   * and E.4 is about why.
   */
  def validateFailFast(form: SignupForm): Either[ValidationError, Signup]

  // ---------------------------------------------------------------------------
  // E.4 Validation, all the failures at once
  //
  // THE exercise of this block. Do not read ahead; try it with a
  // for-comprehension first and find out what happens.
  //
  // What happens is that you cannot. `flatMap` is sequential by definition - it
  // needs the value from step one to decide what step two even is - so it
  // physically cannot look at the second field once the first has failed. This
  // is not a gap in Scala's standard library; it is what the type means.
  //
  // Which leaves you doing it by hand: validate the three fields
  // INDEPENDENTLY, collect the Lefts, and then decide. If there are no Lefts,
  // build the Signup from the Rights.
  //
  // This is the moment where every functional library in the ecosystem earns
  // its keep - Cats calls the type `Validated` and the operation `mapN`, and
  // once you have written this by hand you will know exactly what it does and
  // why it is a different type from Either rather than a method on it. Blip
  // does not use those libraries, so knowing the hand-rolled version is not a
  // consolation prize; it is the version you will ship.
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
  // D.5 again, one type along. The functions are the same shape, which is the
  // observation worth having.
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
   * There is a single method on List that does exactly this, and finding it is
   * half the exercise.
   *
   * This is D.5's `collectKnown` grown a memory: same "process what you can"
   * behaviour, except now the things you dropped are still in your hand and you
   * can log them. That difference is worth more than it looks - "we imported
   * 9,998 of 10,000 rows and here are the two" is a good morning, and "we
   * imported some rows" is an incident.
   */
  def partitionResults[E, A](results: List[Either[E, A]]): (List[E], List[A])

  // ---------------------------------------------------------------------------
  // E.6 D.6, answered
  //
  // Block D's `managerEmail` returned None four different ways and left the
  // on-call engineer to guess which. Same lookup, same chain, except every step
  // now says what it could not find.
  //
  // `Option.toRight(error)` is the whole trick: it turns "nothing" into "this
  // specific nothing". Once each step produces an Either, the for-comprehension
  // is the one you already wrote in D.6, unchanged.
  // ---------------------------------------------------------------------------

  /**
   * `managerEmailEither(directory, "carla") == Left(NoManagerRecorded("carla"))`
   * `managerEmailEither(directory, "dora")  == Left(NoSuchPerson("ghost"))`
   *
   * Note that second one: the missing person is the MANAGER, and the error says
   * so. An error that names the wrong thing is worse than no error at all.
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
   * Once this exists, `validateAll` is two applications of it and the "collect
   * the Lefts by hand" plumbing disappears. Worth doing in that order: write
   * the plumbing first so you feel it, then delete it.
   *
   * Compare with D.4's `map2`, which reported one absence out of two. The type
   * is what changed, not the cleverness.
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
   * `times` below 1 still means one attempt - refusing to try at all is never
   * what the caller meant.
   *
   * A test counts the calls, so an implementation that always runs three times
   * and picks a winner afterwards will be caught. Note that this needs no
   * laziness of any kind: `action` is an explicit function and you decide when
   * to call it.
   */
  def retry[A](times: Int)(action: () => Try[A]): Try[A]

  /**
   * SE3. Translate an error from one layer's vocabulary into another's.
   *
   * `mapError(Left(UnknownSku("x")))(e => s"pricing: $e") == Left("pricing: ...")`
   * A `Right` passes through untouched.
   *
   * One line, and the reason it matters is architectural: the storage layer's
   * errors should not leak into the HTTP layer's responses. Every boundary in a
   * system is a place where this function, or something like it, should be
   * called - and where it is missing, you find out because a database
   * constraint name ends up in a customer-facing error message.
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
   * This is what an import endpoint actually needs, and it is `validateAll`
   * plus `zipWithIndex` plus a decision about what to do with the pieces. The
   * decision is the interesting part: a batch that is 99% good is still
   * entirely rejected here, which is a choice, and E.5's `partitionResults`
   * would have made the other one.
   */
  def validateBatch(forms: List[SignupForm]): Either[List[(Int, ValidationError)], List[Signup]]
}

// -----------------------------------------------------------------------------
// Fixture types
// -----------------------------------------------------------------------------

/**
 * The API you have been handed. It throws, and its signatures do not mention
 * it. This is not a strawman - it is what `Map.apply` and integer division do,
 * and what most of the JDK does.
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
