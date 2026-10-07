package com.rockthecode.day2

import scala.collection.mutable.ListBuffer

/**
 * Day 2, Block A 

 * Run the tests with:
 *   sbt "testOnly com.rockthecode.day2.BlockAExercisesSuite"
 *
 * Run the report demo with:
 *   sbt "runMain com.rockthecode.day2.BlockAExercises"
 */
object BlockAExercises extends BlockA {

  // ---------------------------------------------------------------------------
  // A.1 The rewrite
  //
  // THIS IS THE CODE YOU ARE REPLACING.  Do not change it. 
  // ---------------------------------------------------------------------------

  def imperativeSummarize(orders: List[Order], threshold: BigDecimal): OrderSummary = {
    var total: BigDecimal = 0
    val large = ListBuffer.empty[Order]
    var largest: Order = null

    val arr = orders.toArray
    var i = 0
    while (i < arr.length) {
      val order = arr(i)
      total += order.amount
      if (order.amount > threshold) large += order
      if (largest == null || order.amount > largest.amount) largest = order
      i += 1
    }

    OrderSummary(total, large.toList, Option(largest))
  }

  // ... and here is your version.

  def totalValue(orders: List[Order]): BigDecimal = ???

  def largeOrders(orders: List[Order], threshold: BigDecimal): List[Order] = ???

  def largestOrder(orders: List[Order]): Option[Order] = ???

  def summarize(orders: List[Order], threshold: BigDecimal): OrderSummary = ???

  // ---------------------------------------------------------------------------
  // A.2 `.copy` and the fold
  // ---------------------------------------------------------------------------

  def applyTransaction(account: Account, tx: Transaction): Account = ???

  def applyAll(account: Account, txs: List[Transaction]): Account = ???

  // ---------------------------------------------------------------------------
  // A.3 The report renderer
  // ---------------------------------------------------------------------------

  def formatDuration(millis: Long): String = ???

  def renderReport(requests: List[Request]): List[String] = ???

  // ===========================================================================
  //
  //  STRETCH SECTION - only if you finished the block above.
  //
  // ===========================================================================

  def transfer(from: Account, to: Account, amount: BigDecimal): (Account, Account) = ???

  def applyTransfers(bank: Map[String, Account], transfers: List[Transfer]): Map[String, Account] = ???

  def applyAllTracking(account: Account, txs: List[Transaction]): (Account, List[Rejection]) = ???

  def parseDuration(text: String): Long = ???

  def summarizeTraffic(requests: List[Request]): TrafficSummary = ???

  // ---------------------------------------------------------------------------
  // The part that prints
  // ---------------------------------------------------------------------------

  val sampleTraffic: List[Request] = List(
    Request("GET", "/api/orders", 200, 43),
    Request("POST", "/api/orders", 201, 1290),
    Request("GET", "/api/orders/8812", 404, 7),
    Request("DELETE", "/api/sessions/current", 204, 61000),
    Request("GET", "/health", 200, 0)
  )

  def main(args: Array[String]): Unit =
    renderReport(sampleTraffic).foreach(println)
}
