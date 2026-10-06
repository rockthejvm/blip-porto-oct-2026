# Day 3 candidate — Event-Sourced Account Ledger: implementation plan

Plan for building the artifact described in [day3-ledger-spec.md](day3-ledger-spec.md) (the spec,
verbatim) **inside this repo, on this repo's toolchain, in the day-2 style**. Written 2026-09-01,
revised twice the same day after Daniel's decisions (§0). Section references (§n) point into the
spec.

**Status (2026-09-02, evening): built; day-3 choice still open.** The hidden set no longer exists —
all scenarios live in `src/main/resources/ledger` ("no hidden tests", matching the betting
project's briefing philosophy), and `ledger/README.md` is now the single attendee guide
(MILESTONES.md folded in). Earlier status: All tooling is Scala under
`ledger.solution.tools` (`sbt ledgerGen` / `sbt ledgerVerify`); the shell/python scripts from the
first build were removed as duplicate work, and the broader definition-of-done checks (leak audit,
stripped-copy rehearsal, timing) were retired with them — rerun those judgments by hand before
handout. Mentions of `*.sh` / `*.py` files in the body below are the original plan, kept for the
record; their shipped equivalents are `tools.Verify`, `tools.Run --interleave`, and the aliases.
2026-10-06: all markdown consolidated to exactly two files — `ledger/README.md` (attendee guide
with the full protocol split by milestone) and `notes/ledger/README.md` (trainer guide); the
PROTOCOL/TEACHING_NOTES/MODEL_REVIEW/REVIEW_LOG files named below were folded into those. What exists is
described in `notes/ledger/README.md`; what was reviewed is in `notes/ledger/REVIEW_LOG.md`. Deviations
from the plan during the build: `HiddenReferenceSuite` sits in the same test file as the reference suite;
`ReplaySuite` is a method of `LedgerSuite`, not a separate class; the leak audit checks type names in
code only and a short list of model-revealing words in prose (plain English like "Account" is allowed).

**For a future session:** read the spec end to end, then this file. Where this plan and the spec
disagree, this plan wins — every disagreement is listed in §0.

---

## 0. Decisions taken (Daniel, 2026-09-01)

The spec was written by a session that did not know this repo and optimised for a standalone
handout. Daniel's steer: **keep the teaching content, drop the ceremony.**

1. **One of several day-3 candidates.** The bet settlement engine stays intact, renamed to
   `betsettlement` (directory, package `com.rockthecode.betsettlement`,
   `notes/betsettlement-briefing.md`). `training-plan.md §5` / `TODO.md` still describe it as
   "the" day 3; untouched until a candidate is chosen.
2. **Same repo, same toolchain.** Scala 3.8.4, sbt 2.0.7, MUnit 1.2.0, attendees' existing JDK.
   Spec §4 (3.3 LTS / sbt 1.10 / JDK 17) is void. Cask stays in `build.sbt` for betting M5; the
   ledger never imports it. Reference solution lives in a `solution` package like
   `day2.solutions` — same open question about stripping before handout, higher stakes here.
3. **One `Main`, one engine to swap.** `Main` runs a scenario file through whatever
   `LedgerEngine` it is pointed at; attendees change one line to point it at their own object.
   Journal support is part of the single trait with no-op defaults, so nothing needs a second
   trait or a runtime type check.
4. **One test file, one test per milestone, every scenario size, no flags.** No tags, no
   `-D` properties, no fast/huge split. `sbt ledger` runs the suite; the m1..m5 tests sit red
   until reached, exactly like day-2 stretch tests.
5. **Scenarios are resources, grouped by milestone.** `src/main/resources/ledger/m1/…`, read by
   the suite as plain directories. No prefixes, no special cases in code.
6. **Naming: pick one and stay consistent.** Files: `<tier>-<n>.in` / `.out`; replay pairs
   `replay-a.in`, `replay-b.in`, `replay.out`. Model names are the reference's business. The spec's
   mechanical "no PascalCase in `.out`" check is dropped — the reference guarantees the wire
   never names an event by construction.
7. **The reference picks one model** and renders the lossy wire format from it. Attendees decide
   their own.
8. **Broken variants** (spec §17: two `var`-counter engines committed as regression checks on
   the replay test) — **not committed.** The build session verifies once, by hand, that the
   replay test goes red on a `var`-held counter, notes the result in `REVIEW_LOG.md`, and
   deletes the variant. See §2.8.
9. **`SUMMARY` invariant** asserted in the reference — kept.
10. **Generator determinism** rules — kept.
11. **Transfers move to M4.** `m3/replay-*` is transfer-free; `m4/` gets its own replay pair that
    carries the transfer-counter trap.
12. **No provided-code size limit.**

Spec → repo mapping:

| Spec | Here |
|---|---|
| `/student/` | `ledger/` (docs) + `src/main/scala/com/rockthecode/ledger/` + `src/main/resources/ledger/` |
| `/instructor/solution/` | `src/main/scala/com/rockthecode/ledger/solution/` (stripped/branched before handout) |
| `/instructor/hidden-scenarios/`, teaching notes, `grade.sh`, `verify.sh` | `notes/ledger/` (already trainer-internal, already slated to be stripped) |
| `/scripts/gen-huge.scala`, `gen-golden.sh` | `…ledger/solution/tools/GenHuge.scala`, `GenGolden.scala`, run with `sbt runMain`; thin wrappers in `notes/ledger/scripts/` |
| `JournaledLedger` trait | two defaulted methods on `LedgerEngine` |
| `GoldenSpec` + `ReplaySpec`, tags, `--include-tags`, `testOnly -- *m2*` | `LedgerSuite` with tests `m1`…`m5`; `sbt ledger` |

---

## 1. Target layout

```
ledger/                                          # attendee-facing docs
  README.md            §11 brief
  MILESTONES.md        §10 (M3 transfer-free; M4 includes its replay pair)
  PROTOCOL.md          §7 for OPEN DEPOSIT WITHDRAW BALANCE FREEZE UNFREEZE CLOSE
  PROTOCOL-PART2.md    TRANSFER HISTORY SUMMARY — "don't read before lunch"

src/main/resources/ledger/                       # scenarios, grouped by milestone
  m1/  easy-1.in easy-1.out  easy-2.in easy-2.out  medium-1.in medium-1.out  large.in large.out  huge.in huge.out
  m2/  same shape (+ medium-2 for the ERROR lines)
  m3/  easy-1 medium-1 large huge (golden, M1+M2 vocabulary, exercises nothing new — regression)
       replay-a.in replay-b.in replay.out          (+ replay-huge-a.in replay-huge-b.in replay-huge.out)
  m4/  easy-1 easy-2 medium-1 large huge  +  replay-a.in replay-b.in replay.out
  m5/  easy-1 easy-2 medium-1 large huge

src/main/scala/com/rockthecode/ledger/
  LedgerEngine.scala   the contract (§2.1)
  Harness.scala        run / runJournaled (§2.2)
  Main.scala           runMain com.rockthecode.ledger.Main <scenario-file> [journal-file]; `val ledger = MyLedger` is the swap point
  MyLedger.scala       failing stub, verbatim from §6
  domain/README.md     three sentences
  solution/            TRAINER ONLY
    Money.scala Currency.scala Account.scala Command.scala Event.scala Rejection.scala Bank.scala
    Parser.scala Decide.scala Evolve.scala Projections.scala Wire.scala ReferenceLedger.scala
    tools/GenGolden.scala tools/GenHuge.scala

src/test/scala/com/rockthecode/ledger/
  LedgerSuite.scala    abstract class LedgerSuite(engine, dir); class MyLedgerSuite; class ReferenceLedgerSuite

notes/ledger/                                    # trainer-internal
  TEACHING_NOTES.md  MODEL_REVIEW.md  REVIEW_LOG.md  README.md (for Daniel)
  hidden/m1 … m5     same file convention, prefix-free
  scripts/  gen-golden.sh  gen-huge.sh  check-outs.sh  grade.sh <repo>  verify.sh
```

`build.sbt`: `addCommandAlias("ledger", "testOnly com.rockthecode.ledger.MyLedgerSuite")`;
`checkSolutions` extended with `*ReferenceLedgerSuite`. No new dependencies.

---

## 2. Design decisions, with reasoning

### 2.1 One trait, journal methods defaulted

```scala
trait LedgerEngine[S]:
  def empty: S
  def execute(state: S, line: String): (S, List[String])
  // Milestone 3. Until you override these, the journal is a no-op.
  def journalLines(before: S, after: S): List[String] = Nil
  def replay(lines: List[String]): S = empty
```

*Why.* The spec's separate `JournaledLedger` trait forced `Main` and the test to check at runtime
whether `MyLedger` had reached M3 yet. With defaults, the stub compiles on day one, `Main` and the
suite call `replay`/`journalLines` unconditionally, and a team that has not done M3 gets a
natural failure: replay from `empty` restarts the sequence numbers, and the M3 test's diff shows
it. The defaults reveal nothing the original trait did not.

### 2.2 One journaled loop, two callers

`Harness.runJournaled[S](engine, start, input): (S, List[String], List[String])` — final state,
output, journal lines (collected via `journalLines(before, after)` per input line). `Main` with a
journal argument: read the file if present → `replay` → `runJournaled` → append the journal
lines. The M3/M4 tests use the same function, so the test exercises the loop the app runs.

### 2.3 One suite, one test per milestone

```scala
abstract class LedgerSuite(engine: LedgerEngine[?], dir: Path) extends munit.FunSuite:
  test("m1") { golden("m1") }
  test("m2") { golden("m2") }
  test("m3") { golden("m3"); replay("m3") }
  test("m4") { golden("m4"); replay("m4") }
  test("m5") { golden("m5") }

class MyLedgerSuite        extends LedgerSuite(MyLedger,        Path.of("src/main/resources/ledger"))
class ReferenceLedgerSuite extends LedgerSuite(ReferenceLedger, Path.of("src/main/resources/ledger"))
```

- `golden(m)`: every `*.in` with a sibling `.out` in `dir/m`, any size; `Harness.run`; compare
  after per-line `rstrip` and trailing-blank trimming. Collect failures across all files, then
  fail once with, per failing file, `first difference at line k` + expected/actual + up to 5
  further differing lines + `… and N more`. Never a full dump.
- `replay(m)`: `replay-a.in`, `replay-b.in`, `replay.out` (expected output of A++B in one pass).
  Asserts (1) single pass A++B equals `replay.out`; (2) `runJournaled` over A → write to a temp
  file → read back → `replay` → `runJournaled` over B equals the B-tail of `replay.out`.
  Check (1) exists because (2) alone is vacuously true for the stub (a constant engine agrees
  with itself), and it keeps `replay.out` honest as a golden file.
- Files are listed from the filesystem (`Files.list`), cwd = repo root, as `betsettlement/check`
  already assumes. No classpath resource listing.
- Hidden scenarios: the same abstract class, `ReferenceLedgerSuite` gets a second instantiation
  over `notes/ledger/hidden` (`HiddenReferenceSuite`, in the same test file, part of
  `checkSolutions`). `grade.sh` copies `notes/ledger/hidden/mN/*` into a temp copy of a team's
  `src/main/resources/ledger/mN/` and runs `sbt ledger` — more files in the same directories,
  no code path for it.

### 2.4 `Main`

`runMain com.rockthecode.ledger.Main <scenario-file> [journal-file]`. Prints output lines. With a
journal file: replay it first (if it exists), append after. The only line an attendee edits:
`val ledger: LedgerEngine[?] = MyLedger`. Usage + exit 2 on bad args. No other flags.

### 2.5 Scenario files

`src/main/resources/ledger/<mN>/<tier>-<n>.in` + `.out`, tiers `easy`, `medium`, `large`,
`huge`; `large` and `huge` are unnumbered. Replay: `replay-a.in`, `replay-b.in`, `replay.out`
(and `replay-huge-*` in m3). Hidden set uses identical names under `notes/ledger/hidden/`.
Every hand-written `.in` opens with `# purpose:` / `# exercises:` comment lines. Cumulative:
M(n) files use M(<n) commands too.

### 2.6 Naming conventions in the reference

`enum Event { Opened, Deposited, Withdrawn, Frozen, Unfrozen, Closed, TransferDebited,
TransferCredited }` — separate events, stated once at the top of `Event.scala` with the comment
that a single `Adjusted(delta)` is equally valid and the wire cannot tell (§7.3). `enum Rejection`
cases named after the wire codes in PascalCase (`UnknownAccount`, `InsufficientFunds`, …);
`Wire.scala` maps them to kebab-case. `Bank(accounts: Map[String, Account], events: Vector[Event])`.

### 2.7 Lossy output on purpose

The wire shows a deposit, a withdrawal and both halves of a transfer as signed adjustments. The
reference renders from its richer model and drops the distinction; `Wire.effect` is the one
place that happens. Not a constraint on attendees; a fact about the reference.

### 2.8 Counters derived from the log; the one-off `var` check

Two numbers on the wire cannot be computed without the full history: the global sequence number
(`OK 37 …`) and the transfer tag (`xfer=t4`). The reference derives both from `Bank.events`
(`events.size + 1`; count of `TransferDebited`) and stores neither.

The spec asked for two engines with those counters in a `var` to be *committed* as proof that the
replay test catches the bug. Daniel: not committed. Instead, during step 6, the build session
temporarily changes `ReferenceLedger` to hold each counter in a `var`, runs `ReferenceLedgerSuite`,
confirms m3 (sequence) and m4 (transfer) go red while the golden checks stay green, records that
in `REVIEW_LOG.md`, and reverts. Ten minutes, nothing shipped.

`Account.balance` is stored although derivable — commented as "a cache of the fold" (§14 review
item "which one is the truth?").

### 2.9 `SUMMARY` invariant

`Projections.summary` computes `closing` both ways (§7.6) and `require`s equality. A wrong
reference crashes golden generation rather than committing a wrong `.out`. `check-outs.sh`
re-checks the arithmetic over every committed `.out` file.

### 2.10 Generator determinism

`scala.util.Random(seed)` is fine (fixed algorithm). The trap is iterating a `Map`/`Set` —
hash order can change across Scala versions — so `GenHuge` iterates only `Vector`s and sorted
keys. One hard-coded seed per output file. `verify.sh` regenerates to a temp dir and `cmp`s.

### 2.11 Transfers are M4

`m3/replay-*` uses only M1+M2 commands; A records ≥12 facts, B more. `m4/replay-*` repeats the
shape with ≥1 successful TRANSFER on each side (the `t1`-twice trap). `MILESTONES.md`: "M3 green
means the m3 test is green; M4 makes the m4 test — including its replay check — green."

---

## 3. Build steps

### Step 1 — attendee-facing skeleton
- `LedgerEngine`, `Harness`, `Main`, `MyLedger`, `domain/README.md`; empty
  `src/main/resources/ledger/m1..m5/`; `LedgerSuite` with the five tests (they report "no
  scenarios in m1" until step 4); `ledger` alias.
- Check: `sbt compile` green; `sbt ledger` shows five red tests with readable messages.

### Step 2 — reference solution (the big one)
Files per §1 layout. Contents:

| File | Content |
|---|---|
| `Money.scala` | `case class Money(cents: Long)`; `+`, `-`, comparison; `Money.parse: Option[Money]` for `-?\d+(\.\d{1,2})?` |
| `Currency.scala` | `enum Currency { USD, EUR, GBP }`, `fromCode` |
| `Account.scala` | `case class Account(id, currency, balance, status)`; `enum Status { Open, Frozen, Closed }` |
| `Command.scala` | one case per §7.1 verb |
| `Event.scala` | §2.6 |
| `Rejection.scala` | cases + payloads matching §7.4 |
| `Bank.scala` | `Bank(accounts, events)`, `empty`, `nextSeq`, `transfersSoFar`, `account(id)` |
| `Parser.scala` | `enum ParseError` (7 cases); `parse(line): Option[Either[ParseError, Command]]`, `None` for blank/comment; verb → arity → args left to right; `bad-range` after both sequences parse |
| `Decide.scala` | `decide(bank, cmd): Either[Rejection, List[Event]]`; §7.5 order as an explicit chain, commented |
| `Evolve.scala` | `evolve(bank, event): Bank`, total |
| `Projections.scala` | `history`, `summary` (accumulator fold + both-closings `require`), `balance` |
| `Wire.scala` | every protocol string; nothing else builds output |
| `ReferenceLedger.scala` | `object ReferenceLedger extends LedgerEngine[Bank]`; `execute` = parse → query? project : decide → fold evolve → render; `journalLines` = new events, one per line, hand-written format; `replay` = parse + `foldLeft(evolve)` |

- No `given`/`using`/`implicit`, no `var`, no Cask, no `Future`, no `betsettlement` imports.
- Check: §7.3 + §7.6 worked examples byte-exact via `Main`; a 20-line scratch precedence
  scenario matches a hand-derived expectation.

### Step 3 — `GenGolden`
`runMain com.rockthecode.ledger.solution.tools.GenGolden <dir>…` writes `X.out` for every `X.in`
(and `replay.out` from `replay-a.in ++ replay-b.in`); `--check` diffs against committed.
`notes/ledger/scripts/gen-golden.sh` wraps it for `src/main/resources/ledger` and
`notes/ledger/hidden`.

### Step 4 — hand-written scenarios

| File | Purpose |
|---|---|
| `m1/easy-1` | one account: open, three deposits, balance |
| `m1/easy-2` | two accounts interleaved; `unknown-account` on deposit and balance; `account-exists` |
| `m1/medium-1` | three accounts, all currencies, blank/comment lines mid-file, `1`/`1.5`/`1.50`, `unknown-account` before any OPEN, seq continuity across rejections |
| `m1/large` | 6 accounts, ~120 lines, round-robin deposits, rejections interleaved |
| `m2/easy-1` | withdraw happy path; `insufficient-funds`; `non-positive-amount` for 0 and negative |
| `m2/easy-2` | freeze/unfreeze/close lifecycle; `account-frozen`, `account-closed`, `non-zero-balance` |
| `m2/medium-1` | every M2 rejection once; idempotent FREEZE/UNFREEZE consume seq; BALANCE on frozen allowed; re-OPEN after CLOSE; `unsupported-currency` |
| `m2/medium-2` | every `ERROR` variant; in-line precedence (unknown verb > arity > args; lowercase verb echo); malformed lines between valid ones |
| `m2/large` | ~150 lines, 5 accounts, precedence combinations, errors interleaved |
| `m3/easy-1`, `medium-1`, `large` | M1+M2 vocabulary; regression only, nothing new |
| `m3/replay-a`, `replay-b` | transfer-free; A ≥12 facts, B more; B queries A's accounts |
| `m4/easy-1` | two accounts, one transfer, both balances |
| `m4/easy-2` | `same-account-transfer`, `currency-mismatch`, transfer `insufficient-funds` |
| `m4/medium-1` | every TRANSFER rejection in §7.5 order with ordering proofs |
| `m4/large` | ~180 lines, 6 accounts, tags past `t10`, frozen both directions, M2 commands interleaved |
| `m4/replay-a`, `replay-b` | ≥1 successful TRANSFER each side, plus M2 traffic |
| `m5/easy-1` | the §7.6 worked example exactly |
| `m5/easy-2` | HISTORY with 0 facts; SUMMARY over an empty window |
| `m5/medium-1` | SUMMARY windows full/prefix/suffix/mid/empty/from==to; HISTORY after transfers; `bad-sequence`, `bad-range` |
| `m5/large` | ~150 lines, all commands, SUMMARY windows straddling transfers |

Hidden (`notes/ledger/hidden/mN/`): `medium-1` + `large` per milestone plus `m3`/`m4` replay
pairs, covering spec §10's list item by item.

### Step 5 — hand review
Per easy/medium/large `.out` (student + hidden), side by side with its `.in`, a ticked checklist
in `notes/ledger/REVIEW_LOG.md`: seq continuity · every rejection's code and key order ·
precedence cases · money rendering · idempotent FREEZE/UNFREEZE consumed a seq · queries consumed
none · SUMMARY arithmetic by hand on two windows per file · HISTORY indentation/order.
`check-outs.sh` (seq density, SUMMARY arithmetic) runs first. Discrepancy → fix reference →
regenerate all → re-review affected files.

### Step 5a — `GenHuge`
Seeded, milestone-aware (verb sets M1 ⊂ M2 = M3 ⊂ M4 ⊂ M5), weighted 55/30/10/5, crafts each
line against the current reference state to hit the least-covered rejection/ERROR, asserts every
reachable reason ≥2 before writing, prints the coverage table. Emits `mN/huge.in` for m1–m5 and
`m3/replay-huge-a/b`. Check: two runs `cmp` identical; then `gen-golden.sh`.

### Step 6 — suite finished, `var` check
- `LedgerSuite` per §2.3 with the truncated diff. `ReferenceLedgerSuite` + `HiddenReferenceSuite`.
- Stub: five red tests, each message names the first failing file. Reference: all green.
- The §2.8 one-off: `var` sequence → m3 red, `var` transfer → m4 red, goldens green; log; revert.
- Time `sbt ledger` with the reference; record it.

### Step 7 — documents
- `ledger/README.md` in §11 order (two bare signatures only; JPY note; `sbt ledger`; `runMain`
  line; where code goes; design on paper first; pointer to `PROTOCOL.md`).
- `ledger/PROTOCOL.md` (§7 minus TRANSFER/HISTORY/SUMMARY/`bad-sequence`/`bad-range`) and
  `ledger/PROTOCOL-PART2.md`, each with a worked example.
- `ledger/MILESTONES.md`: §10 with M3 transfer-free and cuttable, M4 with its replay pair,
  stretch goals in order, no implementation guidance.
- `notes/ledger/TEACHING_NOTES.md` (§13–15 + the "negatives parse" and "kebab-case blocks
  transcription, not knowledge" notes), `MODEL_REVIEW.md` (§14 one page), `README.md` (strip the
  solution package; `grade.sh` at lunch; regenerating goldens).
- Nothing attendee-visible links to `notes/` or `solution/`.

### Step 8 — `verify.sh`
1. `sbt compile`; `sbt ledger` → 5 failures, no `Exception` text.
2. `sbt "testOnly *ReferenceLedgerSuite *HiddenReferenceSuite"` → green.
3. `check-outs.sh`: `OK <seq>` dense 1..n per `.out`; SUMMARY arithmetic.
4. `gen-huge.sh --check` byte-identical; `gen-golden.sh --check` byte-identical.
5. Every `mN/` has easy/medium/large/huge (+ replay files for m3, m4).
6. Leak grep over attendee-visible files (§16.9 list + every type name harvested from
   `solution/`); allowed hits only in `ledger/README.md`'s two signature lines.
7. Forbidden tokens in attendee-visible ledger code: `given`, `using`, `implicit`, `cask`, `Future`.
8. `grade.sh` against a temp copy with `Main`'s swap line pointed at `ReferenceLedger` (all
   green) and against the stub (all red).
9. `REVIEW_LOG.md` ticks every easy/medium/large file and the §2.8 check.

### Step 9 — dress rehearsal
Copy the repo to a temp dir, delete `solution/` and `notes/`, `sbt compile`, `sbt ledger`:
what an attendee sees on minute one.

### Step 10 — if the ledger is chosen
Rewrite `training-plan.md §5` and `TODO.md` for it; `TEACHING_NOTES.md` becomes the briefing.
Not done unless told.

---

## 4. Pitfalls

- Trailing whitespace: reference emits none; suite strips anyway. HISTORY indentation is two
  spaces — `rstrip` only.
- `bad-arguments` before `bad-*`: `OPEN acc-1` → `bad-arguments OPEN`.
- `non-positive-amount` renders the parsed amount (`amount=-5.00`).
- Idempotent FREEZE/UNFREEZE consume a seq; queries never do; closed accounts reject `BALANCE`.
- `bad-range` is a parse error and beats `unknown-account`.
- Replay check (2) is vacuous alone — always paired with the golden check (1) (§2.3).
- Solution leakage: the same stripping decision as day 2, settle before handout.
- Plain `sbt test` is not a signal in this repo; docs point at `sbt ledger`.

## 5. Effort

| Step | Effort |
|---|---|
| 1 skeleton | 45 min |
| 2 reference | 3–4 h |
| 3 GenGolden | 30 min |
| 4 scenarios (~28 files) | 3 h |
| 5 hand review | 2 h |
| 5a GenHuge | 2 h |
| 6 suite + var check | 1.5 h |
| 7 docs | 2 h |
| 8–9 verify + rehearsal | 1 h |

Two focused sessions. Steps 2 and 4–5 are where mistakes cost student-hours.
