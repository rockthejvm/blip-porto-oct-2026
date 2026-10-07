package com.rockthecode.day2

import scala.util.Try

/**
 * Day 2, Block E
 */
object BlockEExercises extends BlockE {

  def priceOf(sku: String): Try[BigDecimal] = ???

  def unitCost(sku: String, quantity: Int): Try[BigDecimal] = ???

  def priceOrDefault(sku: String, fallback: BigDecimal): BigDecimal = ???

  def priceEither(sku: String): Either[PricingError, BigDecimal] = ???

  def unitCostEither(sku: String, quantity: Int): Either[PricingError, BigDecimal] = ???

  def validateName(raw: String): Either[ValidationError, String] = ???

  def validateEmail(raw: String): Either[ValidationError, String] = ???

  def validateAge(raw: String): Either[ValidationError, Int] = ???

  def validateFailFast(form: SignupForm): Either[ValidationError, Signup] = ???

  def validateAll(form: SignupForm): Either[List[ValidationError], Signup] = ???

  def sequence[E, A](results: List[Either[E, A]]): Either[E, List[A]] = ???

  def traverse[E, A, B](items: List[A])(f: A => Either[E, B]): Either[E, List[B]] = ???

  def partitionResults[E, A](results: List[Either[E, A]]): (List[E], List[A]) = ???

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
