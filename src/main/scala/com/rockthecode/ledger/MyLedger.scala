package com.rockthecode.ledger

import com.rockthecode.ledger.domain.*

/** Your implementation. Replace `Unit` with your own state type.
  *
  * Everything else in this package is provided and complete. Your data model goes in `domain/`; this object connects it
  * to the harness.
  */

object MyLedger extends LedgerEngine[Bank] {

  def empty: Bank = Bank()

  def execute(state: Bank, line: String): (Bank, List[String]) =
    (Bank(), List("ERROR NotImplemented"))
}
