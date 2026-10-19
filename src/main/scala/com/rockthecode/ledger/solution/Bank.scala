package com.rockthecode.ledger.solution

// the events are stored for the journal part
final case class Bank(accounts: Map[String, Account], events: Vector[Event]) {

  def nextSeq: Int = events.size + 1

  def transfersSoFar: Int = events.count {
    case _: Event.TransferDebited => true
    case _ => false
  }

  def account(id: String): Option[Account] = accounts.get(id)
}

object Bank {
  val empty: Bank = Bank(Map.empty, Vector.empty)
}
