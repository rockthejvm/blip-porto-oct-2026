package com.rockthecode.ledger.domain

sealed trait Event

case class Opened(name: String, currency: Currency) extends Event
case class Deposited(name: String, amount: Int) extends Event
case class Withdrawn(name: String, amount: Int) extends Event
