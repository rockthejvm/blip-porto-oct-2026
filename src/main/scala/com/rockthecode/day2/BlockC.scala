package com.rockthecode.day2

/**
 * Day 2, Block C - Modeling with ADTs and pattern matching.
 *
 */
trait BlockC {

  // ---------------------------------------------------------------------------
  // C.1 Shapes
  //
  // Two functions over one small ADT. 
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
   */
  def scale(shape: Shape, factor: Double): Shape

  // ---------------------------------------------------------------------------
  // C.2 An order lifecycle
  // ---------------------------------------------------------------------------

  /**
   * The permitted moves:
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
   */
  def transition(state: OrderState, event: OrderEvent): TransitionResult

  /** Delivered and Cancelled are terminal */
  def isFinal(state: OrderState): Boolean

  // ---------------------------------------------------------------------------
  // C.3 JSON - a recursive ADT
  //
  // STOP. Before you scroll down to the `Json` enum, design it yourself.
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
   *
   * `JObj` holds a List of pairs rather than a Map: field order is
   * then part of the value, so rendering is reproducible. A Map would make the
   * output depend on hashing.
   */
  def render(json: Json): String

  // ---------------------------------------------------------------------------
  // C.4 Reading a command line
  //
  // Pattern matching on lists.
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
   * "Anything else" means anything: `List("add")` with no item is
   * Unknown, and so is `List("add", "milk", "bread")`.
   */
  def interpret(args: List[String]): Command


  // ---------------------------------------------------------------------------
  // C.5 Evaluating an expression
  //
  // A second recursive ADT
  // ---------------------------------------------------------------------------

  /**
   * Work out what an expression comes to.
   *
   * `eval(Add(Lit(2), Lit(3)))            == Some(5)`
   * `eval(Mul(Lit(2), Neg(Lit(3))))       == Some(-6)`
   *
   * None if a division by zero happens ANYWHERE in the tree - including in a
   * part of the expression whose result would have been multiplied by zero
   * anyway. 
   */
  def eval(expr: Expr): Option[BigDecimal]

  // ===========================================================================
  //
  //  STRETCH SECTION - only if you finished the block above.
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
   */
  def at(json: Json, path: List[String]): Option[Json]

  /**
   * SC2. Every string anywhere in the document, in the order they appear.
   *
   * Object FIELD NAMES do not count - only `JStr` values.
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
   * SC4. Simplify an expression by applying these rules:
   *
   *   x + 0  ->  x        0 + x  ->  x
   *   x * 1  ->  x        1 * x  ->  x
   *   x * 0  ->  0        0 * x  ->  0
   *   x / 1  ->  x
   *   -(-x)  ->  x
   *
   * No constant folding: `Lit(2) + Lit(3)` stays exactly as it is.
   */
  def simplify(expr: Expr): Expr

  /**
   * SC5. Every state reachable from here, following legal transitions, in any
   * number of steps - including the state you started in.
   *
   * `reachableStates(OrderState.Shipped)   == Set(Shipped, Delivered)`
   * `reachableStates(OrderState.Delivered) == Set(Delivered)`
   *
   */
  def reachableStates(from: OrderState): Set[OrderState]

  // ---------------------------------------------------------------------------
  // SC6. A JSON converter, as a type class
  //
  //   trait JsonConverter[T] { def convert(value: T): Json }
  //
  // ...and then one instance per type, registered with `given` so the compiler
  // can find it without being told.
  //
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
// The types
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

/** Why a transition was refused.  */
enum Refusal {
  case AlreadyFinished(state: OrderState)
  case Impossible(state: OrderState, event: OrderEvent)
}

/**
 * The result of trying to move: either it moved, or it did not + why.
 */
enum TransitionResult {
  case Moved(to: OrderState)
  case Rejected(reason: Refusal)
}

/**
 * A JSON document.
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
