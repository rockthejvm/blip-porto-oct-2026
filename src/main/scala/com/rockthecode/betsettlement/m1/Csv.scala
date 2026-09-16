package com.rockthecode.betsettlement.m1

import scala.io.Source
import scala.util.Using

/**
 * The plumbing, given to you. Reads a CSV with a header row into a list of
 * maps, one per data row.
 *
 * No quoting, no escaping, no embedded commas - our data has none, and a real
 * CSV parser is not what today is about.
 */
object Csv {

  def rows(path: String): List[Map[String, String]] =
    Using.resource(Source.fromFile(path)) { source =>
      source.getLines().toList.filter(_.trim.nonEmpty) match {
        case Nil => Nil
        case header :: dataRows =>
          val columns = header.split(",", -1).toList
          dataRows.map(row => columns.zip(row.split(",", -1)).toMap)
      }
    }
}
