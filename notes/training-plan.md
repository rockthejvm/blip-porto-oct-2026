# Blip Porto — 3-Day Scala Training (October 2026)

Trainer: Daniel Ciocirlan (Rock the JVM).
Predecessor: "Big Guy" — ran Scala trainings at Blip for many years, strong feedback, moved on.

Living document. Updated as decisions are made.

---

## 1. Client context (from the handover meeting with Big Guy)

**Audience**
- Experienced engineers. They pick up Scala syntax quickly.
- Mixed background: also Java and Kotlin in-house.
- Likely some prior Scala exposure — day 1 material can be skipped/accelerated as needed.

**Where they struggle**
- Functional programming as a *style* — the mindset, not the syntax.
- Immutability.
- `Option`, `Try`, `Either`.
- Lazy lists.

**What to focus on**
- Concurrency: Futures, Java interop, some actors.
- NOT: heavy implicits, abstract type-level machinery, fancy FP libraries.

**Business state**
- No new Scala projects ("hard to hire"), but a large existing estate.
- Both Scala 2 and Scala 3 in production.
- Functional Scala, deliberately plain — no Cats/ZIO ecosystem. Some Akka.

**AI policy**
- Full access for experienced devs.
- No code reaches production without human review.

**What worked well in previous trainings**
- Zoom focus rooms.
- Mini-projects.
- Very practical: lots of small puzzles, ~15 LOC, ~15 minutes each.

**Overall goal**
Teach FUNCTIONAL STYLE. The mindset must be transferable to their Java and Kotlin work.

---

## 2. Training structure

| Day | Theme | Format |
|-----|-------|--------|
| 1 | Scala fundamentals, biased toward FP and immutability rather than loops and variables | Live coding + inline exercises |
| 2 | Heavy FP practice | Themed blocks: short framing → puzzles → walkthrough |
| 3 | Real-life project | Teams of ~3, deliver something demoable |

### Decisions taken

- **Day 1 content stays as sketched, in full.** Nothing is cut. Implicits and concurrency are deliberately kept: the goal there is *recognition* — attendees must not be surprised when they meet this in other people's code. Skipping happens live, based on the room, not in advance.
- **Pre-work is handled by Daniel**: the repo goes out ahead of time so every build is warm and no time is lost to setup on day 1 morning.
- **A test library will be added** as the exercise feedback loop (see TODO).
- **Day 2 is settled block by block**, in conversation, not all at once.
- **Every day-2 block targets 45 minutes**, so blocks carry roughly equal content. Timing is not enforced on the day — Daniel paces to the room's needs.
- **Block 0 (collections drill) exists as a release valve**, in case day 1 runs long and the collections API gets squeezed. Material is pulled from the existing day-1 sketch.
- **Domain flavour is optional per exercise.** Blip's betting domain is fine where it fits naturally, but exercises are not forced into it. Daniel is not a domain expert and will not present as one — neutral, realistic scenarios (orders, requests, latencies, inventories) are preferred wherever the domain adds nothing.
- **Day 3 project is #1: the bet settlement engine.** Domain research pending — to be done after day 2 is settled.

### Cadence to protect

Big Guy's format is the one that worked: ~15 LOC puzzles, ~15 minutes, hands on keyboards. Every block on every day should return to that rhythm rather than drifting into lecture. Breakout/focus rooms for exercise time.

---

## 3. Day 1 — Scala fundamentals

Existing sketch: `src/main/scala/com/rockthecode/day1/`
- `ScalaEssentials.scala` (~519 lines)
- `AdvancedScala.scala` (~497 lines)

Both compile clean against Scala 3.8.4 (8 warnings, no errors).

### 3.1 Current coverage

`ScalaEssentials.scala`
- vals and types; expressions (operators, if/else, code blocks, `Unit`)
- functions, nested functions, `isPrime`
- stack vs tail recursion, `@tailrec`
- string interpolation, default arguments
- OO basics: params vs fields, infix methods, operators-as-methods, `apply`
- objects, companions, the `apply` factory pattern
- inheritance, traits, anonymous classes
- case classes
- generics, bounds, variance intro (`Garage`/`Mechanic`)
- what a function really is (`FunctionX`), lambdas, underscore notation
- HOFs and currying (`nTimes`)
- map/flatMap/filter, for-comprehensions, the chessboard equivalence
- collections: List, Array, Seq, Vector, Set, ranges, tuples, Map
- call-by-name vs call-by-value
- **`MyList`** — the 7-part running exercise, fully solved at the bottom of the file

