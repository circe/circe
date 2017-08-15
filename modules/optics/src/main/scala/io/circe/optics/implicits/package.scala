package io.circe.optics

import io.circe.Json
import io.circe.Json.{ JArray, JObject }
import io.circe.optics.JsonPath.root

package object implicits {

  final implicit class JsonImplicits(json: Json) {

    /** Finds all JsonPath's to fields with the specified key.
      *
      * @param key the field to search for
      * @return a sequence of all possible JsonPath's that lead to fields with the name of the key
      */
    def findPathsToKey(key: String): Seq[JsonPath] =
      findPathsToKeyR(json, key, KeyPathMarker())

    /** Finds all JsonPath's to fields with the specified key but returns
      * the path to the parent JSON of the found field.
      *
      * For example, in:
      *
      * {{{
      * { "top":
      *   { "child": 42 }
      * }
      * }}}
      *
      * `findParentPathsToKey("child")` would return `{ "child": 42 }`.
      *
      * @param key the field to search for
      * @return a sequence of all possible JsonPath's that lead to the parent JSON value
      *         containing the field with the name of the key
      */
    def findParentPathsToKey(key: String): Seq[JsonPath] =
      findPathsToKeyR(json, key, ParentPathMarker())

    /** Finds all JsonPath's to fields with the specified key. However, all found JsonPath's
      * will only traverse as far as `maxDepth` levels deep towards the target key.
      *
      * If the found JsonPath traverses a level that is `<= maxDepth`, then the returned path
      * will lead directly to the target JSON value. If the found JsonPath traverses to a level
      * that is `> maxDepth`, then the JsonPath returned will stop at the `maxDepth` level.
      *
      * Consider the following:
      *
      * {{{
      * { "top":
      *   { "child":
      *     { "bottom": 42 }
      *   }
      * }
      * }}}
      *
      * `findPathsToKey("bottom", 2)` would return `42`.
      *
      * `findPathsToKey("bottom", 1)` would return `{ "bottom": 42 }`.
      *
      * `findPathsToKey("bottom", 0)` would return `{ "child" : { "bottom": 42 } }`.
      *
      * @param key the field to search for
      * @param maxDepth the maximum depth that any path should traverse
      * @return a sequence of all possible JsonPath's that lead to fields with the name of the key
      */
    def findPathsToKey(key: String, maxDepth: Int): Seq[JsonPath] =
      findPathsToKeyR(json, key, MaxDepthPathMarker(maxDepth))

    private case class PathStep(curPath: JsonPath, atField: String, atDepth: Int)
    sealed trait PathMarker {
      def mark(step: PathStep): JsonPath
      def update(step: PathStep): PathMarker
    }

    private case class KeyPathMarker() extends PathMarker {
      override def mark(step: PathStep): JsonPath = step.curPath.selectDynamic(step.atField)
      override def update(step: PathStep): PathMarker = KeyPathMarker()
    }

    private case class ParentPathMarker() extends PathMarker {
      override def mark(step: PathStep): JsonPath = step.curPath
      override def update(step: PathStep): PathMarker = ParentPathMarker()
    }

    private case class MaxDepthPathMarker(maxDepth: Int, maxStep: Option[PathStep] = None) extends PathMarker {
      override def mark(step: PathStep): JsonPath =
        maxStep.fold(step.curPath.selectDynamic(step.atField))(s => s.curPath.selectDynamic(s.atField))
      override def update(step: PathStep): PathMarker =
        if (step.atDepth == maxDepth) {
          MaxDepthPathMarker(maxDepth, Some(step))
        } else {
          MaxDepthPathMarker(maxDepth, maxStep)
        }
    }

    private def findPathsToKeyR[T](json: Json, key: String, marker: PathMarker,
                                  path: JsonPath = root, depth: Int = 0): Seq[JsonPath] = {
      json match {
        case JObject(obj) =>
          obj.toList.flatMap({
            case (field, js) =>
              val step = PathStep(path, field, depth)
              val seq = if (field == key) Seq(marker.mark(step)) else Nil
              seq ++ findPathsToKeyR(js, key, marker.update(step), path.selectDynamic(field), depth + 1)
          })
        case JArray(elems) =>
          elems.zipWithIndex.flatMap({
            case (js, idx) => findPathsToKeyR(js, key, marker, path.index(idx), depth)
          })
        case _ => Nil
      }
    }
  }
}
