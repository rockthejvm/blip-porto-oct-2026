package com.rockthecode.ledger.solution

/**
 * Why `decide` refused a command. One flat enum; each case carries exactly the
 * data its wire line prints. The kebab-case codes live in `Wire`, not here.
 *
 * Design note: `InsufficientFunds` and `NonZeroBalance` both carry a balance and
 * could share a shape. They do not, because they are different facts with
 * different futures - if one of them grows a field the other should not follow.
 */
enum Rejection {
  case UnknownAccount(account: String)
  case AccountExists(account: String)
  case UnsupportedCurrency(currency: String)
  case AccountClosed(account: String)
  case AccountFrozen(account: String)
  case NonPositiveAmount(account: String, amount: Money)
  case InsufficientFunds(account: String, balance: Money, requested: Money)
  case SameAccountTransfer(account: String)
  case CurrencyMismatch(from: String, to: String, fromCurrency: Currency, toCurrency: Currency)
  case NonZeroBalance(account: String, balance: Money)
}
