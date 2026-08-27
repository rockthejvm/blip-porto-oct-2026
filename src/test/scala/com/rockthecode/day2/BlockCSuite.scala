package com.rockthecode.day2

import com.rockthecode.day2.solutions.BlockCSolutions

/**
 * Day 2, Block C - the tests.
 *
 *   sbt blockC                                                 <- attendees
 *   sbt "testOnly com.rockthecode.day2.BlockCSolutionsSuite"   <- trainer
 */
abstract class BlockCSuite(impl: BlockC) extends munit.FunSuite {

  import Shape.*
  import OrderState.*
  import OrderEvent.*
  import TransitionResult.*
  import Refusal.*
  import Json.*
  import Command.*
  import Expr.*

  // --- C.1 -------------------------------------------------------------------

  test("C.1 area") {
    assertEqualsDouble(impl.area(Circle(2)), 12.566370614, 0.000001)
    assertEqualsDouble(impl.area(Rectangle(3, 4)), 12.0, 0.000001)
    assertEqualsDouble(impl.area(Triangle(3, 4)), 6.0, 0.000001)
  }

  test("C.1 scale keeps the kind of shape") {
    assertEquals(impl.scale(Circle(2), 3), Circle(6))
    assertEquals(impl.scale(Rectangle(3, 4), 2), Rectangle(6, 8))
    assertEquals(impl.scale(Triangle(3, 4), 0.5), Triangle(1.5, 2.0))
  }

  test("C.1 scaling by two makes the area four times bigger, whatever the shape") {
    List(Circle(2), Rectangle(3, 4), Triangle(3, 4)).foreach { shape =>
      assertEqualsDouble(impl.area(impl.scale(shape, 2)), impl.area(shape) * 4, 0.000001, s"failed for $shape")
    }
  }

  // --- C.2 -------------------------------------------------------------------

  test("C.2 the happy path, all the way to Delivered") {
    assertEquals(impl.transition(Draft, Place), Moved(Placed))
    assertEquals(impl.transition(Placed, Pay), Moved(Paid))
    assertEquals(impl.transition(Paid, Ship), Moved(Shipped))
    assertEquals(impl.transition(Shipped, Deliver), Moved(Delivered))
  }

  test("C.2 cancelling is allowed early and not late") {
    assertEquals(impl.transition(Draft, Cancel), Moved(Cancelled))
    assertEquals(impl.transition(Placed, Cancel), Moved(Cancelled))
    assertEquals(impl.transition(Paid, Cancel), Moved(Cancelled))
    assertEquals(impl.transition(Shipped, Cancel), Rejected(Impossible(Shipped, Cancel)))
  }

  test("C.2 a final state refuses everything, and says which kind of no it is") {
    OrderEvent.values.foreach { event =>
      assertEquals(impl.transition(Delivered, event), Rejected(AlreadyFinished(Delivered)), s"on $event")
      assertEquals(impl.transition(Cancelled, event), Rejected(AlreadyFinished(Cancelled)), s"on $event")
    }
  }

  test("C.2 you cannot ship what nobody paid for") {
    assertEquals(impl.transition(Draft, Ship), Rejected(Impossible(Draft, Ship)))
    assertEquals(impl.transition(Placed, Deliver), Rejected(Impossible(Placed, Deliver)))
    assertEquals(impl.transition(Draft, Pay), Rejected(Impossible(Draft, Pay)))
  }

  test("C.2 exactly seven of the thirty combinations are legal") {
    val legal =
      for {
        state <- OrderState.values.toList
        event <- OrderEvent.values.toList
        if impl.transition(state, event).isInstanceOf[Moved]
      } yield (state, event)

    assertEquals(legal.size, 7, "four steps forward plus three ways to cancel")
  }

  test("C.2 isFinal") {
    assertEquals(OrderState.values.filter(impl.isFinal).toSet, Set(Delivered, Cancelled))
  }

  // --- C.3 -------------------------------------------------------------------

  private val document =
    JObj(
      List(
        "name" -> JStr("Ana"),
        "active" -> JBool(true),
        "score" -> JNum(BigDecimal("1.5")),
        "nickname" -> JNull,
        "tags" -> JArr(List(JStr("trading"), JStr("porto"))),
        "address" -> JObj(List("city" -> JStr("Porto"), "postcode" -> JStr("4000")))
      )
    )