`AdvancedScala.scala`
- pattern matching (values, objects, case classes, guards, `@` binding, nesting)
- partial functions
- `Option` — construction, combinators, the `Connection` exercise with 3 progressive solutions
- exceptions and `Try` — the `HttpService` exercise with 3 progressive solutions
- `Future` — construction, combinators, `onComplete`, `recover`/`recoverWith`
- `Promise` — plus the `first` / `last` / `retryUntil` exercises
- implicits — conversions, implicit args, resolution scopes, `Ordering`
- "pimp my library" — `implicit class`, enriching `Int` and `String`
- variance positions — the full covariant/contravariant position discussion

### 3.2 The `MyList` running exercise

Seven parts, threaded through the whole day, each unlocking the next concept:

1. Singly linked list of ints: `head`/`tail`/`isEmpty`/`add`/`toString`/`++`, as `Empty`/`Cons`
2. Reimplement with case classes
3. Make it generic and covariant; add `map`/`flatMap`/`filter` with `Predicate`/`Transformer`
4. Replace `Predicate`/`Transformer` with plain functions — the reveal
5. Drive it with anonymous functions
6. Add `zipWith` and `foreach`
7. Add `withFilter`, then run a for-comprehension over your own list

Part 7 is the payoff moment: for-comprehensions are not loops, they are `flatMap` on anything that has `flatMap`. Protect the time for it.

### 3.3 Additions agreed for day 1

These fill genuine gaps rather than replacing anything.

1. **The collections transformation API.** The biggest gap in the current sketch: the collections section covers constructors (`List`, `Set`, `Map`, `Vector`, ranges, tuples) but never `foldLeft`, `groupBy`, `partition`, `sortBy`, `collect`, `sliding`, `zip`, `sum`, `maxByOption`. That API *is* functional style in daily Scala — it is what replaces their `for` loops. Deserves real time and a cheat-sheet handout.
2. **Immutability as an explicitly named topic**, not an assumed one. `val` vs `var`; `case class` `.copy(...)`; why copying is cheap (structural sharing in `List`/`Vector`); "state change = a function `State => State`". This is the transferable-to-Java/Kotlin idea and it sets up day 3.
3. **`sealed` traits / `enum` + ADTs, paired with pattern matching.** Currently case classes and pattern matching are taught but `sealed` never appears. The exhaustivity check is the single best sales pitch for ADT modeling to a Java shop: add a case live, let the compiler list every match that needs updating.
4. **`Either`** — named by Big Guy as a struggle, currently absent everywhere. Introduce it here alongside `Option`/`Try`, practise it on day 2.
5. **`LazyList`** — also named as a struggle, also absent. Introduce briefly (it follows naturally from call-by-name, which is already in the file), practise on day 2.

### 3.4 Repo fixes for day 1

- **"Adobe" appears three times** in `ScalaEssentials.scala` (`helloAdobe`, the `"Hello, Adobe"` literal, and "time at Adobe" in the intros block). Leftover from another client.
- **Side effects in object bodies.** Both files run `println`, `producer.start()`, `Thread.sleep(1000)` and `Thread.sleep(10000)` at object-initialization time. Touching anything in `AdvancedScala` runs all of it, including 11 seconds of sleeping. Split per-topic into small objects with their own entry points.
- **Split `AdvancedScala.scala`** into `PatternMatching.scala`, `ErrorHandling.scala`, `Concurrency.scala`, `Implicits.scala`. Nearly 500 lines in one file is hard to navigate live and hard to reorder.
- **`Promise[A]` → `Promise[A]()`** (3 occurrences) — currently a Scala 3 syntax warning.
- **`daniel likes "Forrest Gump"`** warns: Scala 3 wants `infix def likes`. Either annotate it or use it to make the point that Scala 3 tightened alphanumeric infix.
- **Scala version.** `build.sbt` pins 3.8.4. If Blip production runs 2.13 plus Scala 3 LTS, pin 3.3.x LTS instead so they learn on the compiler they actually use.
- **Compiler options**: add `-deprecation`, `-feature`, `-Wunused:all` so warnings are visible in the room.
- **Scala 2 vs 3 cheat sheet.** They run both. Keep writing brace-style, `new`-style code (reads as valid Scala 2), and hand out a one-pager for the four things that differ and that they will hit: `implicit` vs `given`/`using`, `implicit class` vs `extension`, `sealed trait` vs `enum`, optional braces. Teach one dialect; map it to the other.

---

## 4. Day 2 — Heavy FP practice

**Status: being settled one block at a time. The list below is the proposed skeleton, not a final decision.**

Format per block: ~10 min concept framing → 3–4 puzzles at ~15 LOC / ~15 min → ~10 min walkthrough of one solution → break.

