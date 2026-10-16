package com.rockthecode.day1

import scala.annotation.tailrec

object ScalaEssentials {

  // instructions vs expressions?
  // instructions are EXECUTED, expressions are EVALUATED

  // we think in EXPRESSIONS
  val anIfExpression = if (2 > 3) "bigger" else "smaller" // is a value
  // we avoid variables
  // we work with values

  // what is Unit? "void" in other languages e.g. Java == "no meaningful value"
  // careful with Unit functions, fundamentally Java-like

  // stack and tail recursion?

  /*
    f(3) = 3 * f(2) -> stack = [f(2)]
    f(2) = 2 * f(1) -> stack = [f(1), f(2)]
    f(1) = 1 * f(0) -> stack = [f(0), f(1), f(2)]
    f(0) = 1
    pop stack f(1) = 1 stack [f(1), f(2)]
    pop f(2) = 2 stack [f(2)]
    pop f(3) = 3 * 2 = 6
   */
  // stack recursion
  def factorial(n: Int): Int =
    if (n <= 0) 1
    else n * factorial(n - 1)

  def sum(n: Int): Int =
    if (n <= 0) 1
    else n + sum(n - 1) // +(n, sum(n-1))

  // tail recursion
  // if your return value IS a recursive call, the bytecode will NOT put the call on the stack
  // will replace the stack frame
  /*
    s(3) = s(n = 2, acc = 3) = s(n = 1, acc = 5) = s(n = 0, acc = 1 + 5) = 6
   */
  // tail recursive
  @tailrec // not mandatory, compiler refuses non-tailrec functions
  def sumTailrec(n: Int, acc: Int): Int =
    if (n <= 0) acc
    else sumTailrec(n - 1, n + acc)

  /*
    Exercise:
      - isPrime function, stack then tail
      - concatenate a String n times, stack then tail
   */
  def isPrime(n: Int): Boolean = {
    // for all numbers between i and n/2, n % i != 0
    @tailrec
    def testDivisors(i: Int): Boolean =
      if (n % i == 0) false
      else if (i >= n / 2) true
      else testDivisors(i + 1)

    if (n <= 2) true
    else testDivisors(2)
  }

  // concatN("Scala", 3) = "ScalaScalaScala"
  def concatN(str: String, n: Int): String =
    if (n <= 0) ""
    else str + concatN(str, n - 1)

  // tailrec?
  @tailrec
  def concatTailrec(str: String, n: Int, acc: String): String =
    if (n <= 0) acc
    else concatTailrec(str, n - 1, acc + str)

  // make this tailrec
  def fibonacci(n: Int): Int =
    if (n <= 2) 1
    else fibonacci(n - 1) + fibonacci(n - 2)

  // f(1, 1, 0) = 1
  // f(2, 1, 0) = 2
  def fiboTailrec(n: Int, acc1: Int, acc2: Int): Int =
    if (n <= 0) acc1
    else fiboTailrec(n - 1, acc1 + acc2, acc1)

  /*
    OOP in Scala

    is ?+-? a valid method name in Scala
    what are infix methods?
    left-associative methods = infix position is ARG method obj
      method name ends with :

    3 +: List(...) = List(..).+:(3)

    what does apply() do?
    objects?
   */

  class Person(val name: String) {
    def ??!(movie: String) = println(s"$name likes $movie")
    def -->:(movie: String) = println(s"in soviet russia, $movie likes $name")

    def apply(movie: String) = println(s"$name likes $movie")
  }

  val daniel = new Person("Daniel")

  // all operators, +-/% are methods

  object MyObject { // singleton
    // values, methods, etc.
  }

  // object with the same name as a class(or /trait/enum) = companion
  /*
    properties
    - access private things
    - object used for "static" vals/methods
    - can be used as factories
   */

  object Person {
    val N_HANDS = 2
    def apply(logLine: String) =
      // parsing logic
      new Person(logLine)
  }

  val daniel2 = Person("[INFO] ... Daniel")

  // case classes = records (Java), data class (Kotlin)
  /*
    - immutable
    - convenience .toString, .hashCode, .equals
    - serializable
    - .copy
    - built-in apply methods
    - deconstruction with PATTERN MATCHING
   */

