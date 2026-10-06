package com.rockthecode.ledger.solution

import Mutation._
import ParseError._
import Query._

/** Ways a line can fail to be a command at all. Each maps to one `ERROR` line. */
enum ParseError {
  case UnknownCommand(verb: String)
  case BadArguments(verb: String)
  case BadAccountId(token: String)
  case BadAmount(token: String)
  case BadCurrency(token: String)
  case BadSequence(token: String)
  case BadRange(fromSeq: Int, toSeq: Int)
}

/**
 * Text -> Command. Knows nothing about accounts or balances: it cannot tell you
 * whether an account exists, only whether a token looks like an account id.
 *
 * Precedence within a line: unknown verb, then arity, then arguments left to
 * right. That order is a protocol decision and is spelled out in PROTOCOL.md.
 */
object Parser {

  /** None for blank lines and comments: they produce no output at all. */
  def parse(line: String): Option[Either[ParseError, Command]] = {
    val trimmed = line.trim
    if (trimmed.isEmpty || trimmed.startsWith("#")) None
    else Some(command(trimmed.split("\\s+").toList))
  }

  private def command(tokens: List[String]): Either[ParseError, Command] = tokens match {
    case "OPEN" :: args     => arity(args, 2)("OPEN") { case List(acc, cur) => for { a <- account(acc); c <- currency(cur) } yield Open(a, c) }
    case "DEPOSIT" :: args  => arity(args, 2)("DEPOSIT") { case List(acc, amt) => for { a <- account(acc); m <- amount(amt) } yield Deposit(a, m) }
    case "WITHDRAW" :: args => arity(args, 2)("WITHDRAW") { case List(acc, amt) => for { a <- account(acc); m <- amount(amt) } yield Withdraw(a, m) }
    case "BALANCE" :: args  => arity(args, 1)("BALANCE") { case List(acc) => account(acc).map(Balance(_)) }
    case "FREEZE" :: args   => arity(args, 1)("FREEZE") { case List(acc) => account(acc).map(Freeze(_)) }
    case "UNFREEZE" :: args => arity(args, 1)("UNFREEZE") { case List(acc) => account(acc).map(Unfreeze(_)) }
    case "CLOSE" :: args    => arity(args, 1)("CLOSE") { case List(acc) => account(acc).map(Close(_)) }
    case "TRANSFER" :: args =>
      arity(args, 3)("TRANSFER") { case List(from, to, amt) =>
        for { f <- account(from); t <- account(to); m <- amount(amt) } yield Transfer(f, t, m)
      }
    case "HISTORY" :: args => arity(args, 1)("HISTORY") { case List(acc) => account(acc).map(History(_)) }
    case "SUMMARY" :: args =>
      arity(args, 3)("SUMMARY") { case List(acc, from, to) =>
        for {
          a <- account(acc)
          f <- sequence(from)
          t <- sequence(to)
          _ <- Either.cond(f <= t, (), BadRange(f, t))
        } yield Summary(a, f, t)
      }
    case verb :: _ => Left(UnknownCommand(verb))
    case Nil       => Left(UnknownCommand("")) // unreachable: blank lines never get here
  }

  private def arity(args: List[String], n: Int)(verb: String)(
      build: PartialFunction[List[String], Either[ParseError, Command]]
  ): Either[ParseError, Command] =
    if (args.size == n) build(args) else Left(BadArguments(verb))

  private val AccountId = "[a-z0-9-]{1,32}".r
  private val CurrencyCode = "[A-Z]{3}".r

  private def account(token: String): Either[ParseError, String] =
    if (AccountId.matches(token)) Right(token) else Left(BadAccountId(token))

  private def amount(token: String): Either[ParseError, Money] =
    Money.parse(token).toRight(BadAmount(token))

  /** Well-formed is all the parser checks; `JPY` parses and is then rejected by `decide`. */
  private def currency(token: String): Either[ParseError, String] =
    if (CurrencyCode.matches(token)) Right(token) else Left(BadCurrency(token))

  private def sequence(token: String): Either[ParseError, Int] =
    if (token.matches("\\d+")) token.toIntOption.toRight(BadSequence(token)) else Left(BadSequence(token))
}
