package com.rockthecode.ledger.domain

sealed trait Account
final case class Active(
    name: String,
    amount: Int,
    currency: Currency,
    frozen: Boolean
) extends Account
case object Closed extends Account
