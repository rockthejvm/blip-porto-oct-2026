# The ledger — project guide

*This document is the complete specification of what you are building today: the goal, the rules,
the exact text protocol, and what "done" means at every step. It assumes you know nothing about the
project. There are no other documents.*

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
  should need to know how you represented an account.
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

### The protocol is exact

Every character of the input and output format is specified in the milestone sections below, and
the tests compare byte for byte — "almost" does not exist today. One design note that applies to
all of it: the output format never names the fact your program recorded. It does not say
"Deposited"; it says `+100.00 100.00` — what an observer would notice. How you model the facts
behind the lines, and the mapping from your model to the wire, is yours to write.

**One warning.** The milestones are laid out below in full, including the two commands that arrive
after lunch. Skim the milestone *titles* now, but if you are doing this as the live training day,
**do not read the protocol details of milestones 4 and 5 before the afternoon** — they will spoil a
design decision you are about to make on paper at 9:25, and the decision teaches more if you make
it first.

## 4. Constraints

- Scala standard library plus MUnit. No other dependencies of any kind.
- No `given`/`using`, no implicits, no higher-kinded types, no macros. If you catch yourself
  designing `F[_]` or `trait Aggregate[C, E, S]`, stop: make the concrete thing work.
- No `Future`, no threads, no clocks, no randomness — output must be deterministic.
- `enum` or `sealed trait` for your data types; either is fine.
- Money is not a `Double`. (Compute `0.1 + 0.2` in a REPL before arguing.)
- Supported currencies are exactly `USD`, `EUR`, `GBP`, all with two decimal places. `JPY` and
  other zero-decimal currencies are out of scope on purpose; do not rabbit-hole there.

---

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

---

## 6. Milestone 1 — a vertical slice (10:30–11:30)

**Goal.** The whole pipeline — parse, decide, evolve, render — working end to end for the three
simplest commands. Narrow but complete.

### 6.1 Input format (applies to every milestone)

One command per line. Fields are separated by one or more spaces (or tabs). Leading and trailing
whitespace is stripped. Blank lines, and lines whose first non-space character is `#`, produce
**no output at all** — not even an empty line.

Verbs are uppercase. Account ids match `[a-z0-9-]{1,32}`. Currency codes are three uppercase
letters; the supported ones are `USD`, `EUR`, `GBP`.

The commands of this milestone:

| Command | Arguments |
|---|---|
| `OPEN <acc> <CUR>` | 2 |
| `DEPOSIT <acc> <amount>` | 2 |
| `BALANCE <acc>` | 1 |

### 6.2 Money (applies to every milestone)

An input amount matches `-?\d+(\.\d{1,2})?`. So `100`, `100.5`, `100.50` and `-5.00` all **parse**.
`1.005` does not (too many decimals), nor does `.5`, `5.`, `5,00` or `five`.

Output amounts always have exactly two decimals and no thousands separator: `0.00`, `100.50`,
`1234567.89`. Negative amounts have a leading `-`: `-5.00`.

Yes, a negative amount parses. Parsing only asks "is this shaped like an amount?"; whether the
amount is acceptable is decided later, with a proper rejection (milestone 2's
`non-positive-amount`). Keep the two questions apart.

### 6.3 Output for accepted commands

Every accepted command prints one line per fact it recorded:

```
OK <seq> <acc> <effect>
```

`<seq>` is the **global, 1-based, strictly increasing sequence number** over every fact the ledger
has ever recorded, across all accounts. It never resets, never skips, and is never given to a
rejected command, a query, or a malformed line. A rejection between two accepted commands must not
leave a gap.

The effects of this milestone:

| Effect | Meaning |
|---|---|
| `OPENED <CUR>` | the account now exists, with balance `0.00` |
| `+<amount> <balance>` | a credit, followed by the balance after it |

So `DEPOSIT acc-1 25` on an account holding `100.00` prints `OK <seq> acc-1 +25.00 125.00` — the
amount *and the resulting balance*, both rendered as in §6.2.

### 6.4 Rejections and queries

A rejected command prints exactly one line. In this milestone two rejections exist:

```
REJECTED unknown-account account=<acc>
REJECTED account-exists account=<acc>
```

(Key–value pairs are single-spaced, no spaces around `=`. More rejections, and the rules for
choosing between them, arrive in milestone 2.)

