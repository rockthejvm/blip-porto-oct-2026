# Day 3 — trainer's briefing notes

Companion to [training-plan.md](training-plan.md) §5. This is what to say and when.

---

## Before the day

- Push a repo per team from the template; teams add you as a collaborator.
- Have `reference/m1`…`reference/m4` ready to push, but **do not push them yet** — they go out at
  each checkpoint.
- Check `sbt compile` and `./betsettlement/check m1` work on a clean clone. The first should be instant
  because they ran it during day 1 pre-work; the second downloads nothing new.
- Put the milestone board up: team names down, M0–M5 across.

---

## 09:00 — the briefing (20 min)

Five things, in this order:

1. **What we are building**, in one sentence: the part of a betting platform that works out, once
   the football is over, who won what. Three CSVs in, one CSV out.
2. **Hand out the rules card.** Say plainly: *nobody in this room needs to know anything about
   betting, including me. Everything is on the card, and where it simplifies real settlement the
   card says so.*
3. **How acceptance works.** `./betsettlement/check m1`. No hidden tests; the expected output is in the repo
   and they can read it now. This surprises people and it is worth pausing on — it changes how they
   work, and it means "am I done?" is never a question they have to queue for.
4. **The contract.** M1–M3 is what everyone finishes. M4 is expected. M5 is a bonus. Say this out
   loud so that not reaching M5 does not feel like failing.
5. **How the day runs.** A checkpoint after each milestone, where the reference goes out. If you
   are stuck, take it and carry on — you lose that milestone's code, not your place.

Then teams, and go.

---

## 09:30 — M0, on paper (30 min)

No compiler. The question is: **what are the types?**

Circulate and ask two questions:

- *What happens to a bet on a market that has no result yet?* (Looking for: it is not an error and
  it is not a loss.)
- *What does a voided leg do to an accumulator?* (Looking for: nothing much — which is the answer
  they will not have yet, and that is fine. Plant it.)

**The reveal** is a five-minute walk through the reference model. The thing to draw out is not
"ours is right" — it is *what did you have that we do not, and what did we have that you did not?*
Two differences are worth hunting for:

- Did they give a **leg** its own outcome, or only a bet? The card states the rules leg-first. A
  team that only modelled bet outcomes will rewrite in M3.
- Is **rejection** an `Outcome`, or something else? A rejected bet was never settled — it is the
  left-hand side of an Either, not a fifth kind of settlement. Teams that make it a fifth outcome
  will find M4's ledger awkward, because now they have to filter it back out.

Do not tell them they are wrong. Let M3 do that.

---

## Checkpoints

Each one is 5–8 minutes, and the shape is the same:

1. Push `reference/mN`.
2. Walk the *one thing that changed*. Not the whole file.
3. Say what the next milestone will demand.

| After | Show | Say |
|---|---|---|
| M1 | `Engine.settle` | "Notice `payout` rounds once, at the end. Nothing else in the program rounds anything." |
| M2 | `validate` returning `Either` | "Validation happens first, so nothing downstream has to wonder whether the market exists. And `NoLegs` is unreachable — the type is doing the checking." |
| M3 | `multiplier`'s fold | "There is no case for voided legs. 1.00 is the identity, so they drop out of the product on their own." |
| M4 | `applyEvents` | "Idempotence is free, but only because the state is a Map keyed by market rather than a list of things that happened." |

---

## 13:15 — M3, and the demo round

This is the milestone that decides the day, and the one you inspect.

**Per team, three minutes.** Ask exactly two things:

1. *Show me `check m3` green.*
2. *How did your M0 model survive contact with accumulators?*

The second question is the whole point of the day. Listen for teams who had to change `Bet` — and
ask what they would model differently if they started again.

If a team is still on M2 at 14:00, give them `reference/m2` and move them on. Do not let anyone
spend the afternoon behind.

---

## The things worth saying out loud, once each

- **M3's void rule.** A four-leg accumulator with one voided leg *is* a three-leg accumulator. Not
  by a special case — because 1.00 is the multiplicative identity. If one idea survives the day,
  this is a good candidate.
- **M4's fold.** The ledger is `foldLeft` over events. This is A.2's `applyAll` with a bigger state,
  and it is why day 2 kept saying so.
- **M5's retry loop.** The compare-and-set loop can throw its work away and try again *because
  `withResult` is pure*. That is the day-1 argument for purity turning up somewhere nobody would
  have predicted on day 1.

---

## 16:15 — closing demos (45 min)

Everyone built the same thing, so "here is my app" is dull. Tell them at the briefing that the five
minutes will be:

- **What did M0 get wrong that M3 revealed?**
- **Which stretch did you take, and what did it cost you?**

Close on the arc rather than the project: day 1 was the language, day 2 was the vocabulary, day 3
was the same vocabulary against a problem with real rules. Point out that the settlement engine is
maybe 300 lines and that almost none of it is clever — which is the argument for the style, not the
language.

---

## Things that will go wrong

| | |
|---|---|
| Port 8080 taken (M5) | The reference uses **8899**. Tell them at the M4 checkpoint, not when it breaks. |
| sbt kills the server (M5) | `run / fork := true` is already in `build.sbt`. Do not let anyone remove it. |
| A team modelled rejection as an outcome | Fine until M4. Let them find it; help them refactor rather than rewrite. |
| Rounding disagreements | The card is the authority: HALF_UP, 2dp, once, at the end. B015 is the test case. |
| A team has not committed since morning | Ask every team at the first checkpoint. Once. |
