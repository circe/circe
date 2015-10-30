package io.circe

package object optics {
  def `*`: JsonPath = JsonPath.root
}

package optics {
  object all extends JsonNumberOptics with JsonObjectOptics with JsonOptics
}
