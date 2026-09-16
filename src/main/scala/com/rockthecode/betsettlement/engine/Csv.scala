package com.rockthecode.betsettlement.engine

import scala.io.Source
import scala.util.Using

/**
 * Reading files. Given to you, finished, and not the point of today.
 *
 * `rows` hands you one map per data row, keyed by column name. Turning those
 * maps into YOUR model is your job - that part is modelling, and modelling is
 * the exercise.
 *
 * No quoting, no escaping, no embedded commas: our data has none, and a real
 * CSV parser is a solved problem you should never write again.
 */
object Csv {

  /** Every data row of a CSV with a header, as column name -> value. */
  def rows(path: String): List[Map[String, String]] =
    Using.resource(Source.fromFile(path)) { source =>
      source.getLines().toList.filter(_.trim.nonEmpty) match {
        case Nil => Nil
        case header :: dataRows =>
          val columns = header.split(",", -1).toList
          dataRows.map(row => columns.zip(row.split(",", -1)).toMap)
      }
    }

  /**
   * Split the `legs` column into its raw pieces, so you do not have to fight
   * the string format.
   *
   *   legs("M001:HOME:2.50|M002:AWAY:1.80")
   *     == List(("M001", "HOME", "2.50"), ("M002", "AWAY", "1.80"))
   *
   * Strings in, strings out - deciding what these BECOME is up to you.
   */
  def legs(field: String): List[(String, String, String)] =
    field
      .split('|')
      .toList
      .filter(_.nonEmpty)
      .map(_.split(':') match {
        case Array(marketId, selection, odds) => (marketId, selection, odds)
        case _ => throw new IllegalArgumentException(s"malformed leg: $field")
      })
}
