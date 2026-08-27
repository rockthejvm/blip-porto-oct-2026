package com.rockthecode.day2

import com.rockthecode.day2.solutions.BlockBSolutions

/**
 * Day 2, Block B - the tests.
 *
 *   sbt blockB                                                 <- attendees
 *   sbt "testOnly com.rockthecode.day2.BlockBSolutionsSuite"   <- trainer
 */
abstract class BlockBSuite(impl: BlockB) extends munit.FunSuite {

  private val small = List(3, 1, 4, 1, 5, 9, 2, 6)

  /** Big enough that a hand-rolled recursion dies (it goes around 5,000). */
  private val big = (1 to 20000).toList

  // --- B.1 -------------------------------------------------------------------

  test("B.1 myLength counts") {
    assertEquals(impl.myLength(small), 8)
    assertEquals(impl.myLength(Nil), 0)
    assertEquals(impl.myLength(List("only one")), 1)
  }

  test("B.1 myReverse reverses") {
    assertEquals(impl.myReverse(List(1, 2, 3)), List(3, 2, 1))
    assertEquals(impl.myReverse(Nil), Nil)
    assertEquals(impl.myReverse(impl.myReverse(small)), small, "twice is the identity")
  }

  test("B.1 myMap maps, and keeps the order") {
    assertEquals(impl.myMap(List(1, 2, 3))(_ * 10), List(10, 20, 30))
    assertEquals(impl.myMap(List(1, 2, 3))(_.toString), List("1", "2", "3"), "and may change the type")
    assertEquals(impl.myMap(List.empty[Int])(_ * 10), Nil)
  }

  test("B.1 myFilter filters, and keeps the order") {
    assertEquals(impl.myFilter(small)(_ % 2 == 0), List(4, 2, 6))
    assertEquals(impl.myFilter(small)(_ => false), Nil)
    assertEquals(impl.myFilter(small)(_ => true), small)
  }

  test("B.1 all four survive twenty thousand elements") {
    assertEquals(impl.myLength(big), 20000)
    assertEquals(impl.myReverse(big).head, 20000)
    assertEquals(impl.myMap(big)(_ + 1).head, 2)
    assertEquals(impl.myFilter(big)(_ % 2 == 0).head, 2)
  }

  // --- B.2 -------------------------------------------------------------------

  test("B.2 safeSum has an answer for the empty list, unlike reduce") {
    assertEquals(impl.safeSum(List(1, 2, 3)), 6)
    assertEquals(impl.safeSum(Nil), 0)
  }

  test("B.2 safeMax admits that the empty list has no maximum") {
    assertEquals(impl.safeMax(small), Some(9))
    assertEquals(impl.safeMax(Nil), None)
    assertEquals(impl.safeMax(List(-4)), Some(-4))
  }

  test("B.2 myFoldRight folds from the right") {
    assertEquals(impl.myFoldRight(List(1, 2, 3), 0)(_ + _), 6)
    // subtraction is not associative, so this pins the DIRECTION:
    // 1 - (2 - (3 - 0)) == 2, whereas folding left would give -6
    assertEquals(impl.myFoldRight(List(1, 2, 3), 0)(_ - _), 2)
    assertEquals(impl.myFoldRight(List("a", "b", "c"), "")(_ + _), "abc")
    assertEquals(impl.myFoldRight(List.empty[Int], 42)(_ + _), 42)
  }

  test("B.2 myFoldRight survives two hundred thousand elements") {
    val huge = (1 to 200000).toList
    assertEquals(impl.myFoldRight(huge, 0L)((elem, acc) => elem + acc), 20000100000L)
  }

  // --- B.3 -------------------------------------------------------------------

  test("B.3 encode collapses runs") {
    assertEquals(
      impl.encode(List('a', 'a', 'a', 'b', 'c', 'c')),
      List(('a', 3), ('b', 1), ('c', 2))
    )
  }

  test("B.3 encode handles the edges") {
    assertEquals(impl.encode(List.empty[Char]), Nil)
    assertEquals(impl.encode(List('x')), List(('x', 1)))
    assertEquals(impl.encode(List(1, 1, 1, 1)), List((1, 4)), "one single run")
    assertEquals(impl.encode(List(1, 2, 3)), List((1, 1), (2, 1), (3, 1)), "no runs at all")
  }

  test("B.3 non-adjacent equals are separate runs") {
    assertEquals(impl.encode(List('a', 'b', 'a')), List(('a', 1), ('b', 1), ('a', 1)))
  }

  test("B.3 decode expands runs") {
    assertEquals(impl.decode(List(('a', 3), ('b', 1))), List('a', 'a', 'a', 'b'))
    assertEquals(impl.decode(List.empty[(Char, Int)]), Nil)
  }

