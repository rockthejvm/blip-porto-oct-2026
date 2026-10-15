package com.rockthecode.day2

/** Day 2, Block D - your workspace.
  *
  * Rrule for this block: no `.get`, no `.isDefined`, no `.isEmpty` with an `if` after it. `getOrElse` is fine - it is a
  * different method.
  */
object BlockDExercises extends BlockD {

  def lookupPhone(name: String): Option[String] =
    Option(LegacyDirectory.find(name))

  def phoneOrDefault(name: String): String =
    lookupPhone(name).getOrElse("112")

  def firstAvailable(names: List[String]): Option[String] =
    names.find(n => lookupPhone(n).nonEmpty)

  def firstAvailable_v2(names: List[String]): Option[String] =
    names.flatMap(name => lookupPhone(name)).headOption

  // D.2 Reading configuration
  def parseConfig(raw: Map[String, String]): Option[ServerConfig] =
    for {
      host <- raw.get("host")
      port <- raw.get("port").flatMap(portString => portString.toIntOption)
      timeout <- raw.get("timeout").flatMap(timeoutString => timeoutString.toIntOption)
    } yield ServerConfig(host, port, timeout)

  def parseConfigWithDefaults(raw: Map[String, String], defaults: Map[String, String]): Option[ServerConfig] = ???

  // ---------------------------------------------------------------------------
  // D.3 The drill
  //
  // THIS IS THE CODE YOU ARE REPLACING. Do not change it
  // ---------------------------------------------------------------------------

  def uglyDiscount(customer: Option[Customer], code: Option[String]): BigDecimal =
    if (customer.isDefined) {
      val theCustomer = customer.get
      if (theCustomer.loyaltyYears >= 5) {
        if (code.isDefined && code.get == "EXTRA") BigDecimal(25)
        else BigDecimal(15)
      } else {
        if (code.isDefined && code.get == "EXTRA") BigDecimal(10)
        else BigDecimal(0)
      }
    } else BigDecimal(0)

  // ... and here is your version.

  def discount(customer: Option[Customer], code: Option[String]): BigDecimal = {
    val customerExists = customer.nonEmpty
    val customerLoyal = customer.exists(_.loyaltyYears > 5)
    val extraCoupon = code.exists(_ == "EXTRA")

    (customerExists, customerLoyal, extraCoupon) match {
      case (true, true, true) => BigDecimal(25)
      case (true, true, false) => BigDecimal(15)
      case (true, false, true) => BigDecimal(10)
      case _ => BigDecimal(0)
    }
  }

  def discount2(customer: Option[Customer], code: Option[String]): BigDecimal =
    customer
      .map { c =>
        if (c.loyaltyYears > 5)
          code.filter(_ == "EXTRA").map(_ => BigDecimal(25)).getOrElse(BigDecimal(15))
        else
          code.filter(_ == "EXTRA").map(_ => BigDecimal(10)).getOrElse(BigDecimal(0))
      }
      .getOrElse(BigDecimal(0))

  // D.4 Combining independent Options
  def map2[A, B, C](first: Option[A], second: Option[B])(combine: (A, B) => C): Option[C] = ???

  // D.5 All of them, or what you can get
  def sequence[A](options: List[Option[A]]): Option[List[A]] =
    options.foldRight(Option(List())) { (opt, acc) =>
      (opt, acc) match {
        case (Some(e), Some(l)) => Some(e :: l)
        case _ => None
      }
    }

  def sequence_v2[A](options: List[Option[A]]): Option[List[A]] =
    options.foldRight(Option(List())) { (opt, acc) =>
      for {
        e <- opt
        l <- acc
      } yield e :: l
    }

  def sequenceCheat[A](options: List[Option[A]]): Option[List[A]] =
    traverse(
      options // List[Option[A]]
    )(x => x // Option[A] => Option[A]
    ) // Option[List[A]]

  def traverseCheat[A, B](items: List[A])(f: A => Option[B]): Option[List[B]] =
    sequence(items.map(f))

  def traverse[A, B](items: List[A])(f: A => Option[B]): Option[List[B]] =
    items.foldRight(Option(List())) { (elem, acc) =>
      for {
        e <- f(elem) // Option[B]
        l <- acc
      } yield e :: l
    }

  // example of traverse
  // List[Int] => futures on each => List[Future[String]]
  // you want Future[List[String]]
  // traverse!
  // list.traverse(e => call your api(e)) => Future[List[Results]]

  def collectKnown[A](options: List[Option[A]]): List[A] = ???

  def parseAllInts(raw: List[String]): Option[List[Int]] = ???

  // D.6 Chains of things that might not be there
  def managerEmail(directory: Map[String, Colleague], name: String): Option[String] = ???

  // ===========================================================================
  //
  //  STRETCH SECTION - only if you finished the block above.
  //
  // ===========================================================================

  def chain[A](start: A)(next: A => Option[A]): List[A] = ???

  def traverseValues[K, A, B](values: Map[K, A])(f: A => Option[B]): Option[Map[K, B]] = ???

  def merge[A](left: Option[A], right: Option[A])(combine: (A, A) => A): Option[A] = ???

  def mergeAll[A](options: List[Option[A]])(combine: (A, A) => A): Option[A] = ???
}
