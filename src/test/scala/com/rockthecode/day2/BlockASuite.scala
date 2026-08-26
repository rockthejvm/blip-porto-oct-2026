package com.rockthecode.day2

import com.rockthecode.day2.solutions.BlockASolutions

/**
 * Day 2, Block A - the tests.
 *
 *   sbt "testOnly com.rockthecode.day2.BlockAExercisesSuite"   <- attendees
 *   sbt "testOnly com.rockthecode.day2.BlockASolutionsSuite"   <- trainer
 */
abstract class BlockASuite(impl: BlockA) extends munit.FunSuite {

  private val orders = List(
    Order("o-1", "Ana", BigDecimal(120)),
    Order("o-2", "Bruno", BigDecimal(45)),
    Order("o-3", "Ana", BigDecimal(300)),
    Order("o-4", "Carla", BigDecimal(300)),
    Order("o-5", "Bruno", BigDecimal(15))
  )

  private val threshold = BigDecimal(100)

  // --- A.1 -------------------------------------------------------------------

  test("A.1 totalValue sums every order") {
    assertEquals(impl.totalValue(orders), BigDecimal(780))
    assertEquals(impl.totalValue(Nil), BigDecimal(0))
  }

  test("A.1 largeOrders keeps those strictly above the threshold, in order") {
    assertEquals(impl.largeOrders(orders, threshold).map(_.id), List("o-1", "o-3", "o-4"))
    assertEquals(impl.largeOrders(orders, BigDecimal(300)), Nil, "strictly above, so 300 is out")
    assertEquals(impl.largeOrders(Nil, threshold), Nil)
  }

  test("A.1 largestOrder finds the biggest, earliest on a tie") {
    assertEquals(impl.largestOrder(orders).map(_.id), Some("o-3"))
    assertEquals(impl.largestOrder(Nil), None)
  }

  test("A.1 summarize agrees with the imperative version it replaces") {
    assertEquals(impl.summarize(orders, threshold), BlockAExercises.imperativeSummarize(orders, threshold))
  }

  test("A.1 summarize agrees on the empty list too - where the null used to live") {
    assertEquals(impl.summarize(Nil, threshold), BlockAExercises.imperativeSummarize(Nil, threshold))
    assertEquals(impl.summarize(Nil, threshold).largest, None)
  }

  // --- A.2 -------------------------------------------------------------------

  private val active = Account("acc-1", BigDecimal(100), Active)
  private val frozen = Account("acc-2", BigDecimal(100), Frozen)

  test("A.2 a deposit into an active account raises the balance") {
    assertEquals(impl.applyTransaction(active, Deposit(BigDecimal(25))), active.copy(balance = BigDecimal(125)))
  }

  test("A.2 an affordable withdrawal lowers the balance") {
    assertEquals(impl.applyTransaction(active, Withdrawal(BigDecimal(40))), active.copy(balance = BigDecimal(60)))
  }

  test("A.2 withdrawing exactly the balance is allowed") {
    assertEquals(impl.applyTransaction(active, Withdrawal(BigDecimal(100))), active.copy(balance = BigDecimal(0)))
  }

  test("A.2 an unaffordable withdrawal is rejected, not clamped and not thrown") {
    assertEquals(impl.applyTransaction(active, Withdrawal(BigDecimal(101))), active)
  }

  test("A.2 a frozen account ignores deposits and withdrawals") {
    assertEquals(impl.applyTransaction(frozen, Deposit(BigDecimal(25))), frozen)
    assertEquals(impl.applyTransaction(frozen, Withdrawal(BigDecimal(25))), frozen)
  }

  test("A.2 freezing and unfreezing leave the balance alone") {
    assertEquals(impl.applyTransaction(active, Freeze), active.copy(status = Frozen))
    assertEquals(impl.applyTransaction(frozen, Unfreeze), frozen.copy(status = Active))
    assertEquals(impl.applyTransaction(active, Unfreeze), active, "already active")
  }

