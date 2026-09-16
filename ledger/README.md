# The ledger — project guide

*This document assumes you know nothing about the project. It, plus the two protocol documents it
points to, is the complete specification of what you are building today.*

---

## 1. The goal

You are building a **command-line ledger for bank accounts** — the core of the kind of system that
sits behind a bank, a broker, or a payments platform.

The program reads a scenario file of commands, one per line:

```
OPEN acc-1 USD
DEPOSIT acc-1 100.00
WITHDRAW acc-1 30
BALANCE acc-1
```

and prints one response per command:

```
OK 1 acc-1 OPENED USD
OK 2 acc-1 +100.00 100.00
OK 3 acc-1 -30.00 70.00
BALANCE acc-1 70.00 USD
```

Three kinds of response exist, and telling them apart is most of the design:

- A command that is **accepted** records one or more *facts* — things that are now permanently
  true — and prints one `OK` line per fact.
- A command that is **rejected** records nothing and prints one `REJECTED` line with a precise
  reason (`insufficient-funds`, `account-frozen`, ...).
- A line that is not even a well-formed command records nothing and prints one `ERROR` line.

Every fact carries a **global sequence number**: 1 for the first fact the ledger ever records, then
2, 3, 4... across all accounts, forever. Rejections, queries and errors never consume one. That
little number does a lot of work today, as you will discover.

## 2. What "done" looks like

By the end of the day your team has a ledger that:

- handles accounts in three currencies, with deposits, withdrawals, freezing and closing;
- rejects every invalid operation with the exact reason, choosing the right one when several apply;
- **survives being restarted**: it writes what happened to a journal file and rebuilds its entire
  state from that file alone;
- moves money **atomically between two accounts**, with both sides recorded and tagged;
- answers **historical queries** over its own past — the full history of an account, and aggregated
  statements over any window of sequence numbers;
- passes a test suite of several thousand scenario lines, byte for byte.

That is not a toy. The shape you will have built — requests validated into immutable facts, state
folded from the facts, views computed from the facts — is event sourcing, and it runs real
financial back-ends, Akka/Pekko persistence, Redux, and Kafka-based systems. The debrief will make
those connections; your job until then is just to build a ledger that works.

## 3. How the project works

### The two functions

The entire engine is two pure functions, and everything you write today is one of them or a helper
for one of them:

```scala
def decide(state: State, cmd: Command): Either[Rejection, List[Event]]
def evolve(state: State, event: Event): State
```

- `decide` looks at a request and the current state, and either refuses the request (with a
  reason) or answers with the list of facts it produces. This is the ONLY place validation happens.
- `evolve` applies one fact that has already happened. It is total: it never fails, never checks,
  never says no. Facts are history; history does not argue back.

The state after any sequence of facts is `facts.foldLeft(empty)(evolve)` — state is a *fold*.
If that sentence sounds like day 2, that is not a coincidence.

What `State`, `Command`, `Rejection` and `Event` actually are is **your decision** — the four
names above are a shape, not provided types. Designing them is the first hour of the day.

### What is provided

Four short files in `src/main/scala/com/rockthecode/ledger/` — read them, they total about a
hundred lines, and do not change them:

| File | What it is |
|---|---|
| `LedgerEngine.scala` | The one interface between your code and everything else: `empty`, `execute(state, line)`, and two journal methods (defaulted to no-ops) that you implement in milestone 3. `S`, the state type, is yours; the harness never looks inside it. |
| `Harness.scala` | Runs input lines through an engine by folding `execute` over them. This is the whole application — notice there is no `var`, no mutation and no I/O in it. |
| `Main.scala` | The command-line app: reads a scenario file, prints the responses. Takes an optional journal file. The single line `val ledger = MyLedger` is where it is told which engine to run. |
| `MyLedger.scala` | A stub engine that answers `ERROR NotImplemented` to everything. **This is what you replace.** |

Your code goes in two places:

- `src/main/scala/com/rockthecode/ledger/domain/` — your data model. Nothing outside this package
  should know how you represented an account.
- `MyLedger.scala` — wire your model to the `LedgerEngine` interface: parse the line, `decide`,
  fold `evolve`, render the output.

### How you know you are done: the scenarios

