package com.rockthecode.betsettlement.engine

/**
 * The bet settlement engine. This is where your team works.
 *
 * It compiles and runs right now, and prints a report with no rows in it - so
 * you can see the shape of the day before you have written anything:
 *
 *     ./betsettlement/check m1
 *
 * ...will show you a diff between what you print and what M1 wants. Making that
 * diff empty is milestone 1. Then point it at m2, and so on.
 *
 * To run it directly and look at the output yourself:
 *
 *     sbt "runMain com.rockthecode.betsettlement.engine.Main betsettlement/data/m1"
 *
 * ---------------------------------------------------------------------------
 *
 * Suggested files, so that three people are rarely in the same one:
 *
 *     Model.scala        your types - do M0 on paper before you open this
 *     Parsing.scala      Csv rows -> your model
 *     Engine.scala       validation and settlement. Pure: data in, data out.
 *     Report.scala       your model -> the lines of the report. Builds strings,
 *                        prints nothing.
 *     Main.scala         this file. Reads files, prints lines. The only place
 *                        allowed to do either.
 *
 * That split is a suggestion about merge conflicts as much as about design.
 */
object Main {

  def main(args: Array[String]): Unit = {
    val dataDir = args.headOption.getOrElse("betsettlement/data/m1")

    // Everything you need is in `dataDir`:
    //
    //   Csv.rows(s"$dataDir/bets.csv")           betId, customerId, stake, legs
    //   Csv.rows(s"$dataDir/results.csv")        marketId, outcome
    //   Csv.rows(s"$dataDir/../markets.csv")     marketId, description, selections
    //
    //   Csv.legs("M001:HOME:2.50|M002:AWAY:1.80")

    val report: List[String] = List(
      "betId,customerId,outcome,stake,returned,detail"
      // TODO M1: one line per bet, sorted by betId.
    )

    report.foreach(println)
  }
}
