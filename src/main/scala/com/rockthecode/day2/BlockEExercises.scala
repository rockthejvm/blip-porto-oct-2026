package com.rockthecode.day2

import scala.util.Try

/** Day 2, Block E
  */
object BlockEExercises extends BlockE {

  def totalValueOf(sku: String): Try[BigDecimal] =
    Try(LegacyPricing.totalValueOf(sku))

  def unitCost(sku: String, quantity: Int): Try[BigDecimal] =
    for {
      total <- totalValueOf(sku)
      perUnit <- Try(LegacyPricing.perUnit(total, quantity))
    } yield perUnit

  def priceOrDefault(sku: String, fallback: BigDecimal): BigDecimal = ???

  def priceEither(sku: String): Either[PricingError, BigDecimal] = ???

  def unitCostEither(sku: String, quantity: Int): Either[PricingError, BigDecimal] = ???

  def validateName(raw: String): Either[ValidationError, String] =
    Either.cond(raw.nonEmpty, raw.trim, ValidationError.NameIsEmpty)

  def validateEmail(raw: String): Either[ValidationError, String] =
    Either.cond(raw.contains("@"), raw.trim, ValidationError.EmailIsInvalid(raw))

  def validateAge(raw: String): Either[ValidationError, Int] =
    Either
      .cond(raw.toIntOption.nonEmpty, raw.toInt, ValidationError.AgeIsNotANumber(raw)) match {
      case Left(e) => Left(e)
      case Right(v) if v >= 18 => Right(v)
      case Right(v) => Left(ValidationError.AgeIsTooLow(v))
    }

  def validateFailFast(form: SignupForm): Either[ValidationError, Signup] =
    for {
      n <- validateName(form.name)
      e <- validateEmail(form.email)
      a <- validateAge(form.age)
    } yield Signup(n, e, a)

  def validateAll(form: SignupForm): Either[List[ValidationError], Signup] = {
    val nameEither = validateName(form.name)
    val emailEither = validateEmail(form.email)
    val ageEither = validateAge(form.age)

    val errors = List(nameEither, emailEither, ageEither).collect { case Left(e) =>
      e
    } // List[all errors]

    (nameEither, emailEither, ageEither) match {
      case (Right(n), Right(e), Right(a)) =>
        Right(Signup(n, e, a))
      case _ => Left(errors)
    }
  }

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
