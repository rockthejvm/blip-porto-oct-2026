package com.rockthecode.ledger.domain

final case class Account(
    name: String,
    amount: Int, // TODO - manage money
    currency: Currency,
    frozen: Boolean
)
