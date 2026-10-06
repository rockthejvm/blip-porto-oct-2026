package com.rockthecode.ledger.solution

/**
 * A request. It may be refused, and until `decide` has looked at it nothing
 * has happened. Compare `Event`.
 *
 * Design note: this is a sum of sums. Commands split into mutations (which may
 * record facts) and queries (which never do), and `decide` only accepts a
 * `Mutation` - so "a query cannot consume a sequence number" is not a rule
 * anyone has to remember, it is a function that cannot be called. One flat
 * enum with the queries answering `Right(Nil)` would also work; then the
 * guarantee lives in discipline instead of in a type.
 *
 * `Open` carries the currency as the raw code: a well-formed but unsupported
 * code (`JPY`) is a rejection, not a parse error, so it has to survive parsing.
 */
sealed trait Command

enum Mutation extends Command {
  case Open(account: String, currency: String)
  case Deposit(account: String, amount: Money)
  case Withdraw(account: String, amount: Money)
  case Freeze(account: String)
  case Unfreeze(account: String)
  case Close(account: String)
  case Transfer(from: String, to: String, amount: Money)
}

enum Query extends Command {
  case Balance(account: String)
  case History(account: String)
  case Summary(account: String, fromSeq: Int, toSeq: Int)

  /** Every query is about exactly one account. */
  def account: String
}
