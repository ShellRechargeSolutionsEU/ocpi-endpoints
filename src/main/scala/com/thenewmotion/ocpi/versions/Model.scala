package com.thenewmotion.ocpi.versions

import com.thenewmotion.ocpi.{Enumerable, Nameable}



  case class Endpoint(
    endpointType: EndpointType,
    version: String,
    url:  String
    )

  sealed trait EndpointType extends Nameable
  object EndpointTypeEnum extends Enumerable[EndpointType] {
    case object Locations extends EndpointType {val name = "locations"}
    case object Credentials extends EndpointType {val name = "credentials"}

    val values = List(Locations, Credentials)
  }
