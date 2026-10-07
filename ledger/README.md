# The ledger — project guide

## 1. The goal

You are building a **command-line ledger for bank accounts**. It's the kind of system that
sits behind a bank, a broker, or a payments platform.

You will practice
- event sourcing
- functional programming
- algebraic data types

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

Three kinds of responses:

- A command that is **accepted** records one or more *facts* and prints one `OK` line per fact.
- A command that is **rejected** records nothing and prints one `REJECTED` line with an encoded
  reason (`insufficient-funds`, `account-frozen`, ...).
- A line that is not a well-formed command records nothing and prints one `ERROR` line.

Every fact contains a **global sequence number**: 1 for the first fact the ledger ever records, then
2, 3, 4... across all accounts. Rejections, queries and errors never consume a sequence number.

## 2. What "done" looks like

By the end of the day your team has a ledger that:

- handles accounts in multiple currencies currencies, with deposits, withdrawals, freezing and closing;
- rejects every invalid operation with a reason;
- **survives being restarted**: it writes what happened to a journal file and rebuilds its entire
  state from that file;
- moves money **atomically between two accounts**, with both sides recorded and tagged;
- answers **historical queries** over its own past — the full history of an account, and aggregated
  statements over any window of sequence numbers;
- passes a test suite of several thousand scenario lines.

The project is split into 5 milestones, and we will make sure each milestone passes before moving to the next.
By the end of the project you will have created an **event-sourced ledger** which in real life runs financial backends, Kafka-based systems, Akka/Pekko persistence, and critical systems.

## 3. How the project works

### What is provided

Four files in `src/main/scala/com/rockthecode/ledger/`. Don't change anything but `MyLedger`.

| File | What it is |
|---|---|
| `LedgerEngine.scala` | The one interface between your code and everything else: `empty`, `execute(state, line)`, and two journal methods (defaulted to no-ops) that you implement in milestone 3. `S`, the state type, is yours; the harness never looks inside it. |
| `Harness.scala` | Runs input lines through an engine by folding `execute` over them. This is the whole application — notice there is no `var`, no mutation and no I/O in it. |
| `Main.scala` | The command-line app: reads a scenario file, prints the responses. Takes an optional journal file. The single line `val ledger = MyLedger` is where it is told which engine to run. |
| `MyLedger.scala` | A stub engine that answers `ERROR NotImplemented` to everything. **This is what you replace.** |

Your code should go in two places:

- `src/main/scala/com/rockthecode/ledger/domain/` — your data model.
- `MyLedger.scala` — wire your model to the `LedgerEngine` interface: parse the line, work out
  what it does to your state, render the responses.

### How you know you are done: the scenarios

`src/main/resources/ledger/` has one directory per milestone. Each contains scenario files: an
`.in` file of commands and, next to it, an `.out` file with the exact expected output.

Each milestone comes as a ladder:

| Tier | Size | What it is for |
|---|---|---|
| `easy-*` | ~10 lines | The happy path |
| `medium-*` | 30–60 lines | Every rule of the milestone, still readable |
| `large*` | 100–200 lines | Everything interleaved, meant ot surface out mistakes |
| `huge` | 1000 lines | Machine-generated, if you pass this then you're good |

Run the tests with:

```bash
sbt ledger                                                           # all milestones
sbt "testOnly com.rockthecode.ledger.MyLedgerSuite -- *m2*"           # one milestone
```

There is one test per milestone. A failing test names each failing scenario and shows the first
differing lines — expected vs. yours. **Tests compare byte for byte.**

To run your program by hand on any scenario (or one you write yourself):

```bash
sbt "runMain com.rockthecode.ledger.Main src/main/resources/ledger/m1/easy-1.in"
sbt "runMain com.rockthecode.ledger.Main my-scenario.txt my.journal"   # milestone 3: with a journal
```


**One warning.** The milestones are laid out below in full, including the two commands that arrive
after lunch. I recommend you **NOT** to read milestones 4 and 5 until you reach them, because they spoil a design decision.

## 4. Recommendations

- No need for external libraries, given/using, or other fancy Scala features.
- No `Future`, no threads, no clocks, no randomness — output must be deterministic.
- `enum`s, `case class`es and `sealed` types are your friends.
- `Option`, `Try` and `Either` will prove very useful.
- Money is not a `Double`.
- Supported currencies are exactly `USD`, `EUR`, `GBP`, all with two decimal places. `JPY` and
  other zero-decimal currencies are out of scope.

---

## 5. Milestone 0 — paper model

**Goal.** Design your data model before touching the code.

