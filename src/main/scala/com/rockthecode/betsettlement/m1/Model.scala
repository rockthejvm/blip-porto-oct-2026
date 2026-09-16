package com.rockthecode.betsettlement.m1

/**
 * Milestone 1 - the model.
 *
 * Deliberately the smallest thing that describes the M1 rules and nothing more.
 * A bet has ONE leg, every market has a result, and a bet either won or lost.
 * Later milestones grow this; resist growing it early, because a model that
 * anticipates rules you have not been given is a model nobody can check.
 */

/** One (market, selection, odds) triple. In M1 a bet has exactly one. */
case class Leg(marketId: String, selection: String, odds: BigDecimal)

case class Bet(id: String, customerId: String, stake: BigDecimal, leg: Leg)

/** M1 knows two ways a bet can end. M2 finds three more. */
enum Outcome {
  case Won, Lost
}

case class Settlement(bet: Bet, outcome: Outcome, returned: BigDecimal)
