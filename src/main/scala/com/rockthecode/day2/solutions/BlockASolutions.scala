package com.rockthecode.day2.solutions

import com.rockthecode.day2.*

/**
 * Day 2, Block A - reference solutions.
 */
object BlockASolutions extends BlockA {

  // ---------------------------------------------------------------------------
  // A.1 The rewrite
  // ---------------------------------------------------------------------------

  def totalValue(orders: List[Order]): BigDecimal =
    orders.map(_.amount).sum

  def largeOrders(orders: List[Order], threshold: BigDecimal): List[Order] =
    orders.filter(_.amount > threshold)

  def largestOrder(orders: List[Order]): Option[Order] =
    orders.maxByOption(_.amount)

  def summarize(orders: List[Order], threshold: BigDecimal): OrderSummary =
    OrderSummary(
      total = totalValue(orders),
      large = largeOrders(orders, threshold),
      largest = largestOrder(orders)
    )

  /**
   * The single-pass version, for the walkthrough.
   *
   * This is what somebody in the room will insist on, because the pipeline
   * version "traverses three times". Show it, then ask which one they would
   * rather be handed at 2am. The three pipelines each have a name; this one
   * has a tuple.
   *
   * Land it on: clarity beats traversal count until a profiler says otherwise.
   */
  def summarizeInOnePass(orders: List[Order], threshold: BigDecimal): OrderSummary =
    orders
      .foldLeft(OrderSummary(BigDecimal(0), Nil, None)) { (summary, order) =>
        OrderSummary(
          total = summary.total + order.amount,
          large = if (order.amount > threshold) summary.large :+ order else summary.large,
          largest = summary.largest.filter(_.amount >= order.amount).orElse(Some(order))
        )
      }

  // ---------------------------------------------------------------------------
  // A.2 `.copy` and the fold
  // ---------------------------------------------------------------------------

  def applyTransaction(account: Account, tx: Transaction): Account =
    (account.status, tx) match {
      case (Active, Deposit(amount)) =>
        account.copy(balance = account.balance + amount)
      case (Active, Withdrawal(amount)) if account.balance >= amount =>
        account.copy(balance = account.balance - amount)
      case (_, Freeze) =>
        account.copy(status = Frozen)
      case (_, Unfreeze) =>
        account.copy(status = Active)
      case _ =>
        account // frozen, or insufficient funds: rejected, nothing changes
    }

  def applyAll(account: Account, txs: List[Transaction]): Account =
    txs.foldLeft(account)(applyTransaction)

  // That is the whole of A.2's point, and it is worth saying out loud:
  //
  //   current state == history.foldLeft(initialState)(apply)
  //
  // Day 3's settlement engine is this line with a bigger `apply`.

  // ---------------------------------------------------------------------------
  // A.3 The report renderer
  // ---------------------------------------------------------------------------

  def formatDuration(millis: Long): String = {
    val parts = List(
      millis / 3600000 -> "h",
      (millis % 3600000) / 60000 -> "m",
      (millis % 60000) / 1000 -> "s",
      millis % 1000 -> "ms"
    )

    val rendered = parts.collect { case (value, unit) if value > 0 => s"$value$unit" }

    if (rendered.isEmpty) "0ms" else rendered.mkString(" ")
  }

  def renderLine(req: Request): String =
    f"${req.method}%-6s ${req.path}%-24s ${req.statusCode}%3d  ${formatDuration(req.durationMillis)}%s"

  def renderReport(requests: List[Request]): List[String] =
    requests.map(renderLine)

  // Nothing in this file prints. `BlockAExercises.main` does, once, at the edge.

  // ===========================================================================
  //
  //  STRETCH SECTION - only if you finished the block above.
  //
  // ===========================================================================

  // ---------------------------------------------------------------------------
  // SA1. Transfer
  // ---------------------------------------------------------------------------

  def transfer(from: Account, to: Account, amount: BigDecimal): (Account, Account) = {
    val debited = applyTransaction(from, Withdrawal(amount))

    // The withdrawal rejects itself by returning the account unchanged, so
    // "did it happen?" is just a comparison. No rules are restated here.
    if (debited == from) (from, to)
    else {
      val credited = applyTransaction(to, Deposit(amount))
      // ...and the deposit can refuse too, if `to` is frozen. Then the debit
      // must not stand either - this is the atomicity, and it is the line
      // people forget.
      if (credited == to) (from, to) else (debited, credited)
    }
  }

