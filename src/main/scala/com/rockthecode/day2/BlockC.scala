package com.rockthecode.day2

/**
 * Day 2, Block C - Modeling with ADTs and pattern matching.
 *
 * You have been USING algebraic data types since Block A - `AccountStatus`,
 * `Transaction` - without anyone calling them that. This block is where you
 * design your own.
 *
 * The idea is small and the consequences are large. An ADT says: a value of this
 * type is EXACTLY one of these shapes, and there are no others. Because the
 * compiler knows the list is closed, it can check that your `match` covered it -
 * and it will tell you every place that needs attention when you add a shape.
 * That last part is the whole reason this is worth your time: it turns "find
 * everywhere that handles order states" from a grep into a compile.
 *
 * ---------------------------------------------------------------------------
 * Scala 2 and Scala 3 - you run both, so here is the same type twice.
 *
 *   Scala 3                          Scala 2 (and still valid in 3)
 *   ---------------------------      ---------------------------------------
 *   enum Colour {                    sealed trait Colour
 *     case Red                       case object Red extends Colour
 *     case Green                     case object Green extends Colour
 *     case Custom(hex: String)       case class Custom(hex: String) extends Colour
 *   }                                object Colour { ... }
 *
 * `sealed` is the load-bearing word in the Scala 2 version: it means every
 * subtype must live in this file, which is what lets the compiler know the list
 * is closed. An `enum` is that, with less typing, plus `Colour.values` for free.
 *
 * Block A's `AccountStatus` and `Transaction` are written the Scala 2 way on
 * purpose. This block uses `enum` throughout, so you have seen both by lunch.
 * ---------------------------------------------------------------------------
 *
 * Same rules: no `var`, no `while`, no mutable collection.
 *
 * This trait is the exercise brief. Write your answers in `BlockCExercises`.
 */
trait BlockC {

  // ---------------------------------------------------------------------------
  // C.1 Shapes - the warm-up, and the demo
  //
  // Two functions over one small ADT. Neither is interesting on its own; what
  // is interesting happens at the end, when a fourth shape gets added and the
  // compiler lists every function that now has a hole in it.
  //
  // Note that `area` could just as well be a method ON the enum. It is out here
  // because the point of the exercise is the match - and because in real code
  // the ADT is often in a module you do not own.
  // ---------------------------------------------------------------------------

  /**
   * `area(Circle(2))       == 12.566...`  (pi r squared)
   * `area(Rectangle(3, 4)) == 12.0`
   * `area(Triangle(3, 4))  == 6.0`        (half base times height)
   */
  def area(shape: Shape): Double

  /**
   * Resize a shape, keeping its kind. Every dimension is multiplied by `factor`.
   *
   * `scale(Rectangle(3, 4), 2) == Rectangle(6, 8)`
   *
   * Note what this implies about area: scaling by 2 makes the area FOUR times
   * bigger. One of the tests checks that, on all three shapes at once.
   */
  def scale(shape: Shape, factor: Double): Shape

  // ---------------------------------------------------------------------------
  // C.2 An order lifecycle
  //
  // The one that matters. A state machine is a function (State, Event) => State,
  // except that most (state, event) pairs are nonsense - you cannot ship an
  // order nobody has paid for.
  //
  // So the answer is not a State. It is "either a new state, or a reason why
  // not", and BOTH of those are worth modelling properly.
  // ---------------------------------------------------------------------------

  /**
   * The permitted moves, and nothing else:
   *
   *   Draft    + Place   -> Placed
   *   Placed   + Pay     -> Paid
   *   Paid     + Ship    -> Shipped
   *   Shipped  + Deliver -> Delivered
   *   Cancel             -> Cancelled, from Draft, Placed or Paid only
   *
   * Everything else is refused:
   *   - from a final state (Delivered, Cancelled), any event at all is
   *     `Refusal.AlreadyFinished(state)`
   *   - otherwise it is `Refusal.Impossible(state, event)`
   *
   * Two things to notice while you write it. First, matching on the PAIR
   * `(state, event)` rather than nesting two matches is what makes this
   * readable. Second, the compiler is checking you against thirty
   * combinations - so write the five case lines that cover the seven legal
   * moves, then two ways of saying no, and let the type system carry the rest.
   */
  def transition(state: OrderState, event: OrderEvent): TransitionResult

