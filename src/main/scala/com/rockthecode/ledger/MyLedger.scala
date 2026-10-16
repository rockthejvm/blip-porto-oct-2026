package com.rockthecode.ledger

import com.rockthecode.ledger.domain.*

/** Your implementation. Replace `Unit` with your own state type.
  *
  * Everything else in this package is provided and complete. Your data model goes in `domain/`; this object connects it
  * to the harness.
  */

import domain.*

object MyLedger extends LedgerEngine[Bank] {

  def empty: Bank = Bank(Map(), 1)

  def execute(state: Bank, line: String): (Bank, List[String]) = {
    // parse the command
    val maybeCommand = Parser.parse(line)
    maybeCommand match {
      case None => (state, List())
      case Some(Left(e)) => (state, List("ERROR parsing")) // TODO message
      case Some(Right(c)) => state.run(c)
    }
    // run the command
  }

}
