package com.rockthecode.betsettlement.m3

/**
 * Milestone 3 - the model.
 *
 * One line changed: a bet has a LIST of legs rather than one.
 *
 * That is the whole refactor, and it is worth noticing what did not have to
 * change. `LegOutcome` already carried a multiplier and already had a `Void`
 * case worth 1.00, because M2 followed the rules card and modelled legs even
 * when every bet had exactly one. So M3 does not add concepts - it stops
 * assuming there is only one of something.
 *
 * `Rejection.NoLegs` also becomes reachable for the first time. The rule did
 * not change; the model did. In M2 the type made a bet with no legs
 * unrepresentable, which is the best kind of validation there is.
 */

case class Market(id: String, description: String, selections: Set[String])

case class Leg(marketId: String, selection: String, odds: BigDecimal)

case class Bet(id: String, customerId: String, stake: BigDecimal, legs: List[Leg])

/** How a market finished. Absent from the results map means "not yet". */
enum Result {
  case Winner(selection: String)
  case Void
}

/**
 * What one leg came to. The rules card states the rules leg-first, so the model
 * does too - even though in M2 every bet still has exactly one leg.
 *
 * `Won` carries the multiplier and `Void` is worth 1.00 - which is the fact
 * this whole milestone rests on.
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
