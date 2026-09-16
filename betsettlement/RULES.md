# Bet Settlement — the rules

Everything you need is on this page. **No knowledge of betting is assumed or required** — these
are the rules of *our* system, and where they differ from a real sportsbook, the last section says so.

---

## 1. What we are building

**A bet settlement engine** — the part of a betting platform that decides, once the football is
over, who won what and how much the house owes.

It is a batch program. It reads three CSV files, works out the answer, and prints a CSV report to
standard output. Nothing is written back to disk, there is no database, and — until the bonus
milestone — there is no server. That is deliberate: the interesting part is the settlement logic,
and everything else is plumbing that has been done for you in the skeleton.

### How it runs

```
   markets.csv     the catalogue: which markets exist and what you may bet on
   bets.csv        what customers staked, and at what odds
   results.csv     how each market finished
        |
        v
   [ read the CSV ]          given to you in the skeleton
        |
        v
   [ parse into YOUR model ]  yours - this is modelling, and modelling is the point
        |
        v
   [ validate ]              reject bets that could never be settled
        |
        v
   [ settle ]                per leg, then per bet - this is the actual work
        |
        v
   [ render ]                one CSV line per bet, sorted by betId
        |
        v
   standard output
```

You then compare that output against the expected file for the milestone:

```
./check m1
```

If it prints nothing, you match. If it prints a diff, you do not. That is the whole acceptance
mechanism — there are no hidden tests, and the expected output is in the repo from the start, so
you can look at it whenever you like.

### How it grows

Each milestone adds one thing to the same program. The input file formats never change.

| | What gets added |
|---|---|
| **M1** | Settle single-leg bets against results. Won or lost, and what we return. |
| **M2** | Markets that were voided, markets with no result yet, and bets we refuse outright. |
| **M3** | Accumulators — one bet across several legs, all of which must come in. |
| **M4** | Results stop being a file and become a **stream of events** arriving over time, which you fold into a per-customer ledger. Some arrive twice; some are for markets that do not exist. |
| **M5** *(bonus)* | Put it behind an HTTP server: post results in, read a customer's ledger out, while both happen at once. |

Section 8 works each one through with real numbers.

### The shape of the program

Worth deciding early, because it is the difference between M3 being an afternoon and being twenty
minutes: **keep the settlement logic pure.** Reading files and printing lines happen at the edges;
in between, functions take data and return data, and nothing prints, throws, or mutates.

Everything you did on day 2 was practice for the middle of this pipeline.

---

## 2. Vocabulary

| Term | Meaning |
|---|---|
| **Market** | Something you can bet on — "Porto vs Benfica, Match Result" — offering a fixed set of selections. |
| **Selection** | One possible outcome of a market: `HOME`, `DRAW`, `AWAY`. |
| **Decimal odds** | A payout multiplier that **includes the stake**. €10 at odds 2.50 returns €25.00 — €15 profit plus the €10 back. Decimal odds are never below 1.00; exactly 1.00 means the stake comes back with no profit. |
| **Bet** | A customer stakes an amount on one or more **legs**. |
| **Leg** | One (market, selection, odds) triple inside a bet. A bet with one leg is a *single*; a bet with several is an **accumulator**. |
| **Result** | For each market: the winning selection, or the market was **voided** (abandoned, cancelled — no winner). |
| **Settlement** | What we owe, per bet. |

---

## 3. Settling a bet

Settle each **leg** first, giving it an outcome and a **multiplier**:

| Leg situation | Leg outcome | Multiplier |
|---|---|---|
| The leg's market has no result yet | `PENDING` | — |
| The market's result is `VOID` | `VOID` | **1.00** |
| The result selection equals the leg's selection | `WON` | the leg's odds |
| Anything else | `LOST` | 0 |

Then settle the **bet**, in this order — the first rule that applies wins:

| Condition | Bet outcome | Returned |
|---|---|---|
| Any leg `LOST` | `LOST` | 0.00 |
| Any leg `PENDING` | `PENDING` | 0.00 *(nothing is paid until every leg is known)* |
| Every leg `VOID` | `VOID` | the stake |
| Otherwise | `WON` | stake × the product of every leg's multiplier |

**The void-leg rule is the interesting one.** A voided leg has multiplier 1.00, so a four-leg
accumulator with one voided leg settles as a three-leg accumulator at the same stake. That is the
real rule, and it is not a special case — 1.00 is the multiplicative identity.

Worth noticing before you start writing branches: `VOID` returns the stake, which is exactly what
the `WON` formula produces when every multiplier is 1.00. You may implement it either way. The two
outcomes still need different **labels** in the output.

