package com.thenewmotion.ocpi
package locations

import msgs.{CountryCode, PartyId}
import msgs.v2_1.Locations._
import scala.concurrent.Future
import scalaz._

/**
  * All methods are to be implemented in an idempotent fashion.
  */
trait MspLocationsService {

  /**
    * @return true if the location has been created and false if it has been updated
    */
  def createOrUpdateLocation(cc: CountryCode, operatorId: PartyId, locId: String,
    loc: Location): Future[LocationsError \/ Boolean]

  /**
    * @return true if the EVSE has been added and false if it has been updated
    */
  def addOrUpdateEvse(cc: CountryCode, operatorId: PartyId, locId: String, evseId: String,
    evse: Evse): Future[LocationsError \/ Boolean]

  /**
    * @return true if the Connector has been added and false if it has been updated
    */
  def addOrUpdateConnector(cc: CountryCode, operatorId: PartyId, locId: String, evseId: String,
    connId: String, connector: Connector): Future[LocationsError \/ Boolean]

  def updateLocation(cc: CountryCode, operatorId: PartyId, locId: String,
    locPatch: LocationPatch): Future[LocationsError \/ Unit]

  def updateEvse(cc: CountryCode, operatorId: PartyId, locId: String, evseId: String,
    evsePatch: EvsePatch): Future[LocationsError \/ Unit]

  def updateConnector(cc: CountryCode, operatorId: PartyId, locId: String, evseId: String,
    connId: String, connectorPatch: ConnectorPatch): Future[LocationsError \/ Unit]

  def location(cc: CountryCode, operatorId: PartyId,
    locId: String): Future[LocationsError \/ Location]

  def evse(cc: CountryCode, operatorId: PartyId, locId: String,
    evseId: String): Future[LocationsError \/ Evse]

  def connector(cc: CountryCode, operatorId: PartyId, locId: String, evseId: String,
    connectorId: String): Future[LocationsError \/ Connector]

}
