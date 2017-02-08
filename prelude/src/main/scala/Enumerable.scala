package com.thenewmotion.ocpi


trait Enumerable[T <: Nameable] {
  def values: Iterable[T]
  def withName(name: String): Option[T] = values.find(_.name equals name)
}
