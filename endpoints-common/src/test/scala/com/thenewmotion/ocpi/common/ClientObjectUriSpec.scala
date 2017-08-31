package com.thenewmotion.ocpi.common

import akka.http.scaladsl.model.Uri
import org.specs2.mutable.Specification

class ClientObjectUriSpec extends Specification {

  "Client object URI" >> {
    "can contain a number of extra fields" >> {
      val countryCode = "NL"
      val partyId = "TNM"
      val endpoint = Uri("http://localhost/locations")

      ClientObjectUri(endpoint, countryCode, partyId, "12345").value ===
        Uri("http://localhost/locations/NL/TNM/12345")

      ClientObjectUri(endpoint, countryCode, partyId, "111", "222", "333").value ===
        Uri("http://localhost/locations/NL/TNM/111/222/333")
    }
  }
}