**Every block is targeted at 45 minutes**, purely so the blocks carry roughly equal content. Timing is not enforced on the day — the room's needs win, and blocks get stretched or compressed live.

Eight blocks at 45 min is ~6 hours of block time. Block 0 is the release valve: it only runs if day 1 ran short.

### Repo layout for day-2 blocks

Each block is four files, wired so that one test suite verifies both the stubs and the reference solutions:

| File | Role |
|------|------|
| `day2/BlockX.scala` | A trait carrying the exercise brief as scaladoc on abstract methods, plus the fixture types. This is the spec. |
| `day2/BlockXExercises.scala` | `object BlockXExercises extends BlockX`, every body `???`. Where attendees work. |
| `day2/solutions/BlockXSolutions.scala` | `object BlockXSolutions extends BlockX`, reference implementations, with alternative solutions in comments as walkthrough material. |
| `test/day2/BlockXSuite.scala` | `abstract class BlockXSuite(impl: BlockX)` written once, then instantiated twice: `BlockXExercisesSuite` (red until they solve it) and `BlockXSolutionsSuite` (must always be green). |

Each block's stretch exercises live in **the same four files**, below a `STRETCH SECTION` banner — same trait, same exercises object, same solutions object, same suite. Nothing later in the day depends on any stretch exercise, so they can be handed to the fast half of the room without splitting the group, and a block's suite simply sits red on its stretch tests until someone gets there.

The payoff: solutions can never drift from the exercise signatures, and the trainer can verify the solutions without wading through a deliberately red suite.

Command aliases in `build.sbt`:

```
sbt block0          # attendees: the block they are working on, stretch included
sbt blockA
sbt checkSolutions  # trainer: every reference solution, all 75 green
```

Test library is MUnit 1.2.0.

**Do not use plain `sbt test` to check the solutions.** sbt stops once the
deliberately red exercise suites fail and never reaches the solutions suites -
it reports 74 failures and 0 passes, which looks alarming and means nothing.
`sbt checkSolutions` is the command that answers "is my material still correct".

**Open:** the solutions package currently ships in `src/main`, so attendees can read it. Needs stripping or moving to a branch before the repo goes out.

### Blocks

**Block 0 — Collections drill** (45 min, *conditional*)

Insurance. Day 1 introduces the collections transformation API; if day 1 runs long and that section gets squeezed, this block recovers it. If day 1 covered it properly, skip Block 0 or cherry-pick one exercise as a warm-up. Material is pulled from the existing day-1 sketch so nothing new has to be written for it.

0.1. **Cartesian products.** Reuse the day-1 chessboard for-comprehension: generate all 64 square names (`a1`..`h8`). Then extend it — keep only the dark squares. Re-establishes that a for-comprehension is `flatMap`/`map`/`withFilter`, not a loop.
0.2. **`groupBy`.** Given `List[Employee(name, dept, salary)]`: produce `Map[String, List[String]]` of department → member names sorted alphabetically, and `Map[String, Double]` of department → average salary. The second one forces a fold inside the grouped values.
0.3. **`partition`, `span`, `collect`.** Split a mixed list three different ways; then use `collect` to extract-and-transform in a single pass and compare it against `filter().map()`. Establishes `collect` as the partial-function workhorse (paid off later in Block C).
0.4. **Map merging.** Combine two `Map[String, Int]` inventories, summing the values on key collisions. Classic, and it is a `foldLeft` over a map — which surprises people who think folds are only for lists.
0.5. **`zip`, `zipWithIndex`, `sliding`.** Given a list of daily prices, compute day-over-day deltas and find the largest single-day rise. Show both `sliding(2)` and `xs.zip(xs.tail)` and let them pick.

Deliverable alongside this block: the collections cheat sheet handout.

*Stretch* (`Block0Stretch`) - harder, and nothing depends on them. What makes them hard is that the shape is not obvious: each needs a method they have not reached for, or an accumulator carrying more than one thing.

S1. **Knight moves.** All squares a knight can reach from a given square, sorted. 0.1 again, but generating the offsets and adding them rather than generating squares directly. Solution shows both the generated offsets (`|df| != |dr|`) and the hand-written list - worth asking which they prefer, because "obvious" usually wins.
S2. **Invert an index.** `Map[dept, List[member]]` becomes `Map[member, List[dept]]`, with members allowed in several departments. Flatten to pairs, then regroup. Carries a genuinely nasty trap: flatMapping a `Map` directly rebuilds a `Map` and silently eats duplicate keys - one test exists purely to catch that.
S3. **Rebuild a price series from its deltas.** The inverse of 0.5. The whole exercise is discovering `scanLeft` - foldLeft answers "where did I end up", scanLeft answers "how did I get there". One line once you know.
S4. **The best trade.** Buy one day, sell a later one, maximise profit. The obvious answer is a quadratic for-comprehension and it is the right first answer; then the single-pass fold, whose accumulator carries five things and therefore gets a name. That naming moment is a direct preview of day 3. A trainer-only suite (`Block0AgreementSuite`) cross-checks both solutions against 2000 random series so the "they agree" claim in the walkthrough is machine-verified.

