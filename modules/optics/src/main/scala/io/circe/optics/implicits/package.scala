package io.circe.optics

import io.circe.Json
import io.circe.Json.{ JArray, JObject }
import io.circe.optics.JsonPath.root

package object implicits {

  final implicit class JsonImplicits(json: Json) {

    def findPathsToKey(key: String): Seq[JsonPath] =
      findPathsToKeyR(json, key, new KeyPathMarker())

    def findParentPathsToKey(key: String): Seq[JsonPath] =
      findPathsToKeyR(json, key, new ParentPathMarker())

    def findPathsToKey(key: String, maxDepth: Int): Seq[JsonPath] =
      findPathsToKeyR(json, key, new MaxDepthPathMarker(maxDepth))

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
        if (step.atDepth == maxDepth) MaxDepthPathMarker(maxDepth, Some(step)) else MaxDepthPathMarker(maxDepth)
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