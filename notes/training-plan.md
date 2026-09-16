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
5. **`LazyList`, views and `withFilter`** — **done.** A laziness section is now in `ScalaEssentials.scala`, immediately after the existing call-by-name material, covering `lazy val` (with the val / lazy val / def timing table), `.view` on ordinary collections, `filter` vs `withFilter` (and why an `if` in a for-comprehension compiles to the latter — which is exactly why `MyList` needed a `withFilter`), and `LazyList` including `#::` and the self-referential fibonacci. Ends with a three-part TODO exercise in the file's existing style. **All values verified by running them**, including the "views do not remember" claim (forcing the same view twice does the work twice).

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
sbt blockB
sbt blockC
sbt blockD
sbt blockE
sbt blockF
sbt blockG
sbt checkSolutions  # trainer: every reference solution, all 310 green
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

Theme: every named method they used in Block 0 is a fold underneath. Once you can see that, you can write the operation the standard library did not give you — which is most of what day 3 turns out to be.

*Framing (8 min)* — write `List(1,2,3).map(_ * 10)` as a `foldRight`, then `filter`, then `reverse`. Claim: the fold is the only recursion you need over a list. Then the callback: *this is why `MyList` on day 1 needed all those methods — you could have written one and derived the rest.*

B.1. **Folds are everything.** Reimplement `myLength`, `myReverse`, `myMap`, `myFilter` using only `foldLeft`/`foldRight` and `::`. The discovery is directional: `foldLeft` naturally builds backwards (so `myReverse` is foldLeft with nothing added, and `myMap` needs a final reverse), `foldRight` naturally preserves order. Same fold, opposite grain. Tests run all four at 20k, so an answer that works on three elements and dies on twenty thousand is not an answer.
B.2. **The three traps.** `safeSum` (`fold` has a seed and can change type; `reduce` can do neither, and throws on empty), `safeMax` (why `maxOption` exists), and `myFoldRight` — which must survive 200k.
B.3. **Run-length encoding.** `encode` and `decode`, tested as a round-trip property. Going right-to-left the run being extended is always the head of what you have built, so it can be pattern-matched, bumped and put back — no in-progress run to carry and no flush at the end. The foldLeft version is twice the size. Direction of travel is a design decision, not a coin toss.
B.4. **The longest increasing run** — the run itself, not its length. The one that bites, and the walkthrough candidate. Three bugs in order: the last run never gets compared because the list ended before the run did; the run comes out backwards; and measuring "which is longer" with `.length` inside the fold makes it quadratic.
B.5. **Word frequency.** `topWords(text, n)` as one pipeline. Ties broken alphabetically — without it the answer depends on hash order, is reproducible on a laptop and not on CI. And splitting on `[^a-z]+` mangles "coração", in Porto, in front of the people who wrote the word: use `\p{L}`.

*Measured facts for the B.2 demo* (Scala 3.8.4, this machine — worth re-running live):

| | result |
|---|---|
| stdlib `List.foldRight` at 500k | fine — it reverses and folds left, so it never recurses |
| hand-written recursive `foldRight` | dies between 4,000 and 5,000 |
| `acc :+ x` in a fold at 10k / 20k / 40k | 1.1s / 3.4s / 13.3s — textbook quadratic |
| `x :: acc` then `.reverse` | 2ms / 3ms / 3ms |

**"foldRight blows the stack" is folklore that has been false for `List` since 2.13**, and people still repeat it. The true version is sharper: *your* recursion has a stack, the library's does not, because the library does not recurse. Run the 20k append demo live — 3.4 seconds of silence in the room does more than any slide.

*Stretch* — B said every list operation is a fold; these take that seriously.

SB1. **`foldLeft` using nothing but `foldRight`.** The mind-bender. You cannot build a value, so build a *function* from the right — each step wrapping the last — and apply it to the seed at the end. Five lines. The brief withholds the hint for five minutes on purpose. A randomised test agrees it against the real `foldLeft` on 200 inputs using subtraction, which is the only way to pin the direction.
SB2/SB3. **`groupBy` and `partition` from scratch.** The library is not magic. `myGroupBy` hides B.2's append trap one level down inside a Map; `myPartition` comes out in order for free with `foldRight` and needs two reverses with `foldLeft` — the same choice as B.1.
SB4. **Balanced brackets.** A fold with a list as a stack. The interesting part: once you have seen `([)]` you know the answer, but a fold cannot stop — so "already doomed" has to be carried forward as a value. What they invent is `Either[Error, Stack]` with the error thrown away. Ask what they would want to know when it returns false ("which bracket, at which position?") and answer: that is Block E, one type away.
SB5/SB6. **`unfold`** and Fibonacci built with it. The mirror of fold: one value in, a list out. Must be stack-safe, which is day 1's `@tailrec` plus an accumulator. The seed carries three things, not one — and that is exactly how a `LazyList` is defined in Block F.

