package com.rockthecode.day2.solutions

import com.rockthecode.day2.{Block0, Employee}

/**
 * Day 2, Block 0 - reference solutions.
 *
 * Where more than one solution is worth showing, the alternative is in a
 * comment underneath. Those alternatives are the walkthrough material - the
 * discussion of WHY one reads better is the teaching, not the code itself.
 */
object Block0Solutions extends Block0 {

  // ---------------------------------------------------------------------------
  // 0.1 Cartesian products
  // ---------------------------------------------------------------------------

  private val files: List[Char] = ('a' to 'h').toList
  private val ranks: List[Int] = (1 to 8).toList

  def allSquares: List[String] =
    for {
      file <- files
      rank <- ranks
    } yield s"$file$rank"

  // desugars to exactly this - worth showing side by side:
  //   files.flatMap(file => ranks.map(rank => s"$file$rank"))

  def darkSquares: List[String] =
    for {
      file <- files
      rank <- ranks
      if (file - 'a' + rank) % 2 == 1
    } yield s"$file$rank"

  // ---------------------------------------------------------------------------
  // 0.2 groupBy
  // ---------------------------------------------------------------------------

  def namesByDepartment(employees: List[Employee]): Map[String, List[String]] =
    employees
      .groupBy(_.department)
      .map((department, staff) => (department, staff.map(_.name).sorted))

  // `.view.mapValues(...).toMap` is the lazy equivalent and avoids rebuilding
  // the keys; at this size it makes no difference and reads worse.

  def averageSalaryByDepartment(employees: List[Employee]): Map[String, Double] =
    employees
      .groupBy(_.department)
      .map((department, staff) => (department, staff.map(_.salary).sum.toDouble / staff.size))

  // `groupMapReduce` does grouping, mapping and folding in one pass:
  //   employees.groupMapReduce(_.department)(_.salary.toDouble)(_ + _)
  // ...but that gives you the TOTAL, and you still need the count for a mean.
  // Worth mentioning, not worth reaching for here.

  // ---------------------------------------------------------------------------
  // 0.3 partition, span, collect
  // ---------------------------------------------------------------------------

  def partitionBySalary(employees: List[Employee], threshold: Int): (List[Employee], List[Employee]) =
    employees.partition(_.salary >= threshold)

  def veteransAndNewcomers(employees: List[Employee]): (List[Employee], List[Employee]) =
    employees.span(_.startYear < 2020)

  def seniorNames(employees: List[Employee], threshold: Int): List[String] =
    employees.collect { case employee if employee.salary >= threshold => employee.name.toUpperCase }

  // the two-pass version, for comparison:
  //   employees.filter(_.salary >= threshold).map(_.name.toUpperCase)
  // `collect` wins when the test and the transformation are the same pattern
  // match - e.g. `case Some(x) => x`. Here it is a coin toss, and that is the
  // honest answer to give them.

  // ---------------------------------------------------------------------------
  // 0.4 Map merging
  // ---------------------------------------------------------------------------

  def mergeInventories(a: Map[String, Int], b: Map[String, Int]): Map[String, Int] =
    b.foldLeft(a) { case (merged, (item, quantity)) =>
      merged.updated(item, merged.getOrElse(item, 0) + quantity)
    }

  // ---------------------------------------------------------------------------
  // 0.5 zip, zipWithIndex, sliding
  // ---------------------------------------------------------------------------

  def dailyDeltas(prices: List[BigDecimal]): List[BigDecimal] =
    prices.sliding(2).collect { case List(previous, current) => current - previous }.toList

  // the zip version - shorter, and it degrades gracefully on an empty list
  // because `Nil.tail` would throw but `Nil.zip(...)` is never reached:
  //   if (prices.isEmpty) Nil else prices.zip(prices.tail).map((p, c) => c - p)
  //
  // note the trap: `sliding(2)` on a single-element list yields ONE group of
  // size 1, not zero groups. The `case List(a, b)` pattern quietly drops it,
  // which is why `collect` is the right tool rather than `map`.

  def largestRise(prices: List[BigDecimal]): Option[(Int, BigDecimal)] =
    dailyDeltas(prices).zipWithIndex
      .collect { case (delta, index) if delta > 0 => (index + 1, delta) }
      .maxByOption(_._2)

  // `maxByOption` keeps the FIRST maximum, which is the earliest day - exactly
  // the tie-break the brief asks for. Getting that for free is luck; check it
  // rather than assume it, which is what the test does.

  // ===========================================================================
  //
  //  STRETCH SECTION - only if you finished the block above.
  //
  // ===========================================================================

  // ---------------------------------------------------------------------------
  // S1. Knight moves
  // ---------------------------------------------------------------------------

