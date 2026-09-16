package com.rockthecode.betsettlement.m5

import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec

/**
 * Milestone 5 - the same engine, over HTTP.
 *
 *   sbt "runMain com.rockthecode.betsettlement.m5.Server"
 *
 *   curl localhost:8899/ledger/C001
 *   curl -X POST 'localhost:8899/results?marketId=M004&outcome=HOME'
 *
 * The result arrives as query parameters rather than a JSON body, so that
 * nothing here has to PARSE json - only produce it. Cask will happily decode a
 * JSON body for you with `@cask.postJson` if you would rather; note that it
 * then also encodes your response, so returning an already-rendered string
 * gets you a JSON string containing JSON.
 *
 * Cask is thread-per-request, so two of these can be in flight at the same
 * moment. That is the entire new idea in this milestone: everything else is
 * code you already wrote.
 */
object Server extends cask.MainRoutes {

  override def port: Int = 8899

  private val state = new AtomicReference(EngineState.load("betsettlement/data/m4"))

  @cask.get("/ledger/:customerId")
  def ledger(customerId: String): cask.Response[String] =
    state.get().ledgerFor(customerId) match {
      case Some(entry) =>
        json(
          200,
          Json.JObj(
            List(
              "customerId" -> Json.JStr(entry.customerId),
              "betCount" -> Json.JNum(BigDecimal(entry.betCount)),
              "staked" -> Json.JNum(entry.staked.setScale(2)),
              "returned" -> Json.JNum(entry.returned.setScale(2)),
              "net" -> Json.JNum(entry.net.setScale(2))
            )
          )
        )
      case None =>
        json(404, Json.JObj(List("error" -> Json.JStr("UNKNOWN_CUSTOMER"))))
    }

  @cask.post("/results")
  def postResult(marketId: String, outcome: String): cask.Response[String] = {
    val result = if (outcome == "VOID") Result.Void else Result.Winner(outcome)

    // M4 reported unknown markets and carried on; over HTTP there is somebody
    // to tell, so tell them. Silently accepting a result for a market that does
    // not exist is how a typo in an upstream feed goes unnoticed for a week.
    if (!state.get().markets.contains(marketId))
      json(404, Json.JObj(List("error" -> Json.JStr("UNKNOWN_MARKET"))))
    else
      json(
        200,
        Json.JObj(
          List(
            "accepted" -> Json.JBool(true),
            "settled" -> Json.JNum(BigDecimal(apply(marketId, result)))
          )
        )
      )
  }

  /**
   * Move the state forward by one result, and report how many bets stopped
   * being pending because of it.
   *
   * This is a compare-and-set loop, which is what `updateAndGet` does inside -
   * written out here because we need the BEFORE as well as the after, and
   * because seeing it once is worth more than being told about it.
   *
   * Read the current value, compute the next one, and swap only if nothing
   * changed underneath us in the meantime. If something did, throw away the
   * work and try again with what is there now. Nobody blocks, nothing is
   * locked, and no update is ever lost.
   *
   * The reason it is safe to "throw away the work and try again" is that
   * `withResult` is pure. Running it twice costs a little time and changes
   * nothing else - which is the argument for purity turning up somewhere you
   * would not have predicted on day 1.
   */
  @tailrec
  private def apply(marketId: String, outcome: Result): Int = {
    val before = state.get()
    val after = before.withResult(marketId, outcome)

    if (state.compareAndSet(before, after)) before.pendingCount - after.pendingCount
    else apply(marketId, outcome)
  }

  private def json(status: Int, body: Json): cask.Response[String] =
    cask.Response(Json.render(body), statusCode = status, headers = Seq("Content-Type" -> "application/json"))

  initialize()
}