`src/main/resources/ledger/` has one directory per milestone. Each contains scenario files: an
`.in` file of commands and, next to it, an `.out` file with the exact expected output.
**There are no hidden tests.** Every file the tests run is in the repo, and you are welcome to read
any of them — including before you start.

Each milestone comes as a ladder:

| Tier | Size | What it is for |
|---|---|---|
| `easy-*` | ~10 lines | The happy path. First thing to get green. |
| `medium-*` | 30–60 lines | Every rule of the milestone, exercised at least once. Readable. |
| `large*` | 100–200 lines | Everything interleaved. Where bookkeeping mistakes surface. |
| `huge` | 1000 lines | Machine-generated traffic. Never read it; if the others pass, this one tells you whether your bookkeeping *really* holds up. |

Run the tests with:

```bash
sbt ledger                                                           # all milestones
sbt "testOnly com.rockthecode.ledger.MyLedgerSuite -- *m2*"           # one milestone
```

There is one test per milestone. A failing test names each failing scenario and shows the first
differing lines — expected vs. yours. Tests for milestones you have not reached yet are simply red;
that is normal all day.

To run your program by hand on any scenario (or one you write yourself):

```bash
sbt "runMain com.rockthecode.ledger.Main src/main/resources/ledger/m1/easy-1.in"
sbt "runMain com.rockthecode.ledger.Main my-scenario.txt my.journal"   # milestone 3: with a journal
```

### The protocol documents

Every character of the input and output format is specified — the tests compare byte for byte, so
"almost" does not exist today:

- **[PROTOCOL.md](PROTOCOL.md)** — the commands, the money format, the `OK`/`REJECTED`/`ERROR`
  lines and the rejection precedence for milestones 1–3. Read it before you design.
- **[PROTOCOL-PART2.md](PROTOCOL-PART2.md)** — three more commands for milestones 4–5.
  **Do not read it before lunch.** It is in the repo because hiding files from engineers is silly,
  but it will spoil a design decision you are about to make on paper, and the decision teaches
  more if you make it first.

## 4. Constraints

- Scala standard library plus MUnit. No other dependencies of any kind.
- No `given`/`using`, no implicits, no higher-kinded types, no macros. If you catch yourself
  designing `F[_]` or `trait Aggregate[C, E, S]`, stop: make the concrete thing work.
- No `Future`, no threads, no clocks, no randomness — output must be deterministic.
- `enum` or `sealed trait` for your data types; either is fine.
- Money is not a `Double`. (Compute `0.1 + 0.2` in a REPL before arguing.)
- Supported currencies are exactly `USD`, `EUR`, `GBP`, all with two decimal places. `JPY` and
  other zero-decimal currencies are out of scope on purpose; do not rabbit-hole there.

## 5. Milestone 0 — the model, on paper (9:25–10:10)

**Goal.** Design your data model before touching a keyboard. Laptops closed.

**What to decide.** What is a command? What is a fact? Are they the same type? (Think about it —
one of them can be refused and one of them cannot.) What does the state have to remember, and what
can it recompute from the facts? Where does the sequence number live? What is money? What are the
possible conditions of an account, and what type expresses "exactly one of these at a time"?

**Check.** The trainer comes round at 10:10 for three minutes per team and asks questions about
your model. There is no code to show; a photo of a whiteboard is the artifact.

**You are done when** every member of the team can answer: "what happens, type by type, when the
line `DEPOSIT acc-1 50` arrives?"

## 6. Milestone 1 — a vertical slice (10:30–11:30)

**Goal.** The whole pipeline — parse, decide, evolve, render — working end to end for the three
simplest commands. Narrow but complete.

**Commands.** `OPEN <acc> <CUR>`, `DEPOSIT <acc> <amount>`, `BALANCE <acc>`.

**Rules.**
- `OPEN` creates an account with balance `0.00`; opening an id that already exists is
  `REJECTED account-exists`.
- `DEPOSIT` on an account that does not exist is `REJECTED unknown-account`; otherwise it records a
  fact and the `OK` line shows the amount *and the resulting balance*.
- `BALANCE` prints the balance; it is a query, so it records nothing and consumes no sequence
  number. On an unknown account it is `REJECTED unknown-account`.
- Sequence numbers: 1, 2, 3, ... over accepted commands only. A rejection in the middle must not
  leave a gap.
