package com.thenewmotion.ocpi
package common

import akka.http.scaladsl.server.{Directive0, PathMatcher1}
import akka.http.scaladsl.server.PathMatchers.Segment
import msgs.GlobalPartyId
import akka.http.scaladsl.server.Directives._
import com.thenewmotion.ocpi.msgs.Versions.VersionNumber

trait OcpiDirectives {
  val GlobalPartyIdMatcher: PathMatcher1[GlobalPartyId] = (Segment / Segment).tmap {
    case (cc, p) => Tuple1(GlobalPartyId(cc, p))
  }

  def authPathPrefixGlobalPartyIdEquality(apiUser: GlobalPartyId): Directive0 =
    pathPrefix(GlobalPartyIdMatcher).tflatMap { (urlUser: Tuple1[GlobalPartyId]) =>
      authorize(apiUser == urlUser._1)
    }

  val AnyVersionMatcher: PathMatcher1[VersionNumber] = Segment.map(VersionNumber(_))

  def VersionMatcher(validVersions: Set[VersionNumber]) = AnyVersionMatcher.flatMap {
    case x if validVersions.contains(x) => Some(x)
    case _ => None
  }
}
