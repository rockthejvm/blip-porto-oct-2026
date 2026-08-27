package com.rockthecode.day2.solutions

import com.rockthecode.day2.*

import scala.annotation.tailrec

/**
 * Day 2, Block C - reference solutions.
 */
object BlockCSolutions extends BlockC {

  // ---------------------------------------------------------------------------
  // C.1 Shapes
  // ---------------------------------------------------------------------------

  import Shape.*

  def area(shape: Shape): Double =
    shape match {
      case Circle(radius)          => math.Pi * radius * radius
      case Rectangle(width, height) => width * height
      case Triangle(base, height)   => base * height / 2
    }

  def scale(shape: Shape, factor: Double): Shape =
    shape match {
      case Circle(radius)           => Circle(radius * factor)
      case Rectangle(width, height) => Rectangle(width * factor, height * factor)
      case Triangle(base, height)   => Triangle(base * factor, height * factor)
    }

  // THE DEMO. Live, on screen, at the end of C.1:
  //
  //   add `case Square(side: Double)` to the enum, recompile, and read out the
  //   warnings. Every function with a hole in it, by exact line number, no grep.
  //
  // Then make the point that matters: this only works because the type is
  // SEALED. Delete `sealed` from a Scala 2 ADT (or model it as a plain trait
  // anyone can extend) and the compiler goes quiet - not because the code got
  // safer, but because it stopped being able to help.
  //
  // Worth turning warnings into errors for them too, on screen:
  //   scalacOptions += "-Werror"
  // Non-exhaustive matches are the single best candidate for that flag.

  // ---------------------------------------------------------------------------
  // C.2 An order lifecycle
  // ---------------------------------------------------------------------------

  import OrderState.*
  import OrderEvent.*
  import TransitionResult.*
  import Refusal.*

  def transition(state: OrderState, event: OrderEvent): TransitionResult =
    (state, event) match {
      case (Draft, Place)               => Moved(Placed)
      case (Placed, Pay)                => Moved(Paid)
      case (Paid, Ship)                 => Moved(Shipped)
      case (Shipped, Deliver)           => Moved(Delivered)
      case (Draft | Placed | Paid, Cancel) => Moved(Cancelled)
      case _ if isFinal(state)          => Rejected(AlreadyFinished(state))
      case _                            => Rejected(Impossible(state, event))
    }

  // Five legal moves, two ways of saying no, thirty combinations covered.
  //
  // `Draft | Placed | Paid` is an alternative pattern - one case, three states.
  // Worth showing, because the alternative is three near-identical lines and
  // people write those.
  //
  // Order matters here in a way that is easy to miss: the `isFinal` guard has
  // to come after the legal moves and before the catch-all. Move it to the top
  // and nothing breaks, because no final state has a legal move - but that is
  // luck, not design, and it stops being true the moment someone adds a
  // Reopen event.

  def isFinal(state: OrderState): Boolean =
    state match {
      case Delivered | Cancelled => true
      case _                     => false
    }

  // ---------------------------------------------------------------------------
  // C.3 JSON
  // ---------------------------------------------------------------------------

  import Json.*

  def render(json: Json): String =
    json match {
      case JNull          => "null"
      case JBool(value)   => value.toString
      case JNum(value)    => value.toString
      case JStr(value)    => renderString(value)
      case JArr(items)    => items.map(render).mkString("[", ",", "]")
      case JObj(fields)   =>
        fields.map((name, value) => s"${renderString(name)}:${render(value)}").mkString("{", ",", "}")
    }

  private def renderString(raw: String): String =
    "\"" + raw.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

  // Note the order of the two replacements: backslashes first, then quotes.
  // Do it the other way round and the backslash you just inserted in front of a
  // quote gets escaped again, turning `"` into `\\"`. It is a two-line function
  // with an ordering bug in it, which is why there is a test for exactly that.
  //
  // `mkString("[", ",", "]")` handles the empty case for free - no `if` needed
  // to avoid a trailing comma. Worth pointing at; people reach for a fold.

  // ---------------------------------------------------------------------------
  // C.4 Reading a command line
  // ---------------------------------------------------------------------------

  import Command.*

  def interpret(args: List[String]): Command =
    args match {
      case Nil                       => Help
      case "help" :: Nil             => Help
      case "list" :: Nil             => ListAll
      case "add" :: item :: Nil      => AddItem(item)
      case "remove" :: item :: Nil   => RemoveItem(item)
      case unrecognised              => Unknown(unrecognised)
    }

