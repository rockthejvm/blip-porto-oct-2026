package com.rockthecode.ledger.solution

import Event._

/**
 * Event <-> one line of the journal file. Our own format, read by nobody but
 * `replay`; it stores cents, not rendered amounts, so nothing is re-parsed.
 *
 * `decode` throws on a line it does not recognise. A corrupt journal is not a
 * user input error, it is a broken invariant, and continuing would be worse.
 */
object Journal {

  def encode(event: Event): String = event match {
    case Opened(acc, currency)             => s"opened $acc $currency"
    case Deposited(acc, amount)            => s"deposited $acc ${amount.cents}"
    case Withdrawn(acc, amount)            => s"withdrawn $acc ${amount.cents}"
    case Frozen(acc)                       => s"frozen $acc"
    case Unfrozen(acc)                     => s"unfrozen $acc"
    case Closed(acc)                       => s"closed $acc"
    case TransferDebited(acc, amount, tag) => s"transfer-debited $acc ${amount.cents} $tag"
    case TransferCredited(acc, amount, tag) => s"transfer-credited $acc ${amount.cents} $tag"
  }

  def decode(line: String): Event = line.split(" ").toList match {
    case List("opened", acc, currency)                => Opened(acc, Currency.fromCode(currency).getOrElse(corrupt(line)))
    case List("deposited", acc, cents)                => Deposited(acc, Money(cents.toLong))
    case List("withdrawn", acc, cents)                => Withdrawn(acc, Money(cents.toLong))
    case List("frozen", acc)                          => Frozen(acc)
    case List("unfrozen", acc)                        => Unfrozen(acc)
    case List("closed", acc)                          => Closed(acc)
    case List("transfer-debited", acc, cents, tag)    => TransferDebited(acc, Money(cents.toLong), tag.toInt)
    case List("transfer-credited", acc, cents, tag)   => TransferCredited(acc, Money(cents.toLong), tag.toInt)
    case _                                            => corrupt(line)
  }

  private def corrupt(line: String): Nothing = sys.error(s"corrupt journal line: '$line'")
}
