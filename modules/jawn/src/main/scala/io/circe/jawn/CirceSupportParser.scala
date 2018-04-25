package io.circe.jawn

import io.circe.Json
import jawn.{ RawFacade, SupportParser }

final object CirceSupportParser extends SupportParser[Json] {
  implicit final val facade: RawFacade[Json] = new CirceFacade()
}
