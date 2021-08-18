package io.circe.derivation

import java.util.regex.Pattern

object renaming:
  private val basePattern: Pattern = Pattern.compile("([A-Z]+)([A-Z][a-z])")
  private val swapPattern: Pattern = Pattern.compile("([a-z\\d])([A-Z])")

  val snakeCase: String => String = { s =>
    val partial = basePattern.matcher(s).replaceAll("$1_$2")
    swapPattern.matcher(partial).replaceAll("$1_$2").toLowerCase
  }
  
  val screamingSnakeCase: String => String = { s =>
    val partial = basePattern.matcher(s).replaceAll("$1_$2")
    swapPattern.matcher(partial).replaceAll("$1_$2").toUpperCase
  }
  
  val kebabCase: String => String = { s =>
    val partial = basePattern.matcher(s).replaceAll("$1-$2")
    swapPattern.matcher(partial).replaceAll("$1-$2").toLowerCase
  }

  final def replaceWith(pairs: (String, String)*): String => String =
    original => pairs.toMap.getOrElse(original, original)
