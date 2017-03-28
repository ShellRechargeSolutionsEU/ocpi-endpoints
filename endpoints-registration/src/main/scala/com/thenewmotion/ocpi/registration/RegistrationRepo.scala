package com.thenewmotion.ocpi
package registration

import com.thenewmotion.ocpi.msgs.Ownership.Theirs
import com.thenewmotion.ocpi.msgs.Versions.{Endpoint, VersionNumber}
import com.thenewmotion.ocpi.msgs.v2_1.Credentials.Creds
import com.thenewmotion.ocpi.msgs.{GlobalPartyId, AuthToken}

import scala.concurrent.{Future, ExecutionContext}

trait RegistrationRepo {

  def isPartyRegistered(
    globalPartyId: GlobalPartyId
  )(implicit ec: ExecutionContext): Future[Boolean]

  def findTheirAuthToken(
    globalPartyId: GlobalPartyId
  )(implicit ec: ExecutionContext): Future[Option[AuthToken[Theirs]]]

  // Called after a 3rd party has called our credentials endpoint with a POST
  def persistNewCredsResult(
    globalPartyId: GlobalPartyId,
    version: VersionNumber,
    token: AuthToken[Theirs],
    creds: Creds[Theirs],
    endpoints: Iterable[Endpoint]
  )(implicit ec: ExecutionContext): Future[Unit]

  // Called after a 3rd party has called our credentials endpoint with a PUT
  def persistUpdateCredsResult(
    globalPartyId: GlobalPartyId,
    version: VersionNumber,
    token: AuthToken[Theirs],
    creds: Creds[Theirs],
    endpoints: Iterable[Endpoint]
  )(implicit ec: ExecutionContext): Future[Unit]

  // Called after _we_ start the registration by calling _their_ credentials endpoint with a POST
  // Called after _we_ update the registration by calling _their_ credentials endpoint with a PUT
  def persistRegistrationResult(
    version: VersionNumber,
    token: AuthToken[Theirs],
    creds: Creds[Theirs],
    endpoints: Iterable[Endpoint]
  )(implicit ec: ExecutionContext): Future[Unit]

  def deletePartyInformation(
    globalPartyId: GlobalPartyId
  )(implicit ec: ExecutionContext): Future[Unit]
}
