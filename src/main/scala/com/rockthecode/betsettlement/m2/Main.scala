package com.rockthecode.betsettlement.m2

/** The edge: the only place that reads files and the only place that prints. */
object Main {

  def main(args: Array[String]): Unit = {
    val dataDir = args.headOption.getOrElse("betsettlement/data/m2")

    val processed = Engine.processAll(
      Parsing.bets(dataDir),
      Parsing.markets(dataDir),
      Parsing.results(dataDir)
    )

    Report.render(processed).foreach(println)
  }
}