  test("B.3 decode undoes encode, whatever you throw at it") {
    val cases = List(
      List.empty[Char],
      List('x'),
      "aaabccddddde".toList,
      "abcdef".toList,
      List.fill(500)('z')
    )
    cases.foreach(input => assertEquals(impl.decode(impl.encode(input)), input, s"round trip failed for $input"))
  }

  // --- B.4 -------------------------------------------------------------------

  test("B.4 longestIncreasingRun finds the run in the middle") {
    assertEquals(impl.longestIncreasingRun(List(1, 2, 1, 2, 3, 1)), List(1, 2, 3))
  }

  test("B.4 a run that is still going when the list ends still counts") {
    // The classic bug: the fold only compares when a run ENDS, and this run
    // never ends, so it never gets compared.
    assertEquals(impl.longestIncreasingRun(List(9, 1, 2, 3, 4)), List(1, 2, 3, 4))
    assertEquals(impl.longestIncreasingRun(List(1, 2, 3)), List(1, 2, 3), "the whole list is the run")
  }

  test("B.4 strictly increasing - equal neighbours break the run") {
    // [1, 2] then [2, 3] - both length two, so the earlier one wins
    assertEquals(impl.longestIncreasingRun(List(1, 2, 2, 3)), List(1, 2))
    assertEquals(impl.longestIncreasingRun(List(1, 1, 1)), List(1))
  }

  test("B.4 ties go to the earliest run") {
    assertEquals(impl.longestIncreasingRun(List(1, 2, 0, 5)), List(1, 2), "[1, 2] and [0, 5] tie")
    assertEquals(impl.longestIncreasingRun(List(1, 2, 0, 5, 9)), List(0, 5, 9), "...but this one is not a tie")
  }

  test("B.4 a descending list is a series of runs of one") {
    assertEquals(impl.longestIncreasingRun(List(5, 4, 3)), List(5))
  }

  test("B.4 the edges") {
    assertEquals(impl.longestIncreasingRun(Nil), Nil)
    assertEquals(impl.longestIncreasingRun(List(7)), List(7))
  }

  test("B.4 does not grind to a halt on an already sorted list of 20k") {
    // Measuring "is this run longer" with .length inside the fold makes this
    // quadratic, and this test is where you find that out.
    assertEquals(impl.longestIncreasingRun(big).size, 20000)
  }

  // --- B.5 -------------------------------------------------------------------

  private val text =
    "the quick brown fox jumps over the lazy dog. The dog barks; the fox runs. A dog!"

  test("B.5 topWords counts words, ignoring case and punctuation") {
    assertEquals(impl.topWords(text, 3), List(("the", 4), ("dog", 3), ("fox", 2)))
  }

  test("B.5 ties are broken alphabetically, so the answer is reproducible") {
    // every word appears once, so the order is purely the tie-break
    assertEquals(
      impl.topWords("delta alpha charlie bravo", 4),
      List(("alpha", 1), ("bravo", 1), ("charlie", 1), ("delta", 1))
    )
  }

  test("B.5 asking for more words than exist is not an error") {
    assertEquals(impl.topWords("one two", 10), List(("one", 1), ("two", 1)))
    assertEquals(impl.topWords(text, 0), Nil)
    assertEquals(impl.topWords("", 5), Nil)
    assertEquals(impl.topWords("   ...   ", 5), Nil)
  }

  test("B.5 Portuguese words are words") {
    assertEquals(
      impl.topWords("Coração, coração! Não não NÃO", 2),
      List(("não", 3), ("coração", 2))
    )
  }

  // ===========================================================================
  //
  //  STRETCH SECTION
  //
  // ===========================================================================

  // --- SB1 -------------------------------------------------------------------

  test("SB1 foldLeftViaFoldRight agrees with the real foldLeft") {
    assertEquals(impl.foldLeftViaFoldRight(List(1, 2, 3), 0)(_ + _), 6)
    // subtraction again, because it is the only way to prove the DIRECTION:
    // (((10 - 1) - 2) - 3) == 4
    assertEquals(impl.foldLeftViaFoldRight(List(1, 2, 3), 10)(_ - _), 4)
  }

  test("SB1 foldLeftViaFoldRight can change the type, and build in order") {
    assertEquals(impl.foldLeftViaFoldRight(List(1, 2, 3), "")((acc, n) => acc + n), "123")
    assertEquals(impl.foldLeftViaFoldRight(List.empty[Int], 42)(_ + _), 42)
  }

