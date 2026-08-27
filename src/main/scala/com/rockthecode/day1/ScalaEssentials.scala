package com.rockthecode.day1

import scala.annotation.tailrec

object ScalaEssentials {

  /**
   * Admin stuff
   * - breaks by agreement - I'll give you exercises and you can take breaks in the meantime
   * - recommended: cut off the internal chats/emails unless ACTUALLY urgent
   * - ask any questions at ANY time
   *
   * Intros: 30 seconds with name, position, time at Adobe, favorite project EVER
   *   - start with me
   *   - lay out the room in the notebook!
   */

  /**
   * vals, types
   */

  // defining a value - types are optional
  val helloAdobe: String = "Hello, Adobe" + 2 // the expression is a String: you know string concatenation from Java
  // the type is optional

  /**
   Expressions
   - operators
   - if/else
   - code blocks
   - the Unit type
   */

  val simpleMathExpression: Int = 1 * 30
  val anIfExpression = if (2 > 3) "bigger" else "smaller" // if structures are EXPRESSIONS because they reduce to a value
  val aCodeBlock = { // blocks are expressions whose value is the last expression
    // blocks also act like a scope: you can define values, functions, classes etc.
    val anInnerValue = 2
    anInnerValue + 30
  }

  // the Unit type is for "void" expressions, aka "side effects" = NO MEANINGFUL VALUE
  val theUnit: Unit = println("Unit!")
  val theUnitReference: Unit = () // the only instance of Unit is ()

  /**
   * Functions
   *   - functions inside functions
   *   - (time permitting) ex: isPrime
   */

  def myFunction(myInt: Int, myString: String) = { // function implementations are a single expression: usually code blocks, even if just for code style
    myInt + myString
  }

  // In Scala, we think in terms of recursion instead of "iteration". Example: isPrime.
  def isPrime(n: Int): Boolean = {
    def isPrimeHelper(n: Int, counter: Int): Boolean = { // can define functions inside functions
      if (n/2 <= counter) true
      else if (n % counter == 0) false
      else isPrimeHelper(n, counter + 1)
    }

    isPrimeHelper(n, 2)
  }

  /**
   * stack/tail recursion
   */
  def anotherFactorial(n: Int): BigInt = {
    @tailrec
    def factHelper(x: Int, accumulator: BigInt): BigInt =
      if (x <= 1) accumulator
      else factHelper(x - 1, x * accumulator) // TAIL RECURSION = use recursive call as the LAST expression

    factHelper(n, 1)
  }

  /*
    TODO Exercise:
    1) concatenate a string n times with tail recursion
    2) convert isPrime to be tailrec
   */

  @tailrec
  def concatenateTailrec(aString: String, n: Int, accumulator: String): String =
    if (n <= 0) accumulator
    else concatenateTailrec(aString, n-1, aString + accumulator)

  def isPrimeTailrec(n: Int) = {
    def isPrimeAux(potentialDivisor: Int): Boolean =
      if (n % potentialDivisor == 0) false
      else if (potentialDivisor > n / 2) true
      else isPrimeAux(potentialDivisor + 1)

    if (n == 0 || n == 1 || n == -1) false
    else isPrimeAux(2)
  }

  /**
   * Other basics
   *   - s-interpolators
   *   - default arguments
   */
  val interpolated = s"I got a string: $helloAdobe" // can inject values, variables or expand expressions directly inside a string
  def aFunctionOftenCalledWithSameArgs(x: Int, oftenSameValueArg: Int = 99) = ???

  /**
   * OO basics
   *   - parameters vs fields
   *
   * Method notation
   *   - infix methods on Person
   *   - operators as methods
   *   - apply()
   */

  class Person(/*[1]*/val name: String, /* this is not a field, just a class param */age: Int) {
    val height: Double = 100

    private def canFly/* parameterless methods don't necessarily need parentheses */: Boolean = Person.N_HANDS > 35

    def likes(movie: String) = s"$name likes $movie"

    def ?!!(person: Person) = s"$name: I wonder if I can get along ${person.name}..." // Scala method naming is VERY permissive

    def -->:(gainWeight: Int) = println() // Scala methods with non-alphanumeric characters ending in ':' are LEFT-associative

    def apply(moreWeight: Int) = s"$name gained $moreWeight kg"
  }

  // instantiating
  val daniel = new Person("Daniel", 99)
  // using fields/methods
  daniel.name
  // error if no val

  daniel.likes("Forrest Gump")
  daniel likes "Forrest Gump" // equivalent: infix notation