**Block A — Thinking in expressions** (45 min, warm-up)

The job: hands on keyboards within 10 minutes, and establish the one habit the whole day depends on — reach for an expression, not a statement.

Because day 1 now covers the collections transformation API, this block does not teach `foldLeft`. It assumes it and makes them use it under a constraint. Re-activation, not new material.

*Framing (8 min)* — not slides. Put an imperative Scala snippet on screen, ask the room what it does, then state the constraint for the next eight hours: no `var`, no `while`, no mutable collection. Not because they are evil, but because removing them forces the alternative to become visible. Note that everything today has a direct Kotlin / Java-streams analogue, since that is where most of them will actually apply it.

A.1. **The rewrite.** ~20 lines of working imperative Scala — walk a `List[Order]`, accumulate a running total, collect the ones over a threshold into a buffer, track the largest. Rewrite with no `var`, no `while`, no mutable collection, same output. The test pins the original's output.
  *Teaching moment:* the imperative version does three things in one pass and is hard to name; the functional version is three named pipelines, and nobody cares that it traverses three times.
A.2. **`.copy` and the fold.** Given `case class Account(id: String, balance: BigDecimal, status: Status)` and a `Transaction`, write `applyTransaction(acc, tx): Account` returning a new account. Then `applyAll(acc, txs: List[Transaction]): Account`. Most will write recursion; some will spot the `foldLeft`.
  *Teaching moment:* `applyTransaction` has exactly the shape `(State, Event) => State`, which is precisely `foldLeft`'s function argument — so "replay history to get current state" is one line. Say out loud that this is day 3's project. Costs thirty seconds, plants the seed early.
A.3. **The report renderer.** Two small pure functions: `formatDuration(millis: Long): String` rendering e.g. `2h 15m 3s` (skipping zero units), and `renderLine(req: Request): String` producing one aligned summary line. Then render a whole list and print it with a single `foreach(println)` at the very end.
  Deliberately the easy one — everyone finishes, which resets confidence after A.1 bites some of them. *Teaching moment* is the debrief question: "where did the printing go?" It moved to one place, at the edge, and the interesting part is now testable. That is the referential-transparency argument in ten seconds, without the vocabulary.

*Walkthrough (10 min)* — only A.1. Show two solutions: a literal three-pipeline translation, then a single `foldLeft` producing a tuple. Let them argue. Land on the pipelines — clarity beats traversal count until a profiler says otherwise. This inoculates them against the premature-optimisation objection they will raise all day.

**Block B — Collections and folds** (45 min, the workhorse)
4. Reimplement `map`, `filter`, `reverse`, `length` using only `foldLeft`/`foldRight`.
5. Balance per account from `List[Transaction]`: `groupBy` + `map` + fold, no mutable map.
6. `wordFrequency(text: String): List[(String, Int)]`, top N, sorted descending — in one pipeline.
7. Run-length encode `List[Char] => List[(Char, Int)]` and decode back; round-trip property in the test.
8. Longest strictly-increasing run in a `List[Int]` via `sliding`/`foldLeft`. This one bites — good walkthrough candidate.
9. Drill: why `List.empty[Int].reduce(_ + _)` throws but `.fold(0)(_ + _)` does not; when `foldRight` blows the stack.

**Block C — Modeling with ADTs and pattern matching** (45 min)
10. `Shape` ADT + `area`; add a case live and let the compiler find every incomplete match.
11. Order/bet lifecycle as `enum State` + `enum Event`; `transition(state, event): Either[TransitionError, State]`. Illegal transitions as data, not exceptions.
12. `Json` ADT + recursive `stringify` (~30 min, stretch).
13. Partial functions: `collect` over `List[Any]` to extract and transform only the `Int`s; contrast with `filter().map()` and with a throwing `match`.

**Block D — Option** (45 min)
14. Config parsing from `Map[String, String]` → `Option[ServerConfig]` via for-comprehension. Extends the day-1 `Connection` exercise.
15. **Anti-pattern drill**: rewrite given code so it contains zero `.get` and zero `.isDefined`. Highest-value 15 minutes of the block for a Java-`Optional` audience.
16. Replace a null-returning legacy API with `Option`; `orElse` fallback chain; `fold` to render. Compare `getOrElse` vs `fold` vs pattern matching.
17. Hand-roll `sequence(List[Option[A]]): Option[List[A]]` with `foldRight`; use it for all-or-nothing `List[String] => Option[List[Int]]`.

