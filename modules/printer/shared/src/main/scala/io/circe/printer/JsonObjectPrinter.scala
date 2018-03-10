package io.circe.printer

import io.circe.JsonObject.{LinkedHashMapJsonObject, MapAndVectorJsonObject}

object JsonObjectPrinter {
  final def appendToFolder(jsonObject: LinkedHashMapJsonObject, folder: Printer.PrintingFolder): Unit = {
    val originalDepth = folder.depth
    val p = folder.pieces(folder.depth)
    var first = true
    val iterator = jsonObject.underlying.entrySet.iterator

    folder.writer.append(p.lBraces)

    while (iterator.hasNext) {
      val next = iterator.next()
      val key = next.getKey
      val value = next.getValue

      if (!folder.dropNullValues || !value.isNull) {
        if (!first) folder.writer.append(p.objectCommas)
        folder.onString(key)
        folder.writer.append(p.colons)

        folder.depth += 1
        value.foldWith(folder)
        folder.depth = originalDepth
        first = false
      }
    }

    folder.writer.append(p.rBraces)
  }

  final def appendToFolder(jsonObject: MapAndVectorJsonObject,folder: Printer.PrintingFolder): Unit = {
    val originalDepth = folder.depth
    val p = folder.pieces(folder.depth)
    var first = true
    val keyIterator = jsonObject.underlyingKeys.iterator

    folder.writer.append(p.lBraces)

    while (keyIterator.hasNext) {
      val key = keyIterator.next()
      val value = jsonObject.underlying(key)
      if (!folder.dropNullValues || !value.isNull) {
        if (!first) folder.writer.append(p.objectCommas)
        folder.onString(key)
        folder.writer.append(p.colons)

        folder.depth += 1
        value.foldWith(folder)
        folder.depth = originalDepth
        first = false
      }
    }

    folder.writer.append(p.rBraces)
  }


}
