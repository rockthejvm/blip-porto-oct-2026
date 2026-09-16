package com.rockthecode.ledger.solution

/**
 * A fact. It already happened; it cannot be refused, and `evolve` applies it
 * without looking. The event log is the truth; everything else is a view of it.
 *
 * Design note: credits and debits are separate cases here (`Deposited`,
 * `Withdrawn`, and the two halves of a transfer). A single
 * `Adjusted(account, delta)` event would be an equally valid model, and the wire
 * format cannot tell the two apart - it prints a signed adjustment either way.
 * We chose separate cases because they read like the whiteboard and keep
 * `evolve` free of sign arithmetic.
 */
enum Event {
  case Opened(account: String, currency: Currency)
  case Deposited(account: String, amount: Money)
  case Withdrawn(account: String, amount: Money)
  case Frozen(account: String)
  case Unfrozen(account: String)
  case Closed(account: String)
  case TransferDebited(account: String, amount: Money, transfer: Int)
  case TransferCredited(account: String, amount: Money, transfer: Int)

  /** Every event is about exactly one account. */
  def account: String
}
