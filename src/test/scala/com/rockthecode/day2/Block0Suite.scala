package com.rockthecode.day2

import com.rockthecode.day2.solutions.Block0Solutions

/**
 * Day 2, Block 0 - the tests.
 *
 * The suite is written once, against the `Block0` interface, and then run twice:
 * once against the attendees' stubs and once against the reference solutions.
 * That way the solutions can never drift from the exercise signatures, and the
 * trainer can check the solutions still pass without wading through the
 * (deliberately) red exercise suite.
 *
 *   sbt "testOnly com.rockthecode.day2.Block0ExercisesSuite"   <- attendees
 *   sbt "testOnly com.rockthecode.day2.Block0SolutionsSuite"   <- trainer
 */
abstract class Block0Suite(impl: Block0) extends munit.FunSuite {

  private val staff = List(
    Employee("Ana", "Engineering", 90000, 2016),
    Employee("Bruno", "Engineering", 120000, 2018),
    Employee("Carla", "Engineering", 75000, 2022),
    Employee("Diogo", "Trading", 150000, 2019),
    Employee("Eva", "Trading", 110000, 2021),
    Employee("Filipa", "Support", 60000, 2023)
  )

  // --- 0.1 -------------------------------------------------------------------

  test("0.1 allSquares has 64 squares, file varying slowest") {
    val squares = impl.allSquares
    assertEquals(squares.size, 64)
    assertEquals(squares.take(9), List("a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8", "b1"))
    assertEquals(squares.last, "h8")
    assertEquals(squares.distinct.size, 64)
  }

  test("0.1 darkSquares has 32 squares and knows the corners") {
    val dark = impl.darkSquares
    assertEquals(dark.size, 32)
    assert(dark.contains("a1"), "a1 is dark")
    assert(dark.contains("h8"), "h8 is dark")
    assert(!dark.contains("h1"), "h1 is light")
    assert(!dark.contains("a8"), "a8 is light")
    assertEquals(dark, impl.allSquares.filter(dark.contains), "order must match allSquares")
  }

  // --- 0.2 -------------------------------------------------------------------

  test("0.2 namesByDepartment groups and sorts the names") {
    assertEquals(
      impl.namesByDepartment(staff),
      Map(
        "Engineering" -> List("Ana", "Bruno", "Carla"),
        "Trading" -> List("Diogo", "Eva"),
        "Support" -> List("Filipa")
      )
    )
  }

  test("0.2 namesByDepartment of nobody is an empty map") {
    assertEquals(impl.namesByDepartment(Nil), Map.empty[String, List[String]])
  }

  test("0.2 averageSalaryByDepartment averages within each group") {
    val averages = impl.averageSalaryByDepartment(staff)
    assertEquals(averages.keySet, Set("Engineering", "Trading", "Support"))
    assertEqualsDouble(averages("Engineering"), 95000.0, 0.0001)
    assertEqualsDouble(averages("Trading"), 130000.0, 0.0001)
    assertEqualsDouble(averages("Support"), 60000.0, 0.0001)
  }

  // --- 0.3 -------------------------------------------------------------------

  test("0.3 partitionBySalary splits and preserves order") {
    val (paidWell, paidLess) = impl.partitionBySalary(staff, 100000)
    assertEquals(paidWell.map(_.name), List("Bruno", "Diogo", "Eva"))
    assertEquals(paidLess.map(_.name), List("Ana", "Carla", "Filipa"))
  }

  test("0.3 veteransAndNewcomers splits a sorted list at the first newcomer") {
    val sorted = staff.sortBy(_.startYear)
    val (veterans, newcomers) = impl.veteransAndNewcomers(sorted)
    assertEquals(veterans.map(_.name), List("Ana", "Bruno", "Diogo"))
    assertEquals(newcomers.map(_.name), List("Eva", "Carla", "Filipa"))
  }

  test("0.3 veteransAndNewcomers is span, not partition") {
    // Deliberately out of order: a veteran hides behind a newcomer.
    // `span` stops at the first failure and leaves Ana in the tail.
    // `partition` would have pulled her into the first half.
    val jumbled = List(
      Employee("Bruno", "Engineering", 120000, 2018),
      Employee("Eva", "Trading", 110000, 2021),
      Employee("Ana", "Engineering", 90000, 2016)
    )
    val (veterans, newcomers) = impl.veteransAndNewcomers(jumbled)
    assertEquals(veterans.map(_.name), List("Bruno"))
    assertEquals(newcomers.map(_.name), List("Eva", "Ana"))
  }