*What was dropped from the original sketch:* "balance per account". Block 0.2 already does groupBy-plus-a-fold-inside-the-values and A.2 already does fold-as-state; it would have been the third serving of the same dish.

**Block C — Modeling with ADTs and pattern matching** (45 min)

Theme: they have been *using* ADTs since Block A (`AccountStatus`, `Transaction`) without anyone naming them. C is where they design their own. The idea is small; the consequence is that "find everywhere that handles order states" stops being a grep and becomes a compile.

**Scala 2 / Scala 3 decision:** Block C uses `enum` throughout, and the trait opens with a side-by-side showing the same type as a Scala 2 `sealed trait` + case objects, with `sealed` called out as the load-bearing word. Block A deliberately used the Scala 2 form, so they have seen both by lunch. **Easy to reverse if you would rather stay Scala-2-readable throughout.**

C.1. **Shapes** — `area` and `scale` over a three-case enum. Deliberately dull, because the point comes after: add `case Square(side)` live and the compiler names every function with a hole in it. **Verified:** exactly two `E029` warnings, at two exact line numbers. Follow with the part that matters — this only works because the type is *sealed*; unseal it and the compiler goes quiet, not because the code got safer but because it stopped being able to help. Good moment to show `-Werror`.
C.2. **An order lifecycle.** `transition(state, event)` over 6 states × 5 events. The answer is not a state — it is "a new state, or a reason why not", and both are modelled: `TransitionResult.Moved | Rejected`, with `Refusal.AlreadyFinished | Impossible`. Five case lines cover the seven legal moves; the alternative pattern `Draft | Placed | Paid` replaces three near-identical lines.
C.3. **JSON — a recursive ADT.** **Opens with ten minutes of design, no compiler**: whiteboard the set of cases a JSON value can take, *then* compare with the enum in the file. This is the only genuinely design-shaped part of day 2 and it is the part they will do most often in real work — getting the cases right is most of the job, and the functions almost write themselves afterwards. Then `render`. `JObj` holds a `List` of pairs, not a `Map`, so field order is part of the value and rendering is reproducible — B.5's hash-order bug in a new hat. The escaping has a real ordering bug in it (backslashes before quotes, or the backslash you just inserted gets escaped again) and a test for exactly that.
C.4. **Reading a command line.** `interpret(args: List[String]): Command` as one match. Where `::` and `Nil` stop being constructors and start being patterns — `case "add" :: item :: Nil` reads like the shape of the input. Worth naming: this match is *not* meaningfully exhaustiveness-checked, because `List` is not a closed set of shapes.
C.5. **`eval` over an expression ADT.** A second recursive ADT, gentler than JSON. `Option` for "no answer", and the for-comprehension does the None-propagation without a single `if`. The brief explicitly forbids short-circuiting `Mul(_, Lit(0))` — otherwise `0 * (1/0)` is zero and evaluation order becomes part of the language definition.

*Stretch*

SC1. **`at(json, path)`** — walk down nested objects, `None` on a missing step or a step into a non-object.
SC2. **`collectStrings`** — every `JStr` value in order, ignoring field names. The `(_, value)` underscore is the exercise.
SC3. **`depth`** — empty containers are depth 1, and the empty cases must come *first* because `List.empty.max` throws. B.2 turning up uninvited.
SC4. **`simplify`** — bottom-up in a single pass; one test proves one pass suffices, another proves simplification never changes what an expression evaluates to.
SC5. **`reachableStates`** — a state machine is a graph and this is a graph search, recursive over `(frontier, seen)` rather than a `while` with a mutable Set.
SC6. **A JSON converter as a type class.** The only place today they meet `given`, and it answers what C.3 leaves hanging: `render` turns `Json` into text, but who turns a `User` into `Json`? A method on `User` fails the moment the type belongs to someone else. Eight instances; six are ordinary and two are the point — `optionToJson` and `listToJson` *take* a converter and *return* one, so the compiler assembles `JsonConverter[List[Post]]` out of `JsonConverter[Post]` with nobody ever writing that type down. A test asserts exactly that, and another composes to `List[Option[List[Int]]]`.

*Implementation note:* SC6's instances are declared as **abstract `given`s on the trait**, which is unusual — it is what lets the shared test suite pull the attendee's own instances into scope via `import impl.given`, so the exercise is structurally enforced rather than merely requested. The solution comments say that in real code they belong in the companion object of the type they convert. Verified working.

