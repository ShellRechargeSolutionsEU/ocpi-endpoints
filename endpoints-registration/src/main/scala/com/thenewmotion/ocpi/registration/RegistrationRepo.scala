package com.thenewmotion.ocpi
package registration

import msgs.Ownership.Theirs
import msgs.Versions.{Endpoint, VersionNumber}
import msgs.v2_1.Credentials.Creds
import msgs.{AuthToken, GlobalPartyId}

trait RegistrationRepo[F[_]] {

  def isPartyRegistered(
    globalPartyId: GlobalPartyId
  ): F[Boolean]

  def findTheirAuthToken(
    globalPartyId: GlobalPartyId
  ): F[Option[AuthToken[Theirs]]]

  // Called after a 3rd party has called our credentials endpoint with a POST or a PUT
  def persistInfoAfterConnectToUs(
    globalPartyId: GlobalPartyId,
    version: VersionNumber,
    token: AuthToken[Theirs],
    creds: Creds[Theirs],
    endpoints: Iterable[Endpoint]
  ): F[Unit]

  // Called after _we_ start the registration by calling _their_ credentials endpoint with a POST or a PUT
  def persistInfoAfterConnectToThem(
    version: VersionNumber,
    token: AuthToken[Theirs],
    creds: Creds[Theirs],
    endpoints: Iterable[Endpoint]
  ): F[Unit]

  def deletePartyInformation(
    globalPartyId: GlobalPartyId
  ): F[Unit]
}
