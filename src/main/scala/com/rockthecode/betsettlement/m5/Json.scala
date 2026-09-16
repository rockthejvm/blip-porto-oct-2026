package com.rockthecode.betsettlement.m5

/**
 * Milestone 5 - JSON.
 *
 * This is Block C.3 verbatim: the ADT you designed on day 2, and the renderer
 * you wrote for it. Use your own; it will already do everything this needs.
 *
 * The point of not reaching for a JSON library here is that you do not need
 * one to produce JSON, and the six cases below are genuinely all of it.
 */
enum Json {
  case JNull
  case JBool(value: Boolean)
  case JNum(value: BigDecimal)
  case JStr(value: String)
  case JArr(items: List[Json])
  case JObj(fields: List[(String, Json)])
}

object Json {

  def render(json: Json): String =
    json match {
      case JNull        => "null"
      case JBool(value) => value.toString
      case JNum(value)  => value.toString
      case JStr(value)  => renderString(value)
      case JArr(items)  => items.map(render).mkString("[", ",", "]")
      case JObj(fields) =>
        fields.map((name, value) => s"${renderString(name)}:${render(value)}").mkString("{", ",", "}")
    }

  private def renderString(raw: String): String =
    "\"" + raw.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