- Blank lines and `#` comment lines produce no output at all. Exact formats: PROTOCOL.md §1–§3.

**Checks.** `m1/easy-1` and `easy-2` (happy path, first rejections), `medium-1` and `medium-2`
(every M1 rule, odd-but-legal inputs like `1.5` and 32-character ids), `large` and `large-2`
(six to eight accounts interleaved), `huge`.

**You are done when** the `m1` test is green. Expect the resulting-balance column and the "queries
don't consume a sequence number" rule to be what bites; both are visible in the first ten lines of
any diff.

## 7. Milestone 2 — money and refusals (11:30–12:30)

**Goal.** The full single-account rule set: every way a command can be refused, in the right order,
plus malformed input handled without crashing.

**Commands.** Add `WITHDRAW <acc> <amount>`, `FREEZE <acc>`, `UNFREEZE <acc>`, `CLOSE <acc>`.

**Rules.**
- `WITHDRAW` needs sufficient funds: otherwise `REJECTED insufficient-funds` with the balance and
  the requested amount in the message.
- Amounts must be positive: `0` or negative is `REJECTED non-positive-amount` — for deposits too.
  Note that `-5.00` *parses* fine; refusing it is `decide`'s job, not the parser's.
- A **frozen** account refuses `DEPOSIT` and `WITHDRAW` (`account-frozen`) but still answers
  `BALANCE` and still allows `UNFREEZE` and `CLOSE`. `FREEZE` on an already-frozen account (and
  `UNFREEZE` on a non-frozen one) is an *idempotent success*: it records a fact, prints `OK`,
  consumes a sequence number.
- `CLOSE` requires balance exactly `0.00` (`non-zero-balance` otherwise) and is **terminal**: every
  later command on that account — including `BALANCE` and `DEPOSIT` — is `account-closed`, and
  re-`OPEN`ing the id is `account-exists`.
- **Precedence**: when several rejections apply (a frozen account with insufficient funds, say),
  exactly one is printed, chosen by the fixed order in PROTOCOL.md §5. The golden files are
  unambiguous about this; so is the document.
- Malformed lines print one `ERROR` line — unknown verb first (echoed exactly as typed, so
  `deposit` is `ERROR unknown-command deposit`), then wrong argument count, then bad arguments left
  to right. They never touch state. PROTOCOL.md §7.

**Checks.** `m2/easy-1` (withdrawals and amount rules), `easy-2` (the lifecycle), `medium-1`
(every rejection at least once), `medium-2` (every `ERROR` variant and their in-line precedence),
`medium-3` (precedence pairs in combination: frozen *and* broke, closed *and* negative),
`large`, `large-2`, `huge`.

**You are done when** `m2` is green and `m1` is still green.

## 8. Milestone 3 — the journal (13:15–14:15)

**Goal.** The ledger survives a restart. State is rebuilt from a file of everything that ever
happened — and nothing else.

**Commands.** None new. You override the two defaulted methods on `LedgerEngine`:

```scala
def journalLines(before: S, after: S): List[String]   // what to append after one input line
def replay(lines: List[String]): S                     // rebuild the state from all appended lines
```

**Rules.**
- The journal format is **your choice**. Nothing ever reads it except your `replay`.
- After every input line, `Main` (and the test) appends whatever `journalLines` returns — usually
  one line per newly recorded fact, and nothing for rejections and queries.
- `replay` of everything written so far must produce a state from which the ledger behaves
  *identically* to one that never restarted. Identically includes the sequence number and — think
  ahead — anything else that counts.

**Checks.** The `m3` test does two things:
1. Golden checks as usual (`m3/easy-1`, `medium-1`, `medium-2`, `large`, `large-2`, `huge` — no
   new commands, so these are regression files).
2. The **replay check**, on the `replay-a.in`/`replay-b.in` pairs: it runs A from empty, writes
   your journal lines to a real temp file, reads them back, `replay`s them into a fresh state, runs
   B from that state — and the output of B must equal the corresponding part of `replay.out`,
   which was produced by running A and B in one uninterrupted go.

**You are done when** `m3` is green. If B's output starts with `OK 1` when the file expects
`OK 16`, your sequence number did not survive the restart — which is the single most instructive
failure of the day. Ask yourself where that number should come from.

