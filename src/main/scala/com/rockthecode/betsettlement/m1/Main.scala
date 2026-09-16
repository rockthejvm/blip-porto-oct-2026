package com.rockthecode.betsettlement.m1

/**
 * The edge of the program: the only place that touches the filesystem and the
 * only place that prints.
 *
 *   sbt "runMain com.rockthecode.betsettlement.m1.Main betsettlement/data/m1"
 */
object Main {

  def main(args: Array[String]): Unit = {
    val dataDir = args.headOption.getOrElse("betsettlement/data/m1")

    val settlements = Engine.settleAll(Parsing.bets(dataDir), Parsing.results(dataDir))

    Report.render(settlements).foreach(println)
  }
}
