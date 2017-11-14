package com.thenewmotion.ocpi.msgs.shapeless

import com.thenewmotion.ocpi.msgs.{Resource, ResourceType}
import com.thenewmotion.ocpi.msgs.ResourceType.{Full, Patch}
import scala.annotation.implicitNotFound
import scala.language.higherKinds
import shapeless._
import shapeless.ops.hlist.{IsHCons, Mapper, Zip}

@implicitNotFound("It is not possible to merge ${P} into ${F}")
trait ResourceMerge[F, P] {
  def apply(t: F, u: P): F
}

object ResourceMerge {

  protected object tupleMerger extends Poly1 {
    implicit def atTuple[A] = at[(A, A)] {
      case (b, _) => b
    }

    implicit def atOptRightTuple[A] = at[(A, Option[A])] {
      case (_, Some(a)) => a
      case (o, None)    => o
    }

    implicit def atOptBothTuple[A] = at[(Option[A], Option[A])] {
      case (_, b @ Some(_)) => b
      case (o, None)        => o
    }
  }

  implicit def mergeAnything[
    F <: Resource[Full],
    P <: Resource[Patch],
    PA <: HList,
    FA <: HList,
    FAH,
    FAT <: HList,
    Z <: HList
  ](
    implicit
    fAux: Generic.Aux[F, FA],
    pAux: Generic.Aux[P, PA],
    evHead: IsHCons.Aux[FA, FAH, FAT],
    zipper: Zip.Aux[FA :: (FAH :: PA) :: HNil, Z],
    mapper: Mapper.Aux[tupleMerger.type, Z, FA],
  ): ResourceMerge[F, P] = (t: F, patch: P) => {
    val reprFull: FA = fAux.to(t)
    val reprPatch: (FAH :: PA) = reprFull.head :: pAux.to(patch)

    val zipped: Z = reprFull.zip(reprPatch)
    val merged: FA = zipped.map(tupleMerger)

    fAux.from(merged)
  }
}

object mergeSyntax {
  implicit class MergeSyntax[F <: Resource[Full], B[RT <: ResourceType] >: Resource[RT]](t: F) {
    def merge[P <: B[Patch]](patch: P)(implicit merge: ResourceMerge[F, P]): F = merge(t, patch)
  }
}