  /** Delivered and Cancelled are the end of the road. Everything else is not. */
  def isFinal(state: OrderState): Boolean

  // ---------------------------------------------------------------------------
  // C.3 JSON - a recursive ADT
  //
  // STOP. Before you scroll down to the `Json` enum, design it yourself.
  //
  // Whiteboard, paper, or a scratch file - ten minutes, no compiler. The
  // question is simply: what shapes can a JSON value take? Write down the
  // complete list, with the data each one carries. When you think you are
  // finished, check it against a JSON document you have actually seen.
  //
  // This is the only part of today that is design rather than coding, and it is
  // the part you will do most often in real work. Getting the set of cases
  // right is most of the job; the functions over them almost write themselves
  // afterwards, which is the thing to notice.
  //
  // Then compare with the enum at the bottom of this file. If you got a
  // different set, work out whether it is different-and-fine or
  // different-and-wrong - both happen, and the difference is worth arguing
  // about out loud.
  //
  // Everything so far has been flat. `Json` refers to itself: an array holds
  // more Json, an object holds more Json. Recursive data wants recursive
  // functions, and pattern matching is what makes them readable.
  // ---------------------------------------------------------------------------

  /**
   * Render a `Json` value as compact JSON text - no spaces, no newlines.
   *
   *   JNull                                    -> null
   *   JBool(true)                              -> true
   *   JNum(1.5)                                -> 1.5
   *   JStr("hi")                               -> "hi"
   *   JArr(List(JNum(1), JNull))               -> [1,null]
   *   JObj(List("a" -> JNum(1), "b" -> JArr(Nil))) -> {"a":1,"b":[]}
   *
   * Inside a string, a backslash becomes `\\` and a double quote becomes `\"`.
   * Nothing else is escaped - real JSON has more rules and they are not the
   * point here.
   *
   * `JObj` holds a List of pairs rather than a Map, on purpose: field order is
   * then part of the value, so rendering is reproducible. A Map would make the
   * output depend on hashing, which is Block B.5's bug wearing a new hat.
   */
  def render(json: Json): String

  // ---------------------------------------------------------------------------
  // C.4 Reading a command line
  //
  // Pattern matching on lists, which is where `::` and `Nil` stop being
  // constructors and start being patterns.
  // ---------------------------------------------------------------------------

  /**
   * Turn raw arguments into a `Command`:
   *
   *   Nil                     -> Help
   *   List("help")            -> Help
   *   List("list")            -> ListAll
   *   List("add", "milk")     -> AddItem("milk")
   *   List("remove", "milk")  -> RemoveItem("milk")
   *   anything else           -> Unknown(the arguments, unchanged)
   *
   * "Anything else" genuinely means anything: `List("add")` with no item is
   * Unknown, and so is `List("add", "milk", "bread")`.
   *
   * Write it as one match over the list. `case "add" :: item :: Nil` is a
   * pattern, and it reads exactly like the shape of the input.
   */
  def interpret(args: List[String]): Command


  // ---------------------------------------------------------------------------
  // C.5 Evaluating an expression
  //
  // A second recursive ADT, and a gentler one than JSON - every case is either a
  // number or an operator with children.
  // ---------------------------------------------------------------------------

  /**
   * Work out what an expression comes to.
   *
   * `eval(Add(Lit(2), Lit(3)))            == Some(5)`
   * `eval(Mul(Lit(2), Neg(Lit(3))))       == Some(-6)`
   *
   * None if a division by zero happens ANYWHERE in the tree - including in a
   * part of the expression whose result would have been multiplied by zero
   * anyway. Do not be clever about it: if `0 * (1 / 0)` came out as zero, then
   * the order you happen to evaluate things in would become part of what the
   * language means.
   *
   * The recursion is the easy half. The interesting half is carrying "there is
   * no answer" up through it without writing a single `if` about it - you have
   * `Option`, and `Option` has a for-comprehension.
   */
  def eval(expr: Expr): Option[BigDecimal]