  test("0.3 seniorNames filters and transforms in one pass") {
    assertEquals(impl.seniorNames(staff, 100000), List("BRUNO", "DIOGO", "EVA"))
    assertEquals(impl.seniorNames(staff, 1000000), Nil)
  }

  // --- 0.4 -------------------------------------------------------------------

  test("0.4 mergeInventories sums the overlap and keeps the rest") {
    val porto = Map("bolt" -> 10, "nut" -> 4, "washer" -> 7)
    val lisbon = Map("nut" -> 6, "washer" -> 1, "screw" -> 3)
    assertEquals(
      impl.mergeInventories(porto, lisbon),
      Map("bolt" -> 10, "nut" -> 10, "washer" -> 8, "screw" -> 3)
    )
  }

  test("0.4 mergeInventories with an empty side changes nothing") {
    val porto = Map("bolt" -> 10, "nut" -> 4)
    assertEquals(impl.mergeInventories(porto, Map.empty), porto)
    assertEquals(impl.mergeInventories(Map.empty, porto), porto)
  }

  // --- 0.5 -------------------------------------------------------------------

  private def prices(values: Int*): List[BigDecimal] = values.map(BigDecimal(_)).toList

  test("0.5 dailyDeltas produces n - 1 changes") {
    assertEquals(impl.dailyDeltas(prices(10, 12, 9)), prices(2, -3))
    assertEquals(impl.dailyDeltas(prices(10, 12, 9, 15)), prices(2, -3, 6))
  }

  test("0.5 dailyDeltas of a short series is empty") {
    assertEquals(impl.dailyDeltas(Nil), List.empty[BigDecimal])
    assertEquals(impl.dailyDeltas(prices(10)), List.empty[BigDecimal])
  }

  test("0.5 largestRise reports the day that closed higher") {
    assertEquals(impl.largestRise(prices(10, 12, 9, 15)), Some((3, BigDecimal(6))))
  }

  test("0.5 largestRise breaks ties towards the earliest day") {
    assertEquals(impl.largestRise(prices(10, 12, 9, 11)), Some((1, BigDecimal(2))))
  }

  test("0.5 largestRise is None when nothing ever rises") {
    assertEquals(impl.largestRise(prices(10, 9, 8)), None)
    assertEquals(impl.largestRise(prices(10)), None)
    assertEquals(impl.largestRise(Nil), None)
  }

  // ===========================================================================
  //
  //  STRETCH SECTION
  //
  // ===========================================================================

  // --- S1 --------------------------------------------------------------------

  test("S1 a knight in the middle of the board has eight moves") {
    assertEquals(impl.knightMoves("d4"), List("b3", "b5", "c2", "c6", "e2", "e6", "f3", "f5"))
  }

  test("S1 a knight in a corner has two") {
    assertEquals(impl.knightMoves("a1"), List("b3", "c2"))
    assertEquals(impl.knightMoves("h8"), List("f7", "g6"))
  }

  test("S1 every knight move lands on the board, from every square") {
    val squares = for { file <- ('a' to 'h').toList; rank <- 1 to 8 } yield s"$file$rank"
    val everyMove = squares.flatMap(impl.knightMoves)
    assert(everyMove.forall(squares.contains), "a knight walked off the board")
    assertEquals(everyMove.size, 336, "the standard count of knight moves on an 8x8 board")
  }

  test("S1 knight moves are symmetric - if I can reach you, you can reach me") {
    assert(impl.knightMoves("d4").forall(target => impl.knightMoves(target).contains("d4")))
  }

  // --- S2 --------------------------------------------------------------------

  test("S2 invertIndex turns departments inside out") {
    assertEquals(
      impl.invertIndex(Map("Engineering" -> List("Ana", "Bruno"), "Trading" -> List("Ana"))),
      Map("Ana" -> List("Engineering", "Trading"), "Bruno" -> List("Engineering"))
    )
  }

