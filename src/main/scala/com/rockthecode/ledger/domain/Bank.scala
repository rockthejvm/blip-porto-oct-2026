package com.rockthecode.ledger.domain

import com.rockthecode.ledger.Output

final case class Bank(accounts: Map[String, Account], seq: Int) {
  def run(command: Command): (Bank, List[String]) = command match {
    case Open(name, currency) =>
      val newAccounts = accounts + (name -> Active(name, 0, currency, false))
      val event = Opened(name, currency)
      (Bank(newAccounts, seq + 1), List(Output.write(event, seq))) // TODO
    case _ => (this, List("not implemented yet"))
  }
}