**If you are behind, cut this milestone.** Leave `m3` red and go to M4 — transfers teach more than
file I/O. Come back if there is time.

## 9. Milestone 4 — transfers (14:15–15:30)

**Goal.** Money moves between accounts atomically: one command, two facts, both or neither.

**Commands.** Add `TRANSFER <from> <to> <amount>`. You may now read
[PROTOCOL-PART2.md](PROTOCOL-PART2.md) §1, which is the full specification of this command.

**Rules** (details and exact precedence in PART2):
- A successful transfer records **two facts** — a debit on the source, then a credit on the
  destination — printed as two `OK` lines sharing a tag: `xfer=t1`, `xfer=t2`, ... numbered by
  *successful* transfers over the ledger's whole history. Rejected transfers get no tag.
- Both accounts must exist, be un-closed and un-frozen (in either direction), hold the same
  currency, and the source must have the funds. Ten different rejections can apply; the precedence
  list in PART2 says which one wins. `transfer to yourself` is refused before anything is even
  looked up.
- There is no partial transfer. If anything is wrong, nothing happened.

**Checks.** `m4/easy-1`, `easy-2`, `medium-1` (every transfer rejection in precedence order),
`medium-2` (the combinations: both ends frozen, closed source vs unknown destination),
`large`, `large-2`, `huge` — plus its own replay pairs (`replay-a/b`, `replay-2-a/b`,
`replay-huge-a/b`): after a restart the *transfer tag* must continue too, not just the sequence
number. If you skipped M3, the replay part of `m4` stays red; the golden part still tells you
whether transfers themselves work.

**You are done when** `m4` is green (or green except the replay check, if you cut M3), and
`m1`/`m2` still are. If your `decide` could only ever return a single fact: this is the milestone
that tells you why the signature you were given returns a list.

## 10. Milestone 5 — projections (15:30–16:20)

**Goal.** The ledger answers questions about its own past, computed from the facts alone.

**Commands.** Add `HISTORY <acc>` and `SUMMARY <acc> <fromSeq> <toSeq>` (PART2 §2–§3).

**Rules.**
- `HISTORY` prints a header with the count of the account's facts, then one indented line per fact
  — reusing exactly the same `<seq>` and effect text as the original `OK` lines, transfer tags
  included. If you rendered `OK` lines with a function, you already have this.
- `SUMMARY` aggregates the account's facts inside an inclusive window of *global* sequence numbers:
  opening balance before the window, credits and debits within it, closing balance, and a count of
  facts (lifecycle facts like `FROZEN` count too, with amount zero). `closing` must equal both
  `opening + credits - debits` and the balance at the window's end — if your model lets those
  disagree, the model is telling you something.
- Both are queries: rejected on unknown or closed accounts, answered on frozen ones, never
  recording anything. Bad window arguments are `ERROR bad-sequence` / `ERROR bad-range` (PART2 §3).

**Checks.** `m5/easy-1` (the worked example from the protocol docs, verbatim), `easy-2` (empty
windows and empty histories), `medium-1` and `medium-2` (every window shape: full, prefix, suffix,
mid, empty, one-fact, windows cutting a transfer in half), `large`, `large-2`, `huge`.

**You are done when** all five tests are green. That is the finished project.

## 11. Stretch goals

For teams that finish early, in this order. No golden files — these are demo material for 16:20.

1. **Two-phase transfer** — `initiated` / `completed` / `failed` facts, with a rollback when the
   destination side refuses.
2. **Snapshots** — persist `(state, seq)` periodically so `replay` does not start from fact zero;
   the m3/m4 replay tests must still pass.
3. **Optimistic concurrency** — an expected-version field on commands, refused as
   `REJECTED version-conflict account=<acc> expected=<n> actual=<n>`.
4. **Property tests** — money is conserved by transfers; replay is deterministic; sequence numbers
   are dense; `evolve` handles every fact `decide` can emit.
5. **A query language** — a small ADT of queries over the log, and an interpreter for it.

## 12. Demos (16:20)

Five minutes per team. Everyone built the same ledger, so "here is my app" is not the interesting
part. Come prepared to answer two questions instead: **what did your paper model from 9:25 get
wrong, and which milestone told you?** — and, if you took a stretch goal, what it cost.