**Questions.** What is a command? What is a fact? Are they the same type? (one of them can be refused and one of them cannot.) 
What does the state have to remember, and what
can it recompute from the facts? Where does the sequence number live? What is money? What are the
possible conditions of an account, and what type expresses "exactly one of these at a time"?

**You are done when** every member of the team can answer: "what happens (think in types) when the
line `DEPOSIT acc-1 50` arrives?"

---

## 6. Milestone 1 — initial pipeline + first commands

**Goal.** The whole pipeline — parsing, validation, state change, rendering — working end to end
for Open, Deposit and Balance.

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

### 6.2 Money

An input amount matches `-?\d+(\.\d{1,2})?`. So `100`, `100.5`, `100.50` and `-5.00` all **parse**.
`1.005` does not (too many decimals), nor does `.5`, `5.`, `5,00` or `five`.

Output amounts always have exactly two decimals and no thousands separator: `0.00`, `100.50`,
`1234567.89`. Negative amounts have a leading `-`: `-5.00`.

A negative amount parses. Parsing only checks if the line is a proper amount; whether the
amount is acceptable is decided later, with a rejection (milestone 2's
`non-positive-amount`). Hint: these should be two separate types.

### 6.3 Output for accepted commands

Every accepted command prints one line per fact it recorded:

```
OK <seq> <acc> <effect>
```

`<seq>` is a **global, 1-based, strictly increasing sequence number** over every fact the ledger
has ever recorded, across all accounts. It never resets, never skips, and is never given to a
rejected command, a query, or a malformed line.

You need to handle the following effects:

| Effect | Meaning |
|---|---|
| `OPENED <CUR>` | the account now exists, with balance `0.00` |
| `+<amount> <balance>` | a credit, followed by the balance after it |

So `DEPOSIT acc-1 25` on an account holding `100.00` prints `OK <seq> acc-1 +25.00 125.00`.

### 6.4 Rejections and queries

A rejected command prints exactly one line. In this milestone two rejections exist:

```
REJECTED unknown-account account=<acc>
REJECTED account-exists account=<acc>
```

Key–value pairs are single-spaced, no spaces around `=`.

- `OPEN` on an id that already exists: `account-exists`.
- `DEPOSIT` or `BALANCE` on an id that does not: `unknown-account`.

The `BALANCE` command records nothing, consumes no sequence number, and answers with

```
BALANCE <acc> <amount> <CUR>
```

### 6.5 Checks

`m1/easy-1` and `easy-2` (happy path, first rejections), `medium-1` and `medium-2` (every M1 rule,
odd-but-legal inputs like `1.5` and 32-character ids), `large` and `large-2` (six to eight accounts
interleaved), `huge`.

**You are done when** the `m1` test is green. 

### 6.6 Other Hints

- careful with the resulting-balance column
- some commands don't consume a sequence number, probably best to separate them (as a type)

---

## 7. Milestone 2 — money and refusals

**Goal.** A full single-account rule set, plus rejections and malformed input.

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

### 7.3 Rejection set

One `REJECTED` line, key–value pairs appear in exactly this order,
single-spaced, no spaces around `=`:

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

### 7.4 Rules

- `WITHDRAW` needs sufficient funds: otherwise `insufficient-funds`, with the current balance and
  the requested amount.
- Amounts must be positive: `0` or negative is `non-positive-amount` — for deposits too.
- A well-formed but unsupported currency code (`JPY`) is `unsupported-currency`. (A malformed one,
  like `usd`, is a parse error)
- A **frozen** account refuses `DEPOSIT` and `WITHDRAW` (`account-frozen`) but still answers
  `BALANCE` and still allows `UNFREEZE` and `CLOSE`. `FREEZE` on an already-frozen account (and
  `UNFREEZE` on a non-frozen one) is an *idempotent success*: it records a fact, prints `OK`,
  consumes a sequence number.
- `CLOSE` requires balance exactly `0.00` (`non-zero-balance` otherwise) and is **terminal**: every
  later command on that account — including `BALANCE` and `DEPOSIT` — is `account-closed`, and
  re-`OPEN`ing the id is `account-exists`.

### 7.5 Rejection precedence

When several rejections apply, only one is printed, chosen by this order.

For `DEPOSIT`, `WITHDRAW`, `FREEZE`, `UNFREEZE`, `CLOSE`, `BALANCE`:

1. `unknown-account`
2. `account-closed`
3. `account-frozen`
4. `non-positive-amount`
5. `insufficient-funds`
6. `non-zero-balance`

For `OPEN`: `account-exists` before `unsupported-currency`.

Examples:
- a withdrawal from a frozen account with too little money is `account-frozen`
- a negative deposit to a closed account is `account-closed`

### 7.6 Malformed input

A line that is not a well-formed command prints exactly one `ERROR` line:

