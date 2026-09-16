package com.rockthecode.betsettlement.m5

/**
 * Everything the server knows, as one immutable value.
 *
 * This is the piece that makes M5 tractable. The state is not "a mutable map of
 * results plus a mutable ledger plus a lock"; it is one value, and moving
 * forward means producing a new one. Concurrency then reduces to a single
 * question - how do we swap one value for the next - rather than a question
 * about every field separately.
 */
case class EngineState(
    markets: Map[String, Market],
    bets: List[Bet],
    results: Map[String, Result]
) {

  /** A new state, one result later. Applying the same result twice changes nothing. */
  def withResult(marketId: String, outcome: Result): EngineState =
    if (markets.contains(marketId)) copy(results = results.updated(marketId, outcome))
    else this

  def processed: List[(Bet, Either[Rejection, Settlement])] =
    Engine.processAll(bets, markets, results)

  def ledger: List[LedgerEntry] = Engine.ledger(processed)

  def ledgerFor(customerId: String): Option[LedgerEntry] =
    ledger.find(_.customerId == customerId)

  def pendingCount: Int =
    processed.count {
      case (_, Right(settlement)) => settlement.outcome == Outcome.Pending
      case _                      => false
    }
}

object EngineState {

  def load(dataDir: String): EngineState = {
    val markets = Parsing.markets(dataDir)
    val (results, _) = Engine.applyEvents(Parsing.resultEvents(dataDir), markets)
    EngineState(markets, Parsing.bets(dataDir), results)
  }
}