*Naming note:* `Command.Add`/`Remove` are `AddItem`/`RemoveItem` — they collided with `Expr.Add` under wildcard imports.

*Dropped after review:* `describe` (brittle — the audience would write different strings) and `allowedEvents` (the derive-from-`values` solution was not worth the exercise).

**Block D — Option** (45 min)

Not an introduction — day 1 covers the combinators and Blocks B and C already hand them `Option`. This is the hour where reaching for the right one stops being something they think about.

**One rule for the whole block, and it is the point:** no `.get`, no `.isDefined`, no `.isEmpty` followed by an `if`. Every time you reach for one, a combinator says what you meant more clearly and cannot throw. Finding it is the exercise. (`getOrElse` is fine — different method, always has an answer.)

D.1. **The boundary with null.** `LegacyDirectory.find` returns `String` or null, with nothing in the signature to warn you — what every `getX` in a legacy codebase looks like. `Option(x)` checks; `Some(x)` wraps the null and hands you an Option that lies. **Verified:** writing `Some(...)` fails the test with `Some(null) != None`, which is the clearest possible statement of the bug — worth doing live, then noting that in real code there is no test at that line and the `Some(null)` travels three layers up before exploding somewhere unrelated.
D.2. **Reading configuration.** Day 1's host-and-port exercise grown up. `parseConfigWithDefaults(raw, defaults)` takes a whole map of defaults, so any setting can have one. The real exercise is *where the fallback goes*:

| | on `timeout = "3o00"` |
|---|---|
| `raw.get(k).orElse(defaults.get(k)).flatMap(_.toIntOption)` — resolve, then parse | refuses to start |
| `raw.get(k).flatMap(_.toIntOption).orElse(defaults.get(k).flatMap(...))` — parse, then resolve | silently uses the default |

Both compile, both pass every happy-path test. **Verified:** the second fails exactly one test out of 39 — the one called *"a setting that IS there still has to be a number"*. Show it on screen and run the suite, then point out that in a real codebase nobody wrote that test, because whoever added the fallback was thinking about missing keys and not broken ones. The general rule worth naming: `orElse` means "if you have nothing, try this instead" — putting a parse in front of it converts *broken* into *nothing*, and that conversion is the bug.
D.3. **The drill.** `uglyDiscount` is working code with four `.isDefined`, three `.get`, three nested ifs and a duplicated rule. Rewrite with the same behaviour; the test compares both versions across every combination of inputs. Two combinators do the work, and if the block teaches only two method names make it these: `option.isDefined && p(option.get)` → `option.exists(p)`, and `option.isDefined && option.get == v` → `option.contains(v)`.
D.4. **`map2`** — combining two independent Options. Easy, and it exists mainly so the brief can name what it *cannot* do: if both are missing it reports one absence, because `flatMap` stops at the first. For "which fields of this form are wrong?" that is useless — one complaint per round trip. Nothing about `Option` can fix it; it is a property of the type.
D.5. **All of them, or what you can get.** `sequence`, `traverse`, and `collectKnown` — the last being one word (`flatten`), because `Option` is a collection of at most one element. The lesson is the *contrast*: same input type, same shape of question, and the difference between "one bad record fails the import" and "one bad record vanishes silently until reconciliation". Neither is right; choosing on purpose is. Also: `sequence(Nil) == Some(Nil)` surprises everyone and is not a special case — it falls out of the fold's seed.
D.6. **Chains.** `managerEmail` — four different failures, one `None`. Ask what an on-call engineer would want in the log, and leave it hanging.

*Stretch*

SD1. **`chain`** — follow "and then what?" until it runs out. Stack-safe (tested 100k deep) and cycle-safe: a cycle without a `seen` set is not a wrong answer, it is a hang, and hangs are much harder to diagnose.
SD2. **`traverseValues`** over a Map. The point is that `traverse` did not need to change — it was never about Lists.
SD3/SD4. **`merge` and `mergeAll`** — combine two Options where *either* is enough. The pair with D.4's `map2` is the lesson, and they belong side by side on the board: same signature, opposite answers. `map2` needs both and gives up otherwise; `merge` needs at least one. Merging two layers of configuration or two partial records is `merge`, and reaching for `map2` there throws away everything you did have, silently. `mergeAll` folds it, with a string-concatenation test to prove somebody thought about order.

*Setup for Block E:* D.6 and D.4 both end by naming what `Option` structurally cannot do — say which of four things failed, and report more than one failure. Together with C.2's hand-rolled `TransitionResult`, B's SB4 brackets and A's SA3 string reasons, that is four deliberate setups. E should land as a relief rather than a new topic.

