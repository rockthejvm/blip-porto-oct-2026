package com.rockthecode.ledger

/**
 * Your implementation. Replace `Unit` with your own state type.
 *
 * Everything else in this package is provided and complete. Your data model
 * goes in `domain/`; this object connects it to the harness.
 */
object MyLedger extends LedgerEngine[Unit] {

  def empty: Unit = ()

  def execute(state: Unit, line: String): (Unit, List[String]) =
    ((), List("ERROR NotImplemented"))
}
