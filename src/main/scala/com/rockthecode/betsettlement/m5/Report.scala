package com.rockthecode.betsettlement.m5

/**
 * Milestone 4 - rendering the ledger.
 *
 * The settlement rendering from M3 has not gone anywhere and is still useful
 * for looking at your own work; it is just not what this milestone prints.
 */
object Report {

  val header = "customerId,betCount,staked,returned,net"

  def render(ledger: List[LedgerEntry]): List[String] =
    header :: ledger.map(line)

  private def line(entry: LedgerEntry): String =
    s"${entry.customerId},${entry.betCount},${money(entry.staked)},${money(entry.returned)},${money(entry.net)}"

  private def money(amount: BigDecimal): String = amount.setScale(2).toString
}
