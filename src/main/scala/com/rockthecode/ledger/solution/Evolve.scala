package com.rockthecode.ledger.solution

import Event._

/**
 * (state, fact) -> state. Total: every event is applied, unconditionally, with
 * no `Either` in sight. If the fact is in the log, it happened. Validation was
 * `decide`'s job, once, at the time - it is never re-run, which is why replaying
 * a log written last Tuesday cannot fail.
 *
 * `Bank.empty` folded with this function over the log IS the application state.
 */
object Evolve {

  def evolve(bank: Bank, event: Event): Bank = {
    val accounts = event match {
      case Opened(id, currency)            => bank.accounts + (id -> Account.Active(id, currency, Money.zero, frozen = false))
      case Deposited(id, amount)           => adjust(bank, id, _ + amount)
      case Withdrawn(id, amount)           => adjust(bank, id, _ - amount)
      case TransferDebited(id, amount, _)  => adjust(bank, id, _ - amount)
      case TransferCredited(id, amount, _) => adjust(bank, id, _ + amount)
      case Frozen(id)                      => withActive(bank, id)(_.copy(frozen = true))
      case Unfrozen(id)                    => withActive(bank, id)(_.copy(frozen = false))
      case Closed(id)                      => withActive(bank, id)(a => Account.Closed(a.id, a.currency))
    }
    Bank(accounts, bank.events :+ event)
  }

  private def adjust(bank: Bank, id: String, f: Money => Money): Map[String, Account] =
    withActive(bank, id)(a => a.copy(balance = f(a.balance)))

  // A no-op for an account that does not exist or is already closed. A valid
  // log never contains such an event, and totality means we do not throw.
  private def withActive(bank: Bank, id: String)(f: Account.Active => Account): Map[String, Account] =
    bank.accounts.updatedWith(id)(_.map {
      case a: Account.Active => f(a)
      case closed            => closed
    })
}
