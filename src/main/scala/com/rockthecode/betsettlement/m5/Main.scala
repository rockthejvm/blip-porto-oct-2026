package com.rockthecode.betsettlement.m5

/**
 * The edge. Reads the files, replays the events, settles, prints the ledger.
 *
 * Note where the two kinds of output go: the ledger to standard output, so it
 * can be diffed against the expected file, and the events we could not use to
 * standard error, so that they are visible without corrupting the report.
 * Mixing the two into one stream is how a report becomes unusable by anything
 * downstream of it.
 *
 *   sbt "runMain com.rockthecode.betsettlement.m4.Main betsettlement/data/m4"
 */
object Main {

  def main(args: Array[String]): Unit = {
    val dataDir = args.headOption.getOrElse("betsettlement/data/m4")

    val markets = Parsing.markets(dataDir)
    val bets = Parsing.bets(dataDir)

    val (results, skipped) = Engine.applyEvents(Parsing.resultEvents(dataDir), markets)

    skipped.foreach(event =>
      System.err.println(s"skipped event ${event.sequence}: no such market ${event.marketId}")
    )

    val processed = Engine.processAll(bets, markets, results)

    Report.render(Engine.ledger(processed)).foreach(println)
  }
}