  test("C.3 render the scalars") {
    assertEquals(impl.render(JNull), "null")
    assertEquals(impl.render(JBool(true)), "true")
    assertEquals(impl.render(JBool(false)), "false")
    assertEquals(impl.render(JNum(BigDecimal(1))), "1")
    assertEquals(impl.render(JNum(BigDecimal("1.5"))), "1.5")
    assertEquals(impl.render(JStr("hi")), "\"hi\"")
  }

  test("C.3 render the containers, including the empty ones") {
    assertEquals(impl.render(JArr(Nil)), "[]")
    assertEquals(impl.render(JObj(Nil)), "{}")
    assertEquals(impl.render(JArr(List(JNum(BigDecimal(1)), JNull))), "[1,null]")
    assertEquals(
      impl.render(JObj(List("a" -> JNum(BigDecimal(1)), "b" -> JArr(Nil)))),
      """{"a":1,"b":[]}"""
    )
  }

  test("C.3 render escapes backslashes before quotes, not after") {
    // Get the order wrong and the backslash inserted in front of the quote gets
    // escaped a second time.
    assertEquals(impl.render(JStr("say \"hi\"")), """"say \"hi\""""")
    assertEquals(impl.render(JStr("c:\\tmp")), """"c:\\tmp"""")
    assertEquals(impl.render(JStr("\\\"")), """"\\\""""")
  }

  test("C.3 render a whole document, field order preserved") {
    assertEquals(
      impl.render(document),
      """{"name":"Ana","active":true,"score":1.5,"nickname":null,""" +
        """"tags":["trading","porto"],"address":{"city":"Porto","postcode":"4000"}}"""
    )
  }

  test("C.3 render escapes field names too") {
    assertEquals(impl.render(JObj(List("a\"b" -> JNull))), """{"a\"b":null}""")
  }

  // --- C.4 -------------------------------------------------------------------

  test("C.4 the commands that exist") {
    assertEquals(impl.interpret(Nil), Help)
    assertEquals(impl.interpret(List("help")), Help)
    assertEquals(impl.interpret(List("list")), ListAll)
    assertEquals(impl.interpret(List("add", "milk")), AddItem("milk"))
    assertEquals(impl.interpret(List("remove", "milk")), RemoveItem("milk"))
  }

  test("C.4 the right verb with the wrong number of arguments is not that verb") {
    assertEquals(impl.interpret(List("add")), Unknown(List("add")))
    assertEquals(impl.interpret(List("add", "milk", "bread")), Unknown(List("add", "milk", "bread")))
    assertEquals(impl.interpret(List("list", "everything")), Unknown(List("list", "everything")))
  }

  test("C.4 anything else comes back unchanged") {
    assertEquals(impl.interpret(List("dance")), Unknown(List("dance")))
    assertEquals(impl.interpret(List("")), Unknown(List("")))
  }

  // --- C.5 -------------------------------------------------------------------

  private def lit(n: Int): Expr = Lit(BigDecimal(n))

  test("C.5 eval") {
    assertEquals(impl.eval(lit(42)), Some(BigDecimal(42)))
    assertEquals(impl.eval(Add(lit(2), lit(3))), Some(BigDecimal(5)))
    assertEquals(impl.eval(Mul(lit(2), lit(3))), Some(BigDecimal(6)))
    assertEquals(impl.eval(Div(lit(6), lit(3))), Some(BigDecimal(2)))
    assertEquals(impl.eval(Neg(lit(2))), Some(BigDecimal(-2)))
    assertEquals(impl.eval(Add(Mul(lit(2), lit(3)), Neg(lit(1)))), Some(BigDecimal(5)))
  }

  test("C.5 division by zero has no answer") {
    assertEquals(impl.eval(Div(lit(1), lit(0))), None)
    assertEquals(impl.eval(Add(lit(1), Div(lit(1), lit(0)))), None, "anywhere in the tree")
    assertEquals(
      impl.eval(Mul(lit(0), Div(lit(1), lit(0)))),
      None,
      "no short-circuiting: the answer is not zero"
    )
  }

  // ===========================================================================
  //
  //  STRETCH SECTION
  //
  // ===========================================================================

  // --- SC1 -------------------------------------------------------------------

  test("SC1 at follows a path down") {
    assertEquals(impl.at(document, List("name")), Some(JStr("Ana")))
    assertEquals(impl.at(document, List("address", "city")), Some(JStr("Porto")))
  }

  test("SC1 an empty path has already arrived") {
    assertEquals(impl.at(document, Nil), Some(document))
    assertEquals(impl.at(JNull, Nil), Some(JNull))
  }

  test("SC1 a missing step, or a step into something that is not an object") {
    assertEquals(impl.at(document, List("nope")), None)
    assertEquals(impl.at(document, List("address", "country")), None)
    assertEquals(impl.at(document, List("name", "first")), None, "name is a string, not an object")
    assertEquals(impl.at(document, List("tags", "0")), None, "arrays are not indexed by this")
  }

  // --- SC2 -------------------------------------------------------------------

  test("SC2 collectStrings finds them all, in order, and ignores field names") {
    assertEquals(
      impl.collectStrings(document),
      List("Ana", "trading", "porto", "Porto", "4000")
    )
  }

  test("SC2 collectStrings on things with no strings in them") {
    assertEquals(impl.collectStrings(JNull), Nil)
    assertEquals(impl.collectStrings(JNum(BigDecimal(1))), Nil)
    assertEquals(impl.collectStrings(JArr(Nil)), Nil)
    assertEquals(impl.collectStrings(JStr("just me")), List("just me"))
  }

  // --- SC3 -------------------------------------------------------------------

  test("SC3 depth") {
    assertEquals(impl.depth(JNum(BigDecimal(1))), 1)
    assertEquals(impl.depth(JArr(List(JNum(BigDecimal(1))))), 2)
    assertEquals(impl.depth(JArr(List(JArr(List(JNum(BigDecimal(1))))))), 3)
    assertEquals(impl.depth(document), 3, "object, then address object, then its strings")
  }

  test("SC3 an empty container is not deeper than a scalar") {
    assertEquals(impl.depth(JArr(Nil)), 1)
    assertEquals(impl.depth(JObj(Nil)), 1)
    assertEquals(impl.depth(JArr(List(JArr(Nil)))), 2)
  }

  // --- SC4 -------------------------------------------------------------------

  test("SC4 simplify applies the identity rules") {
    val x = Lit(BigDecimal(7))
    assertEquals(impl.simplify(Add(x, lit(0))), x)
    assertEquals(impl.simplify(Add(lit(0), x)), x)
    assertEquals(impl.simplify(Mul(x, lit(1))), x)
    assertEquals(impl.simplify(Mul(lit(1), x)), x)
    assertEquals(impl.simplify(Mul(x, lit(0))), lit(0))
    assertEquals(impl.simplify(Mul(lit(0), x)), lit(0))
    assertEquals(impl.simplify(Div(x, lit(1))), x)
    assertEquals(impl.simplify(Neg(Neg(x))), x)
  }

  test("SC4 simplify works bottom-up, so one pass is enough") {
    val x = Lit(BigDecimal(7))
    // (x * 1) + 0  ->  x
    assertEquals(impl.simplify(Add(Mul(x, lit(1)), lit(0))), x)
    // -(-(x + 0))  ->  x
    assertEquals(impl.simplify(Neg(Neg(Add(x, lit(0))))), x)
    // ((x / 1) * 1) + (0 * y)  ->  x + 0  ->  x
    assertEquals(impl.simplify(Add(Mul(Div(x, lit(1)), lit(1)), Mul(lit(0), lit(9)))), x)
  }

  test("SC4 simplify leaves alone what it should") {
    assertEquals(impl.simplify(Add(lit(2), lit(3))), Add(lit(2), lit(3)), "no constant folding")
    assertEquals(impl.simplify(Div(lit(1), lit(0))), Div(lit(1), lit(0)), "and no opinions about this")
    assertEquals(impl.simplify(Neg(lit(2))), Neg(lit(2)))
    assertEquals(impl.simplify(Neg(Neg(Neg(lit(2))))), Neg(lit(2)))
  }

  test("SC4 simplify does not change what an expression means") {
    val expressions = List(
      Add(Mul(lit(3), lit(1)), lit(0)),
      Div(Add(lit(8), lit(0)), lit(1)),
      Neg(Neg(Mul(lit(2), lit(3))))
    )
    expressions.foreach(expr => assertEquals(impl.eval(impl.simplify(expr)), impl.eval(expr), s"changed $expr"))
  }

  // --- SC5 -------------------------------------------------------------------

  test("SC5 reachableStates includes where you started") {
    assertEquals(impl.reachableStates(Delivered), Set(Delivered))
    assertEquals(impl.reachableStates(Cancelled), Set(Cancelled))
  }

  test("SC5 reachableStates walks the whole graph") {
    assertEquals(impl.reachableStates(Shipped), Set(Shipped, Delivered))
    assertEquals(impl.reachableStates(Paid), Set(Paid, Shipped, Delivered, Cancelled))
    assertEquals(impl.reachableStates(Placed), Set(Placed, Paid, Shipped, Delivered, Cancelled))
    assertEquals(impl.reachableStates(Draft), OrderState.values.toSet, "everything, from the beginning")
  }

  // --- SC6 -------------------------------------------------------------------

  private val ana = User("Ana", 30, "ana@blip.pt")
  private val feed =
    Feed(
      owner = ana,
      posts = List(
        Post("First", 12, Some("intro")),
        Post("Second", 0, None)
      )
    )

  test("SC6 the simple instances") {
    import impl.given
    assertEquals(JsonConverter[String].convert("hi"), Json.JStr("hi"))
    assertEquals(JsonConverter[Int].convert(42), Json.JNum(BigDecimal(42)))
    assertEquals(JsonConverter[Boolean].convert(true), Json.JBool(true))
  }

  test("SC6 Option becomes null or the value") {
    import impl.given
    assertEquals(JsonConverter[Option[String]].convert(None), Json.JNull)
    assertEquals(JsonConverter[Option[String]].convert(Some("x")), Json.JStr("x"))
    assertEquals(JsonConverter[Option[Int]].convert(Some(1)), Json.JNum(BigDecimal(1)))
  }

  test("SC6 List becomes an array, in order") {
    import impl.given
    assertEquals(impl.render(JsonConverter[List[Int]].convert(List(1, 2, 3))), "[1,2,3]")
    assertEquals(impl.render(JsonConverter[List[String]].convert(Nil)), "[]")
  }

  test("SC6 a case class becomes an object, fields in declaration order") {
    import impl.given
    assertEquals(
      impl.render(JsonConverter[User].convert(ana)),
      """{"name":"Ana","age":30,"email":"ana@blip.pt"}"""
    )
    assertEquals(
      impl.render(JsonConverter[Post].convert(Post("Second", 0, None))),
      """{"title":"Second","likes":0,"tag":null}"""
    )
  }

  test("SC6 the compiler assembles JsonConverter[List[Post]] on its own") {
    import impl.given
    // Nobody wrote an instance for List[Post]. It is built from listToJson and
    // postToJson, at the point of use. That assembly is the whole idea.
    assertEquals(
      impl.render(JsonConverter[Feed].convert(feed)),
      """{"owner":{"name":"Ana","age":30,"email":"ana@blip.pt"},""" +
        """"posts":[{"title":"First","likes":12,"tag":"intro"},""" +
        """{"title":"Second","likes":0,"tag":null}]}"""
    )
  }

  test("SC6 instances compose to arbitrary depth") {
    import impl.given
    assertEquals(
      impl.render(JsonConverter[List[Option[List[Int]]]].convert(List(Some(List(1)), None))),
      "[[1],null]"
    )
  }

}

/** Red until the attendees make it green. That is the point. */
class BlockCExercisesSuite extends BlockCSuite(BlockCExercises)

/** Green, always. If this one goes red, the reference solutions are wrong. */
class BlockCSolutionsSuite extends BlockCSuite(BlockCSolutions)