  /**
   * The eight offsets, generated rather than typed out: two squares on one
   * axis and one on the other means the two deltas have different magnitudes.
   *
   * Typing the list by hand is a perfectly good answer too, and it is arguably
   * clearer. Worth asking the room which they prefer - the generated version is
   * cleverer, the literal version is obvious, and "obvious" usually wins.
   */
  private val knightOffsets: List[(Int, Int)] =
    for {
      fileDelta <- List(-2, -1, 1, 2)
      rankDelta <- List(-2, -1, 1, 2)
      if fileDelta.abs != rankDelta.abs
    } yield (fileDelta, rankDelta)

  def knightMoves(square: String): List[String] = {
    val file = square.head - 'a'
    val rank = square.tail.toInt

    val moves =
      for {
        (fileDelta, rankDelta) <- knightOffsets
        newFile = file + fileDelta
        newRank = rank + rankDelta
        if newFile >= 0 && newFile <= 7
        if newRank >= 1 && newRank <= 8
      } yield s"${('a' + newFile).toChar}$newRank"

    moves.sorted
  }

  // ---------------------------------------------------------------------------
  // S2. Inverting an index
  // ---------------------------------------------------------------------------

  def invertIndex(byDepartment: Map[String, List[String]]): Map[String, List[String]] =
    byDepartment.toList
      .flatMap((department, members) => members.map(member => (member, department)))
      .groupBy((member, _) => member)
      .map((member, pairs) => (member, pairs.map((_, department) => department).sorted))

  // `groupMap` collapses the last two steps - it groups and maps the values in
  // one go, so you never build the intermediate lists of pairs:
  //
  //   byDepartment.toList
  //     .flatMap((department, members) => members.map(member => (member, department)))
  //     .groupMap((member, _) => member)((_, department) => department)
  //     .map((member, departments) => (member, departments.sorted))
  //
  // Note the `.toList` at the start. Without it you are flatMapping a Map,
  // which tries to build a Map, and duplicate keys silently eat entries.
  // That is a genuinely nasty bug - worth showing it happening.

  // ---------------------------------------------------------------------------
  // S3. Rebuilding a series
  // ---------------------------------------------------------------------------

  def rebuildPrices(opening: BigDecimal, deltas: List[BigDecimal]): List[BigDecimal] =
    deltas.scanLeft(opening)(_ + _)

  // The whole exercise is knowing `scanLeft` exists. foldLeft answers "where
  // did I end up"; scanLeft answers "how did I get there" - which is what you
  // want for a running balance, a cumulative total, or an audit trail.
  //
  // Note the length: scanLeft emits the seed as well, so n deltas give n + 1
  // prices. That is exactly why `rebuildPrices` and `dailyDeltas` are inverses.

  // ---------------------------------------------------------------------------
  // S4. The best trade
  // ---------------------------------------------------------------------------

  /**
   * The obvious version: every buy day paired with every later sell day.
   *
   * Correct, readable, and quadratic. Most of the room will write this, and
   * they should - it is the right first answer. `maxByOption` keeps the first
   * maximum it meets, and the pairs are generated buy-major, so the tie-break
   * falls out for free.
   */
  def bestTradeQuadratic(prices: List[BigDecimal]): Option[(Int, Int, BigDecimal)] = {
    val indexed = prices.zipWithIndex

    val trades =
      for {
        (buyPrice, buyDay) <- indexed
        (sellPrice, sellDay) <- indexed
        if sellDay > buyDay
        if sellPrice > buyPrice
      } yield (buyDay, sellDay, sellPrice - buyPrice)

    trades.maxByOption((_, _, profit) => profit)
  }

  /**
   * The single-pass version. Walking the list once, the only thing worth
   * remembering is the cheapest day so far - because the best trade ending
   * today is "today's price minus the cheapest price behind me".
   *
   * The accumulator carries five things, so it gets a name. A tuple of five
   * would work and would be unreadable; this is the moment to introduce
   * "the fold's accumulator is a domain object", which is how day 3's
   * settlement engine is built.
   */
  private case class Scan(
      cheapestDay: Int,
      cheapestPrice: BigDecimal,
      buyDay: Int,
      sellDay: Int,
      profit: BigDecimal
  )

  def bestTrade(prices: List[BigDecimal]): Option[(Int, Int, BigDecimal)] =
    prices.headOption
      .map { opening =>
        prices.zipWithIndex.tail.foldLeft(Scan(0, opening, -1, -1, BigDecimal(0))) {
          case (scan, (price, day)) =>
            val candidateProfit = price - scan.cheapestPrice

            val afterSelling =
              if (candidateProfit > scan.profit)
                scan.copy(buyDay = scan.cheapestDay, sellDay = day, profit = candidateProfit)
              else scan

            // strictly cheaper, so the EARLIEST cheapest day is the one we keep
            if (price < scan.cheapestPrice) afterSelling.copy(cheapestDay = day, cheapestPrice = price)
            else afterSelling
        }
      }
      .filter(_.profit > 0)
      .map(scan => (scan.buyDay, scan.sellDay, scan.profit))
}
