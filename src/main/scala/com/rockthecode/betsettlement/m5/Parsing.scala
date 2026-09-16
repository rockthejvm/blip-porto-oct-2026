package com.rockthecode.betsettlement.m5

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
        legs = row("legs").split('|').filter(_.nonEmpty).map(leg).toList
      )
    }

  private def leg(raw: String): Leg =
    raw.split(':') match {
      case Array(marketId, selection, odds) => Leg(marketId, selection, BigDecimal(odds))
      case _                                => throw new IllegalArgumentException(s"malformed leg: $raw")
    }

  /** Results as they arrived. Not sorted, not deduplicated, not filtered. */
  def resultEvents(dataDir: String): List[ResultEvent] =
    Csv.rows(s"$dataDir/result-events.csv").map { row =>
      ResultEvent(
        sequence = row("sequence").toInt,
        marketId = row("marketId"),
        outcome = if (row("outcome") == "VOID") Result.Void else Result.Winner(row("outcome"))
      )
    }
}
