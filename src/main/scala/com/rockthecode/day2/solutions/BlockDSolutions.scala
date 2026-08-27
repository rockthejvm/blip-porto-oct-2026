package com.rockthecode.day2.solutions

import com.rockthecode.day2.*

import scala.annotation.tailrec

/**
 * Day 2, Block D - reference solutions.
 */
object BlockDSolutions extends BlockD {

  // ---------------------------------------------------------------------------
  // D.1 The boundary with code that returns null
  // ---------------------------------------------------------------------------

  def lookupPhone(name: String): Option[String] =
    Option(LegacyDirectory.find(name))

  // `Option(x)` is `if (x == null) None else Some(x)`. `Some(x)` is not.
  //
  // Worth doing live: change this to `Some(...)`, watch the "nobody" test fail,
  // and read the failure - it says `Some(null) != None`, which is the clearest
  // possible statement of what has gone wrong. Then point out that in real code
  // there is no test at that line, and the Some(null) travels three layers up
  // before it explodes somewhere that looks unrelated.

  def phoneOrDefault(name: String): String =
    lookupPhone(name).getOrElse("unknown")

  def firstAvailable(names: List[String]): Option[String] =
    names.flatMap(lookupPhone).headOption

  // Honest caveat, worth one sentence and no more: this looks EVERY name up and
  // then takes the first answer. For a Map lookup that costs nothing. If each
  // lookup were an HTTP call you would want it to stop at the first hit, and
  // the tool for that is laziness - which is Block F. Note it and move on.

  // ---------------------------------------------------------------------------
  // D.2 Reading configuration
  // ---------------------------------------------------------------------------

  def parseConfig(raw: Map[String, String]): Option[ServerConfig] =
    for {
      host <- raw.get("host")
      port <- raw.get("port").flatMap(_.toIntOption)
      timeout <- raw.get("timeout").flatMap(_.toIntOption)
    } yield ServerConfig(host, port, timeout)

  def parseConfigWithDefaults(raw: Map[String, String], defaults: Map[String, String]): Option[ServerConfig] = {
    def setting(key: String): Option[String] = raw.get(key).orElse(defaults.get(key))

    for {
      host <- setting("host")
      port <- setting("port").flatMap(_.toIntOption)
      timeout <- setting("timeout").flatMap(_.toIntOption)
    } yield ServerConfig(host, port, timeout)
  }

  // The whole exercise is one local helper and where it sits in the pipeline.
  //
  //   RESOLVE, then parse:  raw.get(k).orElse(defaults.get(k)).flatMap(_.toIntOption)
  //   parse, then RESOLVE:  raw.get(k).flatMap(_.toIntOption).orElse(defaults.get(k).flatMap(_.toIntOption))
  //
  // Both compile. Both pass every happy-path test. They differ on exactly one
  // input - a setting that is present in `raw` and is nonsense - and there the
  // second one silently swallows the typo and uses the default.
  //
  // Show the wrong one on screen and run the suite: one test fails, the one
  // called "a setting that IS there still has to be a number". Then point out
  // that in a real codebase nobody wrote that test, because the person writing
  // the fallback was thinking about missing keys and not about broken ones.
  //
  // The general shape, worth naming: `orElse` means "if you have NOTHING, try
  // this instead". Putting a parse in front of it converts "broken" into
  // "nothing", and that conversion is the bug.

  // ---------------------------------------------------------------------------
  // D.3 The drill
  // ---------------------------------------------------------------------------

  def discount(customer: Option[Customer], code: Option[String]): BigDecimal = {
    val loyal = customer.exists(_.loyaltyYears >= 5)
    val hasExtra = code.contains("EXTRA")

    (customer.isEmpty, loyal, hasExtra) match {
      case (true, _, _)      => BigDecimal(0)
      case (_, true, true)   => BigDecimal(25)
      case (_, true, false)  => BigDecimal(15)
      case (_, false, true)  => BigDecimal(10)
      case (_, false, false) => BigDecimal(0)
    }
  }

  // Two named booleans and one table. The rules are now visible as a table
  // rather than buried in nesting, which is what makes it obvious that
  // "no customer" and "not loyal, no code" happen to give the same answer -
  // a fact the original hides in two different branches.
  //
  // The combinators that did the work:
  //
  //   customer.isDefined && customer.get.loyaltyYears >= 5
  //     becomes customer.exists(_.loyaltyYears >= 5)
  //
  //   code.isDefined && code.get == "EXTRA"
  //     becomes code.contains("EXTRA")
  //
  // Both are total, neither can throw, and both read as English. If somebody
  // asks about `forall`: it is `exists`'s twin and it is TRUE for None, which
  // is exactly right for "no rule has been violated" and exactly wrong for
  // "somebody qualified". Knowing which you mean is the skill.
  //
  // A perfectly good alternative, for the walkthrough:
  //
  //   customer.fold(BigDecimal(0)) { c =>
  //     (c.loyaltyYears >= 5, code.contains("EXTRA")) match {
  //       case (true, true)   => BigDecimal(25)
  //       ...
  //     }
  //   }

  // ---------------------------------------------------------------------------
  // D.4 Combining independent Options
  // ---------------------------------------------------------------------------

  def map2[A, B, C](first: Option[A], second: Option[B])(combine: (A, B) => C): Option[C] =
    for {
      a <- first
      b <- second
    } yield combine(a, b)