  // traits, inheritance?
  // Scala allows SINGLE-CLASS inheritance, MULTIPLE TRAIT inheritance
  // class Thing extends Trait
  // anonymous classes?

  trait Animal {
    def eat(): String
  }

  class OneTimeThing extends Animal {
    def eat() = "woof, I'm eating!"
  }

  val thing = new OneTimeThing

  val thing2 = new Animal {
    override def eat(): String = "...."
  }

  /*
    Singly linked list in Scala
    trait MyList
      - head: Int
      - tail: MyList
      - isEmpty: Boolean
      - toString: String
      - add(elem: Int): MyList
      - ++(list: MyList): MyList

    2 subtypes
      - Empty = holds nothing, head = throw new NoSuchElementException
      - NonEmpty(first: Int, rest: MyList)
   */

  trait Predicate[A] {
    def test(value: A): Boolean
  }

  trait Transformer[A, B] {
    def transform(value: A): B
  }

  class IsEven extends Predicate[Int] {
    override def test(value: Int): Boolean = value % 2 == 0
  }

  class Doubler extends Transformer[Int, Int] {
    override def transform(value: Int): Int = value * 2
  }

  class Expander extends Transformer[Int, MyList[Int]] {
    // transform(3) = [3,2,1]
    override def transform(value: Int): MyList[Int] =
      if (value <= 0) new Empty
      else NonEmpty(value, transform(value - 1))
  }

  trait MyList[A] {
    def head: A
    def tail: MyList[A]
    def isEmpty: Boolean
    def toString: String
    def add(elem: A): MyList[A] = new NonEmpty(elem, this)
    def ++(list: MyList[A]): MyList[A]

    // FP trio
    def map[B](transformer: A => B): MyList[B]
    def filter(predicate: A => Boolean): MyList[A]
    def flatMap[B](transformer: A => MyList[B]): MyList[B]

    // functional transformations
    // runs the fn on every element of the list
    def foreach(fn: A => Unit): Unit
    // [1,2,3].zipWith(["a", "b", "c"], (int, string) => repeats the string int times) = ["a", "bb", "ccc"]
    def zipWith[B, C](list: List[B], fn: (A, B) => C): MyList[C]

    // [1,2,3].reverse = [3,2,1]
    def reverse: MyList[A] = reverseAux(Empty[A]())

    // method: take head from this list, put it in the other list, until this becomes empty
    @tailrec
    private def reverseAux(acc: MyList[A]): MyList[A] =
      if (this.isEmpty) acc
      else tail.reverseAux(acc.add(head))

    // FOLD - foldLeft/foldRight
    def foldLeft[B](acc: B, transformer: (B, A) => B): B
    def foldRight[B](acc: B, transformer: (B, A) => B): B
  }

  case class Empty[A]() extends MyList[A] {
    override def head: A = throw new NoSuchElementException
    override def tail: MyList[A] = throw new NoSuchElementException
    override def isEmpty: Boolean = true
    override def toString: String = "[]"
    override def ++(list: MyList[A]): MyList[A] = list

    def map[B](transformer: A => B): MyList[B] = Empty[B]()
    def filter(predicate: A => Boolean): MyList[A] = Empty[A]()
    def flatMap[B](transformer: A => MyList[B]): MyList[B] = Empty[B]()

    override def foreach(fn: A => Unit): Unit = {}
    override def zipWith[B, C](list: List[B], fn: (A, B) => C): MyList[C] =
      if (list.isEmpty) Empty[C]()
      else throw new RuntimeException("zip must have lists of the same size!")

    override def foldLeft[B](acc: B, transformer: (B, A) => B): B = acc
    override def foldRight[B](acc: B, transformer: (B, A) => B): B = acc
  }

