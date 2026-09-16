package com.rockthecode.betsettlement.m1

import scala.math.BigDecimal.RoundingMode

/**
 * Milestone 1 - the work.
 *
 * Pure: data in, data out. Nothing here reads a file, prints, throws or
 * mutates, which is why it is the part you can reason about and the part the
 * next four milestones keep rather than rewrite.
 */
object Engine {

  def settleAll(bets: List[Bet], results: Map[String, String]): List[Settlement] =
    bets.sortBy(_.id).map(bet => settle(bet, results))

  def settle(bet: Bet, results: Map[String, String]): Settlement =
    if (results.get(bet.leg.marketId).contains(bet.leg.selection))
      Settlement(bet, Outcome.Won, payout(bet.stake, bet.leg.odds))
    else
      Settlement(bet, Outcome.Lost, BigDecimal(0).setScale(2))

  /**
   * Rounded HALF_UP to 2dp, once, here at the end. Nowhere else in the program
   * rounds anything - see the rules card, section 5.
   */
  private def payout(stake: BigDecimal, multiplier: BigDecimal): BigDecimal =
    (stake * multiplier).setScale(2, RoundingMode.HALF_UP)
}
