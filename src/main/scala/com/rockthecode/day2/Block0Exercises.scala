package com.rockthecode.day2

/** Run the tests with: sbt "testOnly com.rockthecode.day2.Block0ExercisesSuite"
  *
  * No `var`, no `while`, no mutable collections!
  */
object Block0Exercises extends Block0 {

  val cols = ('a' to 'h').toList
  val rows = (1 to 8).toList

  // 0.1 Cartesian products
  override def allSquares: List[String] =
    for {
      col <- cols
      row <- rows
    } yield s"$col$row"

  override def darkSquares: List[String] =
    for {
      col <- cols
      row <- rows if (row + (col - 'a' + 1)) % 2 != 0
    } yield s"$col$row"

  // 0.2 groupBy
  val employees = List(
    Employee("Alice", "Engineering", 95000, 8),
    Employee("Bob", "Sales", 72000, 3),
    Employee("Carol", "Engineering", 105000, 12),
    Employee("David", "HR", 68000, 6),
    Employee("Eva", "Marketing", 75000, 2),
    Employee("Frank", "Sales", 81000, 9),
    Employee("Grace", "HR", 79000, 4),
    Employee("Henry", "Engineering", 88000, 5),
    Employee("Ivy", "Marketing", 83000, 7),
    Employee("Jack", "Sales", 65000, 1)
  )

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
