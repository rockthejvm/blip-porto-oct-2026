package com.rockthecode.ledger.solution.tools

import com.rockthecode.ledger.solution.ReferenceLedger

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters._

/**
 * Trainer tool: (re)generate every expected-output file from the reference.
 *
 *     sbt "runMain com.rockthecode.ledger.solution.tools.GenGolden src/main/resources/ledger"   (or: sbt ledgerGen)
 *
 * Walks `<root>/mN/`. For every `X.in` writes `X.out`; for every `Y-a.in` with
 * a sibling `Y-b.in` writes `Y.out` from A followed by B in one run.
 * `Verify` is the read-only counterpart that checks instead of writing.
 */
object GenGolden {

  def main(args: Array[String]): Unit = {
    val roots = args.map(Path.of(_)).toList
    roots.flatMap(root => milestones(root).flatMap(jobsIn)).foreach { case (out, input) =>
      val expected = Harness.run(ReferenceLedger, input)
      val current = if (Files.exists(out)) Some(Files.readAllLines(out, UTF_8).asScala.toList) else None
      if (current.contains(expected)) println(s"  unchanged  $out")
      else { Files.write(out, expected.asJava, UTF_8); println(s"  written    $out (${expected.size} lines)") }
    }
  }

  private def milestones(root: Path): List[Path] =
    Files.list(root).iterator.asScala.filter(Files.isDirectory(_)).toList.sortBy(_.toString)

  /** (output file, input lines) for every golden in one milestone directory. */
  private def jobsIn(dir: Path): List[(Path, List[String])] = {
    val ins = Files.list(dir).iterator.asScala.filter(_.toString.endsWith(".in")).toList.sortBy(_.toString)
    val plain = ins.filterNot(p => isPart(p, "-a.in") || isPart(p, "-b.in")).map { in =>
      (sibling(in, ".in", ".out"), read(in))
    }
    val pairs = ins.filter(isPart(_, "-a.in")).flatMap { a =>
      val b = sibling(a, "-a.in", "-b.in")
      if (Files.exists(b)) Some((sibling(a, "-a.in", ".out"), read(a) ++ read(b))) else None
    }
    plain ++ pairs
  }

  private def isPart(p: Path, suffix: String): Boolean = {
    val n = p.getFileName.toString
    n.contains("replay") && n.endsWith(suffix)
  }
  private def sibling(p: Path, from: String, to: String): Path =
    p.resolveSibling(p.getFileName.toString.stripSuffix(from) + to)
  private def read(p: Path): List[String] = Files.readAllLines(p, UTF_8).asScala.toList
}
