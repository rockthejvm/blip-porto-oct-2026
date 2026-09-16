# Ledger — trainer's notes on the repo

Day-3 candidate B (decision between this and `betsettlement` still open). Everything
attendee-facing lives in `ledger/` (the guide plus the two protocol documents),
`src/main/resources/ledger/` (all scenarios — there is no hidden set; "no hidden tests" is part of
the pitch, as in the betting project) and `com.rockthecode.ledger` (code). Everything in this
directory and in `com.rockthecode.ledger.solution` — including the `solution` test file and the two
trainer aliases in `build.sbt` — is trainer-only.

## Before handing out

- **Strip `src/main/scala/com/rockthecode/ledger/solution/`, `src/test/scala/com/rockthecode/ledger/solution/`,
  `notes/`, and the `ledgerGen`/`ledgerVerify` aliases** from the copy attendees get. Same decision
  as the day-2 solutions package. A leaked solution kills the modelling exercise.
- Check `sbt compile` and `sbt ledger` (five red tests with readable messages) on the stripped copy.
- Hand out PROTOCOL.md on paper at 9:00 if you can; PROTOCOL-PART2.md is in the repo but you *say*
  when it may be read (14:15).

## Commands

| | |
|---|---|
| `sbt ledger` | attendee suite (`MyLedgerSuite`), one test per milestone |
| `sbt "testOnly *ReferenceLedgerSuite"` | reference against every scenario; also part of `sbt checkSolutions` |
| `sbt ledgerGen` | regenerate everything the tools own: huge `.in` files (seeded, byte-identical) and every `.out` |
| `sbt ledgerVerify` | check every committed scenario file — OK / NOT OK per file with the first differing line; also sequence-number density and SUMMARY arithmetic |
| `sbt "runMain com.rockthecode.ledger.solution.tools.Run [--interleave] x.in"` | reference output for one scenario; `--interleave` shows input and output side by side, the hand-review format |

## Lunch progress check

There is no hidden grading set: every scenario ships in the repo, and a milestone's pass/fail in
`sbt ledger` *is* the grade. Over lunch, glance at each team's repo (you are a collaborator): the
last commit and which tests are green tell you who is where. The nastier scenarios that used to be
hidden are the `medium-2`/`medium-3`/`large-2`/`replay-2-*` files, in the same directories as the
rest.

## Changing the protocol or the reference

1. Edit the solution; `sbt "testOnly *ReferenceLedgerSuite"` — a changed rule fails the goldens,
   which is the point.
2. `sbt ledgerGen` to regenerate. **Then hand-review the affected easy/medium/large files** with
   `Run --interleave` and tick them in [REVIEW_LOG.md](REVIEW_LOG.md). The huge files are only as
   trusted as the small ones.
3. `sbt ledgerVerify` and `sbt checkSolutions`.

## Regression checks that are not committed

The replay test's value depends on it catching a counter held in a `var`. Checked by hand on
2026-09-01 (see REVIEW_LOG.md): a `var` sequence counter turns `m3` and `m4` red on their "after
replaying" checks only; a `var` transfer counter turns `m4` red only. Repeat after any change to
`LedgerSuite.replay`.
