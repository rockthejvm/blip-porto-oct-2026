package com.rockthecode.betsettlement.m2

/**
 * Milestone 2 - the model.
 *
 * Three things arrived that M1 pretended did not exist: markets can be voided,
 * markets can have no result yet, and some bets should never have been taken.
 *
 * The third one is different in kind from the other two, and the model says so.
 * A voided or pending bet WAS settled - we looked at it and the answer is
 * "stake back" or "not yet". A rejected bet was never settled at all. That is
 * why rejection is not another `Outcome`: it is the left-hand side.
 */

case class Market(id: String, description: String, selections: Set[String])

case class Leg(marketId: String, selection: String, odds: BigDecimal)

case class Bet(id: String, customerId: String, stake: BigDecimal, leg: Leg)

/** How a market finished. Absent from the results map means "not yet". */
enum Result {
  case Winner(selection: String)
  case Void
}

/**
 * What one leg came to. The rules card states the rules leg-first, so the model
 * does too - even though in M2 every bet still has exactly one leg.
 *
 * `Won` carries the multiplier and `Void` implies 1.00, which is the fact M3
 * is built on.
 */
enum LegOutcome {
  case Won(multiplier: BigDecimal)
  case Lost
  case Void
  case Pending
}

enum Outcome {
  case Won, Lost, Void, Pending
}

case class Settlement(bet: Bet, outcome: Outcome, returned: BigDecimal)

/** Why a bet was never settled. A reason is data, not a string. */
enum Rejection {
  case NoLegs
  case InvalidStake(stake: BigDecimal)
  case InvalidOdds(odds: BigDecimal)
  case UnknownMarket(marketId: String)
  case UnknownSelection(marketId: String, selection: String)
}
