package com.thenewmotion.ocpi
package locations

import cats.Applicative
import com.thenewmotion.ocpi.common.CreateOrUpdateResult
import msgs.GlobalPartyId
import msgs.v2_1.Locations._
import cats.syntax.either._
import cats.syntax.option._
import com.thenewmotion.ocpi.locations.LocationsError.IncorrectLocationId

/**
  * All methods are to be implemented in an idempotent fashion.
  */
trait MspLocationsService[F[_]] {

  protected[locations] def createOrUpdateLocation(
    apiUser: GlobalPartyId,
    locId: LocationId,
    loc: Location
  )(
    implicit A: Applicative[F]
  ): F[Either[LocationsError, CreateOrUpdateResult]] = {
    if (loc.id == locId) {
      createOrUpdateLocation(apiUser, loc)
    } else
      Applicative[F].pure(
        IncorrectLocationId(s"Token id from Url is $locId, but id in JSON body is ${loc.id}".some).asLeft
      )
  }

  def createOrUpdateLocation(
    globalPartyId: GlobalPartyId,
    loc: Location
  ): F[Either[LocationsError, CreateOrUpdateResult]]

  def addOrUpdateEvse(
    globalPartyId: GlobalPartyId,
    locId: LocationId,
    evseUid: EvseUid,
    evse: Evse
  ): F[Either[LocationsError, CreateOrUpdateResult]]

  def addOrUpdateConnector(
    globalPartyId: GlobalPartyId,
    locId: LocationId,
    evseUid: EvseUid,
    connId: ConnectorId,
    connector: Connector
  ): F[Either[LocationsError, CreateOrUpdateResult]]

  def updateLocation(
    globalPartyId: GlobalPartyId,
    locId: LocationId,
    locPatch: LocationPatch
  ): F[Either[LocationsError, Unit]]

  def updateEvse(
    globalPartyId: GlobalPartyId,
    locId: LocationId,
    evseUid: EvseUid,
    evsePatch: EvsePatch
  ): F[Either[LocationsError, Unit]]

  def updateConnector(
    globalPartyId: GlobalPartyId,
    locId: LocationId,
    evseUid: EvseUid,
    connId: ConnectorId,
    connectorPatch: ConnectorPatch
  ): F[Either[LocationsError, Unit]]

  def location(
    globalPartyId: GlobalPartyId,
    locId: LocationId
  ): F[Either[LocationsError, Location]]

  def evse(
    globalPartyId: GlobalPartyId,
    locId: LocationId,
    evseUid: EvseUid
  ): F[Either[LocationsError, Evse]]

  def connector(
    globalPartyId: GlobalPartyId,
    locId: LocationId,
    evseUid: EvseUid,
    connectorId: ConnectorId
  ): F[Either[LocationsError, Connector]]

}
