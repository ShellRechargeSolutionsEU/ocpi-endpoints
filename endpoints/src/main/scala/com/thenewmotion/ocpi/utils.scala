package com.thenewmotion.ocpi

/**
 *
 */

  trait Nameable {
    val name: String
  }

  trait Enumerable[T <: Nameable] {
    def values: Seq[T]
    def withName(name: String): Option[T] = values.find(_.name equals name)
  }

