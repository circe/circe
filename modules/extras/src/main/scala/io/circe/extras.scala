package io.circe

import scala.collection.immutable.{ Map, Set }

package object extras {

  /**
   * For a [[Json]], sanitize any [[JsonObject]]'s key's values not in the whitelist.
   *
   * The original author's motivation for this function should explain why someone may want to use it.
   *
   * When the author had defined an API Integration, namely an HTTP Client talking to a 3rd Party API, he needed
   * a way to log the full HTTP Response's JSON Payload. However, the entire JSON could not be logged since
   * it contained sensitive information. So, that resulted in the author writing a function for
   * sanitizing JSON, i.e. apply a function: [[Json]] => [[Json]] that replaces sensitive keys' values with
   * dummy values, e.g. "XXXX". A set of keys' values serves as a whitelist.
   *
   * For example, let's say that an HTTP API responds with the following JSON:
   *
   * val json = { "x" : true, "y" : 42, "z" : "<insert sensitive info here>" }
   *
   * Let's assume that fields x and y are safe to log, however z is not as it contains sensitive information. This
   * function can then be applied to only show x and y as-is:
   *
   * sanitizeKeys(
   *  json,
   *  Set("x", "y"),
   *  _ => Json.fromString("X"),
   *  Json.fromString("X"),
   *  _ => Json.fromString("X"),
   *  _ => Json.fromString("X")
   * )
   *
   * would then output:
   * { "x" : true, "y" : 42, "z" : "X" }
   *
   * @param json JSON to sanitize
   * @param whitelist Set of JSON Objects' keys whose values can be shown as-is
   * @param onBoolean_ Sanitizing function used if the key's value is a [[Boolean]]
   * @param onNull_ Sanitizing function used if the key's value is a [[Json.Null]]
   * @param onString_ Sanitizing function used if the key's value is a [[String]]
   * @param onNumber_ Sanitizing function used if the key's value is a [[JsonNumber]]
   * @return Sanitized JSON
   */
  def sanitizeKeys(
    json: Json,
    whitelist: Set[String],
    onBoolean_ : Boolean => Json,
    onNull_ : Json,
    onString_ : String => Json,
    onNumber_ : JsonNumber => Json
  ): Json = {
    val sanitizedFolder: Json.Folder[Json] = new Json.Folder[Json] { self: Json.Folder[Json] =>
      override def onNull: Json                       = onNull_
      override def onBoolean(value: Boolean): Json    = onBoolean_(value)
      override def onNumber(value: JsonNumber): Json  = onNumber_(value)
      override def onString(value: String): Json      = onString_(value)
      override def onArray(value: Vector[Json]): Json = {
        val sanitized: Vector[Json]                   =
          value.map { j: Json =>
            sanitizeKeys(j, whitelist, onBoolean, onNull, onString, onNumber)
          }
        Json.fromValues(sanitized)
      }
      override def onObject(obj: JsonObject): Json = {
        val sanitized: Map[String, Json] = obj.toMap.map {
          case (key, value) =>
            // Remember: if the key is whitelisted, then the key's value must be shown as-is
            if (whitelist.contains(key)) {
              val newValue: Json =
                value
                  .withArray(onArray)
                  .withObject(onObject)
              (key, newValue)
            } else { // Otherwise, sanitize the key's value since it's not whitelisted
              val newValue: Json =
                value.foldWith(self)
              (key, newValue)
            }
        }
        Json.fromJsonObject(
          JsonObject.fromMap(sanitized)
        )
      }
    }
    json.foldWith(sanitizedFolder)
  }

}
