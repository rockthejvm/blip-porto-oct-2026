package com.rockthecode.ledger

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters._

/**
 * The ledger tests - one test per milestone.
 *
 *     sbt ledger        <- attendees (runs MyLedgerSuite)
 *
 * Each test runs every scenario in `src/main/resources/ledger/mN/` through the
 * engine and compares the output with the expected `.out` file next to it.
 * Milestones 3 and 4 additionally check that a journal written by scenario A,
 * then replayed into a fresh engine before running scenario B, gives the same
 * output for B as running A and B in one go.
 *
 * Tests for milestones you have not reached yet are simply red.
 */
abstract class LedgerSuite[S](engine: LedgerEngine[S], root: Path) extends munit.FunSuite {

  test("m1") { milestone("m1") }
  test("m2") { milestone("m2") }
  test("m3") { milestone("m3") }
  test("m4") { milestone("m4") }
  test("m5") { milestone("m5") }

  // ---------------------------------------------------------------------------

  private def milestone(m: String): Unit = {
    val dir = root.resolve(m)
    val files = if (Files.isDirectory(dir)) Files.list(dir).iterator.asScala.toList.sortBy(_.getFileName.toString) else Nil
    val inputs = files.filter(_.toString.endsWith(".in"))
    if (inputs.isEmpty) fail(s"no scenarios found in $dir")

    val goldens = inputs.filterNot(isReplayPart).flatMap(golden)
    val replays = inputs.filter(_.getFileName.toString.endsWith("-a.in")).flatMap(replay)
    val failures = goldens ++ replays
    if (failures.nonEmpty) fail(s"${failures.size} scenario(s) failed in $m\n\n" + failures.mkString("\n\n"))
  }

  private def isReplayPart(in: Path): Boolean = {
    val name = in.getFileName.toString
    name.contains("replay") && (name.endsWith("-a.in") || name.endsWith("-b.in"))
  }

  /** Plain golden check: run `X.in`, compare with `X.out`. None if they match or `X.out` is absent. */
  private def golden(in: Path): Option[String] = {
    val out = sibling(in, ".in", ".out")
    if (!Files.exists(out)) None
    else compare(display(in), read(out), Harness.run(engine, read(in)))
  }

  /**
   * Replay check for `replay*-a.in` + `replay*-b.in` + `replay*.out`:
   *  1. A then B in one run must match the `.out` file;
   *  2. A alone, journal written and read back, B from the replayed state must
   *     match the B part of the `.out` file.
   */
  private def replay(a: Path): List[String] = {
    val b = sibling(a, "-a.in", "-b.in")
    val out = sibling(a, "-a.in", ".out")
    if (!Files.exists(b) || !Files.exists(out)) Nil
    else {
      val expected = read(out)
      val linesA = read(a)
      val linesB = read(b)

      val onePass = Harness.run(engine, linesA ++ linesB)
      val single = compare(s"${display(a)} + ${display(b)} in one run", expected, onePass)

      val (_, outputA, journal) = Harness.runJournaled(engine, engine.empty, linesA)
      val journalFile = Files.createTempFile("ledger-", ".journal")
      val restarted =
        try {
          Files.write(journalFile, journal.asJava, UTF_8)
          engine.replay(Files.readAllLines(journalFile, UTF_8).asScala.toList)
        } finally Files.deleteIfExists(journalFile)
      val (_, outputB, _) = Harness.runJournaled(engine, restarted, linesB)
      val afterRestart = compare(s"${display(b)} after replaying the journal of ${display(a)}", expected.drop(outputA.size), outputB)

      single.toList ++ afterRestart.toList
    }
  }

  // ---------------------------------------------------------------------------

  private def compare(name: String, expected: List[String], actual: List[String]): Option[String] = {
    val e = trim(expected)
    val a = trim(actual)
    val diffs = (0 until math.max(e.size, a.size)).filter(i => e.lift(i) != a.lift(i))
    if (diffs.isEmpty) None
    else {
      val shown = diffs.take(5).map { i =>
        s"  line ${i + 1}:\n    expected: ${e.lift(i).getOrElse("<end of file>")}\n    actual:   ${a.lift(i).getOrElse("<end of file>")}"
      }
      val more = if (diffs.size > 5) s"\n  ... and ${diffs.size - 5} more differing line(s)" else ""
      val sizes = if (e.size != a.size) s" (expected ${e.size} lines, got ${a.size})" else ""
      Some(s"$name: first difference at line ${diffs.head + 1}$sizes\n" + shown.mkString("\n") + more)
    }
  }

  private def trim(lines: List[String]): List[String] =
    lines.map(_.replaceAll("\\s+$", "")).reverse.dropWhile(_.isEmpty).reverse

  private def read(p: Path): List[String] = Files.readAllLines(p, UTF_8).asScala.toList

  private def sibling(p: Path, from: String, to: String): Path =
    p.resolveSibling(p.getFileName.toString.stripSuffix(from) + to)

  private def display(p: Path): String = root.relativize(p).toString
}

class MyLedgerSuite extends LedgerSuite(MyLedger, Path.of("src/main/resources/ledger"))
