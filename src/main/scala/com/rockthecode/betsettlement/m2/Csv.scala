package com.rockthecode.betsettlement.m2

import scala.io.Source
import scala.util.Using

/** Plumbing, given to you. Header row, no quoting, no escaping. */
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