  test("S2 invertIndex sorts each member's departments, so the answer is reproducible") {
    val index = Map(
      "Trading" -> List("Ana"),
      "Support" -> List("Ana"),
      "Engineering" -> List("Ana")
    )
    assertEquals(impl.invertIndex(index), Map("Ana" -> List("Engineering", "Support", "Trading")))
  }

  test("S2 invertIndex loses nobody - the classic Map-flatMap bug") {
    // Every department has exactly one member, and they are all different
    // people, so nothing may be dropped on the way through.
    val index = Map("A" -> List("one"), "B" -> List("two"), "C" -> List("three"))
    assertEquals(impl.invertIndex(index).keySet, Set("one", "two", "three"))
  }

  test("S2 invertIndex of nothing, and of empty departments") {
    assertEquals(impl.invertIndex(Map.empty), Map.empty[String, List[String]])
    assertEquals(impl.invertIndex(Map("Empty" -> Nil)), Map.empty[String, List[String]])
  }

  // --- S3 --------------------------------------------------------------------

  test("S3 rebuildPrices walks the deltas from the opening price") {
    assertEquals(impl.rebuildPrices(BigDecimal(10), prices(2, -3, 6)), prices(10, 12, 9, 15))
  }

  test("S3 rebuildPrices with no deltas is just the opening price") {
    assertEquals(impl.rebuildPrices(BigDecimal(10), Nil), prices(10))
  }

  test("S3 rebuildPrices and dailyDeltas are inverses") {
    val series = prices(10, 12, 9, 15, 15, 4)
    val deltas = prices(2, -3, 6, 0, -11)
    assertEquals(impl.rebuildPrices(series.head, deltas), series)
  }

  // --- S4 --------------------------------------------------------------------

  test("S4 bestTrade buys the dip and sells the peak after it") {
    assertEquals(impl.bestTrade(prices(7, 1, 5, 3, 6, 4)), Some((1, 4, BigDecimal(5))))
  }

  test("S4 bestTrade will not sell before it buys") {
    // The lowest price is the LAST one, so the cheap day is unusable.
    assertEquals(impl.bestTrade(prices(4, 9, 2)), Some((0, 1, BigDecimal(5))))
  }

  test("S4 bestTrade is None when the series never rises") {
    assertEquals(impl.bestTrade(prices(7, 6, 4, 3, 1)), None)
    assertEquals(impl.bestTrade(prices(5, 5, 5)), None, "flat is not profitable")
    assertEquals(impl.bestTrade(prices(5)), None)
    assertEquals(impl.bestTrade(Nil), None)
  }

  test("S4 bestTrade breaks ties towards the earliest buy, then the earliest sell") {
    assertEquals(impl.bestTrade(prices(1, 3, 1, 3)), Some((0, 1, BigDecimal(2))))
  }

  test("S4 bestTrade handles a long series without falling over") {
    // 0, 1, 2, ... 4999 - the answer is buy first, sell last.
    val rising = (0 until 5000).map(BigDecimal(_)).toList
    assertEquals(impl.bestTrade(rising), Some((0, 4999, BigDecimal(4999))))
  }
}

/** Red until the attendees make it green. That is the point. */
class Block0ExercisesSuite extends Block0Suite(Block0Exercises)

/** Green, always. If this one goes red, the reference solutions are wrong. */
class Block0SolutionsSuite extends Block0Suite(Block0Solutions)

/**
 * Trainer-only cross-check: the obvious quadratic answer and the clever
 * single-pass one must agree on everything, including the tie-breaks.
 *
 * This is the claim made during the S4 walkthrough, so it is worth having a
 * machine confirm it rather than a whiteboard.
 */
class Block0AgreementSuite extends munit.FunSuite {

  test("S4 the quadratic and single-pass answers agree on 2000 random series") {
    val random = new scala.util.Random(20261026)

    val disagreements =
      (1 to 2000).flatMap { _ =>
        val length = random.nextInt(12)
        val series = List.fill(length)(BigDecimal(random.nextInt(20)))
        val quadratic = Block0Solutions.bestTradeQuadratic(series)
        val singlePass = Block0Solutions.bestTrade(series)
        if (quadratic == singlePass) None else Some((series, quadratic, singlePass))
      }

    assertEquals(disagreements, Seq.empty, "the two solutions disagree")
  }
}