  daniel ?!! new Person("Gigi", 45) // wacky method names: used in Akka a lot

  // even common operators are methods
  val simpleMath = 1 + 2
  val mathYoudNeverWrite = 1.+(2) // equivalent

  // left-associative methods
  42 -->: daniel

  daniel(2) // == daniel.apply(2)

  /**
   * Objects
   */

  object MySingleton { // singleton pattern in (even) one line
    val aField = 2
    def aMethod = println("Singleton")
  }

  val theSingleton = MySingleton

  object Person { // class + object of the same name in the same file = companions
    private val N_HANDS = 2
    def apply(name: String, age: Int) = new Person(name, age) // the apply factory method pattern
  }

  val newPerson = Person("Mary", 23) // often practiced - same as Person.apply("Mary", 23)

  /**
   * Inheritance
   *   - Animals and Carnivores
   *   - overriding methods and polymorphism
   */

  class Animal {
    def eat() = println("eating")
  }

  trait Carnivore {
    def eat(animal: Animal): Unit
  }

  trait Vegetarian {

  }

  class Crocodile extends Animal with Carnivore { // single class, multi-trait inheritance
    override def eat(animal: Animal): Unit = println("crunch")
  }

  val animal: Animal = new Crocodile // subtype polymorphism
  animal.eat() // the runtime decides which eat() method is called

  // anonymous classes
  val trex = new Carnivore {
    override def eat(animal: Animal): Unit = println("ROOOOOOARRRRRRR!")
  }

  /*
    * TODO Exercise - MyList part 1: create a singly linked list of integers
    *	- needs to have head/tail/isEmpty/add/toString/++
    *	- implemented as Empty/Cons
    */

  /**
   * Case classes: lightweight data structures with little functionality used for data storage and domain modeling.
   */

  case class City(country: String, population: Int)

  // construction without new
  val bucharest = City("Romania", 2000000)
  // all parameters are auto-promoted to fields
  val romania = bucharest.country
  // sensible equals and hashcode
  val anotherBucharest = City("Romania", 2000000)
  val sameBucharest = bucharest == anotherBucharest // by the way, Scala uses == in the same way as Java uses equals
  // plus PATTERN MATCHING (which we'll see later) + some other niceties

  // TODO MyList part 2: use case classes for the MyList implementations

  /**
   * Generics
   *   - bounded types with cars
   *   - variance with cars
   */

  class Car
  class Supercar extends Car

  class Garage[+C]
  class Mechanic[-C]

  val supercarGarage: Garage[Car] = new Garage[Supercar] // I want a garage and I get a specialized garage
  val taxiDriver: Mechanic[Supercar] = new Mechanic[Car] // I want a mechanic of a supercar and I get a mechanic that can repair ANY car

  /*
    Intuition:
    - covariant = contains T
    - contravariant = acts/can act on T
   */

  /* TODO Exercise MyList part 3:
   *   - make the structures generic and covariant
   *   - add map, flatMap and filter with Predicates and Transformers
   */

  /**
   * What's a function, really
   *   - the OO-FP dream
   *   - function instance creation
   *   - functions are actually instances of the FunctionX trait with an apply method
   */

  val anonymousMultiplier = new Function1[Int, Int] {
    override def apply(arg: Int) = arg * 10
  }
  val two = anonymousMultiplier(1)

  // TODO MyList part 4: replace Predicates and Transformers with functions

  /**
   * Anonymous functions
   *   - lambdas demo
   *   - underscore notation
   */

  val anotherAnonymousMultiplier = (x: Int) => 10 * x // equivalent
  // anonymous functions are called "lambdas"
  // [hover over the function to show the function type]
  val anotherAnonymousMultiplierAlt = { (x: Int) =>
    // use the rest of this space as a block of code
    val multiple = 10
    x * multiple
  }

  val yetAnotherAnonymousMultiplier: Int => Int = _ * 10 // once the compiler knows the function type, you can use the shorthand notation
  val aSumFunction: (Int, Int) => Int = _ + _ // (a, b) => a + b, each underscore = one argument

  // TODO MyList part 5: test the list with anonymous functions

  /**
   * HOFs and curries
   *
   * HOF = higher-order function = function that takes other functions as arguments/returns other functions as results
   * `map`, `flatMap`, `filter` are all HOFs.
   */

