package com.rockthecode.ledger.solution

import Event._

/**
 * Read models computed from the log. Nothing here changes anything; each
 * function is a fold over (a filtered view of) `bank.events`.
 */
object Projections {

  /** One fact about one account, with its global sequence number and the account's balance right after it. */
  final case class Fact(seq: Int, event: Event, balanceAfter: Money)

  /** How a fact moves the balance. Lifecycle facts move it by zero. */
  def delta(event: Event): Money = event match {
    case Deposited(_, amount)           => amount
    case TransferCredited(_, amount, _) => amount
    case Withdrawn(_, amount)           => Money(-amount.cents)
    case TransferDebited(_, amount, _)  => Money(-amount.cents)
    case Opened(_, _) | Frozen(_) | Unfrozen(_) | Closed(_) => Money.zero
  }

  /** Every fact about `account`, oldest first, with running balances. Empty for an unknown account. */
  def facts(bank: Bank, account: String): List[Fact] = {
    val own = bank.events.zipWithIndex.collect { case (e, i) if e.account == account => (e, i + 1) }
    own
      .foldLeft((Money.zero, Vector.empty[Fact])) { case ((balance, acc), (event, seq)) =>
        val after = balance + delta(event)
        (after, acc :+ Fact(seq, event, after))
      }
      ._2
      .toList
  }

  /** The SUMMARY accumulator. `closing` is derived, so it cannot disagree with the parts. */
  final case class Summary(opening: Money, credits: Money, debits: Money, count: Int) {
    def closing: Money = opening + credits - debits
  }

  /**
   * Aggregate one account's facts over the inclusive window [fromSeq, toSeq] of
   * GLOBAL sequence numbers. A window with none of the account's facts is
   * fine: zeros, and opening == closing == the balance going into the window.
   */
  def summary(bank: Bank, account: String, fromSeq: Int, toSeq: Int): Summary = {
    val all = facts(bank, account)
    val opening = all.filter(_.seq < fromSeq).lastOption.map(_.balanceAfter).getOrElse(Money.zero)

    val result = all
      .filter(f => f.seq >= fromSeq && f.seq <= toSeq)
      .foldLeft(Summary(opening, Money.zero, Money.zero, 0)) { (acc, fact) =>
        val d = delta(fact.event)
        if (d.isPositive) acc.copy(credits = acc.credits + d, count = acc.count + 1)
        else acc.copy(debits = acc.debits - d, count = acc.count + 1)
      }

    // Free property test: the fold over the window must land where the running
    // balance says the account was at toSeq. If it ever does not, the model is wrong.
    val balanceAtTo = all.filter(_.seq <= toSeq).lastOption.map(_.balanceAfter).getOrElse(Money.zero)
    require(result.closing == balanceAtTo, s"SUMMARY $account $fromSeq $toSeq: closing ${result.closing} != balance $balanceAtTo")
    result
  }
}
