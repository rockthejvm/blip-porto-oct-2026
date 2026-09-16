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
      case Opened(id, currency)          => bank.accounts + (id -> Account(id, currency, Money.zero, Status.Open))
      case Deposited(id, amount)         => adjust(bank, id, _ + amount)
      case Withdrawn(id, amount)         => adjust(bank, id, _ - amount)
      case TransferDebited(id, amount, _)  => adjust(bank, id, _ - amount)
      case TransferCredited(id, amount, _) => adjust(bank, id, _ + amount)
      case Frozen(id)                    => setStatus(bank, id, Status.Frozen)
      case Unfrozen(id)                  => setStatus(bank, id, Status.Open)
      case Closed(id)                    => setStatus(bank, id, Status.Closed)
    }
    Bank(accounts, bank.events :+ event)
  }

  // Both helpers are no-ops for an account that does not exist. A valid log
  // never contains such an event, and totality means we do not throw.
  private def adjust(bank: Bank, id: String, f: Money => Money): Map[String, Account] =
    bank.accounts.updatedWith(id)(_.map(a => a.copy(balance = f(a.balance))))

  private def setStatus(bank: Bank, id: String, status: Status): Map[String, Account] =
    bank.accounts.updatedWith(id)(_.map(_.copy(status = status)))
}