**Block E — Try, Either, error modeling** (45 min)

The payoff block. Five setups from earlier in the day all cash in here, and the framing should open by naming them: A's SA3 (rejection reasons as bare Strings), B's SB4 (a matcher that knows the input is broken and cannot say where), C's C.2 (`TransitionResult.Moved | Rejected` — a type they designed by hand), D's D.4 (`map2` reporting one absence out of two), D's D.6 (four failures arriving as one `None`).

**Put C.2 on screen first.** `Either[L, R]` *is* that type, already written, with map/flatMap/for-comprehension attached. And `Option` is `Either` with the left side thrown away — which is exactly why D.6 could not tell you what went wrong.

Three tools, three jobs, and mixing them up is most of the confusion: **exceptions** are invisible in the type and fine only at the edges; **`Try`** carries a Throwable you did not choose and its job is the *boundary with code that throws*; **`Either`** carries a type *you* designed and its job is everywhere else. The move the block teaches: catch at the boundary, classify immediately, never let a Throwable travel through your own code.

E.1. **`Try` at the boundary.** `LegacyPricing` throws — `Map.apply` and division by zero, so not a strawman. `Try(...)` catches the way `Option(...)` checked for null in D.1: same idea, one layer up. `unitCost` chains two throwing calls in a for-comprehension with no error handling written anywhere.
E.2. **Try in, Either out.** `.toEither` then `.left.map(classify)` — three lines, and after the third the Throwable is gone and everything downstream deals in a closed set. Two exception types become two domain errors. The detail worth a sentence: **the sku is in scope at the call site and is not in the exception** — context is always richer where the call happens, which is the real argument for translating early.
E.3. **Validation, fail-fast.** `Either.cond` (right before left, reads backwards three times then never again) and `option.toRight(error)`, the most useful method in the block.
E.4. **Validation, accumulating — THE exercise.** Ask for it with a for-comprehension first and let someone write it. **Verified:** that version passes the happy path *and* "one problem is still a list of one", then fails exactly the two accumulating tests. Then ask why, and answer from the signature of `flatMap`: it takes `A => Either[E, B]`, so with no `A` there is no next step to look at. Either does not *decline* to accumulate — it cannot. What had to change is that the three validations now run **independently** before anything is combined, which is why libraries model this as a separate type (Cats calls it `Validated`) rather than a method on Either. Blip does not use those libraries, so the hand-rolled version is not a consolation prize — it is what they will ship.
E.5. **Lists of results.** `sequence`, `traverse`, `partitionResults`. Character for character this is D.5 with `Option` swapped for `Either`; put the files side by side — the point is not that they are similar, it is that *nothing about the algorithm depended on which type it was*. `partitionResults` is `collectKnown` grown a memory: "we imported 9,998 of 10,000 rows and here are the two" is a good morning; "we imported some rows" is an incident.
E.6. **D.6, answered.** Same lookup, same for-comprehension, `.toRight(...)` on each line — and the function goes from "no" to "here is what I could not find, and whose". **Verified:** naming the wrong entity (the person instead of their manager) fails two tests. An error that names the wrong thing is worse than no error, because it sends the on-call engineer to the wrong place.

*Stretch*

SE1. **`map2Accumulating`** — E.4 generalised; the `case (Left(e1), Left(e2)) => Left(e1 ++ e2)` line is the entire exercise. Write E.4's plumbing by hand first, then delete it.
SE2. **`retry`** — last failure wins, because it describes the state the world ended up in. Needs no laziness at all: `action` is an explicit `() => Try[A]`, which is worth pointing out, since "make it a function" is often the simpler answer to a laziness problem.
SE3. **`mapError`** — one line, and architectural: storage errors must not leak into HTTP responses. Where it is missing you find out because a database constraint name shows up in a customer-facing message.
SE4. **`validateBatch`** — every error in every row, tagged with its row index. The decision hiding inside is worth twenty seconds: a batch 9,998 rows good is rejected entirely. Right for a financial import, wrong for a log ingest, and E.5's `partitionResults` is the other choice with shorter code. Pick on purpose — the theme since D.5.

**Block F — Laziness** (45 min)

Everything so far has been eager: write an expression, it runs. This block is the other option — describing work now and doing it later, or never. Three mechanisms in increasing order of disruption: **by-name parameters** (the expression is passed, and re-evaluated at every use), **`lazy val`** (once, on first use, then remembered), **`LazyList`** (tail computed on demand and remembered, which is what makes an infinite one possible).

Two earlier promises get paid off here: D.1's `firstAvailable` looked up every name to take the first answer, and B.SB5's `unfold` could only ever finish.

