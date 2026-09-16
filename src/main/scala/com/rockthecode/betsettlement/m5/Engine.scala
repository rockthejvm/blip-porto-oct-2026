package com.rockthecode.betsettlement.m5

import scala.math.BigDecimal.RoundingMode

/**
 * Milestone 4 - the work. Still pure, and still the same shape.
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

  /**
   * Replay the events in sequence order, building up what we know about each
   * market.
   *
   * Two things the stream does that a finished file never did:
   *
   *   - the same result can arrive twice. `updated` with the same value leaves
   *     the map exactly as it was, so applying an event twice is the same as
   *     applying it once. That is idempotence, and here it is free - but only
   *     because the state is a Map keyed by market rather than a list of things
   *     that happened.
   *
   *   - a result can name a market we do not have. We keep it to one side and
   *     carry on; one bad event must not stop the run.
   *
   * This is A.2's `applyAll` and SG3's `foldSequentially`, with a bigger state
   * and a log alongside it.
   */
  def applyEvents(
      events: List[ResultEvent],
      markets: Map[String, Market]
  ): (Map[String, Result], List[ResultEvent]) =
    events.sortBy(_.sequence).foldLeft((Map.empty[String, Result], List.empty[ResultEvent])) {
      case ((known, skipped), event) =>
        if (markets.contains(event.marketId)) (known.updated(event.marketId, event.outcome), skipped)
        else (known, event :: skipped)
    } match {
      case (known, skipped) => (known, skipped.reverse)
    }

  /**
   * One row per customer, from the bets that were actually settled.
   *
   * Rejected bets are dropped by the `collect` - they never existed as far as
   * the ledger is concerned, which is why they contribute to neither the count
   * nor the stake. A pending bet is kept: its stake is committed even though
   * nothing has come back yet.
   */
  def ledger(processed: List[(Bet, Either[Rejection, Settlement])]): List[LedgerEntry] =
    processed
      .collect { case (bet, Right(settlement)) => (bet, settlement) }
      .groupBy((bet, _) => bet.customerId)
      .toList
      .sortBy((customerId, _) => customerId)
      .map { (customerId, entries) =>
        LedgerEntry(
          customerId = customerId,
          betCount = entries.size,
          staked = entries.map((bet, _) => bet.stake).sum,
          returned = entries.map((_, settlement) => settlement.returned).sum
        )
      }

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