  /*
    thousandX = nTimes(tenx, 3) =
      (x: Int) => nTimes(tenx, 2)(tenx(x))
      (x: Int) => nTimes(tenx, 1)(tenx(tenx(x))
      (x: Int) => nTimes(tenx, 0)(tenx(tenx(tenx(x)))
      tenx(tenx(tenx(x)))
   */
  def nTimes(f: Int => Int, n: Int): Int => Int = {
    if (n <= 0) (x: Int) => x
    else (x: Int) => nTimes(f, n-1)(f(x))
  }

  val millionx: Int => Int = nTimes(x => x * 10, 6)
  println(millionx(2)) // 2000000

  // TODO MyList part 6: add zipWith and foreach HOFs

  /**
   * map, flatMap, filter, for comprehension
   *   - equivalence between a map chain and a for
   *   - whiteboard test: chessboard
   */

  // TODO MyList part 7: add withFilter and do a for-comprehension on our own list

  // a double-for COMPREHENSION (please don't use the "loop" term)
  val simpleList = List(1, 2, 3)
  val charList = List('a', 'b', 'c', 'd')

  // does a cartesian product
  val chessboard: List[String] = for {
    char <- charList // flatMap
    element <- simpleList if element % 2 == 0 // map with a withFilter
  } yield s"$char$element"

  // equivalent
  val altChessboard = charList.flatMap(char => simpleList.filter(element => element % 2 == 0).map(element => s"$char$element"))

  // same for comprehension on OUR list!
  val simpleMyList = Cons(1, Cons(2, Cons(3, Empty)))
  val simpleCharList = Cons('a', Cons('b', Empty))
  val myChessboard = for {
    char <- simpleCharList
    element <- simpleMyList
  } yield s"$char$element"

  /**
   * Collections
   *   - lists, arrays, sequences, vectors, sets
   *   - tuples and maps
   */

  // lists
  val aList = List(1,2,3,4,5)
  val firstElement = aList.head
  val rest = aList.tail
  val aPrependedList = 0 :: aList // List(0,1,2,3,4,5)
  val anExtendedList = 0 +: aList :+ 6 // List(0,1,2,3,4,5,6)

  // arrays
  val anArray = Array.ofDim[Int](2, 3) // array of arrays
  val accessingAnElement = anArray(1)(2)

  // sequences
  val aSequence: Seq[Int] = Seq(1,2,3) // Seq.apply(1,2,3)
  val accessedElement = aSequence(1) // the element at index 1: 2

  // vectors: fast Seq implementation
  val aVector = Vector(1,2,3,4,5)

  // sets = no duplicates
  val aSet = Set(1,2,3,4,1,2,3) // Set(1,2,3,4)
  val setHas5 = aSet.contains(5) // false
  val anAddedSet = aSet + 5 // Set(1,2,3,4,5)
  val aRemovedSet = aSet - 3 // Set(1,2,4)

  // ranges
  val aRange = 1 to 1000
  val twoByTwo = aRange.map(x => 2 * x).toList // List(2,4,6,8..., 2000)

  // tuples = groups of values under the same value
  val aTuple = ("Bon Jovi", "Rock", 1982)

  // maps
  val aPhonebook: Map[String, Int] = Map(
    ("Daniel", 6437812),
    "Jane" -> 327285 // ("Jane", 327285)
  )

  val phonebookHasDaniel = aPhonebook.contains("Daniel")
  val danielsPhone = aPhonebook("Daniel")
  val contacts = aPhonebook.keySet

  /**
   *
   * Bonus (time permitting) CBN vs CBV
   * - CBV evaluates the argument before it is passed
   * - CBN passes the expression literally, and it's evaluated every time it is used, when it's used
   */

  def calledByValue(x: Long): Unit = {
    println("by value: " + 1257387745764245L)
    println("by value: " + 1257387745764245L)
  }

  def calledByName(x: => Long): Unit = {
    println("by name: " + System.nanoTime())
    println("by name: " + System.nanoTime())
  }

  calledByValue(1257387745764245L)
  calledByName(System.nanoTime())

  /**
   * Laziness
   *   - lazy val
   *   - views: the same laziness, on collections you already have
   *   - filter vs withFilter, and what a for-comprehension really compiles to
   *   - LazyList: lists that do not end
   *
   * Call-by-name above was "do not evaluate this until it is used". Everything
   * here is that same idea, applied to values and to collections.
   *
   * (Day 2 has a whole block of practice on this. Here we just want the shapes
   * to be familiar.)
   */

  // A lazy val is evaluated on FIRST USE, and then remembered.
  lazy val expensiveConfig: Map[String, String] = {
    println("...reading the config file, once")
    Map("host" -> "localhost")
  }

