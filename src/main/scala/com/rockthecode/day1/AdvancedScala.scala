package com.rockthecode.day1


import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.util.{Failure, Random, Success, Try}

object AdvancedScala {

  /**
   * Pattern matching
   *   - on values, objects and case classes
   */

  case object MySingleton
  case class SSI(n1: Int, n2: Int)
  case class PersonCC(name: String, age: Int, ssi: SSI)

  val unknownValue: Any = 1
  unknownValue match {
    case _: RuntimeException => 534
    case 1 => 42
    case "the string" => 99
    case MySingleton => 578293
    case PersonCC("bob", a, _) if a > 42 => 5839
    case p @ PersonCC(name, age, ssi @ SSI(_, _)) => 52849 // can reuse p, name and age, plus you can nest patterns
    case aTuple @ (1, _) => 5738
    case List(1,2,_) => 5839 // lists can be deconstructed

  }

  // TODO exercise on typed lists

  /**
   * PartialFunctions
   */

  val maybeApplicable: PartialFunction[Int, String] = { // equivalent with x => x match { ... }
    case 1 => "first"
    case 2 => "second"
    case 3 => "third"
    case 57832 => "lucky"
    case _ => "nothing"
  }

  // PartialFunctions have some special methods
  maybeApplicable.isDefinedAt(54)
  val extendedApplicable = maybeApplicable.orElse[Int, String] {
    case 99 => "luckier"
  }

  // PFs extend functions, so you can pass them to higher-order functions like map
  List(1,2,3,56).map {
    case 1 => "a string"
    case 2 => "..."
    case _ => ""
  }

  /**
   * Options
   *   - show slides
   *   - construction, map, flatMap, filter, orElse, getOrElse
   */

  def myUnsafeMethod: String = null
  val anOption = Option(myUnsafeMethod) // None

  Some(2).filter(_ % 2 != 0) // None

  // for-comprehensions on options
  val anotherOption = for {
    value <- anOption
  } yield value + 1

  Option(null).orElse(Some(2))
  // NEVER CALL Some(null), you'll kill us all

  // show map, flatMap, filter
  // show pattern matching


  // TODO exercise with unsafe APIs and Option composition

  // --------------- given:
  val config: Map[String, String] = Map(
    // fetched from elsewhere
    "host" -> "176.45.36.1",
    "port" -> "80"
  )

  class Connection {
    def connect = "Connected" // connect to some server
  }
  object Connection {
    val random = new Random(System.nanoTime())

    def apply(host: String, port: String): Option[Connection] =
      if (random.nextBoolean()) Some(new Connection)
      else None
  }

  val host = config.get("host") // this is an Option[Int]
  val port = config.get("port") // this is an Option[Int]

  // ----------------- Task: obtain a connection instance with the host and port, then print the result of calling its connect method

  // ----------------- Solution 1: step by step with Java analogies
  /*
    if (h != null)
      if (p != null)
        return Connection.apply(h, p)

    return null
   */
  val connection = host.flatMap(h => port.flatMap(p => Connection.apply(h, p)))
  /*
    if (c != null)
      return c.connect
    return null
   */
  val connectionStatus = connection.map(c => c.connect)
  // if (connectionStatus == null) println(None) else print (Some(connectionstatus.get))
  println(connectionStatus)
  /*
    if (status != null)
      println(status)
   */
  connectionStatus.foreach(println)

  // ------------------ Solution 2: chained calls
  config.get("host")
    .flatMap(host => config.get("port")
      .flatMap(port => Connection(host, port))
      .map(connection => connection.connect))
    .foreach(println)

  // ------------------ Solution 3: for-comprehensions
  val forConnectionStatus = for {
    host <- config.get("host")
    port <- config.get("port")
    connection <- Connection(host, port)
  } yield connection.connect
  forConnectionStatus.foreach(println)

  /**
   * Exceptions and Try
   *   - throwing; throwing returns Nothing
   *   - catching exceptions
   *   - wrapping try/catches into Try
   *   - map, flatMap, filter
   */

  // standard try-catch
  val potentialException = try {
    // code that can throw exceptions
    throw new RuntimeException
  } catch {
    case something: Exception => 43
    case e: RuntimeException => 42
  }

  def potentiallyThrowException: Int = throw new NullPointerException
  // wrap the computation in a Try
  val aTry = Try(potentiallyThrowException)

  // Try objects support map, flatMap, filter
  aTry.map(_ + 1)
  val anotherTry = for {
    value <- aTry
  } yield value + 1

  // TODO exercise with unsafe APIs and try composition

  // ------------------- given: watch the names so they don't conflict with the earlier exercise

  val tryHost = "localhost"
  val tryPort = "8080"
  def renderHTML(page: String) = println(page)

  class TConnection {
    def fetch(url: String): String = {
      val random = new Random(System.nanoTime())
      if (random.nextBoolean()) "<html>...</html>"
      else throw new RuntimeException("Connection interrupted")
    }

