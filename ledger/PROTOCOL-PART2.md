# Protocol — part 2

**Don't read this before lunch.** It will spoil a design decision you are about to make on paper,
and the decision is worth more if you make it first. (It is in the repo because hiding files from
engineers is silly, not because you are meant to open it.)

This part adds `TRANSFER` (milestone 4) and `HISTORY` / `SUMMARY` (milestone 5). Everything in
[PROTOCOL.md](PROTOCOL.md) still applies.

## 1. `TRANSFER <from> <to> <amount>`

Moves money between two accounts of the same currency. A successful transfer records **two facts**
and prints two `OK` lines, the debit first, both carrying the same transfer tag:

```
OK 7 acc-1 -50.00 45.00 xfer=t1
OK 8 acc-2 +50.00 50.00 xfer=t1
```

The tag is `t` followed by a 1-based count of **successful** transfers in the ledger's whole history.
Rejected transfers do not advance it. Like the sequence number, it must survive a restart.

Precedence, checked in exactly this order:

1. `same-account-transfer` — `REJECTED same-account-transfer account=<acc>` (checked before either account is looked up)
2. `unknown-account` on the source
3. `unknown-account` on the destination
4. `account-closed` on the source
5. `account-closed` on the destination
6. `account-frozen` on the source
7. `account-frozen` on the destination
8. `non-positive-amount` (`account=` is the source)
9. `currency-mismatch` — `REJECTED currency-mismatch from=<acc> to=<acc> from-currency=<CUR> to-currency=<CUR>`
10. `insufficient-funds` (`account=` is the source)

A frozen account can be neither the source nor the destination of a transfer.

## 2. `HISTORY <acc>`

A read-only view of one account's facts, oldest first, reusing the `<effect>` grammar of the `OK`
lines exactly (including `xfer=` tags):

```
HISTORY <acc> <count>
  <seq> <effect>
  <seq> <effect>
```

The header gives the number of facts; each detail line is indented by exactly two spaces. `HISTORY`
on an unknown or closed account is a rejection (same precedence as `BALANCE`); on a frozen account
it is answered. It records nothing and consumes no sequence number.

## 3. `SUMMARY <acc> <fromSeq> <toSeq>`

Aggregates one account's facts over the **inclusive** window of global sequence numbers
`[fromSeq, toSeq]`, and prints one line:

```
SUMMARY <acc> <fromSeq> <toSeq> opening=<amount> credits=<amount> debits=<amount> closing=<amount> count=<n>
```

- `opening` — the account's balance after all of its facts with `seq < fromSeq` (`0.00` if none).
- `credits`, `debits` — sums of the account's credits and debits with `fromSeq <= seq <= toSeq`.
  Debits are reported as a positive number.
- `closing` — `opening + credits - debits`. It must also equal the balance after all of the
  account's facts with `seq <= toSeq`; if your implementation can disagree with itself here,
  something is wrong.
- `count` — the number of the account's facts in the window, **including** `OPENED`, `FROZEN`,
  `UNFROZEN` and `CLOSED`.

A window with none of the account's facts is fine: `credits=0.00 debits=0.00 count=0`, and
`opening` equals `closing`. Sequence numbers in the window need not exist yet.

Two more `ERROR` lines, checked after the account id and before anything is looked up:

```
ERROR bad-sequence <token>          # <fromSeq> or <toSeq> does not match \d+ (checked left to right)
ERROR bad-range <fromSeq>-<toSeq>   # fromSeq > toSeq, e.g. "ERROR bad-range 9-5"
```

`SUMMARY` on an unknown or closed account is a rejection; on a frozen one it is answered.

## 4. Worked example

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
