package com.thenewmotion.ocpi


trait Enumerable[T <: Nameable] {
  def values: Seq[T]
  def withName(name: String): Option[T] = values.find(_.name equals name)
}