  test("SB1 foldLeftViaFoldRight matches foldLeft on a whole lot of inputs") {
    val random = new scala.util.Random(20261026)
    (1 to 200).foreach { _ =>
      val xs = List.fill(random.nextInt(30))(random.nextInt(100))
      assertEquals(impl.foldLeftViaFoldRight(xs, 7)(_ - _), xs.foldLeft(7)(_ - _), s"disagreed on $xs")
    }
  }

  // --- SB2 / SB3 -------------------------------------------------------------

  test("SB2 myGroupBy groups, keeping the original order inside each group") {
    assertEquals(
      impl.myGroupBy(List("ant", "bee", "asp", "bat", "cow"))(_.head),
      Map('a' -> List("ant", "asp"), 'b' -> List("bee", "bat"), 'c' -> List("cow"))
    )
    assertEquals(impl.myGroupBy(List.empty[String])(_.head), Map.empty[Char, List[String]])
  }

  test("SB2 myGroupBy agrees with the real groupBy") {
    assertEquals(impl.myGroupBy(small)(_ % 3), small.groupBy(_ % 3))
  }

  test("SB2 myGroupBy does not grind on 20k in one group") {
    assertEquals(impl.myGroupBy(big)(_ => "all")("all").size, 20000)
  }

  test("SB3 myPartition splits, keeping order in both halves") {
    assertEquals(impl.myPartition(small)(_ % 2 == 0), (List(4, 2, 6), List(3, 1, 1, 5, 9)))
    assertEquals(impl.myPartition(small)(_ => true), (small, Nil))
    assertEquals(impl.myPartition(List.empty[Int])(_ => true), (Nil, Nil))
  }

  test("SB3 myPartition agrees with the real partition") {
    assertEquals(impl.myPartition(small)(_ > 3), small.partition(_ > 3))
  }

  // --- SB4 -------------------------------------------------------------------

  test("SB4 balanced brackets are balanced") {
    assertEquals(impl.isBalanced(""), true)
    assertEquals(impl.isBalanced("()"), true)
    assertEquals(impl.isBalanced("([{}])"), true)
    assertEquals(impl.isBalanced("()[]{}"), true)
    assertEquals(impl.isBalanced("a(b[c]{d})e"), true, "non-brackets are ignored")
    assertEquals(impl.isBalanced("no brackets here at all"), true)
  }

  test("SB4 unbalanced brackets are not") {
    assertEquals(impl.isBalanced("("), false, "opened and never closed")
    assertEquals(impl.isBalanced(")"), false, "closed and never opened")
    assertEquals(impl.isBalanced(")("), false, "right count, wrong order")
    assertEquals(impl.isBalanced("([)]"), false, "interleaved, not nested")
    assertEquals(impl.isBalanced("(]"), false, "wrong closer")
    assertEquals(impl.isBalanced("(()"), false)
  }

  test("SB4 a doomed string stays doomed, however it continues") {
    // once ([)] has gone wrong, nothing later can rescue it
    assertEquals(impl.isBalanced("([)]()()()"), false)
  }

  test("SB4 deep nesting does not overflow") {
    assertEquals(impl.isBalanced("(" * 50000 + ")" * 50000), true)
  }

  // --- SB5 / SB6 -------------------------------------------------------------

  test("SB5 unfold builds a list from a seed") {
    assertEquals(impl.unfold(1)(n => if (n > 5) None else Some((n, n + 1))), List(1, 2, 3, 4, 5))
    assertEquals(impl.unfold(1)(_ => None), Nil, "a step that never starts")
  }

  test("SB5 unfold can change type on the way out") {
    assertEquals(
      impl.unfold(3)(n => if (n <= 0) None else Some((s"item-$n", n - 1))),
      List("item-3", "item-2", "item-1")
    )
  }

  test("SB5 unfold survives a hundred thousand elements") {
    assertEquals(impl.unfold(0)(n => if (n >= 100000) None else Some((n, n + 1))).size, 100000)
  }

  test("SB6 fibonacci") {
    assertEquals(impl.fibonacci(0), Nil)
    assertEquals(impl.fibonacci(1), List(BigInt(0)))
    assertEquals(impl.fibonacci(2), List(BigInt(0), BigInt(1)))
    assertEquals(impl.fibonacci(7), List(0, 1, 1, 2, 3, 5, 8).map(BigInt(_)))
  }

  test("SB6 fibonacci at 200, where a Long would have silently lied to you") {
    val fibs = impl.fibonacci(201)
    assertEquals(fibs.size, 201)
    assertEquals(fibs.last, BigInt("280571172992510140037611932413038677189525"))
  }
}

/** Red until the attendees make it green. That is the point. */
class BlockBExercisesSuite extends BlockBSuite(BlockBExercises)

/** Green, always. If this one goes red, the reference solutions are wrong. */
class BlockBSolutionsSuite extends BlockBSuite(BlockBSolutions)