  // ===========================================================================
  //
  //  STRETCH SECTION - only if you finished the block above.
  //
  //  C.3 showed a recursive ADT. These go further into it: walking a tree,
  //  searching it, rewriting it, and finally treating a state machine as the
  //  graph it secretly is.
  //
  //  Nothing later in the day depends on any of these.
  //
  // ===========================================================================

  /**
   * SC1. Follow a path into a JSON document.
   *
   * `at(doc, List("user", "address", "city"))` walks down three nested objects
   * and returns what it finds. None if any step is missing, or if a step tries
   * to look up a field on something that is not an object.
   *
   * `at(doc, Nil) == Some(doc)` - an empty path has already arrived.
   *
   * Arrays are not indexed here; a path segment is always a field name.
   */
  def at(json: Json, path: List[String]): Option[Json]

  /**
   * SC2. Every string anywhere in the document, in the order they appear.
   *
   * Object FIELD NAMES do not count - only `JStr` values. Look inside arrays,
   * inside objects, and inside arrays inside objects.
   */
  def collectStrings(json: Json): List[String]

  /**
   * SC3. How deeply nested is this document?
   *
   * A scalar - null, a boolean, a number, a string - has depth 1. A container
   * has one more than its deepest child. An EMPTY container has depth 1, since
   * it has no children to be deeper than.
   *
   * `depth(JNum(1))                          == 1`
   * `depth(JArr(List(JNum(1))))              == 2`
   * `depth(JArr(List(JArr(List(JNum(1))))))  == 3`
   * `depth(JArr(Nil))                        == 1`
   */
  def depth(json: Json): Int

  /**
   * SC4. Simplify an expression by applying these rules, and only these:
   *
   *   x + 0  ->  x        0 + x  ->  x
   *   x * 1  ->  x        1 * x  ->  x
   *   x * 0  ->  0        0 * x  ->  0
   *   x / 1  ->  x
   *   -(-x)  ->  x
   *
   * No constant folding: `Lit(2) + Lit(3)` stays exactly as it is.
   *
   * The shape that makes this work: simplify the CHILDREN first, then look at
   * what you now have and apply a rule to it. Bottom-up in one pass is enough
   * for these rules - `(x * 1) + 0` collapses to `x` without any need to loop
   * until nothing changes. Convince yourself of that before you write a loop.
   */
  def simplify(expr: Expr): Expr

  /**
   * SC5. Every state reachable from here, following legal transitions, in any
   * number of steps - including the state you started in.
   *
   * `reachableStates(OrderState.Shipped)   == Set(Shipped, Delivered)`
   * `reachableStates(OrderState.Delivered) == Set(Delivered)`
   *
   * A state machine is a graph and this is a graph search, so it needs a
   * "keep going until nothing new turns up" loop. Write that as a recursive
   * function over (frontier, seen) rather than as a `while` with a mutable
   * `Set` - it is the same algorithm and it is shorter.
   *
   * Build it on `transition`, not on a second copy of the rules. If the rules
   * change, exactly one place should need editing.
   */
  def reachableStates(from: OrderState): Set[OrderState]

