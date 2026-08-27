package com.rockthecode.day2

import scala.util.Try

/**
 * Day 2, Block E - your workspace.
 *
 * Briefs are the scaladoc on `BlockE`.
 *
 * Rules: no `.get` on a `Try` or an `Option`, and no `throw` anywhere in here.
 *
 *   sbt blockE
 */
object BlockEExercises extends BlockE {

  // E.1 Try, at the boundary with code that throws
  def priceOf(sku: String): Try[BigDecimal] = ???

  def unitCost(sku: String, quantity: Int): Try[BigDecimal] = ???

  def priceOrDefault(sku: String, fallback: BigDecimal): BigDecimal = ???

  // E.2 Crossing the boundary: Try in, Either out
  def priceEither(sku: String): Either[PricingError, BigDecimal] = ???

  def unitCostEither(sku: String, quantity: Int): Either[PricingError, BigDecimal] = ???

  // E.3 Validation, one failure at a time
  def validateName(raw: String): Either[ValidationError, String] = ???

  def validateEmail(raw: String): Either[ValidationError, String] = ???

  def validateAge(raw: String): Either[ValidationError, Int] = ???

  def validateFailFast(form: SignupForm): Either[ValidationError, Signup] = ???

  // E.4 Validation, all the failures at once
  def validateAll(form: SignupForm): Either[List[ValidationError], Signup] = ???

  // E.5 Lists of results
  def sequence[E, A](results: List[Either[E, A]]): Either[E, List[A]] = ???

  def traverse[E, A, B](items: List[A])(f: A => Either[E, B]): Either[E, List[B]] = ???

  def partitionResults[E, A](results: List[Either[E, A]]): (List[E], List[A]) = ???

  // E.6 D.6, answered
  def managerEmailEither(directory: Map[String, Colleague], name: String): Either[LookupError, String] = ???

  // ===========================================================================
  //
  //  STRETCH SECTION - only if you finished the block above.
  //
  // ===========================================================================

  def map2Accumulating[E, A, B, C](
      first: Either[List[E], A],
      second: Either[List[E], B]
  )(combine: (A, B) => C): Either[List[E], C] = ???

  def retry[A](times: Int)(action: () => Try[A]): Try[A] = ???

  def mapError[E, F, A](result: Either[E, A])(f: E => F): Either[F, A] = ???

  def validateBatch(forms: List[SignupForm]): Either[List[(Int, ValidationError)], List[Signup]] = ???
}
