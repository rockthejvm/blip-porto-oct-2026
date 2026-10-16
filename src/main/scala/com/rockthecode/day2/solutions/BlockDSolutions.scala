package com.rockthecode.day2.solutions

import com.rockthecode.day2.*

import scala.annotation.tailrec

/** Day 2, Block D - reference solutions.
  */
object BlockDSolutions extends BlockD {

  // ---------------------------------------------------------------------------
  // D.1 The boundary with code that returns null
  // ---------------------------------------------------------------------------

  def lookupPhone(name: String): Option[String] =
    Option(LegacyDirectory.find(name))

  // `Option(x)` is `if (x == null) None else Some(x)`. `Some(x)` is not.

  def phoneOrDefault(name: String): String =
    lookupPhone(name).getOrElse("unknown")

  def firstAvailable(names: List[String]): Option[String] =
    names.flatMap(lookupPhone).headOption

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

  // ---------------------------------------------------------------------------
  // D.3 The drill
  // ---------------------------------------------------------------------------

  def discount(customer: Option[Customer], code: Option[String]): BigDecimal = {
    val loyal = customer.exists(_.loyaltyYears >= 5)
    val hasExtra = code.contains("EXTRA")

    (customer.isEmpty, loyal, hasExtra) match {
      case (true, _, _) => BigDecimal(0)
      case (_, true, true) => BigDecimal(25)
      case (_, true, false) => BigDecimal(15)
      case (_, false, true) => BigDecimal(10)
      case (_, false, false) => BigDecimal(0)
    }
  }

  def discountv2(customer: Option[Customer], code: Option[String]): Int =
    customer
      .map { c =>
        if (c.loyaltyYears >= 5) code.filter(_ == "EXTRA").map(_ => 25).getOrElse(15)
        else code.filter(_ == "EXTRA").map(_ => 10).getOrElse(0)
      }
      .getOrElse(0)

  // ---------------------------------------------------------------------------
  // D.4 Combining independent Options
  // ---------------------------------------------------------------------------

  def map2[A, B, C](first: Option[A], second: Option[B])(combine: (A, B) => C): Option[C] =
    for {
      a <- first
      b <- second
    } yield combine(a, b)

  // `first.zip(second).map(combine.tupled)` is the same thing and shows that
  // `Option` has `zip`.

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

  def traverse[A, B](items: List[A])(f: A => Option[B]): Option[List[B]] =
    items.foldRight(Option(List.empty[B])) { (item, accumulated) =>
      for {
        value <- f(item)
        rest <- accumulated
      } yield value :: rest
    }

  def collectKnown[A](options: List[Option[A]]): List[A] =
    options.flatten

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

  def traverseValues[K, A, B](values: Map[K, A])(f: A => Option[B]): Option[Map[K, B]] =
    traverse(values.toList)((key, value) => f(value).map(converted => (key, converted)))
      .map(_.toMap)

  def merge[A](left: Option[A], right: Option[A])(combine: (A, A) => A): Option[A] =
    (left, right) match {
      case (Some(l), Some(r)) => Some(combine(l, r))
      case (Some(l), None) => Some(l)
      case (None, r) => r
    }

  def mergeAll[A](options: List[Option[A]])(combine: (A, A) => A): Option[A] =
    options.foldLeft(Option.empty[A])((accumulated, next) => merge(accumulated, next)(combine))

}
