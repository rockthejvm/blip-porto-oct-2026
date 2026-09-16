package com.rockthecode.betsettlement.m3

import scala.math.BigDecimal.RoundingMode

/**
 * Milestone 3 - the work. Still pure, and still the same shape.
 *
 * Every function below is M2's function with "the leg" replaced by "the legs".
 * `settleLeg` is untouched. `payout` is untouched. Only `validate` and
 * `combine` had anything to say about how many legs there were, and only they
 * changed.
 *
 * If you found yourself writing a separate path for single bets, this is the
 * milestone to delete it: a single is an accumulator with one leg, and the fold
 * over one element does exactly the right thing on its own.
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
      _ <- Either.cond(bet.legs.nonEmpty, (), Rejection.NoLegs)
      _ <- Either.cond(bet.stake > 0, (), Rejection.InvalidStake(bet.stake))
      _ <- firstProblem(bet.legs.find(_.odds < 1))(leg => Rejection.InvalidOdds(leg.odds))
      _ <- firstProblem(bet.legs.find(leg => !markets.contains(leg.marketId)))(leg =>
        Rejection.UnknownMarket(leg.marketId)
      )
      _ <- firstProblem(bet.legs.find(leg => !markets(leg.marketId).selections.contains(leg.selection)))(leg =>
        Rejection.UnknownSelection(leg.marketId, leg.selection)
      )
    } yield bet

  /**
   * "If you found an offending leg, that is a Left; otherwise carry on."
   *
   * `Option.toLeft` is the combinator nobody finds on their own: `Some(x)`
   * becomes `Left(x)` and `None` becomes `Right(())`, which is exactly the
   * shape a for-comprehension over Either wants.
   */
  private def firstProblem(offender: Option[Leg])(reason: Leg => Rejection): Either[Rejection, Unit] =
    offender.map(reason).toLeft(())

  // Note the last check reaches into `markets(...)` without an Option, which is
  // safe only because the check above it has already run. That is the
  // for-comprehension buying us something real: order is part of the meaning.

  def settle(bet: Bet, results: Map[String, Result]): Settlement =
    combine(bet, bet.legs.map(leg => settleLeg(leg, results)))

  def settleLeg(leg: Leg, results: Map[String, Result]): LegOutcome =
    results.get(leg.marketId) match {
      case None                              => LegOutcome.Pending
      case Some(Result.Void)                 => LegOutcome.Void
      case Some(Result.Winner(s)) if s == leg.selection => LegOutcome.Won(leg.odds)
      case Some(Result.Winner(_))            => LegOutcome.Lost
    }

  /**
   * The bet-level rules, in the order the card gives them. First match wins,
   * and the order is load-bearing: a bet with one winning leg and one pending
   * leg is PENDING, not WON.
   */
  private def combine(bet: Bet, outcomes: List[LegOutcome]): Settlement =
    if (outcomes.contains(LegOutcome.Lost)) Settlement(bet, Outcome.Lost, zero)
    else if (outcomes.contains(LegOutcome.Pending)) Settlement(bet, Outcome.Pending, zero)
    else if (outcomes.forall(_ == LegOutcome.Void)) Settlement(bet, Outcome.Void, bet.stake.setScale(2))
    else Settlement(bet, Outcome.Won, payout(bet.stake, multiplier(outcomes)))

  /**
   * The product of every leg's multiplier.
   *
   * A voided leg contributes 1.00, so it drops out of the product entirely -
   * which is why a four-leg accumulator with one voided leg settles exactly
   * like the three-leg accumulator it has become. There is no special case
   * here because 1.00 is the multiplicative identity, and that is the whole
   * trick of this milestone.
   */
  private def multiplier(outcomes: List[LegOutcome]): BigDecimal =
    outcomes.foldLeft(BigDecimal(1)) {
      case (running, LegOutcome.Won(legMultiplier)) => running * legMultiplier
      case (running, LegOutcome.Void)               => running * 1
      case (running, _)                             => running
    }

  private val zero = BigDecimal(0).setScale(2)

  /** Rounded HALF_UP to 2dp, once, at the end. Nowhere else rounds. */
  private def payout(stake: BigDecimal, multiplier: BigDecimal): BigDecimal =
    (stake * multiplier).setScale(2, RoundingMode.HALF_UP)
}