  test("A.2 applyAll replays a history in order") {
    val history = List(
      Deposit(BigDecimal(50)),      // 150
      Withdrawal(BigDecimal(30)),   // 120
      Freeze,
      Deposit(BigDecimal(1000)),    // ignored, frozen
      Unfreeze,
      Withdrawal(BigDecimal(20))    // 100
    )
    assertEquals(impl.applyAll(active, history), Account("acc-1", BigDecimal(100), Active))
  }

  test("A.2 applyAll of no transactions is the account itself") {
    assertEquals(impl.applyAll(active, Nil), active)
  }

  test("A.2 order matters - the same transactions, replayed differently") {
    val ordered = List(Deposit(BigDecimal(100)), Withdrawal(BigDecimal(150)))
    val reversed = ordered.reverse
    assertEquals(impl.applyAll(active, ordered).balance, BigDecimal(50))
    assertEquals(impl.applyAll(active, reversed).balance, BigDecimal(200), "the withdrawal was rejected first")
  }

  // --- A.3 -------------------------------------------------------------------

  test("A.3 formatDuration skips zero units") {
    assertEquals(impl.formatDuration(0L), "0ms")
    assertEquals(impl.formatDuration(999L), "999ms")
    assertEquals(impl.formatDuration(1000L), "1s")
    assertEquals(impl.formatDuration(61000L), "1m 1s")
    assertEquals(impl.formatDuration(3600000L), "1h")
    assertEquals(impl.formatDuration(8103000L), "2h 15m 3s")
    assertEquals(impl.formatDuration(8103456L), "2h 15m 3s 456ms")
  }

  test("A.3 renderLine pads the method and the path and reuses formatDuration") {
    val request = Request("GET", "/api/orders", 200, 8103000L)
    val expected = "GET".padTo(6, ' ') + " " + "/api/orders".padTo(24, ' ') + " " + "200" + "  " + "2h 15m 3s"
    assertEquals(impl.renderLine(request), expected)
  }

  test("A.3 renderLine does not truncate something longer than the padding") {
    val request = Request("DELETE", "/api/sessions/current/everywhere", 204, 61000L)
    val expected = "DELETE" + " " + "/api/sessions/current/everywhere" + " " + "204" + "  " + "1m 1s"
    assertEquals(impl.renderLine(request), expected)
  }

  test("A.3 renderReport is one line per request, in order, and prints nothing") {
    val report = impl.renderReport(BlockAExercises.sampleTraffic)
    assertEquals(report.size, BlockAExercises.sampleTraffic.size)
    assertEquals(report, BlockAExercises.sampleTraffic.map(impl.renderLine))
    assert(report.head.startsWith("GET"))
  }

  test("A.3 renderReport of nothing is nothing") {
    assertEquals(impl.renderReport(Nil), Nil)
  }

  // ===========================================================================
  //
  //  STRETCH SECTION
  //
  // ===========================================================================

  private val ana = Account("ana", BigDecimal(100), Active)
  private val bruno = Account("bruno", BigDecimal(50), Active)
  private val carla = Account("carla", BigDecimal(75), Frozen)

  private val bank = List(ana, bruno, carla).map(account => account.id -> account).toMap

  private def totalMoney(accounts: Map[String, Account]): BigDecimal =
    accounts.values.map(_.balance).sum

  // --- SA1 -------------------------------------------------------------------

  test("SA1 an affordable transfer moves the money") {
    assertEquals(
      impl.transfer(ana, bruno, BigDecimal(30)),
      (ana.copy(balance = BigDecimal(70)), bruno.copy(balance = BigDecimal(80)))
    )
  }

  test("SA1 transferring the whole balance is allowed") {
    val (from, to) = impl.transfer(ana, bruno, BigDecimal(100))
    assertEquals(from.balance, BigDecimal(0))
    assertEquals(to.balance, BigDecimal(150))
  }

