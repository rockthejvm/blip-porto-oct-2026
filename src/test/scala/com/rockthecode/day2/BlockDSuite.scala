package com.rockthecode.day2

import com.rockthecode.day2.solutions.BlockDSolutions

/**
 * Day 2, Block D - the tests.
 *
 *   sbt blockD                                                 <- attendees
 *   sbt "testOnly com.rockthecode.day2.BlockDSolutionsSuite"   <- trainer
 */
abstract class BlockDSuite(impl: BlockD) extends munit.FunSuite {

  // --- D.1 -------------------------------------------------------------------

  test("D.1 lookupPhone finds somebody who exists") {
    assertEquals(impl.lookupPhone("ana"), Some("+351 911 111 111"))
    assertEquals(impl.lookupPhone("bruno"), Some("+351 922 222 222"))
  }

  test("D.1 lookupPhone returns None, not Some(null)") {
    // If this says `Some(null) != None`, you used Some where you needed Option.
    assertEquals(impl.lookupPhone("nobody"), None)
    assertEquals(impl.lookupPhone(""), None)
  }

  test("D.1 phoneOrDefault decides what to do about the absence, at the edge") {
    assertEquals(impl.phoneOrDefault("ana"), "+351 911 111 111")
    assertEquals(impl.phoneOrDefault("nobody"), "unknown")
  }

  test("D.1 firstAvailable takes the first that works") {
    assertEquals(impl.firstAvailable(List("nobody", "bruno", "ana")), Some("+351 922 222 222"))
    assertEquals(impl.firstAvailable(List("ana")), Some("+351 911 111 111"))
  }

  test("D.1 firstAvailable when nothing works") {
    assertEquals(impl.firstAvailable(List("nobody", "nobody else")), None)
    assertEquals(impl.firstAvailable(Nil), None)
  }

  // --- D.2 -------------------------------------------------------------------

  private val complete = Map("host" -> "localhost", "port" -> "80", "timeout" -> "3000")

  test("D.2 parseConfig on a complete, well-formed map") {
    assertEquals(impl.parseConfig(complete), Some(ServerConfig("localhost", 80, 3000)))
  }

  test("D.2 parseConfig needs all three keys") {
    assertEquals(impl.parseConfig(complete - "host"), None)
    assertEquals(impl.parseConfig(complete - "port"), None)
    assertEquals(impl.parseConfig(complete - "timeout"), None)
    assertEquals(impl.parseConfig(Map.empty), None)
  }

  test("D.2 parseConfig needs the numbers to be numbers") {
    assertEquals(impl.parseConfig(complete + ("port" -> "eighty")), None)
    assertEquals(impl.parseConfig(complete + ("port" -> "")), None)
    assertEquals(impl.parseConfig(complete + ("timeout" -> "3o00")), None)
  }

  private val defaults = Map("port" -> "8080", "timeout" -> "5000")

  test("D.2 defaults fill the gaps") {
    assertEquals(
      impl.parseConfigWithDefaults(Map("host" -> "h", "port" -> "80"), defaults),
      Some(ServerConfig("h", 80, 5000))
    )
    assertEquals(
      impl.parseConfigWithDefaults(Map("host" -> "h"), defaults),
      Some(ServerConfig("h", 8080, 5000))
    )
  }

  test("D.2 raw wins wherever it has an opinion") {
    assertEquals(impl.parseConfigWithDefaults(complete, defaults), Some(ServerConfig("localhost", 80, 3000)))
  }

  test("D.2 a setting that IS there still has to be a number") {
    // THE test of this exercise. Fall back on a parse failure instead of on a
    // missing key and this is the only thing that catches you.
    assertEquals(impl.parseConfigWithDefaults(Map("host" -> "h", "timeout" -> "3o00"), defaults), None)
    assertEquals(impl.parseConfigWithDefaults(Map("host" -> "h", "port" -> "eighty"), defaults), None)
  }

  test("D.2 a setting missing from both is still missing") {
    assertEquals(impl.parseConfigWithDefaults(Map("port" -> "80"), defaults), None, "no host anywhere")
    assertEquals(impl.parseConfigWithDefaults(Map.empty, Map.empty), None)
  }

  test("D.2 a broken default is not rescued either") {
    assertEquals(impl.parseConfigWithDefaults(Map("host" -> "h"), Map("port" -> "eighty", "timeout" -> "1")), None)
  }

  test("D.2 no defaults at all behaves like parseConfig") {
    List(complete, complete - "host", complete + ("port" -> "x"), Map.empty[String, String]).foreach { raw =>
      assertEquals(impl.parseConfigWithDefaults(raw, Map.empty), impl.parseConfig(raw), s"disagreed on $raw")
    }
  }