---

## 4. Rejecting a bet

A bet is `REJECTED` — never settled — if **any** of these hold:

- it has no legs;
- the stake is not greater than zero;
- any leg's odds are **less than** 1.00;
- any leg names a market that is not in the catalogue;
- any leg names a selection that market does not offer.

A rejected bet returns nothing and never appears in the ledger. Report the **first** failing reason
in the order listed above, spelled exactly like this in the `detail` column:

| Reason | `detail` |
|---|---|
| No legs | `NO_LEGS` |
| Stake not greater than zero | `INVALID_STAKE 0.00` |
| Odds less than 1.00 | `INVALID_ODDS 0.90` |
| Market not in the catalogue | `UNKNOWN_MARKET M999` |
| Selection not offered by that market | `UNKNOWN_SELECTION M001:YES` |

Odds of **exactly 1.00 are legal**. They mean "your stake back, no profit" — which is the same
multiplier a voided leg gets, so the two behave identically. Only odds below 1.00 are nonsense,
because they would take money off a winning bet.

---

## 5. Money

- Stakes and odds arrive with **2 decimal places**.
- Multipliers are **never rounded** — multiply at full precision.
- The **returned** amount is rounded **HALF_UP to 2 decimal places, once, at the very end**.

So a €10.00 treble at 1.33 × 1.33 × 1.33 returns `10.00 × 2.352637 = 23.52637` → **23.53**. Round
each leg on the way and you will get 23.52, and the expected output will disagree with you.

---

## 6. The data

All files are CSV with a header row. They live in `betsettlement/data/`.

### `markets.csv`
```
marketId,description,selections
M001,Porto vs Benfica - Match Result,HOME|DRAW|AWAY
```
`selections` is pipe-separated. This is the catalogue: a market not listed here does not exist.

### `bets.csv`
```
betId,customerId,stake,legs
B001,C001,10.00,M001:HOME:2.50
B002,C002,5.00,M001:DRAW:3.40|M002:AWAY:1.80
```
`legs` is pipe-separated, and each leg is `marketId:selection:odds`. One leg is a single; more than
one is an accumulator. **The format does not change between milestones** — M1 simply gets a file in
which every bet happens to have one leg.

### `results.csv`
```
marketId,outcome
M001,HOME
M002,VOID
```
`outcome` is either a selection or the literal `VOID`. A market absent from this file has no result
yet, and bets on it are `PENDING`.

### `result-events.csv` — milestone 4 only
```
sequence,marketId,outcome
1,M001,HOME
2,M003,VOID
3,M001,HOME
4,M999,AWAY
```
Results arriving over time, in `sequence` order. Expect **duplicates** (applying one twice must
change nothing) and results for **markets that do not exist** (report and carry on).

---

## 7. What you print

### Settlements — milestones 1 to 3
CSV, header row, **sorted by `betId` ascending**:
```
betId,customerId,outcome,stake,returned,detail
B001,C001,WON,10.00,25.00,
B002,C002,LOST,5.00,0.00,
B009,C003,REJECTED,10.00,0.00,UNKNOWN_MARKET M999
```
`outcome` is one of `WON`, `LOST`, `VOID`, `PENDING`, `REJECTED`. `detail` is empty except for
`REJECTED`, where it carries the reason.

**Pending bets are printed like any other.** A run does not wait for every market to finish — when
the report is produced there will normally be fixtures still to play, and "we owe nothing on this
one *yet*" is exactly what an operator needs to see. A pending bet is a row with `0.00` returned,
not a missing row.

### Ledger — milestone 4
CSV, header row, **sorted by `customerId` ascending**:
```
customerId,betCount,staked,returned,net
C001,3,30.00,45.00,15.00
```
- `betCount` counts every bet that was not rejected, including pending ones.
- `staked` is the total stake of those bets.
- `returned` is the total returned so far — a pending bet contributes 0.
- `net` is `returned - staked`.

Rejected bets contribute nothing at all: not to the count, not to the stake.

---

## 8. The milestones

Every example below uses the same catalogue, so the numbers carry from one milestone to the next.

```
markets.csv
M001,Porto vs Benfica - Match Result,HOME|DRAW|AWAY
M002,Sporting vs Braga - Match Result,HOME|DRAW|AWAY
M003,Porto vs Benfica - Both Teams To Score,YES|NO
M004,Boavista vs Vitoria - Match Result,HOME|DRAW|AWAY
M005,Estoril vs Rio Ave - Match Result,HOME|DRAW|AWAY
M006,Famalicao vs Gil Vicente - Match Result,HOME|DRAW|AWAY

results.csv
M001,HOME     M002,AWAY     M003,VOID     M005,VOID     M006,HOME
                                          (M004 has no result yet)
```

