# Ledger — trainer's guide

Day-3 candidate B (decision between this and `betsettlement` still open). This file is the single
trainer document: the repo mechanics, the run of day, what to say and ask, and the review record.
The attendees' single document is [`ledger/README.md`](../../ledger/README.md) — the full guide
with the protocol folded into the milestone sections.

## 1. The repo

Attendee-facing: `ledger/README.md`, `src/main/resources/ledger/` (ALL scenarios — there is no
hidden set; "no hidden tests" is part of the pitch, as in the betting project), and
`com.rockthecode.ledger` minus the `solution` package. Trainer-only: this directory,
`com.rockthecode.ledger.solution` (main and test), and the two trainer aliases in `build.sbt`.

**Before handing out:** strip `src/main/scala/com/rockthecode/ledger/solution/`,
`src/test/scala/com/rockthecode/ledger/solution/`, `notes/`, and the `ledgerGen`/`ledgerVerify`
aliases — same decision as the day-2 solutions package. A leaked solution kills the modelling
exercise. Then check `sbt compile` and `sbt ledger` (five red tests with readable messages) on the
stripped copy.

### Commands

| | |
|---|---|
| `sbt ledger` | attendee suite (`MyLedgerSuite`), one test per milestone |
| `sbt "testOnly *ReferenceLedgerSuite"` | reference against every scenario; also part of `sbt checkSolutions` |
| `sbt ledgerGen` | regenerate everything the tools own: huge `.in` files (seeded, byte-identical) and every `.out` |
| `sbt ledgerVerify` | check every committed scenario file — OK / NOT OK per file with the first differing line; plus sequence-number density and SUMMARY arithmetic |
| `sbt "runMain com.rockthecode.ledger.solution.tools.Run [--interleave] x.in"` | reference output for one scenario; `--interleave` shows input and output side by side, the hand-review format |

### Changing the protocol or the reference

1. Edit the solution; `sbt "testOnly *ReferenceLedgerSuite"` — a changed rule fails the goldens,
   which is the point.
2. `sbt ledgerGen` to regenerate. **Then hand-review the affected easy/medium/large files** with
   `Run --interleave` and tick them in §8 below. The huge files are only as trusted as the small
   ones.
3. `sbt ledgerVerify` and `sbt checkSolutions`. Keep the worked examples in `ledger/README.md`
   (§7.7 and §10.4 there) in sync — their canonical copies live in `scratch/protocol-example.*`
   and `scratch/protocol2-example.*` and are diffed against the reference.

## 2. The day

| Time | Block |
|---|---|
| 9:00–9:25 | Kickoff: the two-function framing — `decide(state, cmd): Either[Rejection, List[Event]]` / `evolve(state, event): State`, state as `foldLeft(evolve)` — presented by YOU; it is deliberately NOT in the attendee README (removed 2026-10-06: nothing in the starter code shows it, so it belongs to the live discussion). Then a walk through `Harness.run` (it *is* the application), and `sbt ledger` red on everyone's machine |
| 9:25–10:10 | Data modelling on paper. Laptops closed. |
| 10:10–10:30 | Model review, ~3 min per team, with the checklist in §4 |
| 10:30–11:30 | M1 — vertical slice |
| 11:30–12:30 | M2 — rejections and money |
| 12:30–13:15 | Lunch. Progress check: each team's `sbt ledger` is the grade; glance at repos for last commit + green tests. |
| 13:15–14:15 | M3 — journal and replay. The M4/M5 sections of the guide stay unread until 14:15 |
| 14:15–15:30 | M4 — transfers. "You may now read sections 9 and 10." |
| 15:30–16:20 | M5 — projections, or stretch goals |
| 16:20–17:00 | Demos, 5 min per team, then the debrief |

## 3. Things to say out loud, once each

- **There are no hidden tests.** Say it at the briefing and pause on it: every scenario and every
  expected output is in the repo, readable now. It changes how people work — "am I done?" is never
  a question they queue to ask you.
- **Don't read sections 9 and 10 before the afternoon.** The guide says it twice; say it once out
  loud, with the reason: it spoils a design decision they are about to make, and the decision
  teaches more if they make it first. (A team that peeks loses a discovery, not the day.)
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

## 4. Model review — 10:10, three minutes per team

Ask; do not tell. Each question surfaces one mistake without naming it.

