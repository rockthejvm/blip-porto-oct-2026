# TODO — Blip Porto training prep

Companion to [training-plan.md](training-plan.md). Nothing here has been started yet.

Status key: `[ ]` open · `[~]` in progress · `[x]` done · `[?]` needs a decision from Daniel or from Blip

---

## Now — day 2 design

- [x] **Day 2 is settled.** All eight blocks have briefs, stubs, reference solutions and tests.
  - [x] Block 0 — collections drill *(conditional; overflow buffer for day 1)* — code, solutions, stretch set and tests written
  - [x] Block A — thinking in expressions — code, solutions, stretch set and tests written
  - [x] Block B — collections and folds — code, solutions, stretch section and tests written
  - [x] Block C — ADTs and pattern matching — code, solutions, stretch section and tests written
  - [x] Block D — Option — code, solutions, stretch section and tests written
  - [x] Block E — Try, Either, error modeling — code, solutions, stretch section and tests written
  - [x] Block F — laziness — code, solutions, stretch section and tests written
  - [x] Block G — Futures and concurrency — code, solutions, stretch section and tests written
- [ ] Write the collections cheat sheet handout (ships with Block 0 / day-1 collections section).
- [x] **Actors — decided: not covered.** They cannot be practised without an introduction first, and that costs more time than the day has. Tell Blip so the omission is explicit.
- [ ] Mention to Blip that actors are out of scope, since Big Guy's handover mentioned them.

**Standing constraint:** don't force the betting domain into exercises. Use it where it fits; otherwise neutral realistic scenarios. Daniel is not a domain expert and won't present as one.

## Now — day 3

Day 2 is done. Design is settled (plan §5); this is the build.

- [x] **Project shape settled**: guided milestone ladder M0–M5, golden-file acceptance, git per team, Cask server as bonus.
- [x] **Cask verified** — 0.11.3 on Scala 3.8.4, routing + `@cask.postJson` + `AtomicReference` across requests.
- [x] **Rules card written** — `betsettlement/RULES.md`. Vocabulary, leg/bet settlement tables, rejection rules, the rounding stipulation, all four file formats, the exact output formats, and an explicit "where this is simplified" section.
- [ ] **Light domain research** — only enough to make terms, odds and sample data plausible. The rules are stipulated, so this is an afternoon, not a project.
- [x] Reference model designed — `betsettlement/m1` through `m5`, each a standalone package.
- [x] Sample data generated — `betsettlement/data/`, 6 markets and 16 bets covering every rule in the card.
- [x] Expected output per milestone — `betsettlement/expected/m1..m4.csv`, generated from the rules, not typed.
- [x] `betsettlement/check` written — `./betsettlement/check m1` prints `m1: OK` or a diff.
- [x] Attendee skeleton built — `betsettlement/engine/` with a finished `Csv` (rows + leg splitting) and a `Main` that compiles and prints an empty report, so `./betsettlement/check m1` shows a useful diff on minute one. `.scalafmt.conf` added.
- [x] Reference solutions written, one standalone package per milestone. All four batch milestones verified against the expected files; M5 verified by curl.
- [x] Briefing doc written — `notes/betsettlement-briefing.md`. Per-team checklist lives in `betsettlement/README.md`.
- [ ] Decide how the template repo reaches Blip's internal git host.

**Notes for the skeleton build:**
- `run / fork := true` is required in `build.sbt` — without it sbt's classloader tears the M5 server down on shutdown.
- The M5 server listens on **8899**, not 8080; 8080 was already taken on this machine and will be on some of theirs.
- `POST /results` takes **query parameters**, not a JSON body: nothing then has to *parse* JSON, only produce it. (`@cask.postJson` also JSON-encodes your response, so an already-rendered string comes back as a JSON string containing JSON.)

## Now — day 3, candidate B: the event-sourced ledger

Built 2026-09-01 from the spec in `notes/day3-ledger-spec.md`, plan in `notes/day3-ledger-plan.md`.
Lives alongside the betting engine (renamed `betsettlement`); the choice between the two is open.

