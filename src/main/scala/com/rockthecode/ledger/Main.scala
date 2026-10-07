package com.rockthecode.ledger

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{Files, Path, StandardOpenOption}
import scala.jdk.CollectionConverters._

/**
 * The ledger as a command-line program.
 *
 *     sbt "runMain com.rockthecode.ledger.Main src/main/resources/ledger/m1/easy-1.in"
 *     sbt "runMain com.rockthecode.ledger.Main my-scenario.txt my.journal"
 *
 * Reads a scenario file, prints one response per command. With a second
 * argument, the ledger is first rebuilt from that journal file (if it exists)
 * and everything recorded during this run is appended to it - so running two
 * scenarios back to back against the same journal behaves like one long run.
 *
 * This is the only file in the program that reads or writes anything.
 */
object Main {

  /** The engine this program runs. Point it at your own object. */
  val ledger = MyLedger

  def main(args: Array[String]): Unit = args.toList match {
    case scenario :: Nil           => run(ledger, Path.of(scenario), None)
    case scenario :: journal :: Nil => run(ledger, Path.of(scenario), Some(Path.of(journal)))
    case _ =>
      System.err.println("usage: Main <scenario-file> [journal-file]")
      sys.exit(2)
  }

  private def run[S](engine: LedgerEngine[S], scenario: Path, journal: Option[Path]): Unit = {
    val input = Files.readAllLines(scenario, UTF_8).asScala.toList

    val start = journal match {
      case Some(path) if Files.exists(path) => engine.replay(Files.readAllLines(path, UTF_8).asScala.toList)
      case _                                => engine.empty
    }

    val (_, output, journalLines) = runJournaled(engine, start, input)

    journal.foreach { path =>
      Files.write(path, journalLines.asJava, UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
    }
    output.foreach(println)
  }

  private def runJournaled[S](engine: LedgerEngine[S], start: S, input: List[String]): (S, List[String], List[String]) = {
    val (state, out, journal) =
      input.foldLeft((start, Vector.empty[String], Vector.empty[String])) { case ((state, out, journal), line) =>
        val (next, responses) = engine.execute(state, line)
        (next, out ++ responses, journal ++ engine.journalLines(state, next))
      }
    (state, out.toList, journal.toList)
  }
}
