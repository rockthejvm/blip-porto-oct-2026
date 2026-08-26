# TODO — Blip Porto training prep

Companion to [training-plan.md](training-plan.md). Nothing here has been started yet.

Status key: `[ ]` open · `[~]` in progress · `[x]` done · `[?]` needs a decision from Daniel or from Blip

---

## Now — day 2 design

- [~] **Settle day 2 block by block.** One block per conversation, in order. Decide topics, then exercises, then write the stubs.
  - [x] Block 0 — collections drill *(conditional; overflow buffer for day 1)* — code, solutions, stretch set and tests written
  - [x] Block A — thinking in expressions — code, solutions, stretch set and tests written
  - [ ] Block B — collections and folds
  - [ ] Block C — ADTs and pattern matching
  - [ ] Block D — Option
  - [ ] Block E — Try, Either, error modeling
  - [ ] Block F — laziness
  - [ ] Block G — Futures and concurrency
- [ ] Write the collections cheat sheet handout (ships with Block 0 / day-1 collections section).
- [?] **Actors**: 30–45 min conceptual demo at the end of day 2, or trade a block for a real one? Depends on how much Akka they actually run.
- [?] **Akka or Pekko?** Ask Blip. Example imports and licensing talk depend on the answer.

**Standing constraint:** don't force the betting domain into exercises. Use it where it fits; otherwise neutral realistic scenarios. Daniel is not a domain expert and won't present as one.

## Next — day 3

- [ ] **Research the betting domain** before designing the project. Markets, selections, odds formats, bet types, settlement rules, voids/pushes, each-way, accumulators, cash-out. Daniel does not know this domain — needs enough to write a credible brief and answer questions in the room.
- [ ] Design the bet settlement engine project: model skeleton, file formats, sample data.
- [ ] Generate the sample data files (markets, bets, results).
- [ ] Write the starter repo: skeleton + a few passing tests + a failing target test.
- [ ] Write the ranked stretch-goal list.
- [ ] Write the per-team "definition of done" checklist.
- [ ] Write the briefing doc handed out at the start of day 3.

## Build and repo infrastructure

- [x] **Add a test library** — MUnit 1.2.0, `% Test`. Still worth checking what Blip uses internally, in case ScalaTest syntax would be more familiar.
- [x] Add `scalacOptions`: `-deprecation`, `-feature`. `-Wunused:all` still open — it will be noisy on the day-1 files.
- [?] **Decide how solutions are distributed.** They currently live in `com.rockthecode.day2.solutions` in `src/main`, which means attendees can read them. Options: strip that package from the advance repo and merge it live after each block, or move it to a `solutions` branch. Must be settled before the repo goes out.
- [ ] Consider a `notes/day2-block-format.md` or README section explaining the exercise/solutions/test layout to attendees.
- [ ] Every later block needs a stretch section too, in the same four files below a `STRETCH SECTION` banner. Budget for it when designing B through G.
- [x] Command aliases in `build.sbt`: `block0`, `blockA`, `checkSolutions`.
- [!] **Never check material with plain `sbt test`** — it stops at the deliberately red exercise suites and never reaches the solutions, reporting 74 failures / 0 passes. Use `sbt checkSolutions` (75 green).
- [?] **Scala version.** Currently pinned to 3.8.4. Switch to 3.3.x LTS if that is closer to Blip production. Confirm what they run.
- [ ] Decide what ships in the advance repo vs. what is added live. `notes/` is trainer-internal — strip it, gitignore it, or move it out before sending the repo to attendees.
- [ ] Add a README for attendees: how to build, how to run, what to do if setup breaks.

## Day 1 — additions (agreed)

Nothing gets cut. Implicits and concurrency stay — the goal there is recognition of other people's code.

- [ ] **Collections transformation API**: `foldLeft`, `groupBy`, `partition`, `sortBy`, `collect`, `sliding`, `zip`, `sum`, `maxByOption`. The biggest gap — this is what replaces their `for` loops.
- [ ] **Collections cheat sheet** handout.
- [ ] **Immutability as a named topic**: `val` vs `var`, `.copy`, structural sharing, "state change = `State => State`".
- [ ] **`sealed` traits / `enum` + ADTs** with pattern matching, and the live exhaustivity demo (add a case, watch the compiler list every match).
- [ ] **`Either`** — introduce alongside `Option`/`Try`. Named by Big Guy as a struggle, currently absent.
- [ ] **`LazyList`** — brief introduction; follows naturally from call-by-name, which is already in the file. Practised on day 2.
- [ ] **Scala 2 vs 3 cheat sheet**: `implicit` vs `given`/`using`, `implicit class` vs `extension`, `sealed trait` vs `enum`, optional braces.

## Day 1 — repo fixes

- [ ] **Replace "Adobe" with "Blip"** — 3 occurrences in `ScalaEssentials.scala` (`helloAdobe`, the `"Hello, Adobe"` literal, "time at Adobe" in the intros block).
- [ ] **Remove side effects from object bodies.** `println`, `producer.start()`, `Thread.sleep(1000)`, `Thread.sleep(10000)` all run at object-init time — touching anything in `AdvancedScala` runs everything, including 11 seconds of sleeping. Split per-topic into small objects with their own entry points.
- [ ] **Split `AdvancedScala.scala`** (~497 lines) into `PatternMatching.scala`, `ErrorHandling.scala`, `Concurrency.scala`, `Implicits.scala`.
- [ ] `Promise[A]` → `Promise[A]()` — 3 occurrences, currently a Scala 3 syntax warning.
- [ ] `daniel likes "Forrest Gump"` — annotate `infix def likes`, or use the warning to make the point about Scala 3 tightening alphanumeric infix.
- [ ] Consider pre-writing the generic signatures for `MyList` part 3 so attendees fill in bodies instead of fighting `[S >: T]` type errors.

## Logistics

- [x] Send the repo to attendees in advance so every build is warm on day 1. *(Daniel handles this.)*
- [ ] Confirm the room setup: focus/breakout rooms for exercise time — this is what worked in previous trainings.
- [ ] Confirm group size and how many teams of ~3 that makes for day 3.
- [?] Ask Blip how much prior Scala experience the attendees have, so day 1 can be paced rather than skipped on the fly.
