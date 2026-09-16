package com.rockthecode.ledger.solution

/**
 * Money as an integer count of minor units (cents). Never a Double: 0.1 + 0.2
 * is not 0.3 in binary floating point, and a ledger that is off by a cent is
 * wrong.
 *
 * All supported currencies have two decimals, so one `Money` type serves all of
 * them; the currency itself lives on the account.
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

  /**
   * Parses `100`, `100.5`, `100.50`, `-5.00`. Three or more decimals do not
   * parse. A negative amount DOES parse - whether it is acceptable is not the
   * parser's business, because the parser has no state and no rejection
   * vocabulary. `decide` refuses it as `non-positive-amount`.
   */
  def parse(token: String): Option[Money] = token match {
    case Amount(sign, whole, fraction) if whole.length <= 15 =>
      val minor = (Option(fraction).getOrElse("") + "00").take(2).toLong
      val cents = whole.toLong * 100 + minor
      Some(Money(if (sign == "-") -cents else cents))
    case _ => None
  }
}
