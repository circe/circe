package io.circe.refined.info.instances

import io.circe.refined.info.TypeInfo

object EmptyTypeInfo extends TypeInfo[Any] {
  override val describe: String = ""
}
