package com.rockthecode.ledger.solution

enum Account {
  case Active(id: String, currency: Currency, balance: Money, frozen: Boolean)
  case Closed(id: String, currency: Currency)

  /** What every account has, whatever state it is in. */
  def id: String
  def currency: Currency
}
