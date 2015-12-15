package com.thenewmotion.ocpi.locations

import com.thenewmotion.ocpi.msgs.v2_0.Locations._
import scalaz._

case class CpoId(countryCode: String, partyId: String)

trait MspLocationsService {

  def createLocation(cpo: CpoId, locId: String, loc: Location): LocationsError \/ Unit

  def addEvse(cpo: CpoId, locId: String, evseId: String, evse: Evse): LocationsError \/ Unit

  def addConnector(cpo: CpoId, locId: String, evseId: String, connId: String, connector: Connector): LocationsError \/ Unit

  def updateLocation(cpo: CpoId, locId: String, locPatch: LocationPatch): LocationsError \/ Unit

  def updateEvse(cpo: CpoId, locId: String, evseId: String, evsePatch: EvsePatch): LocationsError \/ Unit

  def updateConnector(cpo: CpoId, locId: String, evseId: String, connId: String, connectorPatch: ConnectorPatch): LocationsError \/ Unit

  def location(cpo: CpoId, locId: String): LocationsError \/ Location

  def evse(cpo: CpoId, locId: String, evseId: String): LocationsError \/ Evse

  def connector(cpo: CpoId, locId: String, evseId: String, connectorId: String): LocationsError \/ Connector

}
