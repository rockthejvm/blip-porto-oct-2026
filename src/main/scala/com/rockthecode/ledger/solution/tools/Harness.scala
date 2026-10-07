package com.rockthecode.ledger.solution.tools

import com.rockthecode.ledger.LedgerEngine
/**
 * Runs input lines through an engine, threading the state.
 *
 * This is the whole application. Note what is NOT here: no vars, no mutation,
 * no I/O. The state of the system is a fold over its inputs.
 */
object Harness {

  /** Run every input line from the empty state; return the output lines. */
  def run[S](engine: LedgerEngine[S], input: List[String]): List[String] =
    input
      .foldLeft((engine.empty, Vector.empty[String])) { case ((state, out), line) =>
        val (next, responses) = engine.execute(state, line)
        (next, out ++ responses)
      }
      ._2
      .toList

  /**
   * The same fold, but starting from a chosen state and also collecting what
   * the engine wants written to its journal. Used by `Main` when it receives a
   * journal file, and by the milestone 3 test.
   *
   * Returns (final state, output lines, journal lines).
   */
  def runJournaled[S](engine: LedgerEngine[S], start: S, input: List[String]): (S, List[String], List[String]) = {
    val (state, out, journal) =
      input.foldLeft((start, Vector.empty[String], Vector.empty[String])) { case ((state, out, journal), line) =>
        val (next, responses) = engine.execute(state, line)
        (next, out ++ responses, journal ++ engine.journalLines(state, next))
      }
    (state, out.toList, journal.toList)
  }
}