**Testing laziness means testing what did not happen**, so most tests count evaluations. **Verified:** the eager version of every one of these returns *the right value* and fails only on the count. That asymmetry is the whole block — say it out loud, because it is why this class of bug survives code review.

F.1. **By-name.** `myIf` proves `if` is not special syntax — once arguments can be by-name, `withResource`, `retry`, `time` and every logging API are ordinary functions. `logIfEnabled` is why `logger.debug(s"...")` doesn't build the string. `evaluateOnce` is the trap: `(value, value)` returns the right answer and evaluates twice, because a by-name parameter *is* the expression, substituted at every use. The fix is one word, and the three-row table is worth the board: `val` once now / `lazy val` once on first use / `def` and `=>` every use. Nearly every "why is this called twice" in a code review is somebody who wanted the middle row.
F.2. **Views.** `firstMatching` is D.1's function with `.view` added — put them on screen together: identical code one word apart, 5 lookups against 100. Two notes worth thirty seconds: `.toList` is not optional (a view is a description, not an answer), and **views do not memoise** — force one twice and everything runs twice. That is the difference from LazyList and it sets up F.3.
F.3. **LazyList.** `naturals`, `fibonacci`, `primes`. The fibonacci brief poses the objection deliberately: `#::` ends in a colon, so it is right-associative, `current #:: from(...)` means `from(...).#::(current)`, and the tail is the *receiver* — evaluating a receiver to dispatch on it would never return. The answer is that there is no `#::` method on LazyList at all; there is `implicit def toDeferrer[A](l: => LazyList[A])`, and **the conversion takes the tail by name**, so the recursive call is captured unevaluated on its way to becoming the receiver. The head is by-name too, which is what SF4 relies on. **Measured** by counting entries into the body of `from`: 1 with nothing taken, 5 after `take(5)`, 10 after `take(10)` on the same list. Put a counter in it live — this is the one place where the mechanism genuinely surprises people, and somebody will ask. The sieve reads as English — *"the first candidate is prime, and the rest of the primes are the sieve of everything it does not divide"* — is defined in terms of itself, and terminates. In an eager language that line is an infinite loop; here it is a definition. Honest footnote: it gets slow, because element n passes through n stacked filters. Beautiful definition, not a fast sieve.
F.4. **`unfoldLazy`** and `collatz`. Put it beside B.SB5: that one needed `@tailrec`, an accumulator and a reverse and could only finish; this one needs none of them, is shorter, and can run forever. Not tail recursive and doesn't need to be — the recursive call is the by-name tail of `#::`.
F.5. **Backoff schedule.** `LazyList.iterate` and a cap. The point is that the *policy is now a value* — printable, testable, configurable, passable — rather than arithmetic buried in a loop. E.SE2's `retry` should take one of these as an argument, and that is a better `retry`.

*Stretch*

SF1. **`memoize[A, B]`** with a `mutable.Map` and `getOrElseUpdate` — the honest implementation, not the LazyList curiosity. **This is the one exercise of the day where a mutable collection is the right answer**, and the reason is the real lesson: a cache is state that outlives a call and is shared between calls, and behind an `A => B` signature there is no immutable way to say that — you would have to hand the cache back and then it is an accumulator, not a cache. So the rule was never "never mutate", it is *"mutate in one place, behind a boundary, where nobody outside can see it"*. The caller gets a plain function, cannot reach the Map, and cannot observe the difference except in the speed; `memoize(f)(x)` is still `f(x)`. Contrast with a `var` in the middle of domain logic — same keyword, entirely different thing, and telling them apart is worth more than either rule. Two honest caveats in the solution: it is not thread-safe (two threads can both miss and both compute — wasteful for a pure function, a real bug otherwise, and that is Block G), and it never evicts, which is a memory leak with good manners. `getOrElseUpdate` takes its second argument by-name, which is Block F quietly doing its job inside a standard library method.
SF2. **Pascal's triangle** — pad each end with zero, zip the shifted copies, add.
SF3. **`iterateUntilStable`** — "the steps zipped with themselves shifted by one, and the first place they agree". The specification *is* the code. Contains the one defensible `.get` of the day, and the solution says so explicitly rather than hiding it — a `.get` you can justify is different from the ones Block D banned, and pretending otherwise teaches cargo cult.
SF4. **Hamming numbers** — the hardest thing in day 2. The sequence defined in terms of itself: 1, followed by the merge of itself times two, three and five. Circular, so it needs `lazy val`; terminates, because `#::` has a by-name tail. The merge's third branch is the deduplication (6 arrives twice). The naive candidate-testing version is correct and unusable — the 1,000th Hamming number is 51,200,000 — and the test makes that point without anyone having to say it. **Measured:** the good version reaches element 1,000 in 4ms.