- `OPEN` on an id that already exists: `account-exists`.
- `DEPOSIT` or `BALANCE` on an id that does not: `unknown-account`.

`BALANCE` is a **query**: it records nothing, consumes no sequence number, and answers

```
BALANCE <acc> <amount> <CUR>
```

### 6.5 Checks

`m1/easy-1` and `easy-2` (happy path, first rejections), `medium-1` and `medium-2` (every M1 rule,
odd-but-legal inputs like `1.5` and 32-character ids), `large` and `large-2` (six to eight accounts
interleaved), `huge`.

**You are done when** the `m1` test is green. Expect the resulting-balance column and the "queries
don't consume a sequence number" rule to be what bites; both are visible in the first ten lines of
any diff.

---

## 7. Milestone 2 — money and refusals (11:30–12:30)

**Goal.** The full single-account rule set: every way a command can be refused, in the right order,
plus malformed input handled without crashing.

### 7.1 New commands

| Command | Arguments |
|---|---|
| `WITHDRAW <acc> <amount>` | 2 |
| `FREEZE <acc>` | 1 |
| `UNFREEZE <acc>` | 1 |
| `CLOSE <acc>` | 1 |

### 7.2 New effects

| Effect | Meaning |
|---|---|
| `-<amount> <balance>` | a debit, followed by the balance after it |
| `FROZEN` | the account is now frozen |
| `UNFROZEN` | the account is now not frozen |
| `CLOSED` | the account is now closed |

### 7.3 The full rejection vocabulary

Exactly one `REJECTED` line, never more. Key–value pairs appear in exactly this order,
single-spaced, no spaces around `=`; amounts rendered as in §6.2 (`amount=-5.00`, `amount=0.00`):

```
REJECTED unknown-account account=<acc>
REJECTED account-exists account=<acc>
REJECTED unsupported-currency currency=<CUR>
REJECTED account-closed account=<acc>
REJECTED account-frozen account=<acc>
REJECTED non-positive-amount account=<acc> amount=<amount>
REJECTED insufficient-funds account=<acc> balance=<amount> requested=<amount>
REJECTED non-zero-balance account=<acc> balance=<amount>
```

(Milestone 4 adds two more for transfers.)

### 7.4 The rules

- `WITHDRAW` needs sufficient funds: otherwise `insufficient-funds`, with the current balance and
  the requested amount.
- Amounts must be positive: `0` or negative is `non-positive-amount` — for deposits too.
- A well-formed but unsupported currency code (`JPY`) is `unsupported-currency`. (A malformed one,
  like `usd`, is a parse error — §7.6.)
- A **frozen** account refuses `DEPOSIT` and `WITHDRAW` (`account-frozen`) but still answers
  `BALANCE` and still allows `UNFREEZE` and `CLOSE`. `FREEZE` on an already-frozen account (and
  `UNFREEZE` on a non-frozen one) is an *idempotent success*: it records a fact, prints `OK`,
  consumes a sequence number.
- `CLOSE` requires balance exactly `0.00` (`non-zero-balance` otherwise) and is **terminal**: every
  later command on that account — including `BALANCE` and `DEPOSIT` — is `account-closed`, and
  re-`OPEN`ing the id is `account-exists`.

### 7.5 Rejection precedence

When several rejections apply, exactly one is printed, chosen by this order.

For `DEPOSIT`, `WITHDRAW`, `FREEZE`, `UNFREEZE`, `CLOSE`, `BALANCE`:

1. `unknown-account`
2. `account-closed`
3. `account-frozen`
4. `non-positive-amount`
5. `insufficient-funds`
6. `non-zero-balance`

For `OPEN`: `account-exists` before `unsupported-currency`.

So a withdrawal from a frozen account with too little money is `account-frozen`, and a negative
deposit to a closed account is `account-closed`. The golden files are unambiguous about every such
pair; so is this list.

### 7.6 Malformed input

A line that is not a well-formed command prints exactly one `ERROR` line and changes nothing:

```
ERROR unknown-command <verb>
ERROR bad-arguments <verb>
ERROR bad-account-id <token>
ERROR bad-amount <token>
ERROR bad-currency <token>
```

Order of checks within a line: the verb first (`<verb>` is echoed exactly as written, so
`deposit acc-1 5` gives `ERROR unknown-command deposit`), then the number of arguments, then the
arguments themselves from left to right. `bad-currency` is about shape only: `usd` is
`bad-currency`, `JPY` parses and is then rejected with `unsupported-currency`.

