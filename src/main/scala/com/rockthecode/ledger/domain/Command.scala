package com.rockthecode.ledger.domain

sealed trait Command

sealed trait Action extends Command
case class Open(name: String, currency: Currency) extends Action
case class Deposit(name: String, amount: Int) extends Action
case class Withdraw(name: String, amount: Int) extends Action

sealed trait Query extends Command
case class Balance(name: String) extends Query
