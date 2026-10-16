package com.rockthecode.ledger.domain

sealed trait Currency
case object EUR extends Currency
case object GBP extends Currency
case object USD extends Currency

object Currency {
  def apply(value: String): Currency = ???
}
