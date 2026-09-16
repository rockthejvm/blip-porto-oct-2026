package com.rockthecode.ledger.solution.tools

import com.rockthecode.ledger.solution.ReferenceLedger

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters._

/**
 * Trainer tool: run a scenario through the reference and print the output.
 *
 *     sbt "runMain com.rockthecode.ledger.solution.tools.Run some-scenario.in"
 *     sbt "runMain com.rockthecode.ledger.solution.tools.Run --interleave some-scenario.in"
 *
 * `--interleave` prints each input line next to the output it produced - the
 * format used when hand-reviewing a scenario against the protocol.
 */
object Run {

  def main(args: Array[String]): Unit = {
    val interleave = args.contains("--interleave")
    args.filterNot(_ == "--interleave").foreach { file =>
      val lines = Files.readAllLines(Path.of(file), UTF_8).asScala.toList
      if (interleave)
        lines.foldLeft(ReferenceLedger.empty) { (state, line) =>
          val (next, out) = ReferenceLedger.execute(state, line)
          out match {
            case Nil => println(line)
            case first :: rest =>
              println(f"$line%-44s | $first")
              rest.foreach(r => println(f"${""}%-44s | $r"))
          }
          next
        }
      else
        lines.foldLeft(ReferenceLedger.empty) { (state, line) =>
          val (next, out) = ReferenceLedger.execute(state, line)
          out.foreach(println)
          next
        }
    }
  }
}