  // --- D.3 -------------------------------------------------------------------

  test("D.3 discount agrees with the ugly version on every combination") {
    val customers = List(None, Some(Customer("Ana", 7)), Some(Customer("Bruno", 1)), Some(Customer("Zed", 5)))
    val codes = List(None, Some("EXTRA"), Some("OTHER"), Some(""))

    for {
      customer <- customers
      code <- codes
    } assertEquals(
      impl.discount(customer, code),
      BlockDExercises.uglyDiscount(customer, code),
      s"disagreed for customer=$customer code=$code"
    )
  }

  test("D.3 the specific answers, in case both versions are wrong together") {
    assertEquals(impl.discount(Some(Customer("Ana", 7)), Some("EXTRA")), BigDecimal(25))
    assertEquals(impl.discount(Some(Customer("Ana", 7)), None), BigDecimal(15))
    assertEquals(impl.discount(Some(Customer("Bruno", 1)), Some("EXTRA")), BigDecimal(10))
    assertEquals(impl.discount(Some(Customer("Bruno", 1)), None), BigDecimal(0))
    assertEquals(impl.discount(None, Some("EXTRA")), BigDecimal(0))
  }

  test("D.3 five years is loyal") {
    assertEquals(impl.discount(Some(Customer("Zed", 5)), None), BigDecimal(15))
    assertEquals(impl.discount(Some(Customer("Zed", 4)), None), BigDecimal(0))
  }

  // --- D.4 -------------------------------------------------------------------

  test("D.4 map2 combines two present values") {
    assertEquals(impl.map2(Some(2), Some(3))(_ + _), Some(5))
    assertEquals(impl.map2(Some("a"), Some(1))((s, n) => s * n), Some("a"))
  }

  test("D.4 map2 gives up if either is missing") {
    assertEquals(impl.map2(Some(2), Option.empty[Int])(_ + _), None)
    assertEquals(impl.map2(Option.empty[Int], Some(3))(_ + _), None)
    assertEquals(impl.map2(Option.empty[Int], Option.empty[Int])(_ + _), None)
  }

  // --- D.5 -------------------------------------------------------------------

  test("D.5 sequence is all-or-nothing") {
    assertEquals(impl.sequence(List(Some(1), Some(2), Some(3))), Some(List(1, 2, 3)))
    assertEquals(impl.sequence(List(Some(1), None, Some(3))), None)
    assertEquals(impl.sequence(List(None, None)), None)
  }

  test("D.5 sequence of nothing is Some(Nil), and that is not a special case") {
    assertEquals(impl.sequence(List.empty[Option[Int]]), Some(Nil))
  }

  test("D.5 sequence keeps the order") {
    assertEquals(impl.sequence((1 to 100).toList.map(Some(_))), Some((1 to 100).toList))
  }

  test("D.5 traverse converts as it goes") {
    assertEquals(impl.traverse(List("1", "2", "3"))(_.toIntOption), Some(List(1, 2, 3)))
    assertEquals(impl.traverse(List("1", "x", "3"))(_.toIntOption), None)
    assertEquals(impl.traverse(List.empty[String])(_.toIntOption), Some(Nil))
  }

  test("D.5 sequence is traverse with nothing to do") {
    val options = List(Some(1), Some(2))
    assertEquals(impl.sequence(options), impl.traverse(options)(identity))
  }

  test("D.5 collectKnown keeps what it can") {
    assertEquals(impl.collectKnown(List(Some(1), None, Some(3))), List(1, 3))
    assertEquals(impl.collectKnown(List(None, None)), Nil)
    assertEquals(impl.collectKnown(List.empty[Option[Int]]), Nil)
  }

  test("D.5 collectKnown and sequence answer different questions about the same input") {
    val partial = List(Some(1), None, Some(3))
    assertEquals(impl.collectKnown(partial), List(1, 3), "best effort")
    assertEquals(impl.sequence(partial), None, "all or nothing")
  }

  test("D.5 parseAllInts") {
    assertEquals(impl.parseAllInts(List("10", "20")), Some(List(10, 20)))
    assertEquals(impl.parseAllInts(List("10", "twenty")), None)
    assertEquals(impl.parseAllInts(Nil), Some(Nil))
  }

  // --- D.6 -------------------------------------------------------------------

  private val directory: Map[String, Colleague] =
    Map(
      "ana" -> Colleague("ana", Some("bruno"), Some("ana@blip.pt")),
      "bruno" -> Colleague("bruno", Some("carla"), Some("bruno@blip.pt")),
      "carla" -> Colleague("carla", None, Some("carla@blip.pt")),
      "dora" -> Colleague("dora", Some("ghost"), Some("dora@blip.pt")),
      "eva" -> Colleague("eva", Some("frank"), Some("eva@blip.pt")),
      "frank" -> Colleague("frank", None, None)
    )