  /*
    The three ways to control WHEN work happens - worth memorising, because
    picking the wrong row is one of the most common bugs in Scala code:

      val        evaluated once, right now
      lazy val   evaluated once, on first use, then remembered
      def / =>   evaluated every single time it is used
   */

  def now: Long = System.nanoTime()       // different every time you ask
  lazy val once: Long = System.nanoTime() // the same forever, computed on first ask

  // ---- views: laziness on an ordinary collection ----------------------------

  val manyNumbers = (1 to 1000).toList

  // eager: builds a 1000-element list, then a filtered list, and keeps 3 of them
  val eagerPipeline = manyNumbers.map(_ * 2).filter(_ > 10).take(3)

  // lazy: nothing runs until `toList` asks, and then only far enough to get 3
  val lazyPipeline = manyNumbers.view.map(_ * 2).filter(_ > 10).take(3).toList

  /*
    The mental model: WITHOUT a view, every stage builds a whole intermediate
    collection and hands it to the next one. WITH a view, nothing is built and
    nothing runs; `toList` pulls one element all the way through the pipeline,
    then the next, and stops pulling as soon as `take` has enough.

    Two things to remember:
      - the `.toList` is NOT optional. A view is a description, not an answer.
      - views do NOT remember. Force the same view twice and everything runs
        twice. (LazyList, below, is the one that remembers.)
   */

  // ---- filter vs withFilter -------------------------------------------------

  val evenThenIncremented = manyNumbers.filter(_ % 2 == 0).map(_ + 1)     // builds the filtered list first
  val evenThenFused = manyNumbers.withFilter(_ % 2 == 0).map(_ + 1)       // no intermediate list

  /*
    `withFilter` is `filter` that does not build anything - it just remembers
    the condition and applies it during the next map/flatMap/foreach.

    This is not trivia: an `if` inside a for-comprehension compiles to
    `withFilter`, not to `filter`. That is exactly why MyList needed a
    `withFilter` method for our own for-comprehension to work.
   */

  val evensSquared = for {
    n <- manyNumbers
    if n % 2 == 0     // <- this becomes withFilter
  } yield n * n

  // ---- LazyList: lists that do not end --------------------------------------

  /*
    A LazyList computes its tail only when asked, and then remembers it. So an
    infinite one is not a trick - you describe the whole thing, and only the
    part you actually look at ever exists.

    (In Scala 2.12 and earlier this was called `Stream`. Same idea, LazyList
    fixed some genuinely nasty details about when the head gets evaluated.)
   */

  val naturalNumbers: LazyList[Int] = LazyList.from(0) // infinite, and that is fine
  val firstFiveNaturals = naturalNumbers.take(5).toList // List(0, 1, 2, 3, 4)
  val theThousandth = naturalNumbers.drop(1000).head    // 1000 - and nothing past it was ever computed

  // `#::` builds a LazyList the way `::` builds a List, except the tail is by-name
  def countFrom(n: Int): LazyList[Int] = n #:: countFrom(n + 1)
  val alsoNaturals = countFrom(0).take(5).toList

  // the classic: fibonacci, defined in terms of itself
  lazy val fibs: LazyList[BigInt] = BigInt(0) #:: BigInt(1) #:: fibs.zip(fibs.tail).map((a, b) => a + b)
  val firstTenFibs = fibs.take(10).toList // 0, 1, 1, 2, 3, 5, 8, 13, 21, 34

  /*
    TODO Exercise (time permitting):
    1) infiniteOnes: a LazyList of 1s that never ends
    2) multiples(n): every multiple of n, forever
    3) using naturalNumbers, produce the first 10 perfect squares
   */


  /*
    This MyList implementation contains ALL the exercises
   */
  trait Predicate[-T] {
    def apply(element: T): Boolean
  }

  trait MyFunction[-A, +B] { // used to be MyTransformer = essentially a function A => B
    def apply(element: A): B
  }

  abstract class MyList[+T] {
    def head: T
    def tail: MyList[T]
    def isEmpty: Boolean
    def add[S >: T](element: S): MyList[S]
    def toString: String
    def ++[S >: T](list: MyList[S]): MyList[S]

