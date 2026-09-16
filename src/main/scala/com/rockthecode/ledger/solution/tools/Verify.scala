package com.rockthecode.ledger.solution.tools

import com.rockthecode.ledger.Harness
import com.rockthecode.ledger.solution.ReferenceLedger

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters._

/**
 * Trainer tool: check every scenario file for correctness. One line per file,
 * OK or NOT OK with the first difference. Exit code 1 if anything is NOT OK.
 *
 *     sbt ledgerVerify
 *
 * Checks, per milestone directory:
 *   - every `.in` has its `.out` (replay pairs have their combined one);
 *   - every `.out` is exactly what the reference prints for its `.in`;
 *   - every huge `.in` is exactly what `GenHuge` produces for its seed;
 *   - in every `.out`: `OK` sequence numbers are 1, 2, 3, ... with nothing
 *     else consuming one, and every `SUMMARY` satisfies
 *     closing == opening + credits - debits with all amounts well-formed.
 */
object Verify {

  def main(args: Array[String]): Unit = {
    val roots = (if (args.nonEmpty) args.toList else List("src/main/resources/ledger")).map(Path.of(_))
    val results = roots.flatMap(root => milestones(root).flatMap(dir => checkDir(root, dir))) ++ roots.flatMap(checkHuge)
    results.foreach { case (name, problem) =>
      println(problem.fold(f"OK      $name")(p => f"NOT OK  $name: $p"))
    }
    val bad = results.count(_._2.nonEmpty)
    println()
    println(if (bad == 0) s"${results.size} checks, all OK" else s"$bad of ${results.size} checks NOT OK")
    if (bad > 0) sys.exit(1)
  }

  private def milestones(root: Path): List[Path] =
    if (!Files.isDirectory(root)) Nil
    else Files.list(root).iterator.asScala.filter(Files.isDirectory(_)).toList.sortBy(_.getFileName.toString)

  /** Golden and invariant checks for one milestone directory. Returns (display name, problem?). */
  private def checkDir(root: Path, dir: Path): List[(String, Option[String])] = {
    val files = Files.list(dir).iterator.asScala.toList.sortBy(_.getFileName.toString)
    val ins = files.filter(_.getFileName.toString.endsWith(".in"))
    val (replayParts, plain) = ins.partition(p => isReplay(p) && (name(p).endsWith("-a.in") || name(p).endsWith("-b.in")))

    val goldens = plain.map { in =>
      val out = sibling(in, ".in", ".out")
      display(root, out) -> {
        if (!Files.exists(out)) Some("missing")
        else firstDiff(read(out), Harness.run(ReferenceLedger, read(in)), "committed", "the reference prints")
      }
    }
    val replays = replayParts.filter(p => name(p).endsWith("-a.in")).map { a =>
      val b = sibling(a, "-a.in", "-b.in")
      val out = sibling(a, "-a.in", ".out")
      display(root, out) -> {
        if (!Files.exists(b)) Some(s"missing ${name(b)}")
        else if (!Files.exists(out)) Some("missing")
        else firstDiff(read(out), Harness.run(ReferenceLedger, read(a) ++ read(b)), "committed", "the reference prints")
      }
    }
    val invariants = files.filter(_.getFileName.toString.endsWith(".out")).map { out =>
      s"${display(root, out)} invariants" -> checkInvariants(read(out))
    }
    goldens ++ replays ++ invariants
  }

  /** Every huge `.in` under this root must be exactly what the generator produces. */
  private def checkHuge(root: Path): List[(String, Option[String])] =
    for {
      job <- GenHuge.jobs
      (path, lines) <- GenHuge.render(job, root, quiet = true)
      if Files.exists(path)
    } yield display(root, path) -> firstDiff(read(path), lines, "committed", "the generator produces")

  // ---------------------------------------------------------------------------

  private def firstDiff(committed: List[String], produced: List[String], leftName: String, rightName: String): Option[String] = {
    val i = committed.zipAll(produced, null, null).indexWhere { case (a, b) => a != b }
    def show(s: String) = Option(s).map(x => s"'$x'").getOrElse("<end of file>")
    if (i < 0) None
    else Some(s"line ${i + 1}: $leftName ${show(committed.lift(i).orNull)} but $rightName ${show(produced.lift(i).orNull)}")
  }

  private val WireAmount = """-?\d+\.\d{2}""".r

  private def cents(rendered: String): Option[Long] = rendered match {
    case WireAmount() =>
      val negative = rendered.startsWith("-")
      val digits = rendered.stripPrefix("-").replace(".", "").toLong
      Some(if (negative) -digits else digits)
    case _ => None
  }

  /** Sequence density and SUMMARY arithmetic over one `.out` file. First offence wins. */
  private def checkInvariants(lines: List[String]): Option[String] = {
    var expectedSeq = 1
    lines.zipWithIndex.flatMap { case (line, idx) =>
      val at = s"line ${idx + 1}"
      line.split(" ").toList match {
        case "OK" :: seq :: _ =>
          val problem =
            if (seq.toIntOption.contains(expectedSeq)) None
            else Some(s"$at: sequence number $seq, expected $expectedSeq")
          expectedSeq += 1
          problem
        case "SUMMARY" :: _ :: _ :: _ :: pairs if pairs.nonEmpty =>
          val kv = pairs.flatMap(_.split("=") match { case Array(k, v) => Some(k -> v); case _ => None }).toMap
          List("opening", "credits", "debits", "closing").flatMap(k => if (cents(kv.getOrElse(k, "")).isEmpty) Some(s"$at: bad $k amount") else None).headOption
            .orElse {
              val ok = for {
                o <- cents(kv("opening")); c <- cents(kv("credits")); d <- cents(kv("debits")); cl <- cents(kv("closing"))
              } yield cl == o + c - d
              if (ok.contains(true)) None else Some(s"$at: closing != opening + credits - debits")
            }
        case _ => None
      }
    }.headOption
  }

  private def isReplay(p: Path): Boolean = name(p).contains("replay")
  private def name(p: Path): String = p.getFileName.toString
  private def sibling(p: Path, from: String, to: String): Path = p.resolveSibling(name(p).stripSuffix(from) + to)
  private def read(p: Path): List[String] = Files.readAllLines(p, UTF_8).asScala.toList
  private def display(root: Path, p: Path): String = s"${root.getFileName}/${root.relativize(p)}"
}