  // This is the whole point of list patterns: the code has the same shape as
  // the input. `"add" :: item :: Nil` is read as "the word add, then one more
  // thing, then nothing" - which is the specification, spelled the same way.
  //
  // Note that the compiler does NOT check this one for exhaustiveness in any
  // useful sense: List is not a small closed set of shapes, so the catch-all is
  // doing real work rather than shutting the compiler up. Knowing which of your
  // matches are checked and which are not is a useful instinct.

  // ---------------------------------------------------------------------------
  // C.5 Evaluating an expression
  // ---------------------------------------------------------------------------

  import Expr.*

  def eval(expr: Expr): Option[BigDecimal] =
    expr match {
      case Lit(value)         => Some(value)
      case Neg(inner)         => eval(inner).map(-_)
      case Add(left, right)   => for { l <- eval(left); r <- eval(right) } yield l + r
      case Mul(left, right)   => for { l <- eval(left); r <- eval(right) } yield l * r
      case Div(left, right)   =>
        for {
          l <- eval(left)
          r <- eval(right)
          if r != 0
        } yield l / r
    }

  // The for-comprehension is doing the propagation: if either side is None the
  // whole thing is None, and nobody had to write that. The `if r != 0` guard is
  // a `withFilter` on Option, which turns Some into None - which is exactly the
  // "no answer" this needs.
  //
  // Note what the brief forbade: short-circuiting `Mul(_, Lit(0))` to zero
  // without evaluating the other side. That would make `0 * (1/0)` equal zero,
  // and then evaluation order becomes part of the language definition. Say no.

  // ===========================================================================
  //
  //  STRETCH SECTION
  //
  // ===========================================================================

  // ---------------------------------------------------------------------------
  // SC1 / SC2 / SC3. Walking a tree
  // ---------------------------------------------------------------------------

  def at(json: Json, path: List[String]): Option[Json] =
    (json, path) match {
      case (found, Nil)          => Some(found)
      case (JObj(fields), step :: rest) =>
        fields.collectFirst { case (name, value) if name == step => value }.flatMap(child => at(child, rest))
      case _                     => None // a path segment against a non-object
    }

  // Matching on the pair again, exactly as in C.2. `collectFirst` stops at the
  // first hit and hands back an Option, which is precisely the shape needed to
  // `flatMap` into the next step.

  def collectStrings(json: Json): List[String] =
    json match {
      case JStr(value)  => List(value)
      case JArr(items)  => items.flatMap(collectStrings)
      case JObj(fields) => fields.flatMap((_, value) => collectStrings(value))
      case _            => Nil
    }

  // The underscore in `(_, value)` is the exercise: field NAMES are not values,
  // and it takes one character to say so. Anyone who writes
  // `fields.flatMap((name, value) => name :: collectStrings(value))` has
  // silently invented a different function.

  def depth(json: Json): Int =
    json match {
      case JArr(Nil)    => 1
      case JObj(Nil)    => 1
      case JArr(items)  => 1 + items.map(depth).max
      case JObj(fields) => 1 + fields.map((_, value) => depth(value)).max
      case _            => 1
    }

  // The empty cases come FIRST and they have to: `List.empty.max` throws, which
  // is Block B.2's lesson turning up uninvited. Anyone who writes the general
  // case first gets an exception rather than a wrong answer - which, at least,
  // is the good kind of failure.

  // ---------------------------------------------------------------------------
  // SC4. Simplifying expressions
  // ---------------------------------------------------------------------------

  def simplify(expr: Expr): Expr =
    expr match {
      case Lit(value) => Lit(value)
      case Neg(inner) =>
        simplify(inner) match {
          case Neg(twiceNegated) => twiceNegated // -(-x) = x
          case simplified        => Neg(simplified)
        }
      case Add(left, right) =>
        (simplify(left), simplify(right)) match {
          case (Lit(zero), other) if zero == 0 => other
          case (other, Lit(zero)) if zero == 0 => other
          case (l, r)                          => Add(l, r)
        }
      case Mul(left, right) =>
        (simplify(left), simplify(right)) match {
          case (Lit(zero), _) if zero == 0     => Lit(0)
          case (_, Lit(zero)) if zero == 0     => Lit(0)
          case (Lit(one), other) if one == 1   => other
          case (other, Lit(one)) if one == 1   => other
          case (l, r)                          => Mul(l, r)
        }
      case Div(left, right) =>
        (simplify(left), simplify(right)) match {
          case (other, Lit(one)) if one == 1 => other
          case (l, r)                        => Div(l, r)
        }
    }

