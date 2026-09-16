package com.rockthecode.betsettlement.m1

/**
 * Milestone 1 - rendering.
 *
 * Builds strings. Does not print them: that happens once, in Main, at the edge
 * of the program. Which is why this is testable and a println in a loop is not.
 */
object Report {

  val header = "betId,customerId,outcome,stake,returned,detail"

  def render(settlements: List[Settlement]): List[String] =
    header :: settlements.map(line)

  private def line(settlement: Settlement): String = {
    val bet = settlement.bet
    val outcome = settlement.outcome match {
      case Outcome.Won  => "WON"
      case Outcome.Lost => "LOST"
    }

    s"${bet.id},${bet.customerId},$outcome,${money(bet.stake)},${money(settlement.returned)},"
  }

  private def money(amount: BigDecimal): String = amount.setScale(2).toString
}