---

### M1 — settle a single bet

**Goal:** read the three files, settle every bet, print the report. Every bet has exactly one leg
and every market has a result, so only `WON` and `LOST` can happen.

```
bets.csv
B001,C001,10.00,M001:HOME:2.50
B002,C002,5.00,M001:DRAW:3.40
B003,C001,20.00,M002:AWAY:1.80
```

```
betId,customerId,outcome,stake,returned,detail
B001,C001,WON,10.00,25.00,
B002,C002,LOST,5.00,0.00,
B003,C001,WON,20.00,36.00,
```

B001 backed `HOME` and `HOME` came in: 10.00 × 2.50 = **25.00**. B002 backed `DRAW` and lost the
lot. B003: 20.00 × 1.80 = **36.00**.

**Watch for:** the returned amount includes the stake — a winning bet at 2.50 pays 25.00, not
15.00. And the report is sorted by `betId`, not by whatever order the file happened to be in.

---

### M2 — voids, pending, and bets we refuse

**Goal:** the three situations M1 pretended did not exist.

```
bets.csv  (added)
B004,C003,15.00,M003:YES:1.90        market was voided
B005,C002,8.00,M004:HOME:2.20        no result yet
B006,C003,10.00,M999:HOME:2.00       no such market
B007,C001,10.00,M001:YES:2.00        M001 does not offer YES
B008,C002,0.00,M001:HOME:2.50        stake of zero
B009,C004,10.00,M002:HOME:0.90       odds below 1.00 would take money off a winner
B016,C001,10.00,M001:HOME:1.00       odds of exactly 1.00, which are fine
```

```
B004,C003,VOID,15.00,15.00,
B005,C002,PENDING,8.00,0.00,
B006,C003,REJECTED,10.00,0.00,UNKNOWN_MARKET M999
B007,C001,REJECTED,10.00,0.00,UNKNOWN_SELECTION M001:YES
B008,C002,REJECTED,0.00,0.00,INVALID_STAKE 0.00
B009,C004,REJECTED,10.00,0.00,INVALID_ODDS 0.90
B016,C001,WON,10.00,10.00,
```

**Watch for:** `PENDING` is not an error and not a loss — we simply do not know yet, and the bet
will settle later. `VOID` returns the stake, so the customer is neither up nor down. `REJECTED` is
the only one of the three that is our way of saying the bet was never valid in the first place.

Three different kinds of "no", and the type you choose to represent them is most of this milestone.

B009 and B016 are the boundary, deliberately next to each other: 0.90 is refused, 1.00 is accepted
and pays back exactly the stake. Note that B016 is a `WON` bet that returns the same money as a
`VOID` one — the same amount, a different label, and the customer would want to know which.

---

### M3 — accumulators

**Goal:** a bet spans several legs. All of them must come in.

```
bets.csv  (added)
B010,C001,10.00,M001:HOME:2.50|M002:AWAY:1.80
B011,C002,5.00,M001:HOME:2.50|M002:HOME:3.10
B012,C003,10.00,M001:HOME:2.50|M003:YES:1.90
B013,C004,20.00,M001:HOME:2.50|M004:HOME:2.20
B014,C002,12.00,M003:YES:1.90|M005:HOME:2.40
B015,C004,10.00,M001:HOME:1.33|M002:AWAY:1.33|M006:HOME:1.33
```

```
B010,C001,WON,10.00,45.00,
B011,C002,LOST,5.00,0.00,
B012,C003,WON,10.00,25.00,
B013,C004,PENDING,20.00,0.00,
B014,C002,VOID,12.00,12.00,
B015,C004,WON,10.00,23.53,
```

| Bet | What happened |
|---|---|
| **B010** | Both legs won: 10.00 × 2.50 × 1.80 = **45.00** |
| **B011** | Leg two backed `HOME` and `AWAY` came in. One losing leg sinks the bet. |
| **B012** | Leg two's market was voided, so its multiplier is **1.00**: 10.00 × 2.50 × 1.00 = **25.00**. The four-leg accumulator quietly became a single. |
| **B013** | Leg one won, but leg two has no result. The whole bet waits. |
| **B014** | Both legs voided, so the bet is `VOID` and the stake comes back. |
| **B015** | 10.00 × 1.33 × 1.33 × 1.33 = 23.52637 → **23.53**. Round each leg on the way and you get 23.52. |

