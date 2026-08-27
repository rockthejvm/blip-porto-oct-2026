package com.rockthecode.day2.solutions

import com.rockthecode.day2.*

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

/**
 * Day 2, Block E - reference solutions.
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
  //
  // Worth saying explicitly, because it is the thing that makes Try worth
  // using at all: this is the same code you would write if nothing could fail.
  // Compare it with the try/catch version on the board.

  def priceOrDefault(sku: String, fallback: BigDecimal): BigDecimal =
    priceOf(sku).getOrElse(fallback)

  // ---------------------------------------------------------------------------
  // E.2 Crossing the boundary
  // ---------------------------------------------------------------------------

  def priceEither(sku: String): Either[PricingError, BigDecimal] =
    priceOf(sku).toEither.left.map {
      case _: NoSuchElementException => UnknownSku(sku)
      case other                     => Unexpected(other.getMessage)
    }

  def unitCostEither(sku: String, quantity: Int): Either[PricingError, BigDecimal] =
    for {
      total <- priceEither(sku)
      each <- Try(LegacyPricing.perUnit(total, quantity)).toEither.left.map {
        case _: ArithmeticException => InvalidQuantity(quantity)
        case other                  => Unexpected(other.getMessage)
      }
    } yield each

  // The shape to point at:
  //
  //   Try(thing that throws)      catch it
  //     .toEither                 Either[Throwable, A]
  //     .left.map(classify)       Either[MyError, A]     <- the boundary
  //
  // After that third line the Throwable is gone and everything downstream deals
  // in a closed set of errors it can pattern match on exhaustively. Everything
  // before it is quarantine.
  //
  // Two details worth a sentence each:
  //
  //   - `case other => Unexpected(...)` is not laziness, it is honesty. You
  //     cannot enumerate what a library might throw, so say so in the type
  //     rather than pretending with a partial function that will itself throw
  //     on the day something new comes out.
  //
  //   - the sku is in scope HERE and is not in the exception. Context is always
  //     richer at the call site than inside the failure, which is the real
  //     argument for translating early rather than at the top of the stack.

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

  // `Either.cond(test, right, left)` is the one people never find. Note the
  // argument order: the RIGHT comes before the left, which reads backwards the
  // first three times and then never again.
  //
  // `option.toRight(error)` is the other one, and it is the single most useful
  // method in this block - it is the bridge from every Option-returning thing
  // you already have to an Either that can say why.

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

  // How to run this one in the room:
  //
  //   1. Ask for it with a for-comprehension. Let somebody write it. It
  //      compiles, it passes the happy-path test, and it reports one error.
  //   2. Ask why. The answer is in the signature of flatMap: it takes
  //      `A => Either[E, B]`, so it needs the A to produce the next Either at
  //      all. If there is no A, there is no next step to look at. It is not
  //      that Either declines to accumulate - it cannot.
  //   3. Note what had to change: the three validations now run INDEPENDENTLY,
  //      before anything is combined. That independence is the whole
  //      difference, and it is why libraries model this as a separate type
  //      (Cats calls it `Validated`) rather than a method on Either.
  //
  // The `collect { case Left(e) => e }` is Block 0.3's partial function turning
  // up where it is exactly right. `partitionMap` (see E.5) would do it too.
  //
  // The final match rather than a for-comprehension is the honest version: by
  // the time we get there we have already decided, and all three values are in
  // hand at once. That "all at once" is precisely what flatMap could not give
  // us. SE1 packages the whole manoeuvre into one reusable function.

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

  // Character for character, this is D.5 with `Option` swapped for `Either` and
  // `Some(Nil)` swapped for `Right(Nil)`. Put the two files side by side; the
  // point is not that they are similar, it is that NOTHING about the algorithm
  // depended on which type it was. That observation is what the word
  // "applicative" is pointing at, and they do not need the word.
  //
  // The type ascription on the seed is needed because Scala infers
  // `Right[Nothing, List[A]]` otherwise and then the fold will not typecheck.
  // Mildly annoying, entirely mechanical, and worth showing so it does not
  // derail anybody.

  def partitionResults[E, A](results: List[Either[E, A]]): (List[E], List[A]) =
    results.partitionMap(identity)

  // `partitionMap` is the method nobody finds on their own. It is `partition`
  // for things that are already Either-shaped, and `identity` is the whole
  // implementation.

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

  // Open D.6 and this side by side. The for-comprehension is identical. The
  // only change is `.toRight(...)` on each line - and the function went from
  // "no" to "here is what I could not find and whose it was".
  //
  // Notice line 3 and 4 report the MANAGER's name, not the original person's.
  // Getting that wrong produces an error message that names the wrong entity,
  // which is worse than no message at all because it sends the on-call engineer
  // to the wrong place. There is a test for it.

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
      case (Right(a), Right(b))       => Right(combine(a, b))
      case (Left(e1), Left(e2))       => Left(e1 ++ e2)
      case (Left(errors), _)          => Left(errors)
      case (_, Left(errors))          => Left(errors)
    }

  // The second case is the entire exercise. Everything else is bookkeeping.
  //
  // With this, E.4 becomes:
  //
  //   map2Accumulating(
  //     map2Accumulating(name, email)((n, e) => (n, e)),
  //     age
  //   ) { case ((n, e), a) => Signup(n, e, a) }
  //
  // ...where each field is first widened to Either[List[ValidationError], _]
  // with `.left.map(List(_))`. Not beautiful, and beauty is what a library buys
  // you here. But it is fifteen lines you can read, own and debug, which on a
  // team that has decided against the library ecosystem is the right trade.

  def retry[A](times: Int)(action: () => Try[A]): Try[A] = {
    @tailrec
    def attempt(remaining: Int): Try[A] =
      action() match {
        case success: Success[A]           => success
        case failure: Failure[A] if remaining <= 1 => failure
        case _                             => attempt(remaining - 1)
      }

    attempt(math.max(times, 1))
  }

  // `math.max(times, 1)` because `retry(0)` meaning "do nothing and return
  // what?" has no sensible answer - there is no Try to hand back.
  //
  // Returning the LAST failure rather than the first is deliberate: it
  // describes the state the world ended up in, which is what the log needs.
  //
  // And note there is no laziness here at all. `action` is an ordinary
  // function; you call it when you decide to. Where D.5 needed `.view` to stop
  // early, an explicit `() => A` needs nothing - which is worth pointing out,
  // because "make it a function" is often the simpler answer to a laziness
  // problem.

  def mapError[E, F, A](result: Either[E, A])(f: E => F): Either[F, A] =
    result.left.map(f)

  // One line, and the reason to write it down at all is that people do not know
  // `.left` exists, so they pattern match. It is also `bimap`'s left half, and
  // in Cats it is `leftMap` - names worth recognising in somebody else's code.

  def validateBatch(forms: List[SignupForm]): Either[List[(Int, ValidationError)], List[Signup]] = {
    val validated = forms.zipWithIndex.map((form, index) => (index, validateAll(form)))

    val errors =
      validated.flatMap {
        case (index, Left(rowErrors)) => rowErrors.map(error => (index, error))
        case _                        => Nil
      }

    if (errors.nonEmpty) Left(errors)
    else Right(validated.collect { case (_, Right(signup)) => signup })
  }

  // The decision hiding in here is worth twenty seconds out loud: a batch that
  // is 9,998 rows good and 2 rows bad is rejected in its entirety. That is a
  // choice, it is often the right one for a financial import, and it is exactly
  // the wrong one for a log ingest. E.5's `partitionResults` is the other
  // choice, and the code for it is shorter.
  //
  // Whichever you pick, pick it on purpose - which has been the theme since
  // D.5.
}
