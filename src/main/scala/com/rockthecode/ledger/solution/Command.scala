package com.rockthecode.ledger.solution

/**
 * A request. It may be refused, and until `decide` has looked at it nothing has
 * happened. Compare `Event`.
 *
 * `Open` carries the currency as the raw code: a well-formed but unsupported
 * code (`JPY`) is a rejection, not a parse error, so it has to survive parsing.
 */
enum Command {
  case Open(account: String, currency: String)
  case Deposit(account: String, amount: Money)
  case Withdraw(account: String, amount: Money)
  case Balance(account: String)
  case Freeze(account: String)
  case Unfreeze(account: String)
  case Close(account: String)
  case Transfer(from: String, to: String, amount: Money)
  case History(account: String)
  case Summary(account: String, fromSeq: Int, toSeq: Int)
}
