package com.rockthecode.ledger.domain

sealed trait Rejection
case class UnknownAccount(name: String) extends Rejection
case class AccountExists(name: String) extends Rejection
