package com.thenewmotion.ocpi


trait Nameable {
  def name: String
  override def toString = name.toString
}