  // ---------------------------------------------------------------------------
  // SA2. The whole bank
  // ---------------------------------------------------------------------------

  def applyTransfers(bank: Map[String, Account], transfers: List[Transfer]): Map[String, Account] =
    transfers.foldLeft(bank) { (currentBank, request) =>
      if (request.from == request.to) currentBank
      else
        (currentBank.get(request.from), currentBank.get(request.to)) match {
          case (Some(from), Some(to)) =>
            val (debited, credited) = transfer(from, to, request.amount)
            currentBank.updated(debited.id, debited).updated(credited.id, credited)
          case _ =>
            currentBank // unknown account: skip
        }
    }

  // Same `foldLeft` as A.2, with a Map as the state instead of one account.
  // Worth saying out loud: nothing about `foldLeft` changed. The step function
  // got bigger, and that is all. Day 3's engine is this shape.
  //
  // The `request.from == request.to` guard is not pedantry. Without it, the
  // debit and the credit both land on the same id, the second `updated` wins,
  // and the account is credited without ever being debited. Free money.

  // ---------------------------------------------------------------------------
  // SA3. State plus a log
  // ---------------------------------------------------------------------------

  def applyAllTracking(account: Account, txs: List[Transaction]): (Account, List[Rejection]) = {
    val (finalAccount, rejections) =
      txs.zipWithIndex.foldLeft((account, List.empty[Rejection])) {
        case ((current, log), (tx, index)) =>
          val next = applyTransaction(current, tx)

          if (next != current) (next, log) // it took effect
          else
            tx match {
              case Freeze | Unfreeze =>
                (next, log) // no-ops, never a rejection
              case _ if current.status == Frozen =>
                (next, Rejection(index, "account frozen") :: log)
              case _ =>
                (next, Rejection(index, "insufficient funds") :: log)
            }
      }

    (finalAccount, rejections.reverse)
  }

  // Two things worth pointing at in the walkthrough:
  //
  // 1. The log is built by prepending and reversed once at the end. Appending
  //    with `:+` inside a fold is quadratic on a List. Nobody notices at n = 6
  //    and everybody notices at n = 6 million.
  //
  // 2. "Did it take effect?" is `next != current`, which works because these
  //    are case classes with value equality. It is also slightly too clever: a
  //    deposit of zero is indistinguishable from a rejection. The honest fix is
  //    to have `applyTransaction` say what happened rather than making the
  //    caller guess - which is exactly what Block E is for. Say so.

  // ---------------------------------------------------------------------------
  // SA4. Parsing a duration
  // ---------------------------------------------------------------------------

  private val unitMillis: Map[String, Long] =
    Map("h" -> 3600000L, "m" -> 60000L, "s" -> 1000L, "ms" -> 1L)

  def parseDuration(text: String): Long =
    text
      .split(" ")
      .toList
      .map { token =>
        val digits = token.takeWhile(_.isDigit)
        val unit = token.dropWhile(_.isDigit)
        digits.toLong * unitMillis(unit)
      }
      .sum

  // Splitting on "is it a digit" rather than on position is what makes "ms"
  // fall out for free. Anyone who reaches for `endsWith("s")` will get "2h 15m
  // 3s" wrong by a factor of a thousand, and the round-trip test will catch
  // them - which is a nicer way to learn it than a code review.

  // ---------------------------------------------------------------------------
  // SA5. Traffic summary
  // ---------------------------------------------------------------------------

  def summarizeTraffic(requests: List[Request]): TrafficSummary =
    TrafficSummary(
      total = requests.size,
      byStatusClass = requests.groupBy(request => s"${request.statusCode / 100}xx").map((cls, rs) => (cls, rs.size)),
      p95Millis = percentile(requests.map(_.durationMillis), 0.95),
      slowest = requests.maxByOption(_.durationMillis)
    )

  private def percentile(values: List[Long], fraction: Double): Long =
    if (values.isEmpty) 0L
    else {
      val sorted = values.sorted
      val rank = math.ceil(fraction * sorted.size).toInt
      sorted(math.max(rank - 1, 0))
    }

  // `groupBy(...).map((k, v) => (k, v.size))` is `groupMapReduce`:
  //   requests.groupMapReduce(r => s"${r.statusCode / 100}xx")(_ => 1)(_ + _)
  // Shorter, and a good name to know, but harder to read cold. Show both and
  // let them choose - the point of this block is that the choice is theirs.
}