**Block G — Futures and concurrency** (45 min)

Last, and deliberately so: a `Future` has `map`, `flatMap` and `filter`, so it has a for-comprehension, so by now they already know how to use it. What is genuinely new is that **a Future is already running**. `Option` and `Either` describe a value you have; a Future describes work that started when you wrote it down. Almost every mistake in the block comes from forgetting that, or from forgetting the opposite — that a Future you have not created yet has not started.

Two notes the brief makes explicitly. **`Await` appears in the tests and must not appear in their code** — blocking a thread to wait for a Future gives back everything it bought you, and doing it on the pool the Future needs in order to finish is how services deadlock. And **`ExecutionContext.global` is a work-stealing pool sized to the CPUs** — right for computation, wrong for anything that blocks; fill it with sleeping threads and everything else in the process stops, including the work that would have woken them. The signatures keep the EC out so the exercises stay about Futures, and the brief says so.

G.1. **`sumThree` — the one that matters.** Three independent 300ms calls. The obvious for-comprehension takes 900ms; the right answer takes 300 and differs by two lines. **Verified:** the naive version fails with *"took 904ms for three 300ms calls — that is one after another. When does a Future start running?"* The parameters are by-name (Block F still earning its keep) precisely so the mistake is reproducible; in real code the same thing happens with method calls. The question to leave them with is the whole mental model — *at what moment does a Future start?* — and the answer explains this bug, explains why you cannot retry a Future (only the function that makes one), and explains why by-name parameters keep appearing around them.
G.2. **Fanning out.** `Future.traverse` — the same `traverse` as D.5 and E.5, third container, same name. Then `fetchAllSettled`: make each Future *incapable of failing* first, then combine, so all-or-nothing becomes irrelevant rather than something to work around.
G.3. **When it goes wrong.** `recover` gives a value, `recoverWith` gives another Future — exactly `map` vs `flatMap` on the failure side. `transform` does both sides at once. Worth flagging that both take a PartialFunction, so `case _ =>` swallows the `OutOfMemoryError` too.
G.4. **Promise — where Futures come from.** Three exercises, all the same five lines. `fetchAsFuture` wraps a Java-shaped callback API, and **this is the most directly applicable exercise of the day for a codebase with Java in it**: every callback, listener and `CompletableFuture` becomes composable through exactly this shape. Then `delayed` — *they build the scheduler bridge themselves* rather than being handed one, because the naive `Future { Thread.sleep(...) }` burns a pool thread for the whole delay, and enough concurrent timeouts on the global EC starve the pool that the work being timed needs in order to finish. The trap inside it is `promise.complete(Try(value))` vs `promise.success(value)`: get it wrong and an exception escapes on the scheduler thread where nobody is listening, and **the Future never completes at all** — the caller waits forever for a failure that already happened. **Verified**, with a test that says exactly that. Finally `withTimeout` = `delayed` + `firstCompletedOf`, with the honest caveat that the original Future keeps running; there is no cancellation in `scala.concurrent`.
G.5. **`batched`.** Named for what it does, not what it approximates: batches of `batchSize`, one after another, everything within a batch together. `grouped` + a `foldLeft` over a Future. It *bounds* concurrency but does not *maintain* it — a batch whose slowest call takes a second leaves the other slots idle waiting for the straggler; a true limiter needs a queue and a semaphore. Knowing exactly which of the two you built, and being able to say so in review, is the skill. Note that `Future.traverse` sits *inside* the for-comprehension here, so it is not constructed until the previous batch finished — in G.1 that was the bug, here it is the point.

*Stretch*

SG1. **`firstSuccessOf`** — `Future.firstCompletedOf` takes the first to *finish*, so a source that fails in 1ms beats one that succeeds in 10ms and your fallback never runs. **Verified:** substituting `firstCompletedOf` fails the test. Needs an `AtomicInteger`, which is the smallest honest example of why "just use a var" stops working the moment there are two threads.
SG2. **`retryWithBackoff`** — three blocks arriving at once and none of them knowing about the others: F.5 built the schedule as a lazy infinite value, E.SE2 established that the last failure is the one to report, G.4 made a delay that does not occupy a thread. `delays.tail` walks the schedule as the retries walk it.
SG3. **`foldSequentially`** — `foldLeft` over a Future. It is sequential because `flatMap` cannot construct the next step without the previous value, which is *the same property* that made E.4 unable to accumulate errors. One mechanism, two consequences, and seeing that they are the same thing is roughly the point of the whole day.
SG4. **`memoizeFuture`** — the thundering herd, and the payoff of F.SF1's "not thread-safe... Block G" caveat. The insight is to **cache the Future, not the value**: a Future for work already in progress is a fine thing to hand the second caller. Built on `java.util.concurrent.ConcurrentHashMap.computeIfAbsent`, which everyone in the room already knows from Java and which runs its function at most once per key however many threads arrive together. **Verified:** twenty real threads released simultaneously against the naive `getOrElseUpdate` produces *"20 identical queries hit the database instead of one"*. The solution notes the one honest caveat (`computeIfAbsent` holds a bin lock while the function runs, so a `compute` slow to *return* blocks other writers — with the lock-free `putIfAbsent` + Promise variant given for when that matters), and closes the day on the fact that a failed Future stays cached forever, so caches are easy to write and hard to write well.

*Test-writing note, in case you extend these:* the thundering-herd test needs **real threads and a latch**. `Future.traverse` applies the function on one thread, one after another, so there is no race to lose and the naive version passes. The timing assertions are loose (they look for "three delays instead of one", not milliseconds) and the whole suite was run repeatedly to check it does not flake.

### Actors — decided: not covered

Big Guy asked for "some actors", but actors cannot be practised without first being introduced, and that introduction alone costs more time than the day has. **Decision: no actor slot on day 2.** Worth mentioning to Blip so the omission is explicit rather than discovered.

(The Akka-or-Pekko question therefore no longer blocks anything, though it is still worth knowing if Akka comes up in conversation.)

---

## 5. Day 3 — Project: the bet settlement engine

**Shape:** a guided, milestone-driven project. Not a brief-and-run — everybody walks the same path, at roughly the same pace, with design freedom in three specific places (the modelling exercise, how each milestone is implemented, and the stretch ladder where teams genuinely diverge).

### 5.1 Fixed decisions

- **~20 people, so 6–7 teams of 3.**
- **Three machines per team, coordinating over git.**
- **The betting domain is stipulated on a one-page rules card**, not researched. Everything a team needs is on it, stated as rules. Daniel is not teaching betting; he is handing out a spec, and the card says explicitly where it simplifies.
- **Acceptance is golden files plus a demo**, not a provided test suite. See 5.4.
- **The Cask server is the bonus milestone**, and the first thing to cut if M1–M3 run long. **Verified working:** Cask 0.11.3 on Scala 3.8.4 — routing, `@cask.postJson`, and `AtomicReference` state across requests.

### 5.2 The domain, as stipulated

| Term | Meaning |
|---|---|
| **Market** | Something you can bet on — "Porto vs Benfica, Match Result" — with a fixed set of selections |
| **Selection** | One possible outcome — Home / Draw / Away |
| **Decimal odds** | Payout multiplier including stake. €10 at 2.50 returns €25 |
| **Bet** | A customer stakes an amount on one or more selections at agreed odds |
| **Result** | Per market: the winning selection, or "market voided" |
| **Settlement** | Per bet: Won (stake × odds), Lost (0), Void (stake back), Pending (no result yet) |

Money rounds half-up to 2 decimal places, **once, at payout** — stipulated on the card so it is never argued about in a demo.

### 5.3 Milestone ladder

| | Milestone | Core mechanic | Concepts from days 1–2 | Difficulty |
|---|---|---|---|---|
| **M0** | Model it | Design the ADTs on paper, then compare with the reference model | Block C's JSON design exercise, at project scale | — |
| **M1** | Settle a single bet | Parse a bet file, settle each bet, compute payouts | `traverse`, `Either` + parse errors, exhaustive match, BigDecimal | Moderate |
| **M2** | Voids, pending, rejections | Market voided → stake back; no result yet → Pending; unknown market → typed error | Error ADT, `Either`, Pending-is-not-an-error | Easy–moderate |
| **M3** | Accumulators | One bet, several legs. All must win; payout = stake × product of odds; **a void leg becomes odds 1.0**; any leg pending → whole bet pending | fold over legs, ADT design, M1 becomes the one-leg case | **Hardest** |
| **M4** | The ledger | Fold a stream of result events into per-customer positions; duplicates, late results, unknown markets | fold-as-state (A.2, SG3), idempotence | Moderate |
| **M5** | Cask server *(bonus)* | `POST /results` mutating shared state while `GET /ledger/:customer` reads it | `AtomicReference` under contention (SG4), their own C.3 JSON renderer, Java interop | Moderate |

**M3 is where the day is won or lost.** The void-leg rule is the best thing in the domain: a 4-leg accumulator with one leg voided becomes a 3-leg accumulator at the same stake, because odds 1.0 is the multiplicative identity. Teams who modelled `Settlement` with a payout multiplier get it free; teams who special-cased win/lose rewrite. A real design consequence they can feel.