### 7.7 Worked example

Input:

```
# a first account
OPEN acc-1 USD
DEPOSIT acc-1 100.00
DEPOSIT acc-1 25
WITHDRAW acc-1 30.00
FREEZE acc-1
DEPOSIT acc-1 1
BALANCE acc-1
UNFREEZE acc-1
WITHDRAW acc-1 100
deposit acc-1 5
CLOSE acc-1
WITHDRAW acc-1 95
CLOSE acc-1
BALANCE acc-1
```

Output:

```
OK 1 acc-1 OPENED USD
OK 2 acc-1 +100.00 100.00
OK 3 acc-1 +25.00 125.00
OK 4 acc-1 -30.00 95.00
OK 5 acc-1 FROZEN
REJECTED account-frozen account=acc-1
BALANCE acc-1 95.00 USD
OK 6 acc-1 UNFROZEN
REJECTED insufficient-funds account=acc-1 balance=95.00 requested=100.00
ERROR unknown-command deposit
REJECTED non-zero-balance account=acc-1 balance=95.00
OK 7 acc-1 -95.00 0.00
OK 8 acc-1 CLOSED
REJECTED account-closed account=acc-1
```

### 7.8 Checks

`m2/easy-1` (withdrawals and amount rules), `easy-2` (the lifecycle), `medium-1` (every rejection
at least once), `medium-2` (every `ERROR` variant and their in-line precedence), `medium-3`
(precedence pairs in combination: frozen *and* broke, closed *and* negative), `large`, `large-2`,
`huge`.

**You are done when** `m2` is green and `m1` is still green.

---

## 8. Milestone 3 — the journal (13:15–14:15)

**Goal.** The ledger survives a restart. State is rebuilt from a file of everything that ever
happened — and nothing else.

**Commands.** None new, and nothing about the wire format changes. You override the two defaulted
methods on `LedgerEngine`:

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

---

## 9. Milestone 4 — transfers (14:15–15:30)

*Live training: do not read this section before the afternoon.*

**Goal.** Money moves between accounts atomically: one command, two facts, both or neither.

### 9.1 The command

| Command | Arguments |
|---|---|
| `TRANSFER <from> <to> <amount>` | 3 |

Moves money between two accounts of the same currency. A successful transfer records **two
facts** — a debit on the source, then a credit on the destination — printed as two `OK` lines,
debit first, both carrying the same transfer tag:

```
OK 7 acc-1 -50.00 45.00 xfer=t1
OK 8 acc-2 +50.00 50.00 xfer=t1
```

The tag is `t` followed by a 1-based count of **successful** transfers in the ledger's whole
history. Rejected transfers do not advance it. Like the sequence number, it must survive a restart.

There is no partial transfer. If anything is wrong, nothing happened and one `REJECTED` line is
printed. Two new rejections exist:

```
REJECTED same-account-transfer account=<acc>
REJECTED currency-mismatch from=<acc> to=<acc> from-currency=<CUR> to-currency=<CUR>
```

### 9.2 Transfer precedence

Checked in exactly this order:

1. `same-account-transfer` (checked before either account is looked up)
2. `unknown-account` on the source
3. `unknown-account` on the destination
4. `account-closed` on the source
5. `account-closed` on the destination
6. `account-frozen` on the source
7. `account-frozen` on the destination
8. `non-positive-amount` (`account=` is the source)
9. `currency-mismatch`
10. `insufficient-funds` (`account=` is the source)

A frozen account can be neither the source nor the destination of a transfer.

### 9.3 Checks

`m4/easy-1`, `easy-2`, `medium-1` (every transfer rejection in precedence order), `medium-2` (the
combinations: both ends frozen, closed source vs unknown destination), `large`, `large-2`, `huge` —
plus its own replay pairs (`replay-a/b`, `replay-2-a/b`, `replay-huge-a/b`): after a restart the
*transfer tag* must continue too, not just the sequence number. If you skipped M3, the replay part
of `m4` stays red; the golden part still tells you whether transfers themselves work.

**You are done when** `m4` is green (or green except the replay check, if you cut M3), and
`m1`/`m2` still are. If your `decide` could only ever return a single fact: this is the milestone
that tells you why the signature you were given returns a list.

---

## 10. Milestone 5 — projections (15:30–16:20)

