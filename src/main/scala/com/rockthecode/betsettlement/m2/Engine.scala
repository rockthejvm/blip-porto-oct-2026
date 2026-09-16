package com.rockthecode.betsettlement.m2

import scala.math.BigDecimal.RoundingMode

/**
 * Milestone 2 - the work. Still pure.
 *
 * The shape to notice: validation happens FIRST and returns an Either, so
 * everything downstream of it deals only in bets that could actually be
 * settled. Nothing in `settle` has to wonder whether the market exists.
 */
object Engine {

  def processAll(
      bets: List[Bet],
      markets: Map[String, Market],
      results: Map[String, Result]
  ): List[(Bet, Either[Rejection, Settlement])] =
    bets.sortBy(_.id).map(bet => bet -> process(bet, markets, results))

  def process(
      bet: Bet,
      markets: Map[String, Market],
      results: Map[String, Result]
  ): Either[Rejection, Settlement] =
    validate(bet, markets).map(valid => settle(valid, results))

  /**
   * The reasons are checked in the order the rules card lists them, so that a
   * bet that is wrong in two ways always reports the same one.
   */
  def validate(bet: Bet, markets: Map[String, Market]): Either[Rejection, Bet] =
    for {
      _ <- Either.cond(bet.stake > 0, (), Rejection.InvalidStake(bet.stake))
      _ <- Either.cond(bet.leg.odds >= 1, (), Rejection.InvalidOdds(bet.leg.odds))
      market <- markets.get(bet.leg.marketId).toRight(Rejection.UnknownMarket(bet.leg.marketId))
      _ <- Either.cond(
        market.selections.contains(bet.leg.selection),
        (),
        Rejection.UnknownSelection(bet.leg.marketId, bet.leg.selection)
      )
    } yield bet

  // `NoLegs` cannot happen yet - the type says a bet has exactly one leg, which
  // is the type doing the checking for us. It becomes reachable in M3, and that
  // is a fair thing to point at: the rule did not change, the model did.

  def settle(bet: Bet, results: Map[String, Result]): Settlement =
    combine(bet, settleLeg(bet.leg, results))

  def settleLeg(leg: Leg, results: Map[String, Result]): LegOutcome =
    results.get(leg.marketId) match {
      case None                              => LegOutcome.Pending
      case Some(Result.Void)                 => LegOutcome.Void
      case Some(Result.Winner(s)) if s == leg.selection => LegOutcome.Won(leg.odds)
      case Some(Result.Winner(_))            => LegOutcome.Lost
    }

  /**
   * Combining one leg into a bet. Trivial today; M3 keeps this function and
   * changes only what it folds over.
   */
  private def combine(bet: Bet, legOutcome: LegOutcome): Settlement =
    legOutcome match {
      case LegOutcome.Lost       => Settlement(bet, Outcome.Lost, zero)
      case LegOutcome.Pending    => Settlement(bet, Outcome.Pending, zero)
      case LegOutcome.Void       => Settlement(bet, Outcome.Void, bet.stake.setScale(2))
      case LegOutcome.Won(mult)  => Settlement(bet, Outcome.Won, payout(bet.stake, mult))
    }

  private val zero = BigDecimal(0).setScale(2)

  /** Rounded HALF_UP to 2dp, once, at the end. Nowhere else rounds. */
  private def payout(stake: BigDecimal, multiplier: BigDecimal): BigDecimal =
    (stake * multiplier).setScale(2, RoundingMode.HALF_UP)
}
