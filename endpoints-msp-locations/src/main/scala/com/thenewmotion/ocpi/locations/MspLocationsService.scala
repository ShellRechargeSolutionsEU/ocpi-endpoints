package com.thenewmotion.ocpi
package locations

import msgs.GlobalPartyId
import msgs.v2_1.Locations._
import scala.concurrent.Future

/**
  * All methods are to be implemented in an idempotent fashion.
  */
trait MspLocationsService {

  /**
    * @return true if the location has been created and false if it has been updated
    */
  def createOrUpdateLocation(
    globalPartyId: GlobalPartyId,
    locId: LocationId,
    loc: Location
  ): Future[Either[LocationsError, Boolean]]

  /**
    * @return true if the EVSE has been added and false if it has been updated
    */
  def addOrUpdateEvse(
    globalPartyId: GlobalPartyId,
    locId: LocationId,
    evseUid: EvseUid,
    evse: Evse
  ): Future[Either[LocationsError, Boolean]]

  /**
    * @return true if the Connector has been added and false if it has been updated
    */
  def addOrUpdateConnector(
    globalPartyId: GlobalPartyId,
    locId: LocationId,
    evseUid: EvseUid,
    connId: ConnectorId,
    connector: Connector
  ): Future[Either[LocationsError, Boolean]]

  def updateLocation(
    globalPartyId: GlobalPartyId,
    locId: LocationId,
    locPatch: LocationPatch
  ): Future[Either[LocationsError, Unit]]

  def updateEvse(
    globalPartyId: GlobalPartyId,
    locId: LocationId,
    evseUid: EvseUid,
    evsePatch: EvsePatch
  ): Future[Either[LocationsError, Unit]]

  def updateConnector(
    globalPartyId: GlobalPartyId,
    locId: LocationId,
    evseUid: EvseUid,
    connId: ConnectorId,
    connectorPatch: ConnectorPatch
  ): Future[Either[LocationsError, Unit]]

  def location(
    globalPartyId: GlobalPartyId,
    locId: LocationId
  ): Future[Either[LocationsError, Location]]

  def evse(
    globalPartyId: GlobalPartyId,
    locId: LocationId,
    evseUid: EvseUid
  ): Future[Either[LocationsError, Evse]]

  def connector(
    globalPartyId: GlobalPartyId,
    locId: LocationId,
    evseUid: EvseUid,
    connectorId: ConnectorId
   ): Future[Either[LocationsError, Connector]]

}
