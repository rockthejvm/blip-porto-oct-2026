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
    case Some(Right(cmd)) =>
      Decide.decide(bank, cmd) match {
        case Left(rejection) => (bank, List(Wire.rejected(rejection)))
        case Right(events) =>
          // scanLeft gives the state after each event, which is where the
          // "resulting balance" on every adjustment line comes from.
          val states = events.scanLeft(bank)(Evolve.evolve)
          val next = states.last
          val recorded = events.zip(states.tail).zipWithIndex.map { case ((event, after), i) =>
            Wire.ok(bank.nextSeq + i, event, after.accounts(event.account).balance)
          }
          (next, recorded ++ answer(next, cmd))
      }
  }

  /** Queries print something after `decide` has let them through; everything else prints only its facts. */
  private def answer(bank: Bank, cmd: Command): List[String] = cmd match {
    case Command.Balance(acc)              => List(Wire.balance(bank.accounts(acc)))
    case Command.History(acc)              => Wire.history(acc, Projections.facts(bank, acc))
    case Command.Summary(acc, from, to)    => List(Wire.summary(acc, from, to, Projections.summary(bank, acc, from, to)))
    case _                                 => Nil
  }

  // --- milestone 3 -----------------------------------------------------------

  override def journalLines(before: Bank, after: Bank): List[String] =
    after.events.drop(before.events.size).map(Journal.encode).toList

  override def replay(lines: List[String]): Bank =
    lines.map(Journal.decode).foldLeft(Bank.empty)(Evolve.evolve)
}
