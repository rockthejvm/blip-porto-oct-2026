package com.rockthecode.day2

/**
 * Day 2, Block A - Thinking in expressions.
 *
 * The habit the whole day rests on: reach for an EXPRESSION, not a statement.
 *
 * Ground rules from here to the end of the day:
 *   - no `var`
 *   - no `while`
 *   - no mutable collection
 *
 * Everything in this block has a direct Kotlin / Java-streams analogue. The
 * point is not Scala syntax; it is the shape of the solution.
 *
 * This trait is the exercise brief. Write your answers in `BlockAExercises`.
 */
trait BlockA {

  // ---------------------------------------------------------------------------
  // A.1 The rewrite
  //
  // `BlockAExercises.imperativeSummarize` is working code. It uses a var, a
  // while, a ListBuffer and a null. Produce the same answer without any of them.
  //
  // Start by writing the three parts separately - they are three named pipelines
  // - and only then decide whether to fuse them.
  // ---------------------------------------------------------------------------

  /** Sum of every order's amount. Zero for an empty list. */
  def totalValue(orders: List[Order]): BigDecimal

  /** The orders worth strictly more than `threshold`, in their original order. */
  def largeOrders(orders: List[Order], threshold: BigDecimal): List[Order]

  /**
   * The single most valuable order, or None if there are no orders.
   * On a tie, the EARLIEST of the tied orders wins - the imperative version
   * uses `>`, so it keeps the first one it saw. Match that.
   */
  def largestOrder(orders: List[Order]): Option[Order]

  /** All three of the above, as one value. Compose the functions you just wrote. */
  def summarize(orders: List[Order], threshold: BigDecimal): OrderSummary

  // ---------------------------------------------------------------------------
  // A.2 `.copy` and the fold
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
   * A rejected transaction is not an error here - it just returns the account
   * unchanged. (On day 3 you will want to know WHY it was rejected. That is
   * what Block E is for.)
   */
  def applyTransaction(account: Account, tx: Transaction): Account

  /**
   * Apply a whole history of transactions, in order.
   *
   * Look at the shape of `applyTransaction`: (State, Event) => State.
   * That is exactly the function `foldLeft` wants. Replaying a history to get
   * the current state is one line.
   *
   * Remember this one - it is the heart of day 3's project.
   */
  def applyAll(account: Account, txs: List[Transaction]): Account

  // ---------------------------------------------------------------------------
  // A.3 The report renderer
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
   * One aligned line of the report. Exactly this f-interpolator:
   *
   *   f"${req.method}%-6s ${req.path}%-24s ${req.statusCode}%3d  ${formatDuration(req.durationMillis)}%s"
   *
   * The format is given to you so you can spend the time on the composition
   * rather than on counting spaces. Note that `renderLine` calls
   * `formatDuration` - small pure functions, composed.
   */
  def renderLine(req: Request): String

  /**
   * The whole report, one string per request, in the original order.
   *
   * Notice what is NOT in this signature: printing. `renderReport` builds
   * strings; something at the very edge of the program prints them. That is why
   * this is testable and a println-in-a-loop is not.
   *
   * `BlockAExercises.main` is that edge - it does the printing, once.
   */
  def renderReport(requests: List[Request]): List[String]

  // ===========================================================================
  //
  //  STRETCH SECTION - only if you finished the block above.
  //
  //  A was about turning statements into expressions. These are about what
  //  happens when the thing you are folding gets bigger than a number: two
  //  accounts that must move together, a state plus a log, a whole bank held in
  //  a Map. The answer never changes - build a new value, do not mutate an old
  //  one - but it stops being obvious how.
  //
  //  Nothing later in the day depends on any of these.
  //
  // ===========================================================================

  /**
   * SA1. Move money between two accounts. Return both, updated.
   *
   * The transfer goes through only if BOTH accounts are Active and `from` has
   * at least `amount`. Otherwise both accounts come back untouched - it is
   * all-or-nothing. Money is never created and never destroyed.
   *
   * The interesting part is atomicity. You already have `applyTransaction`;
   * a withdrawal that gets rejected returns the account unchanged, so you can
   * ask whether it happened by comparing before and after. Do not re-implement
   * the rules - compose what you have.
   */
  def transfer(from: Account, to: Account, amount: BigDecimal): (Account, Account)

  /**
   * SA2. Apply a whole batch of transfers to a whole bank.
   *
   * The bank is `Map[accountId, Account]`. Transfers are applied in order, each
   * one seeing the results of the ones before it.
   *
   * A transfer is silently skipped when either account id is unknown, or when
   * `from` and `to` are the same account. (Miss that second one and a
   * self-transfer will happily duplicate money - which is the kind of bug that
   * makes the news.)
   *
   * This is `applyAll` again, one level up: the accumulator is no longer one
   * account, it is the entire bank. `foldLeft` does not care how big the state
   * is.
   *
   * A good sanity check, and the one the tests use: the total money in the bank
   * must be identical before and after.
   */
  def applyTransfers(bank: Map[String, Account], transfers: List[Transfer]): Map[String, Account]

  /**
   * SA3. Replay a history and also report what was refused.
   *
   * Returns the final account AND, in order, every transaction that did not
   * take effect, with its position in the list and why.
   *
   * The two reasons, exactly these strings:
   *   - "account frozen"      - a deposit or withdrawal while Frozen
   *   - "insufficient funds"  - a withdrawal from an Active account that cannot cover it
   *
   * Frozen first: a withdrawal that is both too large and against a frozen
   * account reports "account frozen".
   *
   * `Freeze` and `Unfreeze` always succeed and are never reported.
   *
   * Still one fold. The accumulator now carries two things - the state and the
   * log - which is the whole lesson. Note what you are NOT doing: throwing,
   * printing, or stopping at the first problem.
   *
   * (Block E replaces that `String` with a proper error type. Notice how much
   * you already dislike the String.)
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
   * well-formed and the units are always in descending order. (Handling
   * garbage input needs a way to say "no" - that is Block D.)
   *
   * Watch the units: "s" and "ms" both end in an s.
   *
   * The tests check the round trip in both directions, which is a much stronger
   * statement than any list of examples: format then parse must be the
   * identity.
   */
  def parseDuration(text: String): Long

  /**
   * SA5. Summarise a traffic sample. Pure - it builds a value, it prints nothing.
   *
   *   - `total`          - how many requests
   *   - `byStatusClass`  - "2xx" -> count, "4xx" -> count, ... only the classes
   *                        that actually occur
   *   - `p95Millis`      - the 95th percentile duration, nearest-rank: sort the
   *                        durations ascending and take the one at index
   *                        `ceil(0.95 * n) - 1`. For an empty sample, 0.
   *   - `slowest`        - the slowest request, earliest one on a tie; None if
   *                        there were no requests.
   *
   * Four independent questions about one list. Answer them separately and
   * assemble, exactly as in A.1 - then decide for yourself whether fusing them
   * into one pass would improve anything.
   */
  def summarizeTraffic(requests: List[Request]): TrafficSummary
}

// -----------------------------------------------------------------------------
// Fixture types
// -----------------------------------------------------------------------------

case class Order(id: String, customer: String, amount: BigDecimal)

case class OrderSummary(total: BigDecimal, large: List[Order], largest: Option[Order])

/**
 * Written as a sealed trait rather than a Scala 3 `enum` so that this code reads
 * the same in Scala 2 and Scala 3 - you have both in production.
 */
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