  test("SA1 an unaffordable transfer changes nothing at all") {
    assertEquals(impl.transfer(ana, bruno, BigDecimal(101)), (ana, bruno))
  }

  test("SA1 a frozen sender changes nothing") {
    assertEquals(impl.transfer(carla, bruno, BigDecimal(10)), (carla, bruno))
  }

  test("SA1 a frozen RECIPIENT must not leave the sender debited") {
    // The easy wrong answer debits `from`, discovers `to` will not take the
    // money, and returns anyway. That destroys money.
    assertEquals(impl.transfer(ana, carla, BigDecimal(10)), (ana, carla))
  }

  test("SA1 money is conserved by every transfer, successful or not") {
    val amounts = List(30, 100, 101, 0, 1).map(BigDecimal(_))
    amounts.foreach { amount =>
      val (from, to) = impl.transfer(ana, bruno, amount)
      assertEquals(from.balance + to.balance, ana.balance + bruno.balance, s"lost money on $amount")
    }
  }

  // --- SA2 -------------------------------------------------------------------

  test("SA2 transfers are applied in order, each seeing the last") {
    val transfers = List(
      Transfer("ana", "bruno", BigDecimal(60)),  // ana 40, bruno 110
      Transfer("bruno", "ana", BigDecimal(100)), // ana 140, bruno 10
      Transfer("bruno", "ana", BigDecimal(50))   // rejected: bruno only has 10
    )
    val result = impl.applyTransfers(bank, transfers)
    assertEquals(result("ana").balance, BigDecimal(140))
    assertEquals(result("bruno").balance, BigDecimal(10))
  }

  test("SA2 an unknown account id is skipped, not an error") {
    val transfers = List(
      Transfer("ana", "nobody", BigDecimal(10)),
      Transfer("ghost", "bruno", BigDecimal(10))
    )
    assertEquals(impl.applyTransfers(bank, transfers), bank)
  }

  test("SA2 a self-transfer must not print money") {
    val result = impl.applyTransfers(bank, List(Transfer("ana", "ana", BigDecimal(40))))
    assertEquals(result("ana").balance, BigDecimal(100))
    assertEquals(totalMoney(result), totalMoney(bank))
  }

  test("SA2 no transfer changes the total money in the bank") {
    val transfers = List(
      Transfer("ana", "bruno", BigDecimal(60)),
      Transfer("bruno", "carla", BigDecimal(20)), // carla frozen: refused
      Transfer("ana", "ana", BigDecimal(5)),
      Transfer("bruno", "ana", BigDecimal(1000)), // unaffordable
      Transfer("ghost", "ana", BigDecimal(7)),    // unknown
      Transfer("bruno", "ana", BigDecimal(30))
    )
    assertEquals(totalMoney(impl.applyTransfers(bank, transfers)), totalMoney(bank))
  }

  test("SA2 no transfers at all leaves the bank alone") {
    assertEquals(impl.applyTransfers(bank, Nil), bank)
  }

  // --- SA3 -------------------------------------------------------------------

  test("SA3 a clean history produces no rejections") {
    val history = List(Deposit(BigDecimal(50)), Withdrawal(BigDecimal(30)), Freeze, Unfreeze)
    val (account, rejections) = impl.applyAllTracking(ana, history)
    assertEquals(account, ana.copy(balance = BigDecimal(120)))
    assertEquals(rejections, Nil)
  }

  test("SA3 rejections carry their position and their reason, in order") {
    val history = List(
      Deposit(BigDecimal(50)),     // 0: ok, balance 150
      Withdrawal(BigDecimal(500)), // 1: insufficient funds
      Freeze,                      // 2: ok
      Deposit(BigDecimal(10)),     // 3: account frozen
      Withdrawal(BigDecimal(999)), // 4: frozen wins over insufficient
      Unfreeze,                    // 5: ok
      Withdrawal(BigDecimal(50))   // 6: ok, balance 100
    )
    val (account, rejections) = impl.applyAllTracking(ana, history)
    assertEquals(account, Account("ana", BigDecimal(100), Active))
    assertEquals(
      rejections,
      List(
        Rejection(1, "insufficient funds"),
        Rejection(3, "account frozen"),
        Rejection(4, "account frozen")
      )
    )
  }

