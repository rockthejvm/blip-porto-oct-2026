package com.rockthecode.ledger

/**
 * The single contract between your ledger and the harness.
 *
 * `S` is YOUR state type. The harness never inspects it; it only threads it
 * through. Everything about your data model is your decision.
 */
trait LedgerEngine[S] {

  /** The state of a ledger that has seen no events. */
  def empty: S

  /**
   * Handle one raw input line.
   *
   * Returns the new state and the response lines to print, in order. This
   * function must be pure: same state + same line => same result, always,
   * with no I/O and no mutation.
   */
  def execute(state: S, line: String): (S, List[String])

  // ---------------------------------------------------------------------------
  // Milestone 3. Until you override these two, the journal is a no-op: nothing
  // is written, and a restart comes back with an empty ledger.
  // ---------------------------------------------------------------------------

  /**
   * The lines to append to the journal after one input line took the ledger
   * from `before` to `after`. Empty when nothing was recorded. The format is
   * your choice; only `replay` ever reads it.
   */
  def journalLines(before: S, after: S): List[String] = Nil

  /** Rebuild the state from every line `journalLines` has ever produced. */
  def replay(lines: List[String]): S = empty
}
