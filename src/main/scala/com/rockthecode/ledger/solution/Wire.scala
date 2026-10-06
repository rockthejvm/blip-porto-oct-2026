package com.rockthecode.ledger.solution

import Event._
import ParseError._
import Projections.{Fact, Summary}
import Rejection._

/**
 * Model -> protocol text. Every string the program prints is built here and
 * nowhere else.
 *
 * This object is the boundary between our model and the wire format. The wire
 * is a lossy projection: it does not say "Deposited", it says "+100.00 100.00".
 * If the protocol changed tomorrow, this file changes and the model does not.
 * Note `effect` is used twice - for `OK` lines and for `HISTORY` lines.
 */
object Wire {

  /** Exactly two decimals, no thousands separators, leading `-` when negative. */
  def money(m: Money): String = {
    val sign = if (m.cents < 0) "-" else ""
    val abs = math.abs(m.cents)
    f"$sign${abs / 100}%d.${abs % 100}%02d"
  }

  /** The observable consequence of one fact. Shared by `ok` and `history`. */
  def effect(event: Event, balanceAfter: Money): String = event match {
    case Opened(_, currency)                 => s"OPENED $currency"
    case Deposited(_, amount)                => s"+${money(amount)} ${money(balanceAfter)}"
    case Withdrawn(_, amount)                => s"-${money(amount)} ${money(balanceAfter)}"
    case Frozen(_)                           => "FROZEN"
    case Unfrozen(_)                         => "UNFROZEN"
    case Closed(_)                           => "CLOSED"
    case TransferDebited(_, amount, tag)     => s"-${money(amount)} ${money(balanceAfter)} xfer=t$tag"
    case TransferCredited(_, amount, tag)    => s"+${money(amount)} ${money(balanceAfter)} xfer=t$tag"
  }

  def ok(seq: Int, event: Event, balanceAfter: Money): String =
    s"OK $seq ${event.account} ${effect(event, balanceAfter)}"

  def rejected(r: Rejection): String = "REJECTED " + (r match {
    case UnknownAccount(acc)                  => s"unknown-account account=$acc"
    case AccountExists(acc)                   => s"account-exists account=$acc"
    case UnsupportedCurrency(code)            => s"unsupported-currency currency=$code"
    case AccountClosed(acc)                   => s"account-closed account=$acc"
    case AccountFrozen(acc)                   => s"account-frozen account=$acc"
    case NonPositiveAmount(acc, amount)       => s"non-positive-amount account=$acc amount=${money(amount)}"
    case InsufficientFunds(acc, bal, req)     => s"insufficient-funds account=$acc balance=${money(bal)} requested=${money(req)}"
    case SameAccountTransfer(acc)             => s"same-account-transfer account=$acc"
    case CurrencyMismatch(from, to, fc, tc)   => s"currency-mismatch from=$from to=$to from-currency=$fc to-currency=$tc"
    case NonZeroBalance(acc, bal)             => s"non-zero-balance account=$acc balance=${money(bal)}"
  })

  def error(e: ParseError): String = "ERROR " + (e match {
    case UnknownCommand(verb) => s"unknown-command $verb"
    case BadArguments(verb)   => s"bad-arguments $verb"
    case BadAccountId(token)  => s"bad-account-id $token"
    case BadAmount(token)     => s"bad-amount $token"
    case BadCurrency(token)   => s"bad-currency $token"
    case BadSequence(token)   => s"bad-sequence $token"
    case BadRange(from, to)   => s"bad-range $from-$to"
  })

  def balance(account: Account.Active): String =
    s"BALANCE ${account.id} ${money(account.balance)} ${account.currency}"

  def history(account: String, facts: List[Fact]): List[String] =
    s"HISTORY $account ${facts.size}" :: facts.map(f => s"  ${f.seq} ${effect(f.event, f.balanceAfter)}")

  def summary(account: String, fromSeq: Int, toSeq: Int, s: Summary): String =
    s"SUMMARY $account $fromSeq $toSeq opening=${money(s.opening)} credits=${money(s.credits)} " +
      s"debits=${money(s.debits)} closing=${money(s.closing)} count=${s.count}"
}
