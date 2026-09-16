# Day 3 — the bet settlement engine

Read [RULES.md](RULES.md) first. It is the complete specification: what we are building, how it
works, every settlement rule, and a worked example of each milestone. Nothing about betting is
assumed — it is all on the card.

---

## Getting started

```bash
sbt compile                                   # should already work
./betsettlement/check m1                               # shows what M1 still wants from you
```

`check` runs your program and diffs its output against the expected output for that milestone. It
prints `m1: OK` when you match.

To run your program yourself and look at what it printed:

```bash
sbt "runMain com.rockthecode.betsettlement.engine.Main betsettlement/data/m1"
```

**There are no hidden tests.** The expected output is in `betsettlement/expected/` and you are welcome to
read it whenever you like — including before you start.

---

## Where you work

Everything you write goes in `src/main/scala/com/rockthecode/betsettlement/engine/`. You start with two
files:

| File | |
|---|---|
| `Csv.scala` | Reading CSV, and splitting the `legs` column. Finished; leave it alone. |
| `Main.scala` | Your program. Currently prints an empty report. |

Suggested files to add, chosen so that three people are rarely in the same one:

```
Model.scala      your types - do M0 on paper before you open this
Parsing.scala    Csv rows -> your model
Engine.scala     validation and settlement. Pure: data in, data out.
Report.scala     your model -> lines of text. Builds strings, prints nothing.
Main.scala       reads files, prints lines. The only place allowed to do either.
```

---

## Milestones

Tick a milestone when `./betsettlement/check` is green for it, then tag it:

```bash
git tag m1-done && git push --tags
```

- [ ] **M0 — model it.** On paper, before any code. What are the types? Show them to Daniel, then
      compare with the reference model. Nothing is committed for this one.
- [ ] **M1 — settle single bets.** `./betsettlement/check m1`
- [ ] **M2 — voids, pending, rejections.** `./betsettlement/check m2`
- [ ] **M3 — accumulators.** `./betsettlement/check m3` — *demo this one to Daniel.*
- [ ] **M4 — the event stream and the ledger.** `./betsettlement/check m4`
- [ ] **M5 — the server** *(bonus)*. No expected file; the acceptance test is the curl sequence in
      RULES.md section 8, and the numbers must agree with what M4 printed.

**M1 to M3 is what every team finishes. M4 is expected. M5 is a bonus and it is fine not to get
there.**

If you are stuck at a checkpoint, take the reference for the milestone you are on and carry on with
your own hands from there:

```bash
git fetch reference && git checkout -b m3 reference/m2
```

You lose your code for that milestone. You do not lose your place, and you will not spend the
afternoon behind.

---

## Working as three

- **Commit at least every fifteen minutes.** A team that has not committed since 10:00 and hits a
  conflict at 14:00 is finished for the day.
- **Rebase, do not merge.** `git pull --rebase`.
- **Split by file, not by function.** Agree who owns which file for the current milestone. Three
  people in one 200-line `Engine.scala` will conflict constantly; three people in four files
  rarely will.
- `.scalafmt.conf` is in the repo. Let your editor use it, and formatting will never appear in a
  diff.

---

## Stretch, if you finish

In rough order of how interesting they are:

1. **Cash-out valuation** — what is a partially-settled accumulator worth right now, if the
   remaining legs are priced at their current odds?
2. **Each-way bets** — half the stake on the win, half on the place, settled separately.
3. **Liability report** — per market, what would we owe if each selection won? Which result is
   worst for us?
4. **Fractional and American odds** — `5/2` and `+250` in the odds column, parsed into the decimal
   odds you already handle.
5. **Snapshot and replay** — write the ledger out, read it back, carry on from event 8.

---

## On AI

Use it, exactly as you would at work. The one rule matches Blip's own: **anyone on the team must be
able to explain any line in your repo when asked.** At the M3 demo, expect to be asked.
