package com.thenewmotion.ocpi.msgs.sprayjson.v2_1

import com.thenewmotion.ocpi.msgs.Versions
import spray.json.{JsString, JsValue, JsonFormat, deserializationError}
import Versions._
import DefaultJsonProtocol._

trait VersionsJsonProtocol {

  implicit val endpointIdentifierFormat = new JsonFormat[EndpointIdentifier] {
    override def write(obj: EndpointIdentifier) = JsString(obj.value)

    override def read(json: JsValue) = json match {
      case JsString(s) => EndpointIdentifier(s)
      case x => deserializationError(s"Expected EndpointIdentifier as JsString, but got $x")
    }
  }

  implicit val versionNumberFormat = new JsonFormat[VersionNumber] {
    override def write(obj: VersionNumber) = JsString(obj.toString)

    override def read(json: JsValue) = json match {
      case JsString(s) => VersionNumber(s)
      case x => deserializationError(s"Expected VersionNumber as JsString, but got $x")
    }
  }

  implicit val versionFormat = jsonFormat2(Version)
  implicit val endpointFormat = jsonFormat2(Endpoint)
  implicit val versionDetailsFormat = jsonFormat2(VersionDetails)
}

object VersionsJsonProtocol extends VersionsJsonProtocol
