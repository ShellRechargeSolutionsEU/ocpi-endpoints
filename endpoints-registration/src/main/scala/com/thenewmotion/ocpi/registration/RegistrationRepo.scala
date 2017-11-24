package com.thenewmotion.ocpi
package registration

import msgs.Ownership.Theirs
import msgs.Versions.{Endpoint, VersionNumber}
import msgs.v2_1.Credentials.Creds
import msgs.{GlobalPartyId, AuthToken}

import scala.concurrent.Future

trait RegistrationRepo {

  def isPartyRegistered(
    globalPartyId: GlobalPartyId
  ): Future[Boolean]

  def findTheirAuthToken(
    globalPartyId: GlobalPartyId
  ): Future[Option[AuthToken[Theirs]]]

  // Called after a 3rd party has called our credentials endpoint with a POST or a PUT
  def persistInfoAfterConnectToUs(
    globalPartyId: GlobalPartyId,
    version: VersionNumber,
    token: AuthToken[Theirs],
    creds: Creds[Theirs],
    endpoints: Iterable[Endpoint]
  ): Future[Unit]

  // Called after _we_ start the registration by calling _their_ credentials endpoint with a POST or a PUT
  def persistInfoAfterConnectToThem(
    version: VersionNumber,
    token: AuthToken[Theirs],
    creds: Creds[Theirs],
    endpoints: Iterable[Endpoint]
  ): Future[Unit]

  def deletePartyInformation(
    globalPartyId: GlobalPartyId
  ): Future[Unit]
}
