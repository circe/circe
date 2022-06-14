package io.circe

/** A newtype over a list which describes a path in a JSON object from some
  * point to the root of the JSON object.
  *
  * The ''first'' element in the list is the root of the object (if present),
  * and the ''last'' element in the list is the starting location.
  *
  * This datatype exists so that we can store the minimal amount of
  * information required to give a nice description of some event related to
  * JSON, usually a decoding error. It permits us to avoid creating the
  * message early as much of the time the message is not needed.
  *
  * @note This type works around a number of modeling issues in the Cursors in
  *       Circe. It was created to avoid a mass deprecation on 0.14.x. For
  *       this reason it lacks is intentionally minimal and private. A more
  *       dramatic change which will involve deprecations is planned for
  *       0.15.x.
  */
private[circe] final case class PathToRoot private (value: List[PathToRoot.PathElem]) extends AnyVal {

  /** A `String` representation of the path into a JSON object. For example, for
    * the JSON `{"a": [1,2]}`, a cursor pointing at `2` would yield a
    * `String`of the form `"a[1]"`.
    */
  def asPathString: String =
    PathToRoot.toPathString(this)
}

private[circe] object PathToRoot {
  val empty: PathToRoot = PathToRoot(List.empty)

  /** Used to describe the breadcrumbs back to the root of the JSON.
    */
  sealed abstract class PathElem extends Product with Serializable

  object PathElem {
    final case class ObjectKey(keyName: String) extends PathElem
    final case class ArrayIndex(index: Int) extends PathElem
  }

  /** Convert a [[PathToRoot]] into a `String` representation of a path into a
    * JSON object. For example, for the JSON `{"a": [1,2]}`, a cursor pointing
    * at `2` would yield a `String`of the form `"a[1]"`.
    */
  def toPathString(path: PathToRoot): String = {
    import PathElem._

    if (path.value.isEmpty) {
      ""
    } else {
      path.value.foldLeft((new StringBuilder(path.value.size * 5), true )){
        case ((sb, isFirst), ObjectKey(keyName)) if isFirst =>
          (sb.append(keyName), false)
        case ((sb, _), ObjectKey(keyName)) =>
          (sb.append(".").append(keyName), false)
        case ((sb, _), ArrayIndex(index)) =>
          (sb.append("[").append(index.toString).append("]"), false)
      }._1.toString
    }
  }
}
