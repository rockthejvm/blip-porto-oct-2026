package com.rockthecode.betsettlement.m1

/**
 * Turning rows into the model. Also given to you.
 *
 * Note that `bets.csv` already carries a pipe-separated `legs` column, because
 * the file format is the same for every milestone. In M1 every bet has exactly
 * one leg, so we take the first and ignore the rest - which is a promise the
 * data keeps and the type does not. M3 fixes that.
 */
object Parsing {

  def bets(dataDir: String): List[Bet] =
    Csv.rows(s"$dataDir/bets.csv").map { row =>
      Bet(
        id = row("betId"),
        customerId = row("customerId"),
        stake = BigDecimal(row("stake")),
        leg = leg(row("legs").split('|').head)
      )
    }

  private def leg(raw: String): Leg =
    raw.split(':') match {
      case Array(marketId, selection, odds) => Leg(marketId, selection, BigDecimal(odds))
      case other => throw new IllegalArgumentException(s"malformed leg: $raw")
    }

  /** marketId -> the selection that won. In M1 every market has one. */
  def results(dataDir: String): Map[String, String] =
    Csv.rows(s"$dataDir/results.csv").map(row => row("marketId") -> row("outcome")).toMap
}
