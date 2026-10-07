package com.rockthecode.day2

/**
 * Day 2, Block D - your workspace.
 *
 * Rrule for this block: no `.get`, no `.isDefined`, no `.isEmpty` with an
 * `if` after it. `getOrElse` is fine - it is a different method.
 */
object BlockDExercises extends BlockD {

  def lookupPhone(name: String): Option[String] = ???

  def phoneOrDefault(name: String): String = ???

  def firstAvailable(names: List[String]): Option[String] = ???

  // D.2 Reading configuration
  def parseConfig(raw: Map[String, String]): Option[ServerConfig] = ???

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

  def discount(customer: Option[Customer], code: Option[String]): BigDecimal = ???

  // D.4 Combining independent Options
  def map2[A, B, C](first: Option[A], second: Option[B])(combine: (A, B) => C): Option[C] = ???

  // D.5 All of them, or what you can get
  def sequence[A](options: List[Option[A]]): Option[List[A]] = ???

  def traverse[A, B](items: List[A])(f: A => Option[B]): Option[List[B]] = ???

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