  test("SA3 freezing an already frozen account is not a rejection") {
    val (_, rejections) = impl.applyAllTracking(carla, List(Freeze, Unfreeze, Unfreeze))
    assertEquals(rejections, Nil)
  }

  test("SA3 the final account matches what a plain replay would give") {
    val history = List(Deposit(BigDecimal(50)), Withdrawal(BigDecimal(500)), Freeze, Deposit(BigDecimal(10)))
    assertEquals(impl.applyAllTracking(ana, history)._1, impl.applyAll(ana, history))
  }

  // --- SA4 -------------------------------------------------------------------

  test("SA4 parseDuration reads what formatDuration writes") {
    assertEquals(impl.parseDuration("0ms"), 0L)
    assertEquals(impl.parseDuration("999ms"), 999L)
    assertEquals(impl.parseDuration("1s"), 1000L)
    assertEquals(impl.parseDuration("1m 1s"), 61000L)
    assertEquals(impl.parseDuration("1h"), 3600000L)
    assertEquals(impl.parseDuration("2h 15m 3s"), 8103000L)
    assertEquals(impl.parseDuration("2h 15m 3s 456ms"), 8103456L)
  }

  test("SA4 seconds and milliseconds are not confused") {
    assertEquals(impl.parseDuration("3s"), 3000L)
    assertEquals(impl.parseDuration("3ms"), 3L)
  }

  test("SA4 format then parse is the identity") {
    val durations = List(0L, 1L, 999L, 1000L, 61000L, 3600000L, 8103000L, 8103456L, 86399999L, 1L)
    durations.foreach { millis =>
      assertEquals(impl.parseDuration(impl.formatDuration(millis)), millis, s"round trip failed for $millis")
    }
  }

  // --- SA5 -------------------------------------------------------------------

  private val traffic = BlockAExercises.sampleTraffic

  test("SA5 summarizeTraffic counts requests and status classes") {
    val summary = impl.summarizeTraffic(traffic)
    assertEquals(summary.total, 5)
    assertEquals(summary.byStatusClass, Map("2xx" -> 4, "4xx" -> 1))
  }

  test("SA5 p95 is nearest-rank, so on five samples it is the slowest") {
    assertEquals(impl.summarizeTraffic(traffic).p95Millis, 61000L)
  }

  test("SA5 p95 on twenty samples lands on the nineteenth") {
    val twenty = (1 to 20).toList.map(i => Request("GET", "/x", 200, i.toLong))
    assertEquals(impl.summarizeTraffic(twenty).p95Millis, 19L)
    assertEquals(impl.summarizeTraffic(twenty.reverse).p95Millis, 19L, "order of the input must not matter")
  }

  test("SA5 slowest is the slowest, earliest on a tie") {
    assertEquals(impl.summarizeTraffic(traffic).slowest.map(_.path), Some("/api/sessions/current"))

    val tied = List(
      Request("GET", "/first", 200, 500L),
      Request("GET", "/second", 200, 500L)
    )
    assertEquals(impl.summarizeTraffic(tied).slowest.map(_.path), Some("/first"))
  }

  test("SA5 an empty sample summarises to nothing, without throwing") {
    assertEquals(
      impl.summarizeTraffic(Nil),
      TrafficSummary(total = 0, byStatusClass = Map.empty, p95Millis = 0L, slowest = None)
    )
  }
}

/** Red until the attendees make it green. That is the point. */
class BlockAExercisesSuite extends BlockASuite(BlockAExercises)

/** Green, always. If this one goes red, the reference solutions are wrong. */
class BlockASolutionsSuite extends BlockASuite(BlockASolutions)
