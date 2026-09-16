# Ledger — teaching notes

Companion to the attendee docs in `ledger/`. What to say, when, and what to look for. The one-page
review checklist is [MODEL_REVIEW.md](MODEL_REVIEW.md); the mechanics of the repo are in
[README.md](README.md).

## The day

| Time | Block |
|---|---|
| 9:00–9:25 | Kickoff: the two-function framing, a walk through `Harness.run` (it *is* the application), `sbt ledger` red on everyone's machine |
| 9:25–10:10 | Data modelling on paper. Laptops closed. |
| 10:10–10:30 | Model review, ~3 min per team, with the checklist |
| 10:30–11:30 | M1 — vertical slice |
| 11:30–12:30 | M2 — rejections and money |
| 12:30–13:15 | Lunch. Progress check: each team's `sbt ledger` is the grade; glance at repos for last commit + green tests. |
| 13:15–14:15 | M3 — journal and replay. Hand out nothing; PROTOCOL-PART2 stays closed until 14:15 |
| 14:15–15:30 | M4 — transfers. "You may now read PROTOCOL-PART2.md." |
| 15:30–16:20 | M5 — projections, or stretch goals |
| 16:20–17:00 | Demos, 5 min per team, then the debrief |

## Things to say out loud, once each

- **There are no hidden tests.** Say it at the briefing and pause on it: every scenario and every
  expected output is in the repo, readable now. It changes how people work — "am I done?" is never
  a question they queue to ask you.
- **Commands and facts are different types.** A command is a request that can be refused; a fact
  has already happened. Teams that merge them find out at M3 (validation would have to run again
  on replay) and again at M4 (one command, two facts). Do not say this at 9:00. Ask the review
  questions and let them find it.
- **State is a fold.** `Harness.run` is `foldLeft`. Their `evolve` is the function being folded.
  Day 2's Block B was about this; today it has a point.
- **Parsing is dumb on purpose.** `-5.00` parses. Teams want to reject negatives in the parser;
  the reason not to is that the parser has no state and no rejection vocabulary, so it would need
  a second error channel. One place decides.
- **The wire format hides the model.** Codes are kebab-case so they cannot be pasted into an
  enum; `+100.00 100.00` does not say "Deposited". Be honest about what this buys: it blocks
  transcription, not knowledge. The *set* of failure conditions is a requirement and cannot be
  hidden. What stays open is the structure behind the codes.
- **Idempotent FREEZE consumes a sequence number.** Is that the right design? Good five-minute
  argument at M2; the protocol keeps it simple and says so.
- **M3 is cuttable.** A team at 14:00 without a journal should stub it and go to M4. Say it at
  13:15, not at 14:10.
- **Two counters, both derived.** After M3, ask any team that is green: "where does the sequence
  number come from after a restart?" If the answer is "a var we reset from the journal", they have
  a second source of truth. `m3` catches the ones who forget; the transfer tag in `m4` catches the
  same mistake again one level down.

## Model review (10:10) — see MODEL_REVIEW.md

## Lunch progress check

No hidden tests, so nothing to run: a milestone is done when its test is green, and the teams know
it before you do. Use lunch to look at the repos — who has committed, whose `m2` is green — and to
decide who gets a visit first at 13:15.

## Debrief (16:40)

Where this shape lives in production: CQRS / event sourcing generally; Akka and Pekko persistent
actors; Redux and React reducers (`(state, action) => state` *is* `evolve`); Kafka log compaction;
and double-entry bookkeeping, which has been an append-only log with derived balances since the
fifteenth century.

The honest costs, because they will meet all three:

1. **Replay cost.** Without snapshots, startup is O(history). Stretch goal 2 is the fix and it
   complicates the replay property.
2. **Schema evolution.** Events are forever. Renaming a field in `Deposited` means every journal
   ever written. Versioned events and upcasters are the standard answer and they are tedious.
3. **Projections are eventually consistent** in any system where they are not computed inline as
   here. Operationally that is the hard part, not theoretically.

Close on the day-2 callback: the reason `foldLeft` was sufficient for an entire application is that
the state is immutable and the transition is a total function. Everything else today followed from
those two.

## Things that will go wrong

| | |
|---|---|
| A team's `execute` prints for comment lines | The stub does too. `m1/easy-1` fails with "expected 5 lines, got 7". Point at PROTOCOL §1. |
| `Double` for money | `0.1 + 0.2`. Then `Long` cents. Ten-minute refactor if caught at review, an hour if caught at M2. |
| Sequence number in a `var` | Passes M1, M2, every golden. `m3` fails only on the "after replaying" check. Ask where the number comes from. |
| `decide` returns one `Event` | Fine until 14:15. Let it land. |
| `trait Aggregate[C, E, S]` | Appears around 10:15. "Make one concrete case work first." This is also the abstraction the course excluded. |
| A team reads PROTOCOL-PART2 before lunch | It happens. They lose a discovery, not the day. |
| Plain `sbt test` | Runs the day-2 red suites too and stops. `sbt ledger`. |
