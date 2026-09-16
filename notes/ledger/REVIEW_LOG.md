# Review log

Every hand-written scenario's `.out` was read line by line next to its `.in` (side by side with
the interleave view, today `Run --interleave`) against PROTOCOL.md / PROTOCOL-PART2.md. Checklist per file: sequence
numbers dense and only on accepted commands · every rejection's code and key order · precedence
cases · money rendering · idempotent FREEZE/UNFREEZE consume a number · queries never do · SUMMARY
arithmetic by hand on at least two windows · HISTORY indentation and order · currency conservation
across transfers where the file claims it.

## 2026-09-01 — initial generation (reference at first commit)

| File | Reviewed | Notes |
|---|---|---|
| m1/easy-1, easy-2, medium-1, large | ✔ | large: acct-01/04/06 balances re-added by hand |
| m2/easy-1, easy-2, medium-1, medium-2, large | ✔ | large: ann drains to exactly 0.00; all 56 seq |
| m3/easy-1, medium-1, large, replay | ✔ | replay: B resumes at OK 16; frozen/closed state survives |
| m4/easy-1, easy-2, medium-1, large, replay | ✔ | medium-1 is the §7.5 order, one case per rule; large: USD 1750.03 / EUR 600.00 conserved; replay resumes at OK 13, t3 |
| m5/easy-1, easy-2, medium-1, large | ✔ | medium-1: every SUMMARY matched a hand-derived value; large: straddling windows 12–13, 13–14 checked |
| hidden m1/medium-1, large | ✔ | |
| hidden m2/medium-1, large | ✔ | closed+non-positive, frozen+insufficient |
| hidden m3/medium-1, large, replay | ✔ | replay: B resumes at OK 21 |
| hidden m4/medium-1, large, replay | ✔ | both-frozen + mismatch → frozen source; closed source + unknown dest → unknown; USD 2100.00 / EUR 100.00 conserved; replay resumes at OK 13, t4 |
| hidden m5/medium-1, large | ✔ | windows opening between a transfer's two facts |
| PROTOCOL.md / PART2 worked examples | ✔ | diffed against the reference: identical |

Huge files (`*/huge.in`, `*/replay-huge-*`): not reviewed by hand, by design. Generator coverage
tables printed at generation; the invariant checks (sequence density, SUMMARY arithmetic), today in
`tools.Verify`, pass on all of them.

## `var` counter check — 2026-09-01

- Sequence number held in a `var`, reset on `empty`/`replay`: `m3` and `m4` red, *only* on the
  "after replaying the journal" checks; the one-run goldens stay green. `m1`, `m2`, `m5` green.
- Transfer tag held in a `var`, reset the same way: `m4` red only, same shape.
- Both changes reverted; `ReferenceLedgerSuite` + `HiddenReferenceSuite` green afterwards.

## 2026-09-02 — tooling and layout changes (content unchanged)

The hidden set was merged into `src/main/resources/ledger/` — there is no hidden set any more.
Renames, per milestone: `hidden-medium-1` → `medium-2` (m2: `medium-3`), `hidden-large` →
`large-2`, `hidden-replay-*` → `replay-2-*`. File contents are byte-identical apart from the
`# purpose` headers losing the word "hidden"; the reviews above still stand. `sbt ledgerVerify`
(91 checks) confirms every `.out` still matches the reference and every huge `.in` its generator.
`ledger/README.md` + the two protocol files are now the complete attendee-facing spec
(`MILESTONES.md` was folded into the guide).
