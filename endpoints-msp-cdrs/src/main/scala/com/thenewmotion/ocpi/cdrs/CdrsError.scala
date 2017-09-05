package com.thenewmotion.ocpi
package cdrs

sealed trait CdrsError {
  def reason: Option[String]
}

object CdrsError {
  case class CdrNotFound(reason: Option[String] = None) extends CdrsError
  case class CdrCreationFailed(reason: Option[String] = None) extends CdrsError
}
