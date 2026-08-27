package com.rockthecode.day2

/**
 * Day 2, Block D - Option.
 *
 * You met `Option` on day 1 and you have been handed one by half the exercises
 * since. This block is not an introduction; it is the hour where reaching for
 * the right combinator stops being something you think about.
 *
 * There is one rule for the whole block, and it is the entire point:
 *
 *     NO `.get`. NO `.isDefined`. NO `.isEmpty` followed by an `if`.
 *
 * Not because they are forbidden by law, but because every single time you
 * reach for one, there is a combinator that says what you meant more clearly
 * and cannot throw. Finding it is the exercise. `get` is `Option`'s
 * `NullPointerException` and it is right there in the API for the same reason
 * `null` is still in Java: history.
 *
 * (`getOrElse` is fine. It is a different method and it always has an answer.)
 *
 * Same rules as ever otherwise: no `var`, no `while`, no mutable collection.
 *
 * This trait is the exercise brief. Write your answers in `BlockDExercises`.
 */
trait BlockD {

  // ---------------------------------------------------------------------------
  // D.1 The boundary with code that returns null
  //
  // `LegacyDirectory` (at the bottom of this file) is the Java-shaped API you
  // are wrapping: it returns a String, or null, and its signature tells you
  // nothing about which. Your job is to make sure that null never gets past
  // this line - which is what "wrap it at the boundary" means in practice.
  // ---------------------------------------------------------------------------

  /**
   * Look somebody up, honestly.
   *
   * `lookupPhone("ana")    == Some("+351 911 111 111")`
   * `lookupPhone("nobody") == None`
   *
   * There are two ways to build an Option and only one of them is right here.
   * `Some(x)` wraps whatever you give it, INCLUDING null, and hands you a
   * `Some(null)` - an Option that lies about having a value and blows up
   * somewhere else entirely, an hour later. `Option(x)` checks.
   *
   * The test for "nobody" is really a test for which one you used.
   */
  def lookupPhone(name: String): Option[String]

  /**
   * The same, but for a caller who just wants a string to print.
   *
   * `phoneOrDefault("nobody") == "unknown"`
   *
   * This is the other end of the boundary: at some point somebody has to decide
   * what to do about the absence, and this is that point. Notice how far it is
   * from where the null came from - that distance is the whole benefit.
   */
  def phoneOrDefault(name: String): String

  /**
   * The first person in the list who actually has a number.
   *
   * `firstAvailable(List("nobody", "bruno", "ana")) == Some("+351 922 222 222")`
   * `firstAvailable(List("nobody", "nobody else"))  == None`
   * `firstAvailable(Nil)                            == None`
   *
   * There is a short way to say this with the combinators you already have.
   * Reaching for a fold means you missed it.
   */
  def firstAvailable(names: List[String]): Option[String]

  // ---------------------------------------------------------------------------
  // D.2 Reading configuration
  //
  // Day 1's host-and-port exercise, grown up. Several things must be present
  // and well-formed, and the answer is one value or nothing.
  // ---------------------------------------------------------------------------

  /**
   * Build a `ServerConfig` from raw string settings. Keys: "host", "port",
   * "timeout". All three are required; port and timeout must be whole numbers.
   *
   * `parseConfig(Map("host" -> "localhost", "port" -> "80", "timeout" -> "3000"))`
   *   `== Some(ServerConfig("localhost", 80, 3000))`
   *
   * Anything missing or unparseable gives None.
   *
   * A for-comprehension, and `String.toIntOption` - which exists precisely so
   * that you never write `try { s.toInt } catch { ... }` again.
   */
  def parseConfig(raw: Map[String, String]): Option[ServerConfig]

  /**
   * The same, but falling back to a map of defaults for any setting that is
   * missing from `raw`.
   *
   * `parseConfigWithDefaults(Map("host" -> "h", "port" -> "80"), Map("timeout" -> "5000"))`
   *   `== Some(ServerConfig("h", 80, 5000))`
   *
   * `raw` wins where it has an opinion; `defaults` fills the gaps; a setting
   * that is in neither is still missing, and the answer is still None.
   *
   * THE POINT OF THIS EXERCISE IS WHERE YOU PUT THE FALLBACK. Two pipelines,
   * both plausible, and they disagree about a config file containing
   * `timeout = 3o00`:
   *
   *   resolve, then parse    raw.get(k).orElse(defaults.get(k)).flatMap(_.toIntOption)
   *   parse, then resolve    raw.get(k).flatMap(_.toIntOption).orElse(defaults.get(k).flatMap(...))
   *
   * The first says "you told me nonsense" and refuses to start. The second
   * quietly uses the default, and the typo lives in production until somebody
   * wonders why the timeout is not what the file says. Only one of these is
   * defensible, and it is not the forgiving one.
   *
   * Write a small local helper that resolves one key and reuse it three times.
   */
  def parseConfigWithDefaults(raw: Map[String, String], defaults: Map[String, String]): Option[ServerConfig]

