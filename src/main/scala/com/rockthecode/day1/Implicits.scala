package com.rockthecode.day1

object Implicits {
  /*
		- implicit values + implicit arguments
		- implicit classes => extension methods
		- implicit conversions
   */

  // ordering
  // implicit val => will be used wherever a function requires an implicit of this type
  implicit val descOrdering: Ordering[Int] = Ordering.fromLessThan((a, b) => a > b)
  val myList = List(3, 2, 1, 4, 5)
  val orderedList = myList.sorted // compiler automatically passes an implicit Ordering[Int]

  // implicit argument, e.g. sorted
  def compare[A](a: A, b: A)(implicit ord: Ordering[A]): Boolean =
    ord.lt(a, b)

  case class Person(name: String) {
    def likes(movie: String) = println(s"$name likes $movie")
  }
  object Person {
    // implicit val ord: Ordering[Person] = Ordering.fromLessThan((a, b) => a.name.compareTo(b.name) < 0)
  }

  object ThisCodeNeedsAlphabetic {
    implicit val ord: Ordering[Person] = Ordering.fromLessThan((a, b) => a.name.compareTo(b.name) < 0)
  }

  import ThisCodeNeedsAlphabetic.*

  val isBigger =
    compare(Person("alice"), Person("bob")) // will only compile if there is an implicit value of this type in scope
  /*
		Implicit resolution = where the compiler looks for implicit values
		- local scope
		- imported scope
		- companion objects for all the types involved in the method call

		the compiler will look in these places in this order
		CANNOT have >1 implicits at the same level
   */

  /*
		Best practice:
			- put the most common implicit in the companions of the type
			- if there is NO common implicit that make sense, put them in objects
				and import them manually
   */

  // part 2 - implicit classes for extension methods
  implicit class MyRichInt(n: Int) {
    def times(str: String): String =
      if (n <= 0) ""
      else str + (n - 1).times(str)
  }

  val Scalax3 = 3.times("Scala") // MyRichInt(3).times("Scala")
  // example: 3.seconds

  // 3 - never do this - implicit conversions
  implicit def fromStringToPerson(name: String): Person = Person(name)
  "Bob".likes("Forrest Gump") // fromStringToPerson("Bob").likes(...)
}