    def getSafe(url: String): Try[String] = Try(fetch(url))
  }

  object HttpService {
    val random = new Random(System.nanoTime())

    def getConnection(host: String, port: String): TConnection =
      if (random.nextBoolean()) new TConnection
      else throw new RuntimeException("Someone else took the port")

    def getSafeConnection(host: String, port: String): Try[TConnection] = Try(getConnection(host, port))
  }

  // ------------------- task: try to obtain a connection using the host and port, and if you can, fetch the HTML and print it

  // ------------------- solution 1: step by step
  val possibleConnection = HttpService.getSafeConnection(tryHost, tryPort)
  val possibleHTML = possibleConnection.flatMap(connection => connection.getSafe("/home"))
  possibleHTML.foreach(renderHTML)

  // ------------------- solution 2: try composition with call chains
  HttpService.getSafeConnection(tryHost, tryPort)
    .flatMap(connection => connection.getSafe("/home"))
    .foreach(renderHTML)

  // ------------------- solution 3: for-comprehension
  for {
    connection <- HttpService.getSafeConnection(tryHost, tryPort)
    html <- connection.getSafe("/home")
  } renderHTML(html)




  /**
   * 23. Futures
   *   - construction, map, flatMap, filter, onComplete
   */

  import scala.concurrent.ExecutionContext.Implicits.global // execution contexts are "platforms" for running threads
  import scala.concurrent.duration._

  val aFuture = Future[Int]{
    // code - will run on SOME thread
    42
  }

  aFuture.onComplete { // will be executed on SOME thread
    case Success(value) => println(s"I got the meaning of life: $value ")
    case Failure(ex) => println(s"I failed with the meaning of life: $ex")
  }

  def doSomethingWithYourLife(i: Int): Future[Int] = Future(43)

  // Futures have map, flatMap and filter, so they have for-comprehensions
  val aMappedFuture: Future[Int] = aFuture.map(_ + 1) // another future
  val aFlatMappedFuture: Future[Int] = aFuture.flatMap(doSomethingWithYourLife)
  val aFilteredFuture: Future[Int] = aFuture.filter(_ % 2 == 0)

  val aForFuture = for {
    meaningOfLife <- aFuture
  } yield meaningOfLife + 1

  // you can also wait for a Future, but it's generally bad because we try to avoid blocking
  val waited = Await.result(aFuture, 3.seconds)
  // you can also try to fetch the value of the future at the current moment (also bad, and would be VERY bad if you busy-waited for the value)
  val currentFutureValue = aFuture.value // this is an Option[Try[Int]]; option because it might not have finished, Try because the result can be a failure

  // recovering with another value
  val recoveredFuture = aFuture.recover {
    case e: RuntimeException => 45
  }

  // recoviering with another Future - you can think of this one like flatMap
  val recoveredFuture2 = aFuture.recoverWith {
    case e: RuntimeException => Future(45)
  }

  // promises

  val promise = Promise[Int]() // "controller" over a future
  val future = promise.future

  // thread 1 - "consumer"
  future.onComplete {
    case Success(r) => println("[consumer] I've received " + r)
  }

  // thread 2 - "producer"
  val producer = new Thread(() => {
    println("[producer] crunching numbers...")
    Thread.sleep(500)
    // "fulfilling" the promise
    promise.success(42)
    println("[producer] done")
  })

  producer.start()
  Thread.sleep(1000)

  /*
    1) fulfill a future IMMEDIATELY with a value
    2) inSequence(fa, fb)
    3) first(fa, fb) => new future with the first value of the two futures
    4) last(fa, fb) => new future with the last value
    5) retryUntil[T](action: () => Future[T], condition: T => Boolean): Future[T]
   */

  // 1 - fulfill immediately
  def fulfillImmediately[T](value: T): Future[T] = Future(value)
  // 2 - insequence
  def inSequence[A, B](first: Future[A], second: Future[B]): Future[B] =
    first.flatMap(_ => second)

  // 3 - first out of two futures
  def first[A](fa: Future[A], fb: Future[A]): Future[A] = {
    val promise = Promise[A]
    fa.onComplete(promise.tryComplete)
    fb.onComplete(promise.tryComplete)

    promise.future
  }

  // 4 - last out of the two futures
  def last[A](fa: Future[A], fb: Future[A]): Future[A] = {
    // 1 promise which both futures will try to complete
    // 2 promise which the LAST future will complete
    val bothPromise = Promise[A]
    val lastPromise = Promise[A]
    val checkAndComplete = (result: Try[A]) =>
      if(!bothPromise.tryComplete(result))
        lastPromise.complete(result)

    fa.onComplete(checkAndComplete)
    fb.onComplete(checkAndComplete)

    lastPromise.future
  }

  val fast = Future {
    Thread.sleep(100)
    42
  }

