package com.rockthecode.day2

/**
 * Day 2, Block C - your workspace.
 *
 * Briefs are the scaladoc on `BlockC`. The types are at the bottom of that file
 * and are worth reading before you start - except for `Json`, which you should
 * try to design yourself first. See the note above C.3.
 *
 *   sbt blockC
 */
object BlockCExercises extends BlockC {

  // C.1 Shapes
  def area(shape: Shape): Double = ???

  def scale(shape: Shape, factor: Double): Shape = ???

  // C.2 An order lifecycle
  def transition(state: OrderState, event: OrderEvent): TransitionResult = ???

  def isFinal(state: OrderState): Boolean = ???

  // C.3 JSON
  def render(json: Json): String = ???

  // C.4 Reading a command line
  def interpret(args: List[String]): Command = ???

  // C.5 Evaluating an expression
  def eval(expr: Expr): Option[BigDecimal] = ???

  // ===========================================================================
  //
  //  STRETCH SECTION - only if you finished the block above.
  //
  // ===========================================================================

  def at(json: Json, path: List[String]): Option[Json] = ???

  def collectStrings(json: Json): List[String] = ???

  def depth(json: Json): Int = ???

  def simplify(expr: Expr): Expr = ???

  def reachableStates(from: OrderState): Set[OrderState] = ???

  // SC6 - the type class. Eight instances; `optionToJson` and `listToJson` are
  // the two that matter.

  given stringToJson: JsonConverter[String] = ???

  given intToJson: JsonConverter[Int] = ???

  given booleanToJson: JsonConverter[Boolean] = ???

  given optionToJson[T](using JsonConverter[T]): JsonConverter[Option[T]] = ???

  given listToJson[T](using JsonConverter[T]): JsonConverter[List[T]] = ???

  given userToJson: JsonConverter[User] = ???

  given postToJson: JsonConverter[Post] = ???

  given feedToJson: JsonConverter[Feed] = ???
}
