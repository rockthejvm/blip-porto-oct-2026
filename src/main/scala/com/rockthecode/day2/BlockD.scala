package com.rockthecode.day2

/**
 * Day 2, Block D - Option.
 *
 * Rules:
 *
 *  NO `.get`
 *  NO `.isDefined`
 *  NO `.isEmpty` + `if`
 *
 * `getOrElse` is fine.
 *
 */
trait BlockD {

  // ---------------------------------------------------------------------------
  // D.1 The boundary with code that returns null
  //
  // `LegacyDirectory` (at the bottom of this file) is the Java-shaped API you
  // are wrapping: it returns a String, or null. Your job is to make sure that null never gets past
  // this line.
  // ---------------------------------------------------------------------------

  /**
   * Look somebody up.
   *
   * `lookupPhone("ana")    == Some("+351 911 111 111")`
   * `lookupPhone("nobody") == None`
   *
   */
  def lookupPhone(name: String): Option[String]

  /**
   * The same, but for a caller who just wants a string to print.
   *
   * `phoneOrDefault("nobody") == "unknown"`
   *
   */
  def phoneOrDefault(name: String): String

  /**
   * The first person in the list who actually has a number.
   *
   * `firstAvailable(List("nobody", "bruno", "ana")) == Some("+351 922 222 222")`
   * `firstAvailable(List("nobody", "nobody else"))  == None`
   * `firstAvailable(Nil)                            == None`
   */
  def firstAvailable(names: List[String]): Option[String]

  // ---------------------------------------------------------------------------
  // D.2 Reading configuration
  // ---------------------------------------------------------------------------

  /**
   * Build a `ServerConfig` from raw string settings. Keys: "host", "port",
   * "timeout". All three are required; port and timeout must be whole numbers.
   *
   * `parseConfig(Map("host" -> "localhost", "port" -> "80", "timeout" -> "3000"))`
   *   `== Some(ServerConfig("localhost", 80, 3000))`
   *
   * Anything missing or unparseable gives None.
   */
  def parseConfig(raw: Map[String, String]): Option[ServerConfig]

  /**
   * The same, but falling back to a map of defaults for any setting that is
   * missing from `raw`.
   *
   * `parseConfigWithDefaults(Map("host" -> "h", "port" -> "80"), Map("timeout" -> "5000"))`
   *   `== Some(ServerConfig("h", 80, 5000))`
   *
   */
  def parseConfigWithDefaults(raw: Map[String, String], defaults: Map[String, String]): Option[ServerConfig]

  // ---------------------------------------------------------------------------
  // D.3 The drill
  //
  // `BlockDExercises.uglyDiscount` is working code, but it's full of .isDefined and .get.
  //
  // Write it again with the same behaviour, no `.get`, no `.isDefined`, and no
  // nested ifs. 
  // ---------------------------------------------------------------------------

  def discount(customer: Option[Customer], code: Option[String]): BigDecimal

  // ---------------------------------------------------------------------------
  // D.4 Combining independent Options
  // ---------------------------------------------------------------------------

  /**
   * Combine two Options with a function. Both must be there.
   *
   * `map2(Some(2), Some(3))(_ + _) == Some(5)`
   * `map2(Some(2), None)(_ + _)    == None`
   *
   */
  def map2[A, B, C](first: Option[A], second: Option[B])(combine: (A, B) => C): Option[C]

  // ---------------------------------------------------------------------------
  // D.5 All of them
  // ---------------------------------------------------------------------------

  /**
   * All-or-nothing: `Some` of every value, or None if even one is missing.
   *
   * `sequence(List(Some(1), Some(2))) == Some(List(1, 2))`
   * `sequence(List(Some(1), None))    == None`
   * `sequence(Nil)                    == Some(Nil)`
   */
  def sequence[A](options: List[Option[A]]): Option[List[A]]

  /**
   * The same thing, but doing the conversion as it goes.
   *
   * `traverse(List("1", "2"))(_.toIntOption) == Some(List(1, 2))`
   * `traverse(List("1", "x"))(_.toIntOption) == None`
   */
  def traverse[A, B](items: List[A])(f: A => Option[B]): Option[List[B]]

  /**
   * Best-effort: everything that IS there, in order, ignoring what is not.
   *
   * `collectKnown(List(Some(1), None, Some(3))) == List(1, 3)`
   */
  def collectKnown[A](options: List[Option[A]]): List[A]

  /** Parse every string, or fail. One line, using `traverse`. */
  def parseAllInts(raw: List[String]): Option[List[Int]]

  // ---------------------------------------------------------------------------
  // D.6 Chains of things that might not be there
  // ---------------------------------------------------------------------------

  /**
   * Find somebody's manager's email address.
   *
   */
  def managerEmail(directory: Map[String, Colleague], name: String): Option[String]

  // ===========================================================================
  //
  //  STRETCH SECTION - only if you finished the block above.
  //
  // ===========================================================================

  /**
   * SD1. Follow a chain of "and then what?" until it runs out.
   *
   * `chain("ana")(name => directory.get(name).flatMap(_.managerName))` gives the
   * whole management line above Ana, starting with Ana herself.
   *
   * `chain(1)(n => if (n < 4) Some(n + 1) else None) == List(1, 2, 3, 4)`
   * `chain(1)(_ => None)                             == List(1)`
   */
  def chain[A](start: A)(next: A => Option[A]): List[A]

  /**
   * SD2. `traverse`, but over the values of a Map.
   *
   * `traverseValues(Map("a" -> "1"))(_.toIntOption) == Some(Map("a" -> 1))`
   * `traverseValues(Map("a" -> "x"))(_.toIntOption) == None`
   *
   */
  def traverseValues[K, A, B](values: Map[K, A])(f: A => Option[B]): Option[Map[K, B]]

  /**
   * SD3. Combine two Options where EITHER is enough.
   *
   * `merge(Some(1), Some(2))(_ + _) == Some(3)`
   * `merge(Some(1), None)(_ + _)    == Some(1)`
   * `merge(None, Some(2))(_ + _)    == Some(2)`
   * `merge(None, None)(_ + _)       == None`
   *
   */
  def merge[A](left: Option[A], right: Option[A])(combine: (A, A) => A): Option[A]

  /**
   * SD4. The same, across a whole list, left to right.
   *
   * `mergeAll(List(Some(1), None, Some(2)))(_ + _) == Some(3)`
   * `mergeAll(List(None, None))(_ + _)             == None`
   * `mergeAll(Nil)(_ + _)                          == None`
   */
  def mergeAll[A](options: List[Option[A]])(combine: (A, A) => A): Option[A]
}

// -----------------------------------------------------------------------------
// Types
// -----------------------------------------------------------------------------

/**
 * The Java-like API you are wrapping
 */
object LegacyDirectory {

  private val entries: Map[String, String] =
    Map(
      "ana" -> "+351 911 111 111",
      "bruno" -> "+351 922 222 222",
      "carla" -> "+351 933 333 333"
    )

  /** Returns null when there is no such person. Do not change this. */
  def find(name: String): String = entries.getOrElse(name, null)
}

case class ServerConfig(host: String, port: Int, timeoutMillis: Int)

case class Customer(name: String, loyaltyYears: Int)

case class Colleague(name: String, managerName: Option[String], email: Option[String])
