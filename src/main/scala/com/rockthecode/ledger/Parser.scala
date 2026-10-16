package com.rockthecode.ledger

import domain.*

sealed trait ParseError

object Parser {
  def parse(line: String): Option[Either[ParseError, Command]] = {
    val trimmed = line.trim
    if (trimmed.startsWith("#") || trimmed.isEmpty) None
    else Some(parseCommand(trimmed))
  }

  def parseCommand(input: String): Either[ParseError, Command] = {
    // tokenize line
    // take the "verb" = first token, PM on it
    val tokens = input.split(" ").toList
    tokens match {
      // TODO take care of parsing errors, buster!
      case "OPEN" :: rest =>
        Right(Open(rest.head, Currency(rest.tail.head).get))
      case "DEPOSIT" :: rest =>
        Right(Deposit(rest.head, parseMoney(rest.tail.head)))
      case "WITHDRAW" :: rest =>
        Right(Withdraw(rest.head, parseMoney(rest.tail.head)))
      case "BALANCE" :: rest =>
        Right(Balance(rest.head))
    }
  }

  def parseMoney(input: String): Int = 42
}