*Live training: do not read this section before the afternoon.*

**Goal.** The ledger answers questions about its own past, computed from the facts alone.

### 10.1 `HISTORY <acc>`

A read-only view of one account's facts, oldest first, reusing exactly the same `<seq>` and effect
text as the original `OK` lines — transfer tags included:

```
HISTORY <acc> <count>
  <seq> <effect>
  <seq> <effect>
```

The header gives the number of facts; each detail line is indented by exactly two spaces and
ordered by ascending `<seq>`. If you rendered `OK` lines with a function, you already have this.

### 10.2 `SUMMARY <acc> <fromSeq> <toSeq>`

Aggregates the account's facts inside an **inclusive** window of *global* sequence numbers, and
prints one line:

```
SUMMARY <acc> <fromSeq> <toSeq> opening=<amount> credits=<amount> debits=<amount> closing=<amount> count=<n>
```

- `opening` — the account's balance after all of its facts with `seq < fromSeq` (`0.00` if none).
- `credits`, `debits` — sums of the account's credits and debits with `fromSeq <= seq <= toSeq`.
  Debits are reported as a positive number.
- `closing` — `opening + credits - debits`. It must also equal the balance after all of the
  account's facts with `seq <= toSeq`; if your implementation can disagree with itself here,
  something is wrong.
- `count` — the number of the account's facts in the window, **including** lifecycle facts like
  `OPENED` and `FROZEN` (they move the balance by zero).

A window with none of the account's facts is fine: `credits=0.00 debits=0.00 count=0`, and
`opening` equals `closing`. Sequence numbers in the window need not exist yet.

### 10.3 Query rules and two new errors

Both commands are queries: rejected on unknown or closed accounts (same precedence as `BALANCE`),
answered on frozen ones, never recording anything and never consuming a sequence number.

The window arguments must match `\d+`, checked left to right after the account id; then the range
must be ordered:

```
ERROR bad-sequence <token>          # a window argument that does not match \d+
ERROR bad-range <fromSeq>-<toSeq>   # fromSeq > toSeq, e.g. "ERROR bad-range 9-5"
```

### 10.4 Worked example

Input:

```
OPEN acc-1 USD
DEPOSIT acc-1 100.00
DEPOSIT acc-1 25
WITHDRAW acc-1 30.00
FREEZE acc-1
UNFREEZE acc-1
OPEN acc-2 USD
TRANSFER acc-1 acc-2 50
HISTORY acc-1
BALANCE acc-2
SUMMARY acc-1 1 9
SUMMARY acc-1 3 4
SUMMARY acc-2 1 7
```

Output:

```
OK 1 acc-1 OPENED USD
OK 2 acc-1 +100.00 100.00
OK 3 acc-1 +25.00 125.00
OK 4 acc-1 -30.00 95.00
OK 5 acc-1 FROZEN
OK 6 acc-1 UNFROZEN
OK 7 acc-2 OPENED USD
OK 8 acc-1 -50.00 45.00 xfer=t1
OK 9 acc-2 +50.00 50.00 xfer=t1
HISTORY acc-1 7
  1 OPENED USD
  2 +100.00 100.00
  3 +25.00 125.00
  4 -30.00 95.00
  5 FROZEN
  6 UNFROZEN
  8 -50.00 45.00 xfer=t1
BALANCE acc-2 50.00 USD
SUMMARY acc-1 1 9 opening=0.00 credits=125.00 debits=80.00 closing=45.00 count=7
SUMMARY acc-1 3 4 opening=100.00 credits=25.00 debits=30.00 closing=95.00 count=2
SUMMARY acc-2 1 7 opening=0.00 credits=0.00 debits=0.00 closing=0.00 count=1
```

Note `SUMMARY acc-1 3 4`: `opening` is the balance after fact 2, the window holds facts 3 and 4,
and `closing` is the balance after fact 4. And `SUMMARY acc-2 1 7`: one fact (`OPENED`) in the
window, no money moved, `count=1`.

### 10.5 Checks

`m5/easy-1` (the worked example above, verbatim), `easy-2` (empty windows and empty histories),
`medium-1` and `medium-2` (every window shape: full, prefix, suffix, mid, empty, one-fact, windows
cutting a transfer in half), `large`, `large-2`, `huge`.

**You are done when** all five tests are green. That is the finished project.

---

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
