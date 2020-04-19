package io.circe.extras

import io.circe.{ Json, JsonNumber, JsonObject }

import scala.collection.immutable.{ Map, Set }

object Extras {

  /**
   * For a [[Json]], sanitize any [[JsonObject]]'s key's values if
   * @keysToSanitize contains it.
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
   * @param keysToSanitize Set of JSON Objects' keys to sanitize
   * @param onBoolean Sanitizing function used if the key's value is a [[Boolean]]
   * @param onNull Sanitizing function used if the key's value is a [[Json.Null]]
   * @param onString Sanitizing function used if the key's value is a [[String]]
   * @param onNumber Sanitizing function used if the key's value is a [[JsonNumber]]
   * @return Sanitized JSON
   */
  def sanitizeKeys(
    json: Json,
    keysToSanitize: Set[String],
    onBoolean: Boolean => Json,
    onNull: Json,
    onString: String => Json,
    onNumber: JsonNumber => Json
  ): Json =
    json.withArray { arr: Vector[Json] =>
      val sanitized: Vector[Json] =
        arr.map { j: Json =>
          sanitizeKeys(j, keysToSanitize, onBoolean, onNull, onString, onNumber)
        }
      Json.fromValues(sanitized)
    }.withObject { obj: JsonObject =>
      val map: Map[String, Json] = obj.toMap
      val sanitized: Map[String, Json] = map.collect {
        case (key, value) =>
          if (keysToSanitize.contains(key)) {
            val newValue: Json =
              value.withArray { _ =>
                sanitizeKeys(value, keysToSanitize, onBoolean, onNull, onString, onNumber)
              }.withObject { _ =>
                sanitizeKeys(value, keysToSanitize, onBoolean, onNull, onString, onNumber)
              }.withBoolean(onBoolean).withNull(onNull).withString(onString).withNumber(onNumber)

            (key, newValue)
          } else {
            (key, value)
          }
      }
      Json.fromJsonObject(
        JsonObject.fromMap(sanitized)
      )
    }

}
