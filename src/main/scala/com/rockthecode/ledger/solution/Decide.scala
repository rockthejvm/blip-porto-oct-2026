package com.rockthecode.ledger.solution

import Event._
import Rejection._

/**
 * Mutation -> Either a rejection or the facts to record. This is the only
 * place that validates anything. It never changes state: it only proposes.
 *
 * Queries never arrive here - the `Command` split keeps them out, so the
 * no-sequence-number-for-queries rule is enforced by the signature. The one
 * check queries do need (unknown account, closed account) is `queryable`,
 * which `ReferenceLedger` calls directly.
 *
 * The ORDER of the checks below is a protocol decision (guide, "rejection
 * precedence"), not a coincidence. When several rejections apply, exactly one
 * is emitted, and the golden files depend on which.
 *
 * An accepted mutation always records at least one fact, which `List[Event]`
 * does not express (it admits Nil). A hand-rolled NonEmptyList would - that
 * is stretch-goal material, not skeleton material.
 */
object Decide {

  def decide(bank: Bank, cmd: Mutation): Either[Rejection, List[Event]] = cmd match {
    case Mutation.Open(acc, code) =>
      if (bank.accounts.contains(acc)) Left(AccountExists(acc)) // also covers closed accounts: closure is terminal
      else Currency.fromCode(code).toRight(UnsupportedCurrency(code)).map(c => List(Opened(acc, c)))

    case Mutation.Deposit(acc, amount) =>
      for {
        _ <- unfrozen(bank, acc)
        _ <- positive(acc, amount)
      } yield List(Deposited(acc, amount))

    case Mutation.Withdraw(acc, amount) =>
      for {
        a <- unfrozen(bank, acc)
        _ <- positive(acc, amount)
        _ <- funded(a, amount)
      } yield List(Withdrawn(acc, amount))

    // Freezing a frozen account and unfreezing an open one are idempotent
    // successes: they record a fact and consume a sequence number.
    case Mutation.Freeze(acc)   => queryable(bank, acc).map(_ => List(Frozen(acc)))
    case Mutation.Unfreeze(acc) => queryable(bank, acc).map(_ => List(Unfrozen(acc)))

    case Mutation.Close(acc) =>
      for {
        a <- queryable(bank, acc) // a frozen account may be closed
        _ <- Either.cond(a.balance.isZero, (), NonZeroBalance(acc, a.balance))
      } yield List(Closed(acc))

    case Mutation.Transfer(from, to, amount) =>
      for {
        _ <- Either.cond(from != to, (), SameAccountTransfer(from)) // before either account is looked up
        src0 <- bank.account(from).toRight(UnknownAccount(from))
        dst0 <- bank.account(to).toRight(UnknownAccount(to))
        src1 <- notClosed(src0)
        dst1 <- notClosed(dst0)
        src <- notFrozen(src1)
        _ <- notFrozen(dst1)
        _ <- positive(from, amount)
        _ <- Either.cond(src.currency == dst1.currency, (), CurrencyMismatch(from, to, src.currency, dst1.currency))
        _ <- funded(src, amount)
      } yield {
        val tag = bank.transfersSoFar + 1
        List(TransferDebited(from, amount, tag), TransferCredited(to, amount, tag))
      }
  }

  // --- the checks, in precedence order ----------------------------------------

  /**
   * unknown-account, then account-closed - what every command and query starts
   * with. Note the return type: past this check you are holding an
   * `Account.Active`, so "it has a balance" is carried by the type, not by a
   * comment.
   */
  def queryable(bank: Bank, id: String): Either[Rejection, Account.Active] =
    bank.account(id).toRight(UnknownAccount(id)).flatMap(notClosed)

  /** `queryable`, then account-frozen. For commands that move money. */
  private def unfrozen(bank: Bank, id: String): Either[Rejection, Account.Active] =
    queryable(bank, id).flatMap(notFrozen)

  private def notClosed(a: Account): Either[Rejection, Account.Active] = a match {
    case active: Account.Active => Right(active)
    case _: Account.Closed      => Left(AccountClosed(a.id))
  }

  private def notFrozen(a: Account.Active): Either[Rejection, Account.Active] =
    Either.cond(!a.frozen, a, AccountFrozen(a.id))

  private def positive(id: String, amount: Money): Either[Rejection, Unit] =
    Either.cond(amount.isPositive, (), NonPositiveAmount(id, amount))

  private def funded(a: Account.Active, amount: Money): Either[Rejection, Unit] =
    Either.cond(!(a.balance < amount), (), InsufficientFunds(a.id, a.balance, amount))
}