```
ERROR unknown-command <verb>
ERROR bad-arguments <verb>
ERROR bad-account-id <token>
ERROR bad-amount <token>
ERROR bad-currency <token>
```

Order of checks within a line: the verb first (`<verb>` is echoed exactly as written, so
`deposit acc-1 5` gives `ERROR unknown-command deposit`), then the number of arguments, then the
arguments themselves from left to right. `usd` is
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

## 8. Milestone 3 — the journal

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
- `journalLines(before, after)` is called once per input line, with your state from before and
  after it. Return what the journal should remember about that line — usually one line per newly
  recorded fact, and nothing for rejections, queries and errors.
- The file where the lines are written is not your problem. The main app collects what you return, writes it to the
  journal file, and on a later run hands the whole file back to your `replay`, in order. Your two
  methods never touch the disk.
- `replay` of everything written so far must produce a state from which the ledger behaves
  *identically* to one that never restarted, including the sequence number.

**Checks.** The `m3` test does two things:
1. Checks as usual (`m3/easy-1`, `medium-1`, `medium-2`, `large`, `large-2`, `huge` — no
   new commands, these are regression files).
2. The **replay check**, on the `replay-a.in`/`replay-b.in` pairs: it runs A from empty, writes
   your journal lines to a temp file, reads them back, `replay`s them into a fresh state, runs
   B from that state — and the output of B must equal the corresponding part of `replay.out`,
   which was produced by running A and B in one uninterrupted go.

**You are done when** `m3` is green. If B's output starts with `OK 1` when the file expects
`OK 16`, your sequence number did not survive the restart.

---

## 9. Milestone 4 — transfers

*Recommendation: do not read this section before arriving at this milestone.*

**Goal.** Money moves between accounts atomically.

### 9.1 The command

| Command | Arguments |
|---|---|
| `TRANSFER <from> <to> <amount>` | 3 |

Moves money between two accounts of the **same currency**. A successful transfer records **two
facts** — a debit on the source, then a credit on the destination — printed as two `OK` lines,
debit first, both with the same transfer tag:

```
OK 7 acc-1 -50.00 45.00 xfer=t1
OK 8 acc-2 +50.00 50.00 xfer=t1
```

The tag is `t` followed by a 1-based count of **successful** transfers in the ledger's whole
history. Rejected transfers do not advance it. Like the sequence number, it must survive a restart.

There is no partial transfer. If anything is wrong, nothing happened and one `REJECTED` line is
printed. We now allow two new rejection types:

```
REJECTED same-account-transfer account=<acc>
REJECTED currency-mismatch from=<acc> to=<acc> from-currency=<CUR> to-currency=<CUR>
```

### 9.2 Transfer error precedence

Checked in this order:

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

`m4/easy-1`, `easy-2`, `medium-1` (every transfer rejection in precedence order), `medium-2` (
combinations: both ends frozen, closed source vs unknown destination), `large`, `large-2`, `huge` —
plus its own replay pairs (`replay-a/b`, `replay-2-a/b`, `replay-huge-a/b`)

Careful that after a restart the
*transfer tag* must continue too, not just the sequence number. If you skipped M3, the replay part
of `m4` stays red, and it's okay.

**You are done when** `m4` is green (or green except the replay check, if you cut M3), and
`m1`/`m2` still are.

---

## 10. Milestone 5 — projections

*Recommended to not read this section in advance.*

**Goal.** The ledger answers questions about its own past, computed just from recorded facts.

### 10.1 `HISTORY <acc>`

A read-only view of one account's facts, oldest first, reusing the same `<seq>` and effect
text as the original `OK` lines — transfer tags included:

```
HISTORY <acc> <count>
  <seq> <effect>
  <seq> <effect>
```

The header gives the number of facts; each detail line is indented by exactly two spaces and
ordered by ascending `<seq>`.

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

Both commands are rejected on unknown or closed accounts (same precedence as `BALANCE`),
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

`m5/easy-1` (the worked example above), `easy-2` (empty windows and empty histories),
`medium-1` and `medium-2` (every window shape), `large`, `large-2`, `huge`.

**You are done when** all five tests are green. That is the finished project.

---

## 11. Stretch goals

For teams that finish early, in this order. No golden files — these are demo material for 16:20.

1. **Two-phase transfer** — `initiated` / `completed` / `failed` facts, with a rollback when the
   destination side refuses.
2. **Snapshots** — persist `(state, seq)` periodically so `replay` does not start from fact zero;
   the m3/m4 replay tests must still pass.
3. **Property tests** — examples: money is conserved by transfers; replay is deterministic; sequence numbers
   don't leave gaps; etc.
4. **A query language** — a small ADT of queries over the log, and an interpreter for it.

