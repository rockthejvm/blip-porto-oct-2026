package com.rockthecode.betsettlement.m2

/** Rows into the model. Given to you. */
object Parsing {

  def markets(dataDir: String): Map[String, Market] =
    Csv
      .rows(s"$dataDir/../markets.csv")
      .map { row =>
        val market = Market(row("marketId"), row("description"), row("selections").split('|').toSet)
        market.id -> market
      }
      .toMap

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
      case _                                => throw new IllegalArgumentException(s"malformed leg: $raw")
    }

  /** A market missing from this map simply has no result yet. */
  def results(dataDir: String): Map[String, Result] =
    Csv
      .rows(s"$dataDir/results.csv")
      .map { row =>
        val result = if (row("outcome") == "VOID") Result.Void else Result.Winner(row("outcome"))
        row("marketId") -> result
      }
      .toMap
}
