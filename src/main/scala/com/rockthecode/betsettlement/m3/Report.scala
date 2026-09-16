package com.rockthecode.betsettlement.m3

/**
 * Rendering. Builds strings; prints nothing.
 *
 * This is where the two halves of the Either come back together: a settlement
 * and a rejection are different things all the way through the program, and
 * only become neighbouring rows in a CSV at the very last moment.
 */
object Report {

  val header = "betId,customerId,outcome,stake,returned,detail"

  def render(processed: List[(Bet, Either[Rejection, Settlement])]): List[String] =
    header :: processed.map(line)

  private def line(entry: (Bet, Either[Rejection, Settlement])): String = {
    val (bet, result) = entry

    val (outcome, returned, detail) = result match {
      case Right(settlement) => (label(settlement.outcome), settlement.returned, "")
      case Left(rejection)   => ("REJECTED", BigDecimal(0).setScale(2), describe(rejection))
    }

    s"${bet.id},${bet.customerId},$outcome,${money(bet.stake)},${money(returned)},$detail"
  }

  private def label(outcome: Outcome): String = outcome match {
    case Outcome.Won     => "WON"
    case Outcome.Lost    => "LOST"
    case Outcome.Void    => "VOID"
    case Outcome.Pending => "PENDING"
  }

  private def describe(rejection: Rejection): String = rejection match {
    case Rejection.NoLegs                        => "NO_LEGS"
    case Rejection.InvalidStake(stake)           => s"INVALID_STAKE ${money(stake)}"
    case Rejection.InvalidOdds(odds)             => s"INVALID_ODDS ${money(odds)}"
    case Rejection.UnknownMarket(marketId)       => s"UNKNOWN_MARKET $marketId"
    case Rejection.UnknownSelection(market, sel) => s"UNKNOWN_SELECTION $market:$sel"
  }

  private def money(amount: BigDecimal): String = amount.setScale(2).toString
}
