package com.rockthecode.day2

/**
 * Day 2, Block 0 - Collections drill.
 *
 * CONDITIONAL BLOCK: only run this if day 1 ran long and the collections
 * transformation API got squeezed. If day 1 covered it properly, skip this
 * block or cherry-pick one exercise as a warm-up.
 *
 * Everything here is the collections API you already met on day 1:
 * for-comprehensions, groupBy, partition, span, collect, foldLeft,
 * zip, sliding. No new concepts - this is finger memory.
 *
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
   *
   * Remember: a for-comprehension is not a loop. It is flatMap + map.
   */
  def allSquares: List[String]

  /**
   * Only the dark squares, in the same order as `allSquares`.
   *
   * A square is dark when (fileIndex + rank) is odd, where fileIndex is 0 for
   * 'a' and 7 for 'h'. So a1 and h8 are dark; h1 and a8 are light.
   * There are 32 of them.
   *
   * Add the filter to the for-comprehension rather than filtering afterwards.
   */
  def darkSquares: List[String]

  // ---------------------------------------------------------------------------
  // 0.2 groupBy
  // ---------------------------------------------------------------------------

  /**
   * Department -> the names of everyone in it, sorted alphabetically.
   */
  def namesByDepartment(employees: List[Employee]): Map[String, List[String]]

  /**
   * Department -> the average salary in it.
   *
   * The averaging is a fold hiding inside the grouped values.
   * Departments with no employees simply do not appear in the result.
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
   * prefix - which means this is `span`, not `partition`. Use `span`.
   *
   * The difference matters: `partition` scans the whole list, `span` stops at
   * the first element that fails the test. Feed it an unsorted list and the two
   * give different answers. One of the tests does exactly that.
   */
  def veteransAndNewcomers(employees: List[Employee]): (List[Employee], List[Employee])

  /**
   * The uppercased names of everyone paid at least `threshold`.
   *
   * Do it in ONE pass with `collect` and a partial function.
   * Then write it again as `.filter(...).map(...)` and compare the two.
   */
  def seniorNames(employees: List[Employee], threshold: Int): List[String]

  // ---------------------------------------------------------------------------
  // 0.4 Map merging
  // ---------------------------------------------------------------------------

  /**
   * Merge two warehouse inventories, summing the quantities of items that
   * appear in both.
   *
   * This is a foldLeft - over a Map. Folds are not a List thing.
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
   *
   * Write it both ways - `prices.sliding(2)` and `prices.zip(prices.tail)` -
   * and decide which one you would rather read in six months.
   *
   * (Prices are BigDecimal, not Double. Money is never a Double.)
   */
  def dailyDeltas(prices: List[BigDecimal]): List[BigDecimal]

  /**
   * The single biggest one-day rise: (index of the day that closed higher, the rise).
   *
   * For `List(10, 12, 9, 15)` the rises are +2 (into day 1) and +6 (into day 3),
   * so the answer is `Some((3, 6))`.
   *
   * None if the series never rises, or has fewer than 2 prices.
   * On a tie, report the earliest day.
   */
  def largestRise(prices: List[BigDecimal]): Option[(Int, BigDecimal)]

  // ===========================================================================
  //
  //  STRETCH SECTION
  //
  // ===========================================================================
  //
  //  For whoever finishes Block 0 early. Nobody is expected to reach these, and
  //  nothing later in the day depends on them - so they can be handed out
  //  quietly to the fast half of the room without splitting the group.
  //
  //  Still nothing but the collections API. What makes these harder is that the
  //  SHAPE is not obvious: each one needs a method you probably have not
  //  reached for yet, or an accumulator that carries more than one thing.

  /**
   * S1. Every square a knight can reach from `square`, sorted alphabetically.
   *
   * `knightMoves("d4") == List("b3", "b5", "c2", "c6", "e2", "e6", "f3", "f5")`
   * `knightMoves("a1") == List("b3", "c2")`
   *
   * A knight moves two squares along one axis and one along the other. Moves
   * that leave the board simply do not exist - there is no error to report.
   *
   * Assume `square` is a legal name, "a1" to "h8".
   *
   * This is 0.1 again: a cartesian product with a filter. The only new thing is
   * that you generate the OFFSETS and add them, rather than generating the
   * squares directly. If you find yourself writing out all eight moves by hand,
   * look for the pattern that describes them.
   */
  def knightMoves(square: String): List[String]

  /**
   * S2. Turn a department -> members index inside out: member -> departments.
   *
   *   Map("Engineering" -> List("Ana", "Bruno"), "Trading" -> List("Ana"))
   *     becomes
   *   Map("Ana" -> List("Engineering", "Trading"), "Bruno" -> List("Engineering"))
   *
   * Somebody can belong to more than one department, which is what makes this
   * more than a one-liner. Each member's departments come back sorted
   * alphabetically - a Map has no order of its own, so without sorting the
   * answer would not be reproducible.
   *
   * The move: flatten to pairs first, then regroup. `groupBy` is happy to group
   * by something you compute.
   */
  def invertIndex(byDepartment: Map[String, List[String]]): Map[String, List[String]]

  /**
   * S3. The inverse of `dailyDeltas`: rebuild a price series from its changes.
   *
   * `rebuildPrices(10, List(2, -3, 6)) == List(10, 12, 9, 15)`
   *
   * The opening price is the first element, so n deltas produce n + 1 prices,
   * and no deltas produce just the opening price.
   *
   * `foldLeft` throws away the intermediate results and hands you only the
   * final one. Here you want to KEEP every step. There is exactly one method
   * for that and you have not met it yet - go and find it. Its name is a
   * strong hint about what it does.
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
   *
   * 0.5's `largestRise` found the best ONE-day move. This one allows any gap
   * between buying and selling, which changes the problem completely.
   *
   * Write the obvious version first - every buy paired with every later sell.
   * It is a for-comprehension and it is correct. Then ask what it costs on a
   * series of ten million prices, and write the version that walks the list
   * once. The single-pass accumulator has to carry more than one value; give it
   * a name rather than letting it become a tuple of five things.
   */
  def bestTrade(prices: List[BigDecimal]): Option[(Int, Int, BigDecimal)]
}

/** Shared fixture type for 0.2 and 0.3. */
case class Employee(name: String, department: String, salary: Int, startYear: Int)
