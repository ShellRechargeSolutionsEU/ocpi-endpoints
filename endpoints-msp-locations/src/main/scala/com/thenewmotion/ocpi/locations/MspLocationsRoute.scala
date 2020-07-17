package com.thenewmotion.ocpi
package locations

import _root_.akka.http.scaladsl.marshalling.ToResponseMarshaller
import _root_.akka.http.scaladsl.model.StatusCode
import _root_.akka.http.scaladsl.model.StatusCodes._
import _root_.akka.http.scaladsl.server.Route
import _root_.akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import cats.Applicative
import com.thenewmotion.ocpi.common._
import com.thenewmotion.ocpi.locations.LocationsError._
import com.thenewmotion.ocpi.msgs.OcpiStatusCode._
import com.thenewmotion.ocpi.msgs.{ErrorResp, _}
import com.thenewmotion.ocpi.msgs.v2_1.Locations._

object MspLocationsRoute {
  def apply[F[_]: Applicative: HktMarshallable](
    service: MspLocationsService[F]
  )(
    implicit locationU: FromEntityUnmarshaller[Location],
    locationPU: FromEntityUnmarshaller[LocationPatch],
    evseU: FromEntityUnmarshaller[Evse],
    evsePU: FromEntityUnmarshaller[EvsePatch],
    connectorU: FromEntityUnmarshaller[Connector],
    connectorPU: FromEntityUnmarshaller[ConnectorPatch],
    errorM: ErrRespMar,
    successUnitM: SuccessRespMar[Unit],
    successLocM: SuccessRespMar[Location],
    successEvseM: SuccessRespMar[Evse],
    successConnectorM: SuccessRespMar[Connector]
  ): MspLocationsRoute[F] = new MspLocationsRoute(service)
}

class MspLocationsRoute[F[_]: Applicative: HktMarshallable] private[ocpi](
  service: MspLocationsService[F]
)(
  implicit locationU: FromEntityUnmarshaller[Location],
  locationPU: FromEntityUnmarshaller[LocationPatch],
  evseU: FromEntityUnmarshaller[Evse],
  evsePU: FromEntityUnmarshaller[EvsePatch],
  connectorU: FromEntityUnmarshaller[Connector],
  connectorPU: FromEntityUnmarshaller[ConnectorPatch],
  errorM: ErrRespMar,
  successUnitM: SuccessRespMar[Unit],
  successLocM: SuccessRespMar[Location],
  successEvseM: SuccessRespMar[Evse],
  successConnectorM: SuccessRespMar[Connector]
) extends EitherUnmarshalling
    with OcpiDirectives {

  implicit def locationsErrorResp(
    implicit em: ToResponseMarshaller[(StatusCode, ErrorResp)]
  ): ToResponseMarshaller[LocationsError] = {
    em.compose[LocationsError] { locationsError =>
      val statusCode = locationsError match {
        case _: LocationNotFound | _: EvseNotFound | _: ConnectorNotFound                   => NotFound
        case _: LocationCreationFailed | _: EvseCreationFailed | _: ConnectorCreationFailed => OK
        case _                                                                              => InternalServerError
      }
      statusCode -> ErrorResp(GenericClientFailure, locationsError.reason)
    }
  }

  def apply(
    apiUser: GlobalPartyId
  ): Route =
    handleRejections(OcpiRejectionHandler.Default)(routeWithoutRh(apiUser))

  private val LocationIdSegment = Segment.map(LocationId(_))
  private val EvseUidSegment = Segment.map(EvseUid(_))
  private val ConnectorIdSegment = Segment.map(ConnectorId(_))

  import HktMarshallableSyntax._

  private[locations] def routeWithoutRh(
    apiUser: GlobalPartyId
  ) = {
    (authPathPrefixGlobalPartyIdEquality(apiUser) & pathPrefix(LocationIdSegment)) { locId =>
      pathEndOrSingleSlash {
        put {
          entity(as[Location]) { location =>
            complete {
              service.createOrUpdateLocation(apiUser, locId, location).mapRight { x =>
                (x.httpStatusCode, SuccessResp(GenericSuccess))
              }
            }
          }
        } ~
        patch {
          entity(as[LocationPatch]) { location =>
            complete {
              service.updateLocation(apiUser, locId, location).mapRight { _ =>
                SuccessResp(GenericSuccess)
              }
            }
          }
        } ~
        get {
          complete {
            service.location(apiUser, locId).mapRight { location =>
              SuccessResp(GenericSuccess, None, data = location)
            }
          }
        }
      } ~
      pathPrefix(EvseUidSegment) { evseId =>
        pathEndOrSingleSlash {
          put {
            entity(as[Evse]) { evse =>
              complete {
                service.addOrUpdateEvse(apiUser, locId, evseId, evse).mapRight { x =>
                  (x.httpStatusCode, SuccessResp(GenericSuccess))
                }
              }
            }
          } ~
          patch {
            entity(as[EvsePatch]) { evse =>
              complete {
                service.updateEvse(apiUser, locId, evseId, evse).mapRight { _ =>
                  SuccessResp(GenericSuccess)
                }
              }
            }
          } ~
          get {
            complete {
              service.evse(apiUser, locId, evseId).mapRight { evse =>
                SuccessResp(GenericSuccess, None, data = evse)
              }
            }
          }
        } ~
        (path(ConnectorIdSegment) & pathEndOrSingleSlash) { connId =>
          put {
            entity(as[Connector]) { conn =>
              complete {
                service.addOrUpdateConnector(apiUser, locId, evseId, connId, conn).mapRight { x =>
                  (x.httpStatusCode, SuccessResp(GenericSuccess))
                }
              }
            }
          } ~
          patch {
            entity(as[ConnectorPatch]) { conn =>
              complete {
                service.updateConnector(apiUser, locId, evseId, connId, conn).mapRight { _ =>
                  SuccessResp(GenericSuccess)
                }
              }
            }
          } ~
          get {
            complete {
              service.connector(apiUser, locId, evseId, connId).mapRight { connector =>
                SuccessResp(GenericSuccess, data = connector)
              }
            }
          }
        }
      }
    }
  }
}
