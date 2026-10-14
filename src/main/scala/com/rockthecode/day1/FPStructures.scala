package com.rockthecode.day1

import java.io.FileNotFoundException

object FPStructures {
  object MySingleton

  case class Person(name: String, age: Int)

  // pattern matching = switch on steroids
  val aValue: Any = 42
  val pmDescription = aValue match {
    case 1 => "the one"
    case 2 => "second"
    case MySingleton => "the singleton"
    case Person(name, age) => "a person with ..."
    case h :: t => "a linked list with head and tail"
    // anything with an unapply method
    case a => s"matched something else: $a"
  } // EXPRESSION

  val aList = 2 :: List(3, 4)
  /*
	checks:
		- literals
		- objects/singletons
		- case classes
		- match anything and bind variable
		- _ => match anything
   */

  /** option = a possibly absent value = a "list" with at most one value
    */

  // only 2 possible values: None, Some(v)
  val anOption = Option("https://rockthejvm.com..")
  /*
	in Java
		if (getThing() == null) {
			throw ...
		} else {
			return string.length
		}
   */

  val stringLength = anOption.map(s => s.length)

  // transform options in a chain
  def fetchUrl(url: String): Option[Int] = Some(2)
  /*
		if (url == null) {
		} else {
			result = fetch(url)
			if (result == null ) {
				...
			} else {
				💪
			}
		}
   */
  anOption.flatMap(fetchUrl) // no need for null checks

  // getOrElse
  // isEmpty
  // pattern match
  // filter(p) => if option is empty, then empty; if the value passes, then the same value; otherwise empty
  val description = anOption match {
    case None => "nothing"
    case Some(v) => s"a value: $v"
  }

  val emptyOption: Option[Int] = Option(null) // ok
  /*
		- wrap all your potentially null values in Options
		- then transform Options with FP transformations
   */

  val fetchValue = for {
    url <- anOption
    result <- fetchUrl(url)
  } yield result

  /*
		Try = potentially FAILED computation
   */
  import scala.util.{Try, Success, Failure}
  /*
		in Java
		try {
			somethingDangerous()
		} catch(Exception e) {
			...
		}
   */
  def somethingDangerous(v: Int): Int =
    if (v == 42) throw new RuntimeException("BBOOOOOOOOOOOOOOOM 💥")
    else v + 1

  val aTry = Try(somethingDangerous(32)) // Try[Int]
  val tryDescription = aTry match {
    case Failure(exception) => s"failed: $exception"
    case Success(value) => s"🎉 have a value $value"
  }

  // [1,2,3].map(x => x * 10)
  // List[Int].map(Int => Int): List[Int]

  // Option(10).map(x => x * 10): Option[Int]
  // Try(10).map(x => x * 10): Try[Int]

  // map, flatMap, filter, for comprehensions
  val mappedTry = aTry.map(x => x * 10) // Try[Int]: if the original was successful, value * 10, otherwise that failed
  val filteredTry =
    aTry.filter(_ % 2 == 0) // Try[Int]: if success, filter it (and throw if failed), otherwise keep the failure
  val transformedTry = aTry.flatMap(x =>
    Try(somethingDangerous(x))
  ) // Try[Int]: if success, return the Try by applying the function, otherwise keep the failure

  val tryTransformations = for {
    a <- Try(somethingDangerous(43))
    b <- Try(somethingDangerous(a))
    c <- Try(somethingDangerous(a + 10))
  } yield a + b + c // Try[Int]

  val tryTransformations_v2 =
    Try(somethingDangerous(43)).flatMap { a =>
      Try(somethingDangerous(a)).flatMap { b =>
        Try(somethingDangerous(a + 10)).map { c =>
          a + b + c
        }
      }
    }

  /*
		Either = Left("error") or Right("value")
   */
  val anEither: Either[Exception, Int] = Right(2)
  //             undesirable^      ^desired value
  val eitherDescription = anEither match {
    case Left(value) => "undesirable"
    case Right(value) => "desirable"
  }

  // map, flatMap, for comprehensions
  val mappedEither = anEither.map(x => (1 to x).toList) // Either[Exception, List[Int]]
  val flatMappedEither = anEither.flatMap(x => Right(x * 10)) // Either[Exception, Int]

  val eitherFor = for {
    a <- anEither
    b <- mappedEither
  } yield a

  /*
		Future = a value that will be computed later
   */

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.{Future, Promise}

  val aFuture: Future[Int] = Future {
    Thread.sleep(1000)
    42
  }

  // deal with futures: callbacks
  // callback hell - careful!
  // aFuture.onComplete(x =>
  //   x match {
  //     case Failure(exception) => println(s"future failed: $exception")
  //     case Success(value) => println(s"future ok: $value")
  //   }
  // )

  // functional transformations: map, flatMap, recover, recoverWith
  val aMappedFuture = aFuture.map(_ * 10) // Future[Int]
  val aFlatMappedFuture = aFuture.flatMap(x => Future(x * 1000))
  val aFilteredFuture = aFuture.filter(_ % 2 != 0) // Future[Int]

  val forFuture = for {
    a <- aFuture
    b <- aMappedFuture
  } yield a + b

  // recover
  val recoveredFuture = aFuture.recover { // same as aFuture.recover(x => x match {....})
    case e: RuntimeException => 3
    case e: FileNotFoundException => 4
    case _ => 5
  }

  // recoverWith
  val megaRecoveredFuture = aFuture.recoverWith {
    // instead of one value, RUN OTHER FUTURES
    case e: RuntimeException => Future(3000)
    case e: FileNotFoundException => Future(4000)
    case _ => Future.failed(new RuntimeException("womp"))
  }

  // Promise = container that you can fill later with a value
  val promise = Promise[Int]() // when instantiated, this is an "empty vessel"
  // later = may mean this thread, or another thread, or whatever, just LATER
  promise.success(42)

  // someone else takes hold of this value
  val myFuture = promise.future

  def promiseDemo() = {
    val promise = Promise[Int]()
    val producerThread = new Thread(() => {
      println("[producer] starting...")
      Thread.sleep(1000)
      println("[producer] 3")
      Thread.sleep(1000)
      println("[producer] 2")
      Thread.sleep(1000)
      println("[producer] 1")
      Thread.sleep(1000)
      promise.success(42)
      println("[producer] done")
    })

    val consumerThread = new Thread(() => {
      val myFuture = promise.future
      println("[consumer] starting....")
      val myTransformedFuture = myFuture.map { x =>
        println("[consumer] I HAZ VALUE, me transform it")
        x * 1000
      }

      myFuture.onComplete(println)
    })

    producerThread.start()
    consumerThread.start()
    producerThread.join()
    consumerThread.join()
  }

  def main(args: Array[String]) = {
    promiseDemo()
    Thread.sleep(10000000)
  }
}
