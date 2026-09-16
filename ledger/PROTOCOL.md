# Protocol — part 1

The exact text format your ledger reads and writes. Every character matters: the tests compare your
output with the expected files line by line (trailing whitespace is ignored, nothing else is).

This part covers the commands for milestones 1–3: `OPEN`, `DEPOSIT`, `WITHDRAW`, `BALANCE`,
`FREEZE`, `UNFREEZE`, `CLOSE`. [PROTOCOL-PART2.md](PROTOCOL-PART2.md) adds three more after lunch.

## 1. Input

One command per line. Fields are separated by one or more spaces (or tabs). Leading and trailing
whitespace is stripped. Blank lines, and lines whose first non-space character is `#`, produce
**no output at all** — not even an empty line.

Verbs are uppercase. Account ids match `[a-z0-9-]{1,32}`. Currency codes are three uppercase
letters; the supported ones are `USD`, `EUR`, `GBP`, all with two decimal places.

| Command | Arguments | Milestone |
|---|---|---|
| `OPEN <acc> <CUR>` | 2 | M1 |
| `DEPOSIT <acc> <amount>` | 2 | M1 |
| `BALANCE <acc>` | 1 | M1 |
| `WITHDRAW <acc> <amount>` | 2 | M2 |
| `FREEZE <acc>` | 1 | M2 |
| `UNFREEZE <acc>` | 1 | M2 |
| `CLOSE <acc>` | 1 | M2 |

## 2. Money

An input amount matches `-?\d+(\.\d{1,2})?`. So `100`, `100.5`, `100.50` and `-5.00` all **parse**.
`1.005` does not (too many decimals), nor does `.5`, `5.`, `5,00` or `five`.

Output amounts always have exactly two decimals and no thousands separator: `0.00`, `100.50`,
`1234567.89`. Negative amounts have a leading `-`: `-5.00`.

Yes, a negative amount parses. Parsing only asks "is this shaped like an amount?"; whether the
amount is acceptable is decided later, with a proper rejection (`non-positive-amount`). Keep the two
questions apart.

## 3. Output — accepted commands

Every accepted command prints one line per fact it recorded:

```
OK <seq> <acc> <effect>
```

`<seq>` is a **global, 1-based, strictly increasing sequence number** over every fact the ledger has
ever recorded, across all accounts. It never resets, never skips, and is never given to a rejected
command, a query, or a malformed line.

`<effect>` is one of:

| Effect | Meaning |
|---|---|
| `OPENED <CUR>` | the account now exists |
| `+<amount> <balance>` | a credit, followed by the balance after it |
| `-<amount> <balance>` | a debit, followed by the balance after it |
| `FROZEN` | the account is now frozen |
| `UNFROZEN` | the account is now not frozen |
| `CLOSED` | the account is now closed |

Note what this format does **not** say. It never names the fact your program recorded; it describes
what an observer would notice. Credits and debits look the same apart from the sign. How you model
the facts behind these lines — one kind of adjustment or several, an enum of states or something
else — is your decision, and the mapping from your model to these lines is yours to write.

## 4. Output — rejections

An accepted command may print several lines; a rejected one prints exactly one:

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

Key–value pairs appear in exactly this order, single-spaced, no spaces around `=`. Amounts are
rendered as in §2 (`amount=-5.00`, `amount=0.00`).

## 5. Rejection precedence

When several rejections could apply, exactly one is printed, chosen by this order:

For `DEPOSIT`, `WITHDRAW`, `FREEZE`, `UNFREEZE`, `CLOSE`, `BALANCE`:

1. `unknown-account`
2. `account-closed`
3. `account-frozen`
4. `non-positive-amount`
5. `insufficient-funds`
6. `non-zero-balance`

For `OPEN`: `account-exists` before `unsupported-currency`.

Rules that go with it:

- `FREEZE` on an already frozen account, and `UNFREEZE` on an account that is not frozen, are
  **idempotent successes**: they record a fact, print an `OK` line and consume a sequence number.
- `CLOSE` needs a balance of exactly `0.00`; otherwise `non-zero-balance`.
- **Closure is terminal.** Every command on a closed account is rejected with `account-closed` —
  including `BALANCE` and `DEPOSIT`. There is no reopen; `OPEN` on a closed id is `account-exists`.
- A **frozen** account rejects `DEPOSIT` and `WITHDRAW` with `account-frozen`, but permits
  `BALANCE`, `UNFREEZE` and `CLOSE` (which still needs a zero balance).

## 6. Output — queries

Queries record nothing and consume no sequence number.

```
BALANCE <acc> <amount> <CUR>
```

`BALANCE` on an unknown or closed account is a rejection (§5); on a frozen one it is answered.

## 7. Output — malformed input

A line that is not a well-formed command prints exactly one line and changes nothing:

```
ERROR unknown-command <verb>
ERROR bad-arguments <verb>
ERROR bad-account-id <token>
ERROR bad-amount <token>
ERROR bad-currency <token>
```

Order of checks within a line: the verb first (`<verb>` is echoed exactly as written, so
`deposit acc-1 5` gives `ERROR unknown-command deposit`), then the number of arguments, then the
arguments themselves from left to right. `bad-currency` is about shape only: `usd` is `bad-currency`,
`JPY` parses and is then rejected with `unsupported-currency`.

## 8. Worked example

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
