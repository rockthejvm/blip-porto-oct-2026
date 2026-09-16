package com.rockthecode.ledger.solution

/**
 * The whole state of the ledger: the event log, plus the current view of every
 * account that the log implies.
 *
 * Both counters the protocol needs are DERIVED from the log, never stored.
 * That is what makes replay work: rebuild the log, and the next sequence
 * number and the next transfer tag come back with it. A `var` counter would
 * restart at 1 after every restart.
 */
final case class Bank(accounts: Map[String, Account], events: Vector[Event]) {

  /** The sequence number the next recorded fact will get. */
  def nextSeq: Int = events.size + 1

  /** How many transfers have succeeded so far. The next one is tagged `t${transfersSoFar + 1}`. */
  def transfersSoFar: Int = events.count {
    case _: Event.TransferDebited => true
    case _                        => false
  }

  def account(id: String): Option[Account] = accounts.get(id)
}

object Bank {
  val empty: Bank = Bank(Map.empty, Vector.empty)
}