  val slow = Future {
    Thread.sleep(200)
    45
  }
  first(fast, slow).foreach(f => println("FIRST: " + f))
  last(fast, slow).foreach(l => println("LAST: "  +  l))

  Thread.sleep(1000)

  // retry until
  def retryUntil[A](action: () => Future[A], condition: A => Boolean): Future[A] =
    action()
      .filter(condition)
      .recoverWith {
        case _ => retryUntil(action, condition)
      }

  val random = new Random()
  val action = () => Future {
    Thread.sleep(100)
    val nextValue = random.nextInt(100)
    println("generated " + nextValue)
    nextValue
  }

  retryUntil(action, (x: Int) =>  x < 10).foreach(result => println("settled at " + result))
  Thread.sleep(10000)


  /**
   * 24. Implicits
   *   - implicit conversion string -> person
   *   - ordering demo
   */

  case class Person3(name: String, age: Int) {
    def greet = println(s"Hi, my name is $name and I am $age years old")
  }

  case class Person4(name: String, age: Int) {
    def greet = println(s"Hi, my name is $name and I am $age years old")
  }


  implicit def stringToPerson(name: String): Person3 = Person3(name, 45)
  implicit def stringToPerson2(name: String): Person4 = Person4(name, 45)

  def processPerson(p: Person3) = println()
  processPerson("Daniel") // will be auto-converted to Person3

  def processPersonAsync(p: Person3)(implicit ec: ExecutionContext) = println()
  processPersonAsync(Person3("Daniel", 45)) // execution context is passed implicitly as argument

  // discussion on where the compiler looks for implicit values
  /*
    1. Local scope = everything explicitly defined
    2. Imported scope
    3. Companion objects for the types involved in the method call
    4. Companion objects for the arg types involved in the call
   */
  implicit val reverseOrdering: Ordering[Int] = Ordering.fromLessThan(_ > _)
  println(List(3,4,2,5,1).sorted) // 5 4 3 2 1

  object Person3 {
    // good practice: define implicits on the Person type in its companion object
    implicit val naturalOrdering: Ordering[Person3] = Ordering.fromLessThan((a, b) => a.name.compareTo(b.name) < 0)
  }
  println(List(Person3("Daniel", 45), Person3("Mark", 57823)).sorted)


  /**
   * 25. Pimp my library
   *   - RichInt
   *
   * Ex: enrich the string class
   *   - "6" / 2
   */

  // example of enriching an existing type (which we can't normally modify)
  implicit class MyRichInt(n: Int) {
    def times(s: String): String =
      if (n <= 0) ""
      else s + new MyRichInt(n - 1).times(s)
  }

  3.times("Hello world") // new MyRichInt(3).times("Hello world")

  // example of enriching Int: the duration package
  import scala.concurrent.duration._
  3.minutes


  // exercise: enrich String so you can do math operations between Strings and Ints like in JavaScript
  implicit class MyRichString(s: String) {
    def /(n: Int): Int = s.toInt / n
  }

  println("6" / 2)

  /**
   * 26. Type system mastery: Variance
   *   - "contains T" = covariant, "acts on T" = contravariant
   *   - (massive discussion on variance positions)
   *
   * To prove the compiler errors of my commented code below, write a polymorphic instantiation and try to mess up with the objects.
   *
   * val cage: Cage[Animal] = new Cage[Dog]
   * val vet: Vet[Dog] = new Vet[Animal]
   */

  class Animal
  class Cat extends Animal
  class Dog extends Animal


  /*
   vals are in covariant position

   class Vet[-T <: Animal](val animal: T)
   val myVet: Vet[Dog] = new Vet[Animal](new Crocodile) // nope
  */


  /*
    vars are in covariant position (same argument)
    vars are in contravariant position

    class Cage[+T <: Animal](var animal: T)
    val myCage: Cage[Animal] = new Cage[Dog](lassie)
    myCage.animal = new Crocodile // nope
    */

  /*
    method arguments are in contravariant position
    class Cage[+T <: Animal] {
      def enclose(animal: T): Unit
    }

    val myCage: Cage[Animal] = new Cage[Dog]
    myCage.enclose(new Crocodile) // crocodile is an animal, no?!
   */

  /*
    method return types are in covariant position
    class Vet[-T <: Animal] {
      def getAnimalToDonate(): T
    }

    class CatLoverVet extends Vet[Animal] {
      def getAnimalToDonate(): new Cat
    }

    val myVet: Vet[Dog] = new CatLoverVet
    val d: Dog = myVet.getAnimalToDonate // animal, no?!
   */


  // method arguments
  class Cage[+T <: Animal](animal: T) {
    // def insertAnimal(animal: T) = println () // arguments are in CONTRAVARIANT position
    def get: T = animal
  }

  // method return types
  class Vet[-T <: Animal] {
    def heal(animal: T): Unit = println()
    // def get: T = ??? // method return types are in COVARIANT position
  }
}