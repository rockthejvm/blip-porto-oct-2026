package com.rockthecode.ledger.solution

sealed trait Command

enum Mutation extends Command {
  case Open(account: String, currency: String)
  case Deposit(account: String, amount: Money)
  case Withdraw(account: String, amount: Money)
  case Freeze(account: String)
  case Unfreeze(account: String)
  case Close(account: String)
  case Transfer(from: String, to: String, amount: Money)
}

enum Query extends Command {
  case Balance(account: String)
  case History(account: String)
  case Summary(account: String, fromSeq: Int, toSeq: Int)

  def account: String
}
