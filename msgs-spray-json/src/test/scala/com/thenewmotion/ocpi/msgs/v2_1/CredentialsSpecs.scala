package com.thenewmotion.ocpi.msgs
package v2_1

import Credentials._
import Ownership.{Ours, Theirs}
import CommonTypes._
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope
import spray.json._

class CredentialsSpecs extends SpecificationWithJUnit {

  import DefaultJsonProtocol._
  import CredentialsJsonProtocol._

  "CredentialsResp" should {
    "deserialize" in new CredentialsTestScope {
      credentialsJson1.convertTo[Creds[Ours]] mustEqual credentials1
    }
    "serialize" in new CredentialsTestScope {
      credentials1.toJson mustEqual credentialsJson1
    }
  }


  private trait CredentialsTestScope extends Scope {
    val businessDetails1 = BusinessDetails(
      "Example Operator",
      Some(Image(Url("http://example.com/images/logo.png"), ImageCategory.Operator, "png")),
      Some(Url("http://example.com"))
    )
    val credentials1 = Creds[Ours](
      token = AuthToken[Theirs]("ebf3b399-779f-4497-9b9d-ac6ad3cc44d2"),
      url = Url("https://example.com/ocpi/cpo/"),
      businessDetails = businessDetails1,
      globalPartyId = GlobalPartyId("NL", "EXA")
    )

    val logo1 = businessDetails1.logo.get
    val credentialsJson1 =
      s"""
         |{
         |    "token": "${credentials1.token.value}",
         |    "url": "${credentials1.url}",
         |    "business_details": {
         |        "name": "${credentials1.businessDetails.name}",
         |        "logo": {
         |          "url": "${logo1.url}",
         |          "category": "${logo1.category.name}",
         |          "type": "${logo1.`type`}"
         |        },
         |        "website": "${credentials1.businessDetails.website.get}"
         |    },
         |    "party_id": "${credentials1.globalPartyId.partyId}",
         |    "country_code": "${credentials1.globalPartyId.countryCode}"
         |}
     """.stripMargin.parseJson

  }
}
