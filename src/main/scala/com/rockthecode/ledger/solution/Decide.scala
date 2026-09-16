package com.rockthecode.ledger.solution

import Command._
import Event._
import Rejection._

/**
 * Command -> Either a rejection or the facts to record. This is the only place
 * that validates anything. It never changes state: it only proposes.
 *
 * Queries (`Balance`, `History`, `Summary`) still go through here, because the
 * same precedence applies to them (an unknown or closed account is refused).
 * They just propose no facts.
 *
 * The ORDER of the checks below is a protocol decision (PROTOCOL.md, "rejection
 * precedence"), not a coincidence. When several rejections apply, exactly one is
 * emitted, and the golden files depend on which.
 */
object Decide {

  def decide(bank: Bank, cmd: Command): Either[Rejection, List[Event]] = cmd match {
    case Open(acc, code) =>
      if (bank.accounts.contains(acc)) Left(AccountExists(acc)) // also covers closed accounts: closure is terminal
      else Currency.fromCode(code).toRight(UnsupportedCurrency(code)).map(c => List(Opened(acc, c)))

    case Deposit(acc, amount) =>
      for {
        _ <- active(bank, acc)
        _ <- positive(acc, amount)
      } yield List(Deposited(acc, amount))

    case Withdraw(acc, amount) =>
      for {
        a <- active(bank, acc)
        _ <- positive(acc, amount)
        _ <- funded(a, amount)
      } yield List(Withdrawn(acc, amount))

    case Balance(acc) => existing(bank, acc).map(_ => Nil)

    // Freezing a frozen account and unfreezing an open one are idempotent
    // successes: they record a fact and consume a sequence number.
    case Freeze(acc)   => existing(bank, acc).map(_ => List(Frozen(acc)))
    case Unfreeze(acc) => existing(bank, acc).map(_ => List(Unfrozen(acc)))

    case Close(acc) =>
      for {
        a <- existing(bank, acc) // a frozen account may be closed
        _ <- Either.cond(a.balance.isZero, (), NonZeroBalance(acc, a.balance))
      } yield List(Closed(acc))

    case Transfer(from, to, amount) =>
      for {
        _ <- Either.cond(from != to, (), SameAccountTransfer(from)) // before either account is looked up
        src <- bank.account(from).toRight(UnknownAccount(from))
        dst <- bank.account(to).toRight(UnknownAccount(to))
        _ <- notClosed(src)
        _ <- notClosed(dst)
        _ <- notFrozen(src)
        _ <- notFrozen(dst)
        _ <- positive(from, amount)
        _ <- Either.cond(src.currency == dst.currency, (), CurrencyMismatch(from, to, src.currency, dst.currency))
        _ <- funded(src, amount)
      } yield {
        val tag = bank.transfersSoFar + 1
        List(TransferDebited(from, amount, tag), TransferCredited(to, amount, tag))
      }

    case History(acc)       => existing(bank, acc).map(_ => Nil)
    case Summary(acc, _, _) => existing(bank, acc).map(_ => Nil)
  }

  // --- the checks, in precedence order ----------------------------------------

  /** unknown-account, then account-closed. What every single-account command starts with. */
  private def existing(bank: Bank, id: String): Either[Rejection, Account] =
    bank.account(id).toRight(UnknownAccount(id)).flatMap(notClosed)

  /** `existing`, then account-frozen. For commands that move money. */
  private def active(bank: Bank, id: String): Either[Rejection, Account] =
    existing(bank, id).flatMap(notFrozen)

  private def notClosed(a: Account): Either[Rejection, Account] =
    Either.cond(a.status != Status.Closed, a, AccountClosed(a.id))

  private def notFrozen(a: Account): Either[Rejection, Account] =
    Either.cond(a.status != Status.Frozen, a, AccountFrozen(a.id))

  private def positive(id: String, amount: Money): Either[Rejection, Unit] =
    Either.cond(amount.isPositive, (), NonPositiveAmount(id, amount))

  private def funded(a: Account, amount: Money): Either[Rejection, Unit] =
    Either.cond(!(a.balance < amount), (), InsufficientFunds(a.id, a.balance, amount))
}