  // ---------------------------------------------------------------------------
  // D.3 The drill
  //
  // `BlockDExercises.uglyDiscount` is working code. It is also six lines of
  // `.isDefined` and `.get` wrapped around three nested ifs, and it is exactly
  // what Option code looks like when written by somebody still thinking in
  // null checks.
  //
  // Write it again with the same behaviour, no `.get`, no `.isDefined`, and no
  // nested ifs. The test compares your version against the original on every
  // combination of inputs.
  //
  // Two combinators do most of the work here, and both replace a specific
  // two-part phrase you will recognise from the ugly version:
  //
  //     option.isDefined && predicate(option.get)  ->  option.exists(predicate)
  //     option.isDefined && option.get == value    ->  option.contains(value)
  //
  // If this block only teaches two method names, make it these two.
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
   * Two lines with a for-comprehension, and worth writing down because it is
   * the shape of "several independent things that might be missing, and I want
   * to combine them" - which is most real validation code. Once this exists,
   * `map3` and `map4` are obvious; the moment you want `map7` you have learned
   * something about the design.
   *
   * Notice what it CANNOT do. If both arguments are missing, the answer reports
   * one absence, because `flatMap` stops at the first. For "which fields of
   * this form are wrong?" that is useless - one complaint per round trip.
   * Nothing about `Option` can fix it: it is a property of the type, not of
   * your code. Remember this in forty minutes.
   */
  def map2[A, B, C](first: Option[A], second: Option[B])(combine: (A, B) => C): Option[C]

  // ---------------------------------------------------------------------------
  // D.5 All of them, or what you can get
  //
  // A list of Options is not one situation, it is two, and confusing them is a
  // real bug rather than a style opinion.
  // ---------------------------------------------------------------------------

  /**
   * All-or-nothing: `Some` of every value, or None if even one is missing.
   *
   * `sequence(List(Some(1), Some(2))) == Some(List(1, 2))`
   * `sequence(List(Some(1), None))    == None`
   * `sequence(Nil)                    == Some(Nil)`
   *
   * That last one surprises people every time. There are no missing values in
   * an empty list, so the answer is "yes, here they all are" - and it is not a
   * special case, it falls straight out of the fold's seed. Which fold, and
   * therefore which seed, is the exercise.
   */
  def sequence[A](options: List[Option[A]]): Option[List[A]]

  /**
   * The same thing, but doing the conversion as it goes.
   *
   * `traverse(List("1", "2"))(_.toIntOption) == Some(List(1, 2))`
   * `traverse(List("1", "x"))(_.toIntOption) == None`
   *
   * Once you have this, `sequence` is `traverse(list)(identity)` - which is
   * worth noticing, because in practice `traverse` is the one you reach for and
   * `sequence` is the special case.
   */
  def traverse[A, B](items: List[A])(f: A => Option[B]): Option[List[B]]

  /**
   * Best-effort: everything that IS there, in order, ignoring what is not.
   *
   * `collectKnown(List(Some(1), None, Some(3))) == List(1, 3)`
   *
   * One method. Note that `Option` is quietly a collection of at most one
   * element, which is why this works at all and why `option.map`, `option.filter`
   * and `option.foreach` all mean what you would guess.
   *
   * The judgement call - and it IS a judgement call, on every use site - is
   * whether a missing value should sink the whole batch (`sequence`) or be
   * quietly dropped (`collectKnown`). Choosing the second one by accident is
   * how records go missing without anybody noticing.
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
   * Four things can go wrong and every one of them means the same thing here:
   * the person is not in the directory, they have no manager recorded, their
   * manager is not in the directory, or their manager has no email.
   *
   * `flatMap` chains it, and a for-comprehension reads better once there are
   * more than two steps. Write it both ways and keep the one you would rather
   * find in a pull request.
   *
   * Notice, on the way past, what this function cannot tell its caller: WHICH
   * of the four things went wrong. Sometimes that is fine. When it is not, you
   * want Block E.
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
   *
   * Two things to get right. It must be stack-safe - the test runs it a hundred
   * thousand deep, so `@tailrec` with an accumulator, exactly as in Block B.
   * And it must survive a CYCLE: if a value turns up that you have already
   * seen, stop there and do not include it again. Real org charts contain
   * loops, and so do real linked lists.
   */
  def chain[A](start: A)(next: A => Option[A]): List[A]

  /**
   * SD2. `traverse`, but over the values of a Map.
   *
   * `traverseValues(Map("a" -> "1"))(_.toIntOption) == Some(Map("a" -> 1))`
   * `traverseValues(Map("a" -> "x"))(_.toIntOption) == None`
   *
   * All-or-nothing again, keys untouched. Doing it by converting to a list,
   * traversing, and converting back is a perfectly good answer and is probably
   * what you should write - the interesting part is noticing that `traverse` is
   * not really about Lists at all.
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
   * Compare it with D.4's `map2`, because the pair is the lesson: `map2` needs
   * both and gives up otherwise; `merge` needs at least one and only gives up
   * when there is nothing at all. Merging two partial records, or two layers of
   * configuration, is this shape and not the other one - and reaching for
   * `map2` there quietly throws away everything you did have.
   */
  def merge[A](left: Option[A], right: Option[A])(combine: (A, A) => A): Option[A]

  /**
   * SD4. The same, across a whole list, left to right.
   *
   * `mergeAll(List(Some(1), None, Some(2)))(_ + _) == Some(3)`
   * `mergeAll(List(None, None))(_ + _)             == None`
   * `mergeAll(Nil)(_ + _)                          == None`
   *
   * One fold, and the seed tells you what "nothing at all" means. `combine` is
   * not necessarily commutative, so the order has to be preserved - there is a
   * test with string concatenation to make sure you noticed.
   */
  def mergeAll[A](options: List[Option[A]])(combine: (A, A) => A): Option[A]
}

// -----------------------------------------------------------------------------
// Fixture types
// -----------------------------------------------------------------------------

/**
 * The Java-shaped API you are wrapping. Note the signature: it promises a
 * String and sometimes hands you a null instead, and there is nothing in the
 * type to warn you. This is what every `getX` in a legacy codebase looks like.
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
