package com.thenewmotion.ocpi.msgs

import com.thenewmotion.ocpi.msgs.v2_1.Id

import scala.language.higherKinds

sealed trait ResourceType {
  type F[_]
}

object ResourceType {
  trait Full extends ResourceType { type F[_] = Id[_] }
  trait Patch extends ResourceType { type F[_] = Option[_] }
}

trait Resource[RT <: ResourceType]