  // `first.zip(second).map(combine.tupled)` is the same thing and shows that
  // `Option` has `zip`, which surprises people.
  //
  // The limitation is the interesting part, and it is Block E's opening line:
  // if BOTH are missing, this reports one absence, because flatMap stops at the
  // first. For "which fields of this form are wrong?" that is useless. Nothing
  // about `Option` can fix it - it is a property of the type, not of the code.

  // ---------------------------------------------------------------------------
  // D.5 All of them, or what you can get
  // ---------------------------------------------------------------------------

  def sequence[A](options: List[Option[A]]): Option[List[A]] =
    options.foldRight(Option(List.empty[A])) { (option, accumulated) =>
      for {
        value <- option
        rest <- accumulated
      } yield value :: rest
    }

  // The seed IS the empty-list answer: `Some(Nil)`. Nobody has to write a
  // special case for it, and that is why `sequence(Nil) == Some(Nil)` rather
  // than None. foldRight so it builds in order without a reverse - Block B.1,
  // arriving with a job to do.

  def traverse[A, B](items: List[A])(f: A => Option[B]): Option[List[B]] =
    items.foldRight(Option(List.empty[B])) { (item, accumulated) =>
      for {
        value <- f(item)
        rest <- accumulated
      } yield value :: rest
    }

  // ...and now `sequence` could simply be `traverse(options)(identity)`. Worth
  // showing both and deleting one, live: `traverse` is the general one and
  // `sequence` is what it collapses to when the conversion is "do nothing".

  def collectKnown[A](options: List[Option[A]]): List[A] =
    options.flatten

  // One word, because `Option` is a collection of at most one element and
  // `flatten` on a List[Option[A]] therefore means exactly what you want.
  //
  // The contrast with `sequence` is the actual lesson of D.4 and deserves to be
  // said out loud: same input type, same shape of question, and the difference
  // between "one bad record fails the import" and "one bad record vanishes
  // silently and nobody finds out until the reconciliation". Neither is right;
  // choosing on purpose is right.

  def parseAllInts(raw: List[String]): Option[List[Int]] =
    traverse(raw)(_.toIntOption)

  // ---------------------------------------------------------------------------
  // D.6 Chains of things that might not be there
  // ---------------------------------------------------------------------------

  def managerEmail(directory: Map[String, Colleague], name: String): Option[String] =
    for {
      colleague <- directory.get(name)
      managerName <- colleague.managerName
      manager <- directory.get(managerName)
      email <- manager.email
    } yield email

  // The flatMap version, for comparison:
  //
  //   directory.get(name)
  //     .flatMap(_.managerName)
  //     .flatMap(directory.get)
  //     .flatMap(_.email)
  //
  // Genuinely a toss-up at four steps, and the chain is arguably nicer here
  // because every step is a single method. The for-comprehension wins the
  // moment a step needs to refer to something from two steps earlier - which
  // the chain simply cannot express without nesting.
  //
  // And the point to leave hanging: four different failures, one None. Ask what
  // an on-call engineer would want in the log.

  // ===========================================================================
  //
  //  STRETCH SECTION
  //
  // ===========================================================================

  def chain[A](start: A)(next: A => Option[A]): List[A] = {
    @tailrec
    def follow(current: A, seen: Set[A], accumulated: List[A]): List[A] =
      next(current) match {
        case Some(following) if !seen.contains(following) =>
          follow(following, seen + following, following :: accumulated)
        case _ =>
          accumulated.reverse
      }

    follow(start, Set(start), List(start))
  }

  // Two guards in one pattern: `Some(following) if !seen.contains(following)`
  // handles "the chain ended" and "the chain looped" with the same `case _`,
  // because for this function they mean the same thing - stop here.
  //
  // The `seen` Set is the price of not trusting the data. Somebody will ask
  // whether it is worth it: the honest answer is that a cycle without it is not
  // a wrong answer, it is a hang, and hangs are much harder to diagnose than
  // wrong answers.

  def traverseValues[K, A, B](values: Map[K, A])(f: A => Option[B]): Option[Map[K, B]] =
    traverse(values.toList) { (key, value) => f(value).map(converted => (key, converted)) }
      .map(_.toMap)

  // Out to a List, traverse, back to a Map. Unglamorous and correct.
  //
  // The thing worth noticing: `traverse` did not need to change at all. It was
  // never about Lists - it is about "a container of things that might fail,
  // turned inside out into a possible container of things". Every functional
  // library has this function for every container, under this name, and now it
  // is not a mystery.

  def merge[A](left: Option[A], right: Option[A])(combine: (A, A) => A): Option[A] =
    (left, right) match {
      case (Some(l), Some(r)) => Some(combine(l, r))
      case (Some(l), None)    => Some(l)
      case (None, r)          => r
    }

  def mergeAll[A](options: List[Option[A]])(combine: (A, A) => A): Option[A] =
    options.foldLeft(Option.empty[A])((accumulated, next) => merge(accumulated, next)(combine))

  // Put `map2` and `merge` side by side on the board - they are the same
  // signature apart from the types, and they are opposite answers to the same
  // question:
  //
  //   map2   both required   missing anything  ->  nothing
  //   merge  either will do  missing anything  ->  keep what you have
  //
  // Merging two layers of configuration, or two partial records from two
  // systems, is `merge`. Reaching for `map2` there throws away everything you
  // did have, and it does it silently.
  //
  // `mergeAll`'s seed is `None`, which is what "nothing at all" has to mean,
  // and foldLeft keeps the order - which matters, because `combine` is not
  // required to be commutative and the test uses string concatenation to prove
  // somebody thought about it.
}
