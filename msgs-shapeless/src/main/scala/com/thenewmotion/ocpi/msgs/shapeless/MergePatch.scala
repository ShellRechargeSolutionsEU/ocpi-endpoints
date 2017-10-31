package com.thenewmotion.ocpi.msgs.shapeless

import com.thenewmotion.ocpi.msgs.v2_1.Locations._
import com.thenewmotion.ocpi.msgs.v2_1.Sessions.{Session, SessionPatch}
import com.thenewmotion.ocpi.msgs.v2_1.Tokens._
import shapeless.{Generic, Poly1}

import scala.annotation.implicitNotFound

trait ResourceMerge[F, P] {
  def apply(t: F, u: P): F
}

object ResourceMerge {
  protected object tupleMerger extends Poly1 {
    implicit def atTuple[A] = at[(A, Option[A])] {
      case (_, Some(a)) => a
      case (o, None)    => o
    }

    implicit def atOptTuple[A] = at[(Option[A], Option[A])] {
      case (_, b @ Some(_)) => b
      case (o, None)        => o
    }
  }

  implicit case object MergeToken extends ResourceMerge[Token, TokenPatch] {
    def apply(t: Token, p: TokenPatch): Token = {
      val genFull = Generic[Token]
      val genPatch = Generic[TokenPatch]

      val reprFull = genFull.to(t)
      val reprPatch = genPatch.to(p)

      genFull.from(
        reprFull.zip(reprPatch).map(tupleMerger)
      )
    }
  }

  implicit case object MergeConnector extends ResourceMerge[Connector, ConnectorPatch] {
    def apply(c: Connector, p: ConnectorPatch): Connector = {
      val genFull = Generic[Connector]
      val genPatch = Generic[ConnectorPatch]

      val reprFull = genFull.to(c)
      val reprPatch = genPatch.to(p)

      genFull.from(
        reprFull.zip(reprPatch).map(tupleMerger)
      )
    }
  }

  implicit case object MergeEvse extends ResourceMerge[Evse, EvsePatch] {
    def apply(c: Evse, p: EvsePatch): Evse = {
      val genFull = Generic[Evse]
      val genPatch = Generic[EvsePatch]

      val reprFull = genFull.to(c)
      val reprPatch = genPatch.to(p)

      genFull.from(
        reprFull.zip(reprPatch).map(tupleMerger)
      )
    }
  }

  implicit case object MergeLocation extends ResourceMerge[Location, LocationPatch] {
    def apply(c: Location, p: LocationPatch): Location = {
      val genFull = Generic[Location]
      val genPatch = Generic[LocationPatch]

      val reprFull = genFull.to(c)
      val reprPatch = genPatch.to(p)

      genFull.from(
        reprFull.zip(reprPatch).map(tupleMerger)
      )
    }
  }

  implicit case object MergeSession extends ResourceMerge[Session, SessionPatch] {
    def apply(c: Session, p: SessionPatch): Session = {
      val genFull = Generic[Session]
      val genPatch = Generic[SessionPatch]

      val reprFull = genFull.to(c)
      val reprPatch = genPatch.to(p)

      genFull.from(
        reprFull.zip(reprPatch).map(tupleMerger)
      )
    }
  }
}

object mergeSyntax {
  implicit class MergeSyntax[F](t: F) {
    @implicitNotFound("It is not possible to merge {P} into {F}")
    def merge[P](patch: P)(implicit merge: ResourceMerge[F, P]): F = merge(t, patch)
  }
}
