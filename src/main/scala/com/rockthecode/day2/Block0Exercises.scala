package com.rockthecode.day2

/**
 * Day 2, Block 0 - your workspace.
 *
 * Replace every `???` with an implementation. The brief for each one is the
 * scaladoc on the matching method in `Block0`.
 *
 * Run the tests with:
 *   sbt "testOnly com.rockthecode.day2.Block0ExercisesSuite"
 *
 * Ground rules for the whole of day 2: no `var`, no `while`, no mutable
 * collection. Not because they are evil - because removing them forces the
 * alternative to become visible.
 */
object Block0Exercises extends Block0 {

  // 0.1 Cartesian products
  def allSquares: List[String] = ???

  def darkSquares: List[String] = ???

  // 0.2 groupBy
  def namesByDepartment(employees: List[Employee]): Map[String, List[String]] = ???

  def averageSalaryByDepartment(employees: List[Employee]): Map[String, Double] = ???

  // 0.3 partition, span, collect
  def partitionBySalary(employees: List[Employee], threshold: Int): (List[Employee], List[Employee]) = ???

  def veteransAndNewcomers(employees: List[Employee]): (List[Employee], List[Employee]) = ???

  def seniorNames(employees: List[Employee], threshold: Int): List[String] = ???

  // 0.4 Map merging
  def mergeInventories(a: Map[String, Int], b: Map[String, Int]): Map[String, Int] = ???

  // 0.5 zip, zipWithIndex, sliding
  def dailyDeltas(prices: List[BigDecimal]): List[BigDecimal] = ???

  def largestRise(prices: List[BigDecimal]): Option[(Int, BigDecimal)] = ???

  // ===========================================================================
  //
  //  STRETCH SECTION - only if you finished the block above.
  //
  // ===========================================================================

  def knightMoves(square: String): List[String] = ???

  def invertIndex(byDepartment: Map[String, List[String]]): Map[String, List[String]] = ???

  def rebuildPrices(opening: BigDecimal, deltas: List[BigDecimal]): List[BigDecimal] = ???

  def bestTrade(prices: List[BigDecimal]): Option[(Int, Int, BigDecimal)] = ???
}