**Block E — Try, Either, error modeling** (45 min)
18. Wrap a throwing Java-style API in `Try`; chain two failing calls; recover with a fallback. Reuses the day-1 `HttpService` scaffold.
19. Error ADT + `Try[A] => Either[AppError, A]` at the boundary. `Try` says something broke; `Either` says what — and only one is exhaustively matchable.
20. Validate a `SignupForm` → `Either[AppError, Signup]` fail-fast; then change it to accumulate all errors into `Either[List[AppError], Signup]`. They discover the for-comprehension cannot do it — that is the lesson, and the honest motivation for validation types without importing Cats.
21. `traverse(list)(f: A => Either[E, B]): Either[E, List[B]]`; apply it to a CSV where any bad row fails the batch.
22. Boundary drill: legacy layer gives `Try`, callers want `Either`, a third party wants exceptions. Write all three adapters. Matches their real Java-interop situation.

**Block F — Laziness** (45 min)
23. By-name params: `myIf(cond, thenB: => A, elseB: => A)` and `logDebug(msg: => String)`; prove the expensive string is never built.
24. `lazy val` for expensive init; demonstrate memoization with a `println` in the initializer.
25. `LazyList`: infinite naturals, Fibonacci, primes by sieve; take the first 20 of each.
26. Retry-backoff schedule as `LazyList[FiniteDuration]`, exponential with a cap. Bridges to Futures.

**Block G — Futures and concurrency** (45 min)
27. **Sequential vs parallel.** Three independent 1-second calls: the naive for-comprehension takes 3s because the futures are constructed inside it; construct first, then combine, and it takes 1s. The #1 practical Future bug in real codebases — make everyone measure it.
28. `Future.traverse` to fan out N calls and aggregate; then handle one failing when you want partial results.
29. `withTimeout(f, d)` using `Promise` + a scheduled failure. Reuses `first` from day 1.
30. `recover` vs `recoverWith` vs `transform`; what a failing `filter` does inside a for-comprehension (`NoSuchElementException` — surprising, worth showing).
31. **Wrap a Java callback / `CompletableFuture` API into a `Future` with a `Promise`.** Big Guy's "Java things" requirement; the most directly applicable exercise of the day for their codebase.
32. Execution contexts: starve a small fixed pool with blocking calls, then fix it with a dedicated EC (mention `blocking {}`). They need to know why `global` is a trap.
33. Stretch: process `List[Url]` with at most N in flight, results in order.

### Open question — actors

Big Guy said "some actors", but a real actor block is 3+ hours and competes with the FP goal. Proposal: a 30–45 min conceptual demo at the end of day 2 (message passing, `Behavior`, why it is not a `Future`), unless their Akka usage is heavy enough to justify trading a block for it. **Also confirm: Akka (BSL, licensed) or Pekko?** — example imports depend on it.

---

## 5. Day 3 — Project

**Chosen: bet settlement engine.** Domain research pending.

### The project

Inputs, all as files on disk: market definitions, placed bets, event results.
Output: settled bets with payouts, plus a per-customer ledger.

Why this one: it exercises everything the training builds toward.
- ADTs for market types, bet types, outcomes
- `Either` with a typed error ADT for rejected bets
- `foldLeft` over the results feed to build state
- exhaustive pattern matching for settlement rules
- a stretch ladder that climbs cleanly

Stretch goals, in order: voids and pushes → each-way bets → partial cash-out → accumulators (which force recursion over a tree).

### Ground rules

- Teams of ~3.
- Roughly 5 hours of actual coding.
- **Zero infrastructure**: no DB, no HTTP server, no Docker. Data comes from files.
- Shared starter repo: data files, model skeleton, a few passing tests.
- A ranked stretch-goal list so fast teams keep going and slow teams still deliver.

### Run of day

1. 20 min briefing and team formation.
2. 30 min: teams design their model on paper and show it to Daniel before writing code. Catches the "we're modeling this with mutable maps" teams while it is still cheap.
3. Build.
4. Hard checkpoint at 2/3 through.
5. Last 45 min: 8-minute demos per team.

Each team gets a written "definition of done" checklist so nobody is guessing what to demo.

**On AI**: their policy allows full access for experienced devs, with human review before production. Mirror it — teams may use AI freely, but anyone on the team must be able to explain any line when asked. Turns the policy into a teaching moment instead of an elephant in the room.
