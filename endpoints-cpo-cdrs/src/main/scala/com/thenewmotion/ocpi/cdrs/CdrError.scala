package com.thenewmotion.ocpi.cdrs

trait CdrError {
  def reason: Option[String]
}