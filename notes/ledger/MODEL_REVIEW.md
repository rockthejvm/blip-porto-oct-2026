# Model review — 10:10, three minutes per team

Ask; do not tell. Each question surfaces one mistake without naming it.

| If you see | Ask | Looking for |
|---|---|---|
| One type for commands and facts | "What goes in the log when a withdrawal is refused?" | Nothing — so the log holds a different kind of thing than the request |
| `Double` for money | "Compute `0.1 + 0.2` in the REPL." | `Long` cents, or a `Money` wrapper around one |
| Validation inside `evolve` | "What happens when you replay a log written last Tuesday?" | `evolve` applies, never checks |
| `var` state, `mutable.Map` of accounts | "How would you implement undo?" | Immutable state; a new one per fact |
| `decide` returning a single event | "How many things happen during a transfer?" | Do NOT mention M4. Let the refactor land at 14:15. |
| `trait Aggregate[C, E, S]` | "Make one concrete case work first." | Also: this is precisely the abstraction the course left out |
| Balance stored *and* derivable | "Which one is the truth?" | Either answer is fine; wanting a deliberate one |
| String-typed rejections | "How does the caller tell two of them apart?" | An ADT |
| A model shaped like the wire (`Effect(sign, amount)`) | "If the output format changed tomorrow, how much of your model changes?" | The wire is a projection; a model isomorphic to it skipped the modelling |
| No fact type at all — just `Map[String, Account]` | "Where does the sequence number come from?" | This one is a rewrite, not a refactor. Catch it now or in the first ten minutes of M1. |

Also worth a glance: is status an enum, two booleans, or a nested type? (All defensible — ask why.)
Is the parser separate from `decide`? Does anything in the model mention a string like `"OK"`?
