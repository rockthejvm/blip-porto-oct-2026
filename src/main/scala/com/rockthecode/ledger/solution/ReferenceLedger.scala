package com.rockthecode.ledger.solution

import com.rockthecode.ledger.LedgerEngine

/**
 * The reference ledger: the pieces wired together.
 *
 *     line -> Parser -> Command -> Decide -> events -> fold(Evolve) -> Bank
 *                                                   \-> Wire -> output
 *
 * This object is the only one that knows all the parts exist. Each part knows
 * only its neighbours' types.
 */
object ReferenceLedger extends LedgerEngine[Bank] {

  def empty: Bank = Bank.empty

  def execute(bank: Bank, line: String): (Bank, List[String]) = Parser.parse(line) match {
    case None              => (bank, Nil)
    case Some(Left(error)) => (bank, List(Wire.error(error)))

    // A query cannot record a fact: `decide` never sees it, so no event can
    // exist and no sequence number can move. The guarantee is carried by the
    // Mutation/Query split, not by discipline. The one check a query needs -
    // unknown account, closed account - is the head of the same precedence
    // chain the mutations use.
    case Some(Right(q: Query)) =>
      Decide.queryable(bank, q.account) match {
        case Left(rejection) => (bank, List(Wire.rejected(rejection)))
        case Right(account)  => (bank, answer(bank, q, account))
      }

    case Some(Right(m: Mutation)) =>
      Decide.decide(bank, m) match {
        case Left(rejection) => (bank, List(Wire.rejected(rejection)))
        case Right(events) =>
          // scanLeft gives the state after each event, which is where the
          // "resulting balance" on every adjustment line comes from.
          val states = events.scanLeft(bank)(Evolve.evolve)
          val next = states.last
          val recorded = events.zip(states.tail).zipWithIndex.map { case ((event, after), i) =>
            Wire.ok(bank.nextSeq + i, event, balanceOf(after, event.account))
          }
          (next, recorded)
      }
  }

  /** The `account` here is `Account.Active`: `queryable` already proved it, so `.balance` exists. */
  private def answer(bank: Bank, q: Query, account: Account.Active): List[String] = q match {
    case Query.Balance(_)             => List(Wire.balance(account))
    case Query.History(acc)           => Wire.history(acc, Projections.facts(bank, acc))
    case Query.Summary(acc, from, to) => List(Wire.summary(acc, from, to, Projections.summary(bank, acc, from, to)))
  }

  /** For the OK line after applying one event. A just-closed account reports the 0.00 it was closed with. */
  private def balanceOf(bank: Bank, id: String): Money = bank.accounts(id) match {
    case a: Account.Active => a.balance
    case _: Account.Closed => Money.zero
  }

  // --- milestone 3 -----------------------------------------------------------

  override def journalLines(before: Bank, after: Bank): List[String] =
    after.events.drop(before.events.size).map(Journal.encode).toList

  override def replay(lines: List[String]): Bank =
    lines.map(Journal.decode).foldLeft(Bank.empty)(Evolve.evolve)
}
