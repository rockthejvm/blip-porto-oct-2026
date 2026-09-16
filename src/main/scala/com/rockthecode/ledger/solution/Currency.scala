package com.rockthecode.ledger.solution

/** The supported currencies. All have two decimal places; JPY is deliberately absent. */
enum Currency {
  case USD, EUR, GBP
}

object Currency {
  def fromCode(code: String): Option[Currency] = Currency.values.find(_.toString == code)
}