- [x] Attendee code `com.rockthecode.ledger` (trait, harness, `Main`, stub); `ledger/README.md` is the ONE attendee document (guide + full protocol split by milestone); ALL scenarios in `src/main/resources/ledger/m1..m5` — no hidden set; "no hidden tests" is part of the pitch, as in the betting project.
- [x] Reference solution `com.rockthecode.ledger.solution` and generators; `notes/ledger/README.md` is the ONE trainer document (repo mechanics, run of day, review checklist, review record). Tooling is Scala only: `sbt ledgerGen` regenerates every generated file, `sbt ledgerVerify` checks every committed one (OK / NOT OK + first differing line).
- [x] `ledger/README.md` written so a team that knows nothing can start from it (goal, how it works, per-milestone goal/rules/protocol/checks/passing criteria). Daniel dry-running the project from scratch against it.
- [x] Every hand-written `.out` reviewed line by line — review record in `notes/ledger/README.md` §7.
- [ ] **Decide: betting engine or ledger for day 3** (or offer both). Then rewrite `training-plan.md §5` for the winner.
- [?] Strip `ledger/solution` (main + test) and `notes/` before handout — same mechanism as the day-2 solutions, whatever that turns out to be.
- [ ] If the ledger is chosen: at the 9:00 briefing, say out loud that guide sections 9–10 (M4/M5 protocol) stay unread until after lunch.

## Build and repo infrastructure

- [x] **Add a test library** — MUnit 1.2.0, `% Test`. Still worth checking what Blip uses internally, in case ScalaTest syntax would be more familiar.
- [x] Add `scalacOptions`: `-deprecation`, `-feature`. `-Wunused:all` still open — it will be noisy on the day-1 files.
- [?] **Decide how solutions are distributed.** They currently live in `com.rockthecode.day2.solutions` in `src/main`, which means attendees can read them. Options: strip that package from the advance repo and merge it live after each block, or move it to a `solutions` branch. Must be settled before the repo goes out.
- [ ] Consider a `notes/day2-block-format.md` or README section explaining the exercise/solutions/test layout to attendees.
- [ ] Every later block needs a stretch section too, in the same four files below a `STRETCH SECTION` banner. Budget for it when designing B through G.
- [x] Command aliases in `build.sbt`: `block0`, `blockA`, `blockB`, `blockC`, `blockD`, `blockE`, `blockF`, `blockG`, `checkSolutions`.
- [!] **Never check material with plain `sbt test`** — it stops at the deliberately red exercise suites and never reaches the solutions, reporting 74 failures / 0 passes. Use `sbt checkSolutions` (310 green).
- [?] **Scala version.** Currently pinned to 3.8.4. Switch to 3.3.x LTS if that is closer to Blip production. Confirm what they run.
- [ ] Decide what ships in the advance repo vs. what is added live. `notes/` is trainer-internal — strip it, gitignore it, or move it out before sending the repo to attendees.
- [ ] Add a README for attendees: how to build, how to run, what to do if setup breaks.

## Day 1 — additions (agreed)

Nothing gets cut. Implicits and concurrency stay — the goal there is recognition of other people's code.

- [?] **Confirm the Scala 2 vs 3 call for Block C** — it uses `enum` throughout with a side-by-side comment showing the `sealed trait` form. Flip to sealed-trait style if you would rather keep every file Scala-2-readable.
- [ ] **Collections transformation API**: `foldLeft`, `groupBy`, `partition`, `sortBy`, `collect`, `sliding`, `zip`, `sum`, `maxByOption`. The biggest gap — this is what replaces their `for` loops.
- [ ] **Collections cheat sheet** handout.
- [ ] **Immutability as a named topic**: `val` vs `var`, `.copy`, structural sharing, "state change = `State => State`".
- [ ] **`sealed` traits / `enum` + ADTs** with pattern matching, and the live exhaustivity demo (add a case, watch the compiler list every match).
- [ ] **`Either`** — introduce alongside `Option`/`Try`. Named by Big Guy as a struggle, currently absent.
- [x] **`LazyList`, views, `withFilter`** — laziness section added to `ScalaEssentials.scala` after the call-by-name material, with a TODO exercise. Values verified.
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
