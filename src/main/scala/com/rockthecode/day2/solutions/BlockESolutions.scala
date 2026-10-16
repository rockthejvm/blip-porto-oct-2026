package com.rockthecode.day2.solutions

import com.rockthecode.day2.*

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

/** Day 2, Block E - reference solutions.
  */
object BlockESolutions extends BlockE {

  import PricingError.*
  import ValidationError.*
  import LookupError.*

  // ---------------------------------------------------------------------------
  // E.1 Try, at the boundary
  // ---------------------------------------------------------------------------

  def priceOf(sku: String): Try[BigDecimal] =
    Try(LegacyPricing.unitPrice(sku))

  def unitCost(sku: String, quantity: Int): Try[BigDecimal] =
    for {
      total <- priceOf(sku)
      each <- Try(LegacyPricing.perUnit(total, quantity))
    } yield each

  // Two throwing calls, one for-comprehension, and no error handling written
  // anywhere - `flatMap` on Try short-circuits, so a Failure in the first line
  // means the second never runs.

  def priceOrDefault(sku: String, fallback: BigDecimal): BigDecimal =
    priceOf(sku).getOrElse(fallback)

  // ---------------------------------------------------------------------------
  // E.2 Crossing the boundary
  // ---------------------------------------------------------------------------

  def priceEither(sku: String): Either[PricingError, BigDecimal] =
    priceOf(sku).toEither.left.map {
      case _: NoSuchElementException => UnknownSku(sku)
      case other => Unexpected(other.getMessage)
    }

  def unitCostEither(sku: String, quantity: Int): Either[PricingError, BigDecimal] =
    for {
      total <- priceEither(sku)
      each <- Try(LegacyPricing.perUnit(total, quantity)).toEither.left.map {
        case _: ArithmeticException => InvalidQuantity(quantity)
        case other => Unexpected(other.getMessage)
      }
    } yield each

  // The shape to point at:
  //
  //   Try(thing that throws)      catch it
  //     .toEither                 Either[Throwable, A]
  //     .left.map(classify)       Either[MyError, A]     <- the boundary

  // ---------------------------------------------------------------------------
  // E.3 Validation, one failure at a time
  // ---------------------------------------------------------------------------

  def validateName(raw: String): Either[ValidationError, String] =
    Either.cond(raw.trim.nonEmpty, raw, NameIsEmpty)

  def validateEmail(raw: String): Either[ValidationError, String] =
    Either.cond(raw.contains("@"), raw, EmailIsInvalid(raw))

  def validateAge(raw: String): Either[ValidationError, Int] =
    for {
      age <- raw.toIntOption.toRight(AgeIsNotANumber(raw))
      adult <- Either.cond(age >= 18, age, AgeIsTooLow(age))
    } yield adult

  def validateFailFast(form: SignupForm): Either[ValidationError, Signup] =
    for {
      name <- validateName(form.name)
      email <- validateEmail(form.email)
      age <- validateAge(form.age)
    } yield Signup(name, email, age)

  // ---------------------------------------------------------------------------
  // E.4 Validation, all the failures at once
  // ---------------------------------------------------------------------------

  def validateAll(form: SignupForm): Either[List[ValidationError], Signup] = {
    val name = validateName(form.name)
    val email = validateEmail(form.email)
    val age = validateAge(form.age)

    val errors = List(name, email, age).collect { case Left(error) => error }

    (name, email, age) match {
      case (Right(validName), Right(validEmail), Right(validAge)) =>
        Right(Signup(validName, validEmail, validAge))
      case _ =>
        Left(errors)
    }
  }

  // ---------------------------------------------------------------------------
  // E.5 Lists of results
  // ---------------------------------------------------------------------------

  def sequence[E, A](results: List[Either[E, A]]): Either[E, List[A]] =
    results.foldRight(Right(List.empty[A]): Either[E, List[A]]) { (result, accumulated) =>
      for {
        value <- result
        rest <- accumulated
      } yield value :: rest
    }

  def traverse[E, A, B](items: List[A])(f: A => Either[E, B]): Either[E, List[B]] =
    items.foldRight(Right(List.empty[B]): Either[E, List[B]]) { (item, accumulated) =>
      for {
        value <- f(item)
        rest <- accumulated
      } yield value :: rest
    }

  def partitionResults[E, A](results: List[Either[E, A]]): (List[E], List[A]) =
    results.partitionMap(identity)

  // ---------------------------------------------------------------------------
  // E.6 D.6, answered
  // ---------------------------------------------------------------------------

  def managerEmailEither(directory: Map[String, Colleague], name: String): Either[LookupError, String] =
    for {
      colleague <- directory.get(name).toRight(NoSuchPerson(name))
      managerName <- colleague.managerName.toRight(NoManagerRecorded(name))
      manager <- directory.get(managerName).toRight(NoSuchPerson(managerName))
      email <- manager.email.toRight(NoEmailOnRecord(managerName))
    } yield email

  // ===========================================================================
  //
  //  STRETCH SECTION
  //
  // ===========================================================================

  def map2Accumulating[E, A, B, C](
      first: Either[List[E], A],
      second: Either[List[E], B]
  )(combine: (A, B) => C): Either[List[E], C] =
    (first, second) match {
      case (Right(a), Right(b)) => Right(combine(a, b))
      case (Left(e1), Left(e2)) => Left(e1 ++ e2)
      case (Left(errors), _) => Left(errors)
      case (_, Left(errors)) => Left(errors)
    }

  def retry[A](times: Int)(action: () => Try[A]): Try[A] = {
    @tailrec
    def attempt(remaining: Int): Try[A] =
      action() match {
        case success: Success[A] => success
        case failure: Failure[A] if remaining <= 1 => failure
        case _ => attempt(remaining - 1)
      }

    attempt(math.max(times, 1))
  }

  def mapError[E, F, A](result: Either[E, A])(f: E => F): Either[F, A] =
    result.left.map(f)

  def validateBatch(forms: List[SignupForm]): Either[List[(Int, ValidationError)], List[Signup]] = {
    val validated = forms.zipWithIndex.map((form, index) => (index, validateAll(form)))

    val errors =
      validated.flatMap {
        case (index, Left(rowErrors)) => rowErrors.map(error => (index, error))
        case _ => Nil
      }

    if (errors.nonEmpty) Left(errors)
    else Right(validated.collect { case (_, Right(signup)) => signup })
  }
}