**Watch for:** M1's single bets are just accumulators with one leg — if you find yourself writing a
separate code path for them, you have missed the refactor this milestone is really about. The order
of the bet-level rules matters too: B013 has a winning leg and a pending leg, and it is `PENDING`,
not `WON`.

---

### M4 — results arrive over time, and we keep a ledger

**Goal:** stop reading results from a finished file. They now arrive as a stream of events, and you
fold them into state as they come.

```
result-events.csv
1,M001,HOME
2,M002,AWAY
3,M003,VOID
4,M001,HOME     <- the same result again
5,M999,AWAY     <- a market we have never heard of
6,M005,VOID
7,M006,HOME
```

Apply them in `sequence` order. Event 4 must change nothing — applying a result twice is the same
as applying it once. Event 5 is reported and skipped; it does not stop the run.

Then print the ledger, one row per customer, sorted by `customerId`:

Over all fifteen bets above:

```
customerId,betCount,staked,returned,net
C001,4,50.00,116.00,66.00
C002,4,30.00,12.00,-18.00
C003,2,25.00,40.00,15.00
C004,2,30.00,23.53,-6.47
```

C001 has five bets in the file — B001, B003, B010, B016, and the rejected B007. **The rejected one
is not in the row at all**: not in the count, not in the stake. The other four staked 50.00 and
returned 25.00 + 36.00 + 45.00 + 10.00 = 116.00, so C001 is 66.00 up — B016 at odds 1.00 moved the
staked and returned totals by the same 10.00 and left `net` exactly where it was.

C002 has five, one of them rejected. B005 is still pending and B013's sibling is too: a pending bet
counts toward `betCount` and `staked` and contributes 0.00 to `returned`, which is why C002 shows
30.00 staked against 12.00 back — the 12.00 being B014's voided stake returned.

**Watch for:** the ledger is a `foldLeft` over the events, and the state it carries is your whole
settlement picture. This is the same shape as replaying an account history, and it is the reason
day 2 kept saying so.

---

### M5 — behind a server *(bonus)*

**Goal:** the same engine, reachable over HTTP, with results arriving while somebody is reading.

Two endpoints:

```
GET /ledger/C001
    -> 200  {"customerId":"C001","betCount":4,"staked":50.00,"returned":116.00,"net":66.00}

GET /ledger/C999
    -> 404  {"error":"UNKNOWN_CUSTOMER"}

POST /results?marketId=M004&outcome=HOME
    -> 200  {"accepted":true,"settled":2}

POST /results?marketId=M999&outcome=HOME
    -> 404  {"error":"UNKNOWN_MARKET"}
```

M4 reported unknown markets to standard error and carried on, because there was nobody to tell.
Over HTTP there is, so say so — silently accepting a result for a market that does not exist is how
a typo in an upstream feed goes unnoticed for a week.

The result arrives as query parameters, so nothing in your program has to *parse* JSON — only
produce it, which you already know how to do.

`settled` is how many bets stopped being `PENDING` because of that result — here B005 and B013, the
only two waiting on M004. Post the same result again and the second call settles nothing:

```
POST /results?marketId=M004&outcome=HOME
    -> 200  {"accepted":true,"settled":0}
```

...which is M4's idempotence, now over HTTP. And the ledger has moved:

```
GET /ledger/C002
    -> 200  {"customerId":"C002","betCount":4,"staked":30.00,"returned":29.60,"net":-0.40}
```

B005 came in at 8.00 × 2.20 = 17.60, on top of the 12.00 already there.

**The acceptance criterion is that the server agrees with the batch program.** Load the same data,
post the same results, and `GET /ledger/C001` must produce the numbers M4 printed.

**Watch for:** this is the first time two things touch your state at once. A result being posted
while a ledger is being read must never show a half-applied picture, and two results posted at the
same instant must not lose one of them. You want an `AtomicReference` holding an immutable snapshot
and `updateAndGet` to move it forward — the same fold as M4, one atomic step at a time.

Render the JSON with the renderer you wrote on day 2. It is the same `Json` type.

---

## 9. Where this is simplified

Said plainly so nobody is misled, and so you know what a real system adds:

- **Only decimal odds.** Real books also quote fractional (`5/2`) and American (`+250`).
- **No dead heats.** When two selections tie, a real book pays a reduced stake. Here a market has
  exactly one winner, or it is voided.
- **No each-way**, no partial cash-out, no in-play price changes, no bet limits, no liability caps,
  no customer restrictions.
- **No stake reduction** on voided legs, no "non-runner" adjustments.
- **Money as `BigDecimal`.** Plenty of real systems store integer minor units (cents) instead,
  precisely to avoid the rounding conversation in section 5.

Several of these are on the stretch ladder if you finish early.
