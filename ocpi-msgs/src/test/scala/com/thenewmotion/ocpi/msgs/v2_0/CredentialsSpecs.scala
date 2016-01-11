package com.thenewmotion.ocpi.msgs.v2_0

import com.thenewmotion.ocpi.msgs.v2_0.Credentials.Creds
import CommonTypes.BusinessDetails
import Credentials._
import OcpiJsonProtocol._
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope
import spray.json._

class CredentialsSpecs extends SpecificationWithJUnit {


  "CredentialsResp" should {
    "deserialize" in new CredentialsTestScope {
      credentialsJson1.convertTo[Creds] mustEqual credentials1
    }
    "serialize" in new CredentialsTestScope {
      credentials1.toJson.toString mustEqual credentialsJson1.compactPrint
    }
  }


  private trait CredentialsTestScope extends Scope {

    val credentials1 = Creds(
      token = "ebf3b399-779f-4497-9b9d-ac6ad3cc44d2",
      url = "https://example.com/ocpi/cpo/",
      business_details = BusinessDetails(
        "Example Operator",
        Some("http://example.com/images/logo.png"),
        Some("http://example.com")
      ),
      party_id = "EXA",
      country_code = "NL"
    )


    val credentialsJson1 =
      s"""
         |{
         |    "token": "${credentials1.token}",
         |    "url": "${credentials1.url}",
         |    "business_details": {
         |        "name": "${credentials1.business_details.name}",
         |        "logo": "${credentials1.business_details.logo.get}",
         |        "website": "${credentials1.business_details.website.get}"
         |    },
         |    "party_id": "${credentials1.party_id}",
         |    "country_code": "${credentials1.country_code}"
         |}
     """.stripMargin.parseJson





  }
}