  // val mylist = new NonEmpty(....)
  // val first = mylist.head
  case class NonEmpty[A](override val head: A, override val tail: MyList[A]) extends MyList[A] {
    override def isEmpty: Boolean = false

    override def toString: String = {
      def toStringAux(l: MyList[A]): String =
        if (l.tail.isEmpty) l.head.toString
        else l.head.toString + ", " + toStringAux(l.tail) // TODO: stack recursive

      "[" + toStringAux(this) + "]"
    }

    override def ++(list: MyList[A]): MyList[A] =
      NonEmpty(head, tail ++ list) // TODO: stack recursive

    def map[B](transformer: A => B): MyList[B] =
      NonEmpty[B](transformer(head), tail.map(transformer)) // TODO: stack recursive

    def filter(predicate: A => Boolean): MyList[A] =
      if (predicate(head)) NonEmpty(head, tail.filter(predicate)) // TODO: stack recursive
      else tail.filter(predicate)

    def flatMap[B](transformer: A => MyList[B]): MyList[B] =
      transformer(head) ++ tail.flatMap(transformer) // TODO: stack recursive

    override def foreach(fn: A => Unit): Unit = {
      fn(head)
      tail.foreach(fn)
    }

    override def zipWith[B, C](list: List[B], fn: (A, B) => C): MyList[C] =
      if (list.isEmpty) throw new RuntimeException("zip must have lists of the same size!")
      else
        NonEmpty(
          fn(head, list.head), // apply the function on my head and the other head
          tail.zipWith(
            list.tail,
            fn
          ) // then add the rest of the zip = my tail zipped with the other tail and the same fn
        )

    override def foldLeft[B](acc: B, transformer: (B, A) => B): B =
      tail.foldLeft(transformer(acc, head), transformer)

    override def foldRight[B](acc: B, transformer: (B, A) => B): B =
      transformer(tail.foldRight(acc, transformer), head)
  }

  val numbers = NonEmpty(1, NonEmpty(2, NonEmpty(3, Empty()))) // this is a MyList[Int]
  val strings = NonEmpty("Scala", NonEmpty("rocks", Empty()))
  // transformers example
  // val doubled = aList.map(new Doubler) // [2,4,6]
  // val evenNumbers = aList.filter(new IsEven) // [2]
  // val expanded = aList.flatMap(new Expander) // [1] ++ [2,1] ++ [3,2,1] = [1,2,1,3,2,1]

  class Car(val make: String, val model: String) // class parameters
  val ferrari = new Car("Ferrari", "F50")
  val model = ferrari.model

  // functions and function values
  // FP = functions are values
  // functions are instances of Function0, Function1, Function2, .... Function22 which have apply methods
  trait MyFunction[A, B] {
    def apply(value: A): B
  }

  val aDoubler = new MyFunction[Int, Int] {
    override def apply(value: Int): Int = value * 2
  }

  val ten = aDoubler(5)
  val aDoublerFn: Int => Int = (x: Int) => x * 2 // lambda == function value
  // behind the scenes new Function1[Int, Int] { override def apply(x: Int): Int = x * 2 }

  // syntax sugars
  // Function1[A, B] = A => B
  // Function2[A,B, R] = (A, B) => R
  val aDoublerFn_v2 = (x: Int) => x * 2
  val aDoublerFn_v3: Int => Int = x => x * 2
  val aDoublerFn_v4: Int => Int = _ * 2

  // these are all the same thing:
  // [1,2,3].map(_ * 2)
  // [1,2,3].map(x => x * 2)
  // [1,2,3].map((x: Int) => x * 2)
  // [1,2,3].map(new Function1[Int, Int] { override def apply(x:Int)= x * 2 })

  // underscores:
  // [1,2,3].reduce((a,b) => a + b)
  // [1,2,3].reduce(_ + _) every underscore is a DIFFERENT argument

  // combines one element with all elements of the list
  // example: combineOne(2, ["a", "b"], (int, string) => repeat string int times) = ["aa", "bb"]
  def combineOne[A, B, C](a: A, listB: MyList[B], fn: (A, B) => C): MyList[C] =
    listB.map(b => fn(a, b))

  // combinations of all elements between 2 lists
  // example: combine([1,2,3], ["a", "b"], (int, string) => repeat string int times) = ["a", "b", "aa", "bb", "aaa", "bbb"]
  // hints: use flatMap and map
  // [1,2,3].map(a => combineOne) => [["a", "b"], ["aa", "bb"], ["aaa", "bbb"]]
  // so we use FLATMAP!
  def combineAll[A, B, C](listA: MyList[A], listB: MyList[B], fn: (A, B) => C): MyList[C] =
    listA.flatMap(a => combineOne(a, listB, fn))

  def combineAllBad[A, B, C](listA: MyList[A], listB: MyList[B], fn: (A, B) => C): MyList[MyList[C]] =
    listA.map(a => combineOne(a, listB, fn))