| If you see | Ask | Looking for |
|---|---|---|
| One type for commands and facts | "What goes in the log when a withdrawal is refused?" | Nothing — so the log holds a different kind of thing than the request |
| `Double` for money | "Compute `0.1 + 0.2` in the REPL." | `Long` cents, or a `Money` wrapper around one |
| Validation inside `evolve` | "What happens when you replay a log written last Tuesday?" | `evolve` applies, never checks |
| `var` state, `mutable.Map` of accounts | "How would you implement undo?" | Immutable state; a new one per fact |
| `decide` returning a single event | "How many things happen during a transfer?" | Do NOT mention M4. Let the refactor land at 14:15. |
| `trait Aggregate[C, E, S]` | "Make one concrete case work first." | Also: this is precisely the abstraction the course left out |
| Balance stored *and* derivable | "Which one is the truth?" | Either answer is fine; wanting a deliberate one |
| String-typed rejections | "How does the caller tell two of them apart?" | An ADT |
| A model shaped like the wire (`Effect(sign, amount)`) | "If the output format changed tomorrow, how much of your model changes?" | The wire is a projection; a model isomorphic to it skipped the modelling |
| No fact type at all — just `Map[String, Account]` | "Where does the sequence number come from?" | This one is a rewrite, not a refactor. Catch it now or in the first ten minutes of M1. |
| `Account(balance, status)` with `Status.Closed` | "Your closed account still has a balance field — what's in it, and who guarantees it?" | The sum `Active(currency, balance, frozen)` / `Closed(currency)` has no field to guarantee. Either is fine; the question is whether they *saw* the choice. |
| Queries go through the same path as mutations | "Can your types prove a `BALANCE` never writes?" | `Command = Mutation / Query` as a sum of sums; `decide` only ever sees mutations |

Also worth a glance: is status an enum, two booleans, or a nested type? (All defensible — ask why.)
Is the parser separate from `decide`? Does anything in the model mention a string like `"OK"`?

## 5. Debrief (16:40)

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

## 6. Things that will go wrong

| | |
|---|---|
| A team's `execute` prints for comment lines | The stub does too. `m1/easy-1` fails with "expected 5 lines, got 7". Point at guide §6.1. |
| `Double` for money | `0.1 + 0.2`. Then `Long` cents. Ten-minute refactor if caught at review, an hour if caught at M2. |
| Sequence number in a `var` | Passes M1, M2, every golden. `m3` fails only on the "after replaying" check. Ask where the number comes from. |
| `decide` returns one `Event` | Fine until 14:15. Let it land. |
| `trait Aggregate[C, E, S]` | Appears around 10:15. "Make one concrete case work first." This is also the abstraction the course excluded. |
| A team reads the M4/M5 sections before lunch | It happens. They lose a discovery, not the day. |
| `case Balance(acc) \| Freeze(acc) =>` — E024, "Illegal variable in pattern alternative" | Predictable when collapsing the four single-account commands. Idiomatic fix: an abstract `def account` implemented by every case of the enum — common fields of a sum belong on the sum. (Hit during the 2026-10 dry run.) |
| Plain `sbt test` | Runs the day-2 red suites too and stops. `sbt ledger`. |

## 7. ADT leverage — fold in after the dry run

Agreed 2026-10-06 (while Daniel's from-scratch dummy implementation occupies `MyLedger`): the
following go in once the dry run is done and the reference solution is back in place. None of them
touches the protocol or regenerates a golden file. (2026-10-06: the two solution-side items are in —
behavior-preserving, `ledgerVerify` 91/91 and the reference suite green after the refactor. The
remaining unticked items are doc/stretch work.)

- [x] **The closed-account sum — applied as the reference's primary model (2026-10-06).**
  `Account` is now the sum `Active(id, currency, balance, frozen)` / `Closed(id, currency)`; the
  product-with-Status alternative and the "spec design is type design" line live in
  `Account.scala`'s design comment. Bonus the refactor surfaced: `Decide.queryable` now *returns*
  `Account.Active`, so "past the closed check you are holding a balance" is a type, and
  `Wire.balance` takes `Account.Active` — it cannot be called on a closed account.
- [ ] **The staged exhaustiveness demo (~16:00, or stretch #6).** "The PM wants `NOTE <acc> <text>`,
  recorded as a fact." The new `Event` case makes every non-exhaustive match light up — `evolve`,
  the effect renderer, the journal codec, the history delta — and the compiler hands out the
  complete to-do list. Day 1's exhaustivity demo at project scale. Script it in §2/§3 of this file;
  keep it golden-free.
- [x] **Mutation/Query split — applied in the solution (2026-10-06).** `Command` is now
  `sealed trait Command` with `enum Mutation` and `enum Query` under it; `decide` accepts only
  `Mutation`, queries go through `Decide.queryable` alone, so "a query cannot consume a sequence
  number" is a function that cannot be called. The guide stays silent (review-question-only, per
  the §4 rows) — it must not design for them.
- [ ] **Stretch ladder additions** in `ledger/README.md` §11:
  - hand-rolled `NonEmptyList` for `decide`'s result ("an accepted command records at least one
    fact" as a type; loops back to day 1's `MyList`);
  - sharpen the property-test item with the ADT round-trips — `decode(encode(e)) == e` for every
    constructor (writable only because the sum is closed), and
    `replay(journalOf(log)) == log.foldLeft(empty)(evolve)`;
  - the SUMMARY window monoid: `summary(a,b) ⊕ summary(b+1,c) == summary(a,c)` — and one sentence
    connecting it to snapshots (stretch 2): a snapshot is just reassociating the fold.
- [ ] **Totality counterpoint**, one line in §5's debrief notes: `evolve` is total, `Journal.decode`
  is not (corrupt line) — which side of the `Either` boundary you are on is marked by the types.
- [ ] **Deliberately not doing:** phantom-typed currencies, tagged `from`/`to` id types,
  `Aggregate[C, E, S]` — maximal on paper, but they cross the course's no-fancy-features line, and
  the from/to one doesn't even deliver (two values of one wrapper type still swap silently). Keep
  as an honest remark if it comes up at review.

## 8. Review record

Every hand-written scenario's `.out` was read line by line next to its `.in` (side by side with
the interleave view, today `Run --interleave`) against the protocol. Checklist per file: sequence
numbers dense and only on accepted commands · every rejection's code and key order · precedence
cases · money rendering · idempotent FREEZE/UNFREEZE consume a number · queries never do · SUMMARY
arithmetic by hand on at least two windows · HISTORY indentation and order · currency conservation
across transfers where the file claims it.

