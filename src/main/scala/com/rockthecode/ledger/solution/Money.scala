package com.rockthecode.ledger.solution

/** Money as an integer count of minor units (cents). Never a Double: 0.1 + 0.2 is not 0.3 in binary floating point.
  */
final case class Money(cents: Long) {
  def +(other: Money): Money = Money(cents + other.cents)
  def -(other: Money): Money = Money(cents - other.cents)
  def <(other: Money): Boolean = cents < other.cents
  def isPositive: Boolean = cents > 0
  def isZero: Boolean = cents == 0
}

object Money {
  val zero: Money = Money(0)

  private val Amount = """(-?)(\d+)(?:\.(\d{1,2}))?""".r

  def parse(token: String): Option[Money] = token match {
    case Amount(sign, whole, fraction) if whole.length <= 15 =>
      val minor = (Option(fraction).getOrElse("") + "00").take(2).toLong
      val cents = whole.toLong * 100 + minor
      Some(Money(if (sign == "-") -cents else cents))
    case _ => None
  }
}