  def combineAll_v2[A, B, C](listA: MyList[A], listB: MyList[B], fn: (A, B) => C): MyList[C] =
    listA.flatMap { a =>
      listB.map { b =>
        fn(a, b)
      }
    }

  // for comprehensions
  def combineAll_v3[A, B, C](listA: MyList[A], listB: MyList[B], fn: (A, B) => C): MyList[C] =
    for {
      a <- listA
      b <- listB
    } yield fn(a, b) // same thing
  // this is an EXPRESSION!

  def combineAll3[A, B, C, D](listA: MyList[A], listB: MyList[B], listC: MyList[C], fn: (A, B, C) => D): MyList[D] =
    listA.flatMap { a =>
      listB.flatMap { b =>
        listC.map { c =>
          fn(a, b, c)
        }
      }
    }

  def combineAll3_v2[A, B, C, D](listA: MyList[A], listB: MyList[B], listC: MyList[C], fn: (A, B, C) => D): MyList[D] =
    for {
      a <- listA
      b <- listB
      c <- listC
    } yield fn(a, b, c)

  // partial functions
  def describe(x: Int) = x match {
    case 1 => "the one"
    case 2 => "second"
    case 42 => "meaning of life"
  }

  val pf: PartialFunction[Int, String] = { // same as (x: Int) => x match ...
    case 1 => "the one"
    case 2 => "second"
    case 42 => "meaning of life"
  }

  val stupidTransformation = List(1, 2, 3).map((x: Int) =>
    x match {
      case 1 => "the one"
      case 2 => "second"
      case 42 => "meaning of life"
      case _ => "error"
    }
  )

  // easier to use a syntax sugar
  // SAME THING
  val stupidTransformation_v2 = List(1, 2, 3).map {
    case 1 => "the one"
    case 2 => "second"
    case 42 => "meaning of life"
    case _ => "error"
  }

  /*
    Call by value
    Call by name
   */

  def add(a: Int, b: Int, c: Int) = a + b
  val seven = add(
    2 + 3,
    1 + 1, {
      println("[call by value] I need to be evaluated")
      0
    }
  ) // values are computed FIRST, eagerly

  def addBN(a: => Int, b: => Int, c: => Int) = a + b
  val seven_v2 = addBN(
    2 + 3,
    1 + 1, {
      println("[call by name] I need to be evaluated")
      0
    }
  ) // params are NOT evaluated UNTIL used, BUT will be evaluated EVERY TIME it's used

  // CBN useful for expressions that produce side effects
  // e.g. logging, printing, changing state, dealing with time/randomness, allocating resources

  // lazy evaluation
  lazy val computeMOL = {
    println("figuring out what to do with my foldLeft time")
    40 + 2 // NOT evaluated....
  }

  // CBN evaluates every time it's used
  // lazy val evaluates ONCE

  // syntax sugar: single abstract method pattern (SAM)
  trait Task[A] {
    def execute(thing: Int): A
  }

  val task = new Task[String] {
    override def execute(thing: Int): String = thing.toString
  }

  val task_v2: Task[String] = _.toString

  def main(args: Array[String]): Unit = {
    println(sumTailrec(1000000, 0))
    println(isPrime(2))
    println(isPrime(3))
    println(isPrime(13))
    println(isPrime(2003))
    println(concatN("Scala", 3))
    println(concatTailrec("Scala", 3, ""))
    (1 to 10).foreach(i => println(fiboTailrec(i, 1, 0)))
    daniel.??!("Forrest Gump")
    daniel ??! "Forrest Gump" // infix notation
    "Forrest Gump" -->: daniel // "reverse" infix notation

    daniel.apply("Forrest Gump")
    daniel("Forrest Gump") // same as .apply

    println(numbers)
    println(numbers.map(x => x * 2))
    println(numbers.filter(x => x % 2 == 0))
    println(numbers.flatMap(x => new Expander().transform(x)))
    println(numbers.reverse)

    println(combineAllBad(numbers, strings, (int, string) => concatN(string, int)))
    println(combineAll(numbers, strings, (int, string) => concatN(string, int)))

    // ... until lazy val is used
    val doSomethingWithYourLife = computeMOL * 10
    // and then it will stay evaluated
    val doSomethingWithYour2ndLife = computeMOL * 100
  }

}