    // example: [1,2,3].map(transformer to multiply an element by 10) => [10, 20, 30]
    def map[U](transformer: T => U): MyList[U]
    // example: [1,2,3].flatMap(transformer to create a list of [x, x * 10] for every element) => [1, 10, 2, 20, 3, 30]
    def flatMap[U](transformer: T => MyList[U]): MyList[U]
    // example: [1,2,3].filter(evenFilter) => [2]
    def filter(predicate: T => Boolean): MyList[T]

    // example: [1,2,3].zipWith([a,b,c], (int, char) => (int + char)) => [1a, 2b, 3c]
    def zipWith[S, R](anotherList: MyList[S], comb: (T, S) => R): MyList[R]
    // executes f for every element in the list
    def foreach(f: T => Unit): Unit

    // used for for-comprehensions; in real Scala libraries, evaluates lazily
    def withFilter(predicate: T => Boolean): MyList[T] = filter(predicate)
  }

  object Empty extends MyList[Nothing] {
    def head: Nothing = throw new RuntimeException("No such element for the empty list, dummy")
    def tail: MyList[Nothing] = throw new RuntimeException("No such tail for the empty list, dummy")
    def isEmpty: Boolean = true
    override def add[S >: Nothing](element: S): MyList[S] = new Cons(element, Empty)
    override def toString: String = "[]"
    override def ++[S >: Nothing](anotherList: MyList[S]): MyList[S] = anotherList

    override def map[U](transformer: Nothing => U): MyList[U] = Empty
    override def flatMap[U](transformer: Nothing => MyList[U]): MyList[U] = Empty
    override def filter(predicate: Nothing => Boolean): MyList[Nothing] = Empty

    override def zipWith[S, R](anotherList: MyList[S], comb: (Nothing, S) => R): MyList[R] =
      if (anotherList.isEmpty) Empty
      else throw new RuntimeException("Can't zip with non-empty list, DUMMY")

    override def foreach(f: Nothing => Unit): Unit = ()
  }

  object Cons {
    def apply[T](head: T, tail: MyList[T]) = new Cons(head, tail)
  }
  class Cons[+T](override val head: T, override val tail: MyList[T]) extends MyList[T] { // stands for "constructor" (historical name in Lisp)
    override def isEmpty: Boolean = false
    override def add[S >: T](element: S): MyList[S] = new Cons(element, this)

    override def toString: String = {
      def printElements(list: MyList[T]): String =
        if (list.isEmpty) ""
        else if (list.tail.isEmpty) s"${list.head}"
        else s"${list.head}, ${printElements(list.tail)}"

      s"[${printElements(this)}]"
    }

    /*
      [1 2 3] ++ [4 5]
      = Cons(1, [2 3] ++ [4 5])
      = Cons(1, Cons(2, [3] ++ [4 5]))
      = Cons(1, Cons(2, Cons(3, [4 5])))
     */
    def ++[S >: T](anotherList: MyList[S]): MyList[S] =
      new Cons(head, this.tail ++ anotherList)

    override def map[U](transformer: T => U): MyList[U] =
      new Cons(transformer(head), tail.map(transformer))

    /*
     [1,2,3].flatMap(x -> [x, x * 10]) =
     [1, 10] ++ [2,3].flatMap(...) =
     [1, 10] ++ [2, 20] ++ [3].flatMap(...) =
     [1, 10] ++ [2, 20] ++ [3, 30] ++ [].flatMap(...) =
     [1, 10, 2, 20, 3, 30]
    */
    override def flatMap[U](transformer: T => MyList[U]): MyList[U] =
      transformer(head) ++ tail.flatMap(transformer)

    override def filter(predicate: T => Boolean): MyList[T] =
      if (predicate(head)) new Cons(head, tail.filter(predicate))
      else tail.filter(predicate)

    override def zipWith[S, R](anotherList: MyList[S], comb: (T, S) => R): MyList[R] =
      if (anotherList.isEmpty) throw new RuntimeException("Can't zip with an empty list, DUMMY")
      else new Cons(comb(this.head, anotherList.head), tail.zipWith(anotherList.tail, comb))

    override def foreach(f: T => Unit): Unit = {
      f(head)
      tail.foreach(f)
    }
  }

  val numbers = new Cons(1, new Cons(2, new Cons(3, Empty)))
  val moreNumbers = new Cons(4, new Cons(5, Empty))

  val tenx: Int => Int = x => 10 * x
  val pairWithTen: Int => MyList[Int] = element => new Cons(element, new Cons(10 * element, Empty))
  val evenPredicate: Int => Boolean = element => element % 2 == 0

  def main(args: Array[String]): Unit = {
    println(isPrimeTailrec(2003))
  }
}
