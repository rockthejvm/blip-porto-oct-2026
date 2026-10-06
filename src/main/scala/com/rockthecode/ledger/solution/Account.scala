package com.rockthecode.ledger.solution

/**
 * The current view of one account - a sum, not a record with a status field.
 *
 * Design note: the obvious model is a product, `Account(id, currency, balance,
 * status)` with `enum Status { Open, Frozen, Closed }`. It works, but it can
 * express a state the protocol forbids: a closed account with money in it.
 * That invariant then has to be *guaranteed* by every function that closes an
 * account. Here `Closed` simply has no balance field, so the illegal state
 * cannot be written - the only way to construct it is to give the money up.
 *
 * Look at how the protocol made this possible: `CLOSE` requires a zero
 * balance, and closed accounts reject even `BALANCE` - so nothing ever needs
 * to ask a closed account what it holds. Spec design and type design are the
 * same activity.
 *
 * Within `Active`, `frozen` stays a plain Boolean: frozen accounts genuinely
 * have balances and answer queries, so there is no field to drop and a nested
 * sum would be ceremony.
 */
enum Account {
  case Active(id: String, currency: Currency, balance: Money, frozen: Boolean)
  case Closed(id: String, currency: Currency)

  /** What every account has, whatever state it is in. */
  def id: String
  def currency: Currency
}