  // Children first, then look at what you have. That order is why one pass is
  // enough: by the time the outer `Add` runs its rules, the inner `Mul(x, 1)`
  // has already become `x`, so `Add(x, Lit(0))` is sitting right there ready to
  // collapse. Nobody needs a loop-until-stable.
  //
  // Two honest caveats worth raising:
  //   - `Mul(x, 0) -> 0` throws away x, and if x contained a division by zero
  //     then simplify has just changed the meaning of the program. Real
  //     compilers agonise over exactly this. The tests pin the rule as written.
  //   - the zero and one checks are guards rather than patterns because
  //     BigDecimal literals do not pattern match on numeric value the way you
  //     would hope. Worth a mention when somebody asks why it is not
  //     `case (Lit(0), other)`.

  // ---------------------------------------------------------------------------
  // SC5. A state machine is a graph
  // ---------------------------------------------------------------------------

  def reachableStates(from: OrderState): Set[OrderState] = {
    @tailrec
    def explore(frontier: Set[OrderState], seen: Set[OrderState]): Set[OrderState] =
      if (frontier.isEmpty) seen
      else {
        val next =
          frontier.flatMap { state =>
            OrderEvent.values.toSet.collect(event =>
              transition(state, event) match { case Moved(destination) => destination }
            )
          }

        explore(next.diff(seen), seen ++ next)
      }

    explore(Set(from), Set(from))
  }

  // `collect` with a partial function that only matches `Moved` is doing the
  // filtering and the extraction in one step - Block 0.3, arriving where it is
  // genuinely the right tool rather than a coin toss.
  //
  // The termination argument is `next.diff(seen)`: only genuinely new states go
  // into the next frontier, and there are finitely many states, so the frontier
  // empties. This is breadth-first search with an immutable Set and no `while`
  // in sight - and it is the same "carry the state forward" move as every fold
  // today.

  // ---------------------------------------------------------------------------
  // SC6. A JSON converter, as a type class
  // ---------------------------------------------------------------------------

  import Json.*

  given stringToJson: JsonConverter[String] with {
    def convert(value: String): Json = JStr(value)
  }

  given intToJson: JsonConverter[Int] with {
    def convert(value: Int): Json = JNum(BigDecimal(value))
  }

  given booleanToJson: JsonConverter[Boolean] with {
    def convert(value: Boolean): Json = JBool(value)
  }

  // The two that matter: a converter that TAKES a converter. This is what lets
  // the compiler assemble JsonConverter[List[Post]] out of JsonConverter[Post]
  // without anybody writing that type down.

  given optionToJson[T](using inner: JsonConverter[T]): JsonConverter[Option[T]] with {
    def convert(value: Option[T]): Json = value.fold(JNull)(inner.convert)
  }

  given listToJson[T](using inner: JsonConverter[T]): JsonConverter[List[T]] with {
    def convert(values: List[T]): Json = JArr(values.map(inner.convert))
  }

  given userToJson: JsonConverter[User] with {
    def convert(user: User): Json =
      JObj(
        List(
          "name" -> JsonConverter[String].convert(user.name),
          "age" -> JsonConverter[Int].convert(user.age),
          "email" -> JsonConverter[String].convert(user.email)
        )
      )
  }

  given postToJson: JsonConverter[Post] with {
    def convert(post: Post): Json =
      JObj(
        List(
          "title" -> JsonConverter[String].convert(post.title),
          "likes" -> JsonConverter[Int].convert(post.likes),
          "tag" -> JsonConverter[Option[String]].convert(post.tag)
        )
      )
  }

  given feedToJson: JsonConverter[Feed] with {
    def convert(feed: Feed): Json =
      JObj(
        List(
          "owner" -> JsonConverter[User].convert(feed.owner),
          "posts" -> JsonConverter[List[Post]].convert(feed.posts)
        )
      )
  }

  // Things worth stopping on in the walkthrough:
  //
  // 1. Nobody wrote `JsonConverter[List[Post]]`. The compiler built it, from
  //    `listToJson` plus `postToJson`, at the point of use. That assembly is the
  //    entire idea - and it is why this scales to types you did not anticipate.
  //
  // 2. `JsonConverter[Option[String]]` inside `postToJson` is the same trick one
  //    level down. Delete `optionToJson` and watch the error message: it names
  //    the exact instance it could not find, which is the nicest thing about
  //    this style when it goes wrong.
  //
  // 3. `value.fold(JNull)(inner.convert)` needs the JNull first - fold on Option
  //    takes the empty case first and the function second, which is the opposite
  //    order from how people say it out loud.
  //
  // 4. In real code these live in the COMPANION OBJECT of the type they convert
  //    (`object User { given ... }`), because that is the first place the
  //    compiler looks and it means no imports at the call site. They are
  //    abstract members of the trait here only so the tests can reach yours.
  //
  // And the honest caveat, worth saying because somebody will ask: writing one
  // of these per case class by hand gets old fast. Real libraries derive them
  // automatically, and Scala 3 can too. That is a different training.
}