### 2026-09-01 — initial generation

| File | Reviewed | Notes |
|---|---|---|
| m1 easy-1, easy-2, medium-1, large | ✔ | large: acct-01/04/06 balances re-added by hand |
| m2 easy-1, easy-2, medium-1, medium-2, large | ✔ | large: ann drains to exactly 0.00; all 56 seq |
| m3 easy-1, medium-1, large, replay | ✔ | replay: B resumes at OK 16; frozen/closed state survives |
| m4 easy-1, easy-2, medium-1, large, replay | ✔ | medium-1 is the precedence order, one case per rule; large: USD 1750.03 / EUR 600.00 conserved; replay resumes at OK 13, t3 |
| m5 easy-1, easy-2, medium-1, large | ✔ | medium-1: every SUMMARY matched a hand-derived value; large: straddling windows 12–13, 13–14 checked |
| m1 medium-2, large-2 *(then hidden)* | ✔ | |
| m2 medium-3, large-2 | ✔ | closed+non-positive, frozen+insufficient |
| m3 medium-2, large-2, replay-2 | ✔ | replay-2: B resumes at OK 21 |
| m4 medium-2, large-2, replay-2 | ✔ | both-frozen + mismatch → frozen source; closed source + unknown dest → unknown; USD 2100.00 / EUR 100.00 conserved; replay-2 resumes at OK 13, t4 |
| m5 medium-2, large-2 | ✔ | windows opening between a transfer's two facts |
| Both worked examples | ✔ | diffed against the reference: identical (canonical copies in `scratch/`) |

Huge files (`*/huge.in`, `*/replay-huge-*`): not reviewed by hand, by design. Generator coverage
tables printed at generation; the invariant checks in `tools.Verify` pass on all of them.

### 2026-09-01 — the `var` counter check (regression check on the replay test; not committed)

- Sequence number held in a `var`, reset on `empty`/`replay`: `m3` and `m4` red, *only* on the
  "after replaying the journal" checks; the one-run goldens stay green. `m1`, `m2`, `m5` green.
- Transfer tag held in a `var`, reset the same way: `m4` red only, same shape.
- Both changes reverted; `ReferenceLedgerSuite` green afterwards. **Repeat this check after any
  change to `LedgerSuite.replay`.**

### 2026-09-02 — layout changes (content unchanged)

The hidden set was merged into `src/main/resources/ledger/`: `hidden-medium-1` → `medium-2`
(m2: `medium-3`), `hidden-large` → `large-2`, `hidden-replay-*` → `replay-2-*`; contents
byte-identical apart from headers. `sbt ledgerVerify` (91 checks) green.

### 2026-10-06 — docs consolidated

`ledger/README.md` now contains the whole protocol, split by milestone (PROTOCOL.md and
PROTOCOL-PART2.md folded in; the "don't read before lunch" warning now sits on sections 9–10).
This file absorbed TEACHING_NOTES.md, MODEL_REVIEW.md and REVIEW_LOG.md. Worked examples
re-diffed against their reference-verified canonical copies after the move: identical.
