package com.rockthecode.ledger

import com.rockthecode.ledger.domain.*

object Output {
  def write(e: Event, seq: Int): String = e match {
    case Opened(name, currency) =>
      s"OK $seq $name OPEN $currency"
    case _ => "TODO nothing yet"
  }

  // TODO format properly
  def formatMoney(money: Int): String =
    money.toString
}
