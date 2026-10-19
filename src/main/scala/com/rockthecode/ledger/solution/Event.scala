package com.rockthecode.ledger.solution

enum Event {
  case Opened(account: String, currency: Currency)
  case Deposited(account: String, amount: Money)
  case Withdrawn(account: String, amount: Money)
  case Frozen(account: String)
  case Unfrozen(account: String)
  case Closed(account: String)
  case TransferDebited(account: String, amount: Money, transfer: Int)
  case TransferCredited(account: String, amount: Money, transfer: Int)

  def account: String
}
