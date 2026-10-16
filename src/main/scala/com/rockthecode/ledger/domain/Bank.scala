package com.rockthecode.ledger.domain

/*

	- names of accounts == unique, the id of the account
	- amounts of each account
	- currencies of each account
	- state of account (open, closed, frozed)

	Map[String, Account]
 */
final case class Bank(accounts: Map[String, Account], seq: Int)