  test("D.6 managerEmail down the happy path") {
    assertEquals(impl.managerEmail(directory, "ana"), Some("bruno@blip.pt"))
    assertEquals(impl.managerEmail(directory, "bruno"), Some("carla@blip.pt"))
  }

  test("D.6 all four ways it can fail, and they all look the same") {
    assertEquals(impl.managerEmail(directory, "nobody"), None, "not in the directory")
    assertEquals(impl.managerEmail(directory, "carla"), None, "no manager recorded")
    assertEquals(impl.managerEmail(directory, "dora"), None, "manager not in the directory")
    assertEquals(impl.managerEmail(directory, "eva"), None, "manager has no email")
  }

  // ===========================================================================
  //
  //  STRETCH SECTION
  //
  // ===========================================================================

  // --- SD1 -------------------------------------------------------------------

  test("SD1 chain follows until it runs out") {
    assertEquals(impl.chain(1)(n => if (n < 4) Some(n + 1) else None), List(1, 2, 3, 4))
    assertEquals(impl.chain(1)(_ => None), List(1), "the start is always in the answer")
  }

  test("SD1 chain up a management line") {
    assertEquals(
      impl.chain("ana")(name => directory.get(name).flatMap(_.managerName)),
      List("ana", "bruno", "carla")
    )
    assertEquals(impl.chain("carla")(name => directory.get(name).flatMap(_.managerName)), List("carla"))
  }

  test("SD1 chain stops on a cycle instead of hanging") {
    val loop = Map("a" -> "b", "b" -> "c", "c" -> "a")
    assertEquals(impl.chain("a")(loop.get), List("a", "b", "c"))

    val selfLoop = Map("x" -> "x")
    assertEquals(impl.chain("x")(selfLoop.get), List("x"))
  }

  test("SD1 chain is stack-safe") {
    assertEquals(impl.chain(0)(n => if (n < 100000) Some(n + 1) else None).size, 100001)
  }

  // --- SD2 -------------------------------------------------------------------

  test("SD2 traverseValues converts every value, keys untouched") {
    assertEquals(
      impl.traverseValues(Map("a" -> "1", "b" -> "2"))(_.toIntOption),
      Some(Map("a" -> 1, "b" -> 2))
    )
    assertEquals(impl.traverseValues(Map.empty[String, String])(_.toIntOption), Some(Map.empty[String, Int]))
  }

  test("SD2 traverseValues is all-or-nothing too") {
    assertEquals(impl.traverseValues(Map("a" -> "1", "b" -> "x"))(_.toIntOption), None)
  }

  // --- SD3 / SD4 -------------------------------------------------------------

  test("SD3 merge is happy with either side") {
    assertEquals(impl.merge(Some(1), Some(2))(_ + _), Some(3))
    assertEquals(impl.merge(Some(1), Option.empty[Int])(_ + _), Some(1))
    assertEquals(impl.merge(Option.empty[Int], Some(2))(_ + _), Some(2))
    assertEquals(impl.merge(Option.empty[Int], Option.empty[Int])(_ + _), None)
  }

  test("SD3 merge and map2 disagree, and that is the lesson") {
    val present = Some(1)
    val missing = Option.empty[Int]
    assertEquals(impl.map2(present, missing)(_ + _), None, "map2 needs both")
    assertEquals(impl.merge(present, missing)(_ + _), Some(1), "merge keeps what it has")
  }

  test("SD4 mergeAll folds the whole list") {
    assertEquals(impl.mergeAll(List(Some(1), None, Some(2)))(_ + _), Some(3))
    assertEquals(impl.mergeAll(List(Some(5)))(_ + _), Some(5))
    assertEquals(impl.mergeAll(List(Option.empty[Int], Option.empty[Int]))(_ + _), None)
    assertEquals(impl.mergeAll(List.empty[Option[Int]])(_ + _), None)
  }

  test("SD4 mergeAll preserves order, because combine need not be commutative") {
    assertEquals(impl.mergeAll(List(Some("a"), None, Some("b"), Some("c")))(_ + _), Some("abc"))
  }
}

/** Red until the attendees make it green. That is the point. */
class BlockDExercisesSuite extends BlockDSuite(BlockDExercises)

/** Green, always. If this one goes red, the reference solutions are wrong. */
class BlockDSolutionsSuite extends BlockDSuite(BlockDSolutions)