**M1 becomes a special case of M3** on purpose — they refactor rather than branch, and discover their M1 code was the one-leg instance all along.

**Contract, stated at the briefing:** M1–M3 is what every team finishes. M4 is expected for most. M5 is a bonus and it is fine not to reach it.

**Stretch ladder** (where teams diverge, and what makes demos worth watching): cash-out valuation of a partially-settled accumulator · each-way bets · per-market liability report ("what do we owe if Home wins?") · fractional and American odds parsing · replay from a snapshot.

### 5.4 Acceptance — golden files, not tests

Provided tests cannot work here: teams design their own types, so a suite written against one API will not compile against another. Prose criteria alone are also not enough — every demo becomes a negotiation about rounding and ordering.

**So: fixed input files, and the exact expected output, per milestone.** The criterion is *"your program, structured however you like, prints this."*

- **No coupling to their API.** The contract is at the program boundary, not the function boundary.
- **Self-checking.** A team knows it is done by diffing, in five seconds, without queueing for Daniel.
- **No arguments.** Rounding, ordering and formatting are settled by the file.
- **It rescues the reference-solution escape hatch.** Design freedom and "take the reference and move on" are otherwise in direct conflict: if every team has its own model, nobody can drop in M3 reference code that plugs into their M2. With a whole-program contract, the reference is a whole program — a stuck team loses their code but not their place, and continues into M4 with their own hands.

Ship a ten-line `check` script that diffs their output against the golden file. A tool, not a test suite.

**Demo cadence, given 7 teams.** 7 × 5 = 35 demos would consume the whole day and leave nobody floating to help. So:

- Every milestone is **self-verified** against the golden file and ticked with a git tag.
- **Formal demos to Daniel at M3 only, plus the final round.** ~14 conversations, and M3 is the right one to inspect because it is where modelling decisions surface.
- Daniel circulates the rest of the time, which is where he is most useful anyway.

### 5.5 Git mechanics

- **One repo per team**, from a template on Blip's internal host. Teams add Daniel as a collaborator so progress is visible without walking over.
- **Reference branches pushed at each checkpoint** — `reference/m1`, `reference/m2`… on the template. A stuck team runs `git fetch reference && git checkout -b m3 reference/m2`. Pushed at the checkpoint, so nobody peeks early.
- **Milestone ticks are tags**: `git tag m2-done && git push --tags`. The progress board updates itself.
- **scalafmt config in the repo.** Not aesthetics: three people editing ~500 lines over five hours generate formatting conflicts that eat real time, and one config file prevents all of them.
- **Split by file, not by function** — an owner per area per milestone (parsing / settlement / ledger / report). Three people in one 200-line settlement file will conflict constantly; three people in four files rarely will.
- Say at the briefing: rebase rather than merge, and commit at least every fifteen minutes. A team that has not committed since 10:00 and hits a conflict at 14:00 is finished.

### 5.6 Run of day

| Time | |
|---|---|
| 09:00–09:30 | Briefing, teams, rules card walkthrough |
| 09:30–10:00 | **M0** — model on paper, then the reveal and the argument |
| 10:00–11:00 | **M1** → checkpoint |
| 11:15–12:15 | **M2** → checkpoint |
| 13:15–14:30 | **M3** → checkpoint + **demo to Daniel** |
| 14:30–15:15 | **M4** → checkpoint |
| 15:30–16:15 | **M5** (bonus) / stretch ladder |
| 16:15–17:00 | Demos and wrap |

**Demos change shape under this structure.** Everyone built the same thing, so "here is my app" is dull. The five minutes worth having is: *how did you model it, what did M0 get wrong that M3 revealed, and which stretch did you take?* Tell teams that at the briefing so they take notes as they go.

**AI**: matches Blip's own policy — use it freely, and anyone on the team must be able to explain any line when asked.

### 5.7 Anticipated complexity — honest version

A team of 3 has roughly **5 hours of actual coding**; M1–M5 is on the order of 400–600 lines of production code. Tight. Two things would eat time unproductively and are therefore **pre-built in the skeleton**: CSV parsing, and the HTTP bootstrap.

The Cask dependency is one line and pulls uPickle with it. Responses can be plain strings rendered by **their own Block C.3 JSON renderer**, which avoids teaching uPickle and closes a nice loop.

**The server gives concurrency, not Futures.** Cask is thread-per-request, so what M5 exercises is shared mutable state under contention — `AtomicReference`, compare-and-set, idempotent updates — which pays off SG4 rather than G.1. That is the more valuable half for a betting platform, and day 2 drilled Futures properly already.
