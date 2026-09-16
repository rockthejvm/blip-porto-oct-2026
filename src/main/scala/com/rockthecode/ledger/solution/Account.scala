package com.rockthecode.ledger.solution

/**
 * The lifecycle of an account. Closed is terminal.
 *
 * Design note: two booleans (`frozen`, `closed`) would also work, and would
 * admit a "frozen and closed" state that cannot happen. The enum cannot.
 */
enum Status {
  case Open, Frozen, Closed
}

/**
 * The current view of one account.
 *
 * Design note: `balance` is derivable from the event log, so storing it here is
 * a cache of the fold, not a second source of truth. `evolve` is the only thing
 * that writes it. Recomputing it from events on every command would be equally
 * correct and O(n) instead of O(1).
 */
final case class Account(id: String, currency: Currency, balance: Money, status: Status)
