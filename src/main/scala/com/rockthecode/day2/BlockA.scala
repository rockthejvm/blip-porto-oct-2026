package com.rockthecode.day2

/**
 * Day 2, Block A - Thinking in expressions.
 *   - no `var`
 *   - no `while`
 *   - no mutable collection
 *
 * This trait is the exercise brief. Write your answers in `BlockAExercises`.
 */
trait BlockA {

  // ---------------------------------------------------------------------------
  // A.1 The rewrite
  //
  // `BlockAExercises.imperativeSummarize` is working code. It uses a var, a
  // while, a ListBuffer and a null. Produce the same answer without any of them.
  // ---------------------------------------------------------------------------

  /** Sum of every order's amount. Zero for an empty list. */
  def totalValue(orders: List[Order]): BigDecimal

  /** The orders worth strictly more than `threshold`, in their original order. */
  def largeOrders(orders: List[Order], threshold: BigDecimal): List[Order]

  /**
   * The single most valuable order, or None if there are no orders.
   * On a tie, the EARLIEST of the tied orders wins - the imperative version
   * uses `>`.
   */
  def largestOrder(orders: List[Order]): Option[Order]

  /** All three of the above, as one value. Hint: compose functions. */
  def summarize(orders: List[Order], threshold: BigDecimal): OrderSummary

  // ---------------------------------------------------------------------------
  // A.2 `.copy` and fold
  // ---------------------------------------------------------------------------

  /**
   * Apply one transaction to an account, returning a NEW account.
   * Nothing is ever mutated. Use `.copy`.
   *
   * The rules:
   *   - Deposit(a)    - if the account is Active, balance goes up by `a`.
   *                     If it is Frozen, nothing happens.
   *   - Withdrawal(a) - if the account is Active AND the balance is at least
   *                     `a`, balance goes down by `a`. Otherwise nothing happens.
   *                     Never go negative, never throw.
   *   - Freeze        - status becomes Frozen. Balance untouched.
   *   - Unfreeze      - status becomes Active. Balance untouched.
   *
   * A rejected transaction is not an error, it just returns the account
   * unchanged.
   */
  def applyTransaction(account: Account, tx: Transaction): Account

  /**
   * Apply a whole history of transactions, in order.
   */
  def applyAll(account: Account, txs: List[Transaction]): Account

  // ---------------------------------------------------------------------------
  // A.3 A report renderer
  // ---------------------------------------------------------------------------

  /**
   * Render a duration in milliseconds as a human-readable string.
   *
   * Units are h, m, s, ms. Zero units are skipped. If everything is zero the
   * answer is "0ms".
   *
   *        0 -> "0ms"
   *      999 -> "999ms"
   *     1000 -> "1s"
   *    61000 -> "1m 1s"
   *  3600000 -> "1h"
   *  8103000 -> "2h 15m 3s"
   *  8103456 -> "2h 15m 3s 456ms"
   *
   * Assume `millis >= 0`.
   */
  def formatDuration(millis: Long): String

  /**
   * One aligned line of the report. 
   */
  def renderLine(req: Request): String = 
    f"${req.method}%-6s ${req.path}%-24s ${req.statusCode}%3d  ${formatDuration(req.durationMillis)}%s"

  /**
   * The whole report, one string per request, in the original order.
   */
  def renderReport(requests: List[Request]): List[String]

  // ===========================================================================
  //
  //  STRETCH SECTION - only if you finished the block above.
  //
  // ===========================================================================

  /**
   * SA1. Move money between two accounts. Return both, updated.
   *
   * The transfer goes through only if BOTH accounts are Active and `from` has
   * at least `amount`. Otherwise both accounts come back untouched - it is
   * all-or-nothing. 
   */
  def transfer(from: Account, to: Account, amount: BigDecimal): (Account, Account)

  /**
   * SA2. Apply a whole batch of transfers to a whole bank.
   *
   * The bank is `Map[accountId, Account]`. Transfers are applied in order, each
   * one seeing the results of the ones before it.
   *
   * A transfer is silently skipped when either account id is unknown, or when
   * `from` and `to` are the same account.
   */
  def applyTransfers(bank: Map[String, Account], transfers: List[Transfer]): Map[String, Account]

  /**
   * SA3. Replay a history and also report what was refused.
   *
   * Returns the final account AND, in order, every transaction that did not
   * take effect, with its position in the list and why.
   *
   * There are two possible reasons:
   *   - "account frozen"      - a deposit or withdrawal while Frozen
   *   - "insufficient funds"  - a withdrawal from an Active account that cannot cover it
   *
   * Frozen takes precedence. A withdrawal that is both too large and against a frozen
   * account reports "account frozen".
   *
   * `Freeze` and `Unfreeze` always succeed and are never reported.
   */
  def applyAllTracking(account: Account, txs: List[Transaction]): (Account, List[Rejection])

  /**
   * SA4. Parse a duration string back into milliseconds - the inverse of
   * `formatDuration`.
   *
   *   "0ms"             -> 0
   *   "999ms"           -> 999
   *   "1m 1s"           -> 61000
   *   "2h 15m 3s"       -> 8103000
   *   "2h 15m 3s 456ms" -> 8103456
   *
   * Assume the input is something `formatDuration` produced, so it is always
   * well-formed and the units are always in descending order.
   */
  def parseDuration(text: String): Long

  /**
   * SA5. Summarise a traffic sample.
   *
   *   - `total`          - how many requests
   *   - `byStatusClass`  - "2xx" -> count, "4xx" -> count, ... only the classes
   *                        that actually occur
   *   - `p95Millis`      - the 95th percentile duration, nearest-rank: sort the
   *                        durations ascending and take the one at index
   *                        `ceil(0.95 * n) - 1`. For an empty sample, 0.
   *   - `slowest`        - the slowest request, earliest one on a tie; None if
   *                        there were no requests.
   */
  def summarizeTraffic(requests: List[Request]): TrafficSummary
}

// -----------------------------------------------------------------------------
// Fixture types
// -----------------------------------------------------------------------------

case class Order(id: String, customer: String, amount: BigDecimal)

case class OrderSummary(total: BigDecimal, large: List[Order], largest: Option[Order])

sealed trait AccountStatus
case object Active extends AccountStatus
case object Frozen extends AccountStatus

case class Account(id: String, balance: BigDecimal, status: AccountStatus)

sealed trait Transaction
case class Deposit(amount: BigDecimal) extends Transaction
case class Withdrawal(amount: BigDecimal) extends Transaction
case object Freeze extends Transaction
case object Unfreeze extends Transaction

case class Request(method: String, path: String, statusCode: Int, durationMillis: Long)

case class Transfer(from: String, to: String, amount: BigDecimal)

case class Rejection(index: Int, reason: String)

case class TrafficSummary(
    total: Int,
    byStatusClass: Map[String, Int],
    p95Millis: Long,
    slowest: Option[Request]
)