  // ---------------------------------------------------------------------------
  // SC6. A JSON converter, as a type class
  //
  // The last exercise of the block, and the only place today where you will
  // meet `given`. It is here because you WILL meet it in Blip's codebase, and
  // because it answers a question C.3 leaves hanging: `render` turns a `Json`
  // into text, but who turns a `User` into a `Json`?
  //
  // The obvious answer is a method on `User`. That fails the moment the type
  // belongs to somebody else - you cannot add a method to `java.util.Date`. A
  // type class separates "what this can do" from "the type itself":
  //
  //   trait JsonConverter[T] { def convert(value: T): Json }
  //
  // ...and then one instance per type, registered with `given` so the compiler
  // can find it without being told.
  //
  // Write the eight instances below. Six are ordinary. Two are the point:
  // `optionToJson` and `listToJson` take a converter and RETURN a converter, so
  // the compiler can build `JsonConverter[List[Post]]` out of
  // `JsonConverter[Post]` without you ever writing that type down. Once those
  // exist, `feedToJson` gets `List[Post]` for free.
  //
  // Style note: `JsonConverter[String].convert(x)` works because of the `apply`
  // on the companion, and reads better than `summon[JsonConverter[String]]`.
  //
  // These are declared here as abstract `given`s, which is unusual - it is so
  // that the tests can pull YOUR instances into scope. In real code they would
  // live in the companion object of the type they convert, which is the first
  // place the compiler looks.
  // ---------------------------------------------------------------------------

  given stringToJson: JsonConverter[String]

  given intToJson: JsonConverter[Int]

  given booleanToJson: JsonConverter[Boolean]

  /** None becomes JNull. Some(x) becomes whatever x converts to. */
  given optionToJson[T](using JsonConverter[T]): JsonConverter[Option[T]]

  /** A list becomes a JArr of its converted elements, in order. */
  given listToJson[T](using JsonConverter[T]): JsonConverter[List[T]]

  /** Fields in declaration order: name, age, email. */
  given userToJson: JsonConverter[User]

  /** Fields in declaration order: title, likes, tag. */
  given postToJson: JsonConverter[Post]

  /** Fields in declaration order: owner, posts. */
  given feedToJson: JsonConverter[Feed]
}

// -----------------------------------------------------------------------------
// The types. This is the part that is worth reading slowly.
// -----------------------------------------------------------------------------

enum Shape {
  case Circle(radius: Double)
  case Rectangle(width: Double, height: Double)
  case Triangle(base: Double, height: Double)
}

enum OrderState {
  case Draft, Placed, Paid, Shipped, Delivered, Cancelled
}

enum OrderEvent {
  case Place, Pay, Ship, Deliver, Cancel
}

/** Why a transition was refused. A reason is data, not a string and not an exception. */
enum Refusal {
  case AlreadyFinished(state: OrderState)
  case Impossible(state: OrderState, event: OrderEvent)
}

/**
 * The result of trying to move. Either it moved, or it did not and here is why.
 *
 * Hold on to this shape - "a good answer or a reason" turns out to be so common
 * that the standard library has a name for it. You meet it in Block E.
 */
enum TransitionResult {
  case Moved(to: OrderState)
  case Rejected(reason: Refusal)
}

/**
 * A JSON document.
 *
 * Six cases, and between them they describe every JSON value that exists. That
 * is the claim an ADT makes, and it is why the compiler can be so helpful about
 * it.
 */
enum Json {
  case JNull
  case JBool(value: Boolean)
  case JNum(value: BigDecimal)
  case JStr(value: String)
  case JArr(items: List[Json])
  case JObj(fields: List[(String, Json)])
}

enum Command {
  case Help
  case ListAll
  case AddItem(item: String)
  case RemoveItem(item: String)
  case Unknown(args: List[String])
}

enum Expr {
  case Lit(value: BigDecimal)
  case Add(left: Expr, right: Expr)
  case Mul(left: Expr, right: Expr)
  case Div(left: Expr, right: Expr)
  case Neg(inner: Expr)
}

// -----------------------------------------------------------------------------
// SC6's types
// -----------------------------------------------------------------------------

/**
 * "Something that knows how to turn a T into Json."
 *
 * One instance per type, and the compiler assembles the ones it needs.
 */
trait JsonConverter[T] {
  def convert(value: T): Json
}

object JsonConverter {

  /** So you can write `JsonConverter[String].convert(x)` instead of `summon[...]`. */
  def apply[T](using converter: JsonConverter[T]): JsonConverter[T] = converter
}

case class User(name: String, age: Int, email: String)

case class Post(title: String, likes: Int, tag: Option[String])

case class Feed(owner: User, posts: List[Post])
