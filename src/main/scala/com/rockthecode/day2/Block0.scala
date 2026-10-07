package com.rockthecode.day2


case class Employee(name: String, department: String, salary: Int, startYear: Int)

/**
 * This trait is the exercise brief. Write your answers in `Block0Exercises`.
 */
trait Block0 {

  // ---------------------------------------------------------------------------
  // 0.1 Cartesian products
  // ---------------------------------------------------------------------------

  /**
   * All 64 chessboard square names, as a for-comprehension.
   *
   * Files are 'a' to 'h', ranks are 1 to 8. File varies slowest:
   *   List("a1", "a2", ..., "a8", "b1", ..., "h8")
   */
  def allSquares: List[String]

  /**
   * Only the dark squares, in the same order as `allSquares`.
   *
   * A square is dark when (fileIndex + rank) is odd, where fileIndex is 0 for
   * 'a' and 7 for 'h'. So a1 and h8 are dark; h1 and a8 are light.
   */
  def darkSquares: List[String]

  // ---------------------------------------------------------------------------
  // 0.2 groupBy
  // ---------------------------------------------------------------------------

  /**
   * Department -> the names of everyone in it sorted alphabetically.
   */
  def namesByDepartment(employees: List[Employee]): Map[String, List[String]]

  /**
   * Department -> the average salary in it.
   *
   * Hint: use a fold.
   * Departments with no employees do not appear in the result.
   */
  def averageSalaryByDepartment(employees: List[Employee]): Map[String, Double]

  // ---------------------------------------------------------------------------
  // 0.3 partition, span, collect
  // ---------------------------------------------------------------------------

  /**
   * Split into (paid at least `threshold`, paid less than `threshold`).
   * Relative order is preserved inside each half.
   */
  def partitionBySalary(employees: List[Employee], threshold: Int): (List[Employee], List[Employee])

  /**
   * Split into (veterans, newcomers), where a veteran started before 2020.
   *
   * The input is EXPECTED to be sorted by start year, so the veterans form a
   * prefix. Find the correct function to use for this.
   */
  def veteransAndNewcomers(employees: List[Employee]): (List[Employee], List[Employee])

  /**
   * The uppercased names of everyone paid at least `threshold`.
   *
   * Do it in one pass.
   */
  def seniorNames(employees: List[Employee], threshold: Int): List[String]

  // ---------------------------------------------------------------------------
  // 0.4 Map merging
  // ---------------------------------------------------------------------------

  /**
   * Merge two warehouse inventories, summing the quantities of items that
   * appear in both.
   *
   * No mutable map.
   */
  def mergeInventories(a: Map[String, Int], b: Map[String, Int]): Map[String, Int]

  // ---------------------------------------------------------------------------
  // 0.5 zip, zipWithIndex, sliding
  // ---------------------------------------------------------------------------

  /**
   * Day-over-day changes in a series of closing prices.
   *
   * `dailyDeltas(List(10, 12, 9)) == List(2, -3)`
   * A series of n prices has n - 1 deltas; fewer than 2 prices means no deltas.
   */
  def dailyDeltas(prices: List[BigDecimal]): List[BigDecimal]

  /**
   * The single biggest one-day rise: (index of the day that closed higher, the rise).
   *
   * For `List(10, 12, 9, 15)` the rises are +2 (into day 1) and +6 (into day 3),
   * so the answer is `Some((3, 6))`.
   *
   * Return None if the series never rises, or has fewer than 2 prices.
   * On a tie, report the earliest day.
   */
  def largestRise(prices: List[BigDecimal]): Option[(Int, BigDecimal)]

  // ===========================================================================
  //
  //  STRETCH SECTION
  //
  // ===========================================================================

  /**
   * S1. Every square a knight can reach from `square`, sorted alphabetically.
   *
   * `knightMoves("d4") == List("b3", "b5", "c2", "c6", "e2", "e6", "f3", "f5")`
   * `knightMoves("a1") == List("b3", "c2")`
   *
   * Assume `square` is a legal name, "a1" to "h8".
   */
  def knightMoves(square: String): List[String]

  /**
   * S2. Turn a department -> members index inside out: member -> departments.
   *
   *   Map("Engineering" -> List("Ana", "Bruno"), "Trading" -> List("Ana"))
   *     becomes
   *   Map("Ana" -> List("Engineering", "Trading"), "Bruno" -> List("Engineering"))
   *
   * Somebody can belong to more than one department. Each member's departments come back sorted
   * alphabetically (needs to be reproducible for tests).
   */
  def invertIndex(byDepartment: Map[String, List[String]]): Map[String, List[String]]

  /**
   * S3. The inverse of `dailyDeltas`: rebuild a price series from its changes.
   *
   * `rebuildPrices(10, List(2, -3, 6)) == List(10, 12, 9, 15)`
   *
   * The opening price is the first element, so n deltas produce n + 1 prices,
   * and no deltas produce just the opening price.
   */
  def rebuildPrices(opening: BigDecimal, deltas: List[BigDecimal]): List[BigDecimal]

  /**
   * S4. The most profitable single trade: buy on one day, sell on a LATER one.
   *
   * Returns `(buy index, sell index, profit)`, or None if there is no
   * profitable trade at all - profit must be strictly positive.
   *
   * `bestTrade(List(7, 1, 5, 3, 6, 4)) == Some((1, 4, 5))`   // buy at 1, sell at 6
   * `bestTrade(List(7, 6, 4, 3, 1))    == None`              // never rises
   *
   * On a tie, report the earliest buy; if the buy also ties, the earliest sell.
   */
  def bestTrade(prices: List[BigDecimal]): Option[(Int, Int, BigDecimal)]
}

