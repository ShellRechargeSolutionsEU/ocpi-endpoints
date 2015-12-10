package com.thenewmotion.ocpi.msgs.v2_0

import com.thenewmotion.money.CurrencyUnit
import Locations.CurrentTypeEnum.AC3Phases
import Locations._
import CommonTypes._
import org.joda.time.format.ISODateTimeFormat
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope
import spray.json._
import OcpiJsonProtocol._

class LocationsSpecs extends SpecificationWithJUnit {


  "Evses" should {
    "deserialize" in new LocationsTestScope {
      evseJson1.convertTo[Evse] mustEqual evse1
    }
    "serialize" in new LocationsTestScope {
      evse1.toJson.toString mustEqual evseJson1.compactPrint
    }
  }

   "LocationResp" should {
      "deserialize" in new LocationsTestScope {
         locationRespJson1.convertTo[LocationsData] mustEqual locationResp1
      }
      "serialize" in new LocationsTestScope {
         locationResp1.toJson.toString mustEqual locationRespJson1.compactPrint
      }
   }

  "Operator" should {
    "deserialize" in new LocationsTestScope {
      operatorJsonWithNulls1.convertTo[Operator] mustEqual operator1
    }
    "serialize" in new LocationsTestScope {
      operator1.toJson.toString mustEqual operatorJsonNoNulls1.compactPrint
    }
  }

  "Power" should {
    "deserialize" in new LocationsTestScope {
      powerJson1.convertTo[Power] mustEqual power1
    }
    "serialize" in new LocationsTestScope {
      power1.toJson.toString mustEqual powerJson1.compactPrint
    }
  }

  "PeriodTypeJsonFormat" should {
    val periodType: PeriodType = PeriodTypeEnum.Charging
    val str = "Charging"
    "serialize" in {
      periodType.toJson mustEqual JsString(str)
    }
    "extract" in {
      JsonParser("\"" + str + "\"").convertTo[PeriodType] mustEqual periodType
    }
  }


  private trait LocationsTestScope extends Scope {

    val formatter = ISODateTimeFormat.dateTimeNoMillis().withZoneUTC
    val date1 = formatter.parseDateTime("2010-01-01T00:00:00Z")
    val date2 = formatter.parseDateTime("2020-12-31T23:59:59Z")


    val displayText_standard = List(LocalizedText(Some("nl"), Some("Standaard Tarief")),
      LocalizedText(Some("en"), Some("Standard Tariff")) )
    val displayText_emsp =  List(LocalizedText(Some("nl"), Some("eMSP Tarief")),
      LocalizedText(Some("en"), Some("eMSP Tariff")) )

    val tariff_standard = Tariff(
      tariff_id = "kwrate",
      price_untaxed = Some(0.1936),
      price_taxed = None,
      pricing_unit = PricingUnitEnum.KWhToEV,
      tax_pct = None,
      currency = CurrencyUnit("EUR"),
      condition = None,
      display_text = displayText_standard
    )

    val tariff_emsp = Tariff(
      tariff_id = "kwrate",
      price_untaxed = Some(0.1536),
      price_taxed = None,
      pricing_unit = PricingUnitEnum.KWhToEV,
      tax_pct = None,
      currency = CurrencyUnit("EUR"),
      condition = None,
      display_text = displayText_emsp
    )

    val priceScheme1 = PriceScheme(
      1,
      Some(date1),
      Some(date2),
      Some(List(tariff_standard)),
      displayText_standard
    )

    val priceScheme2 = PriceScheme(
      2,
      Some(date1),
      Some(date2),
      Some(List(tariff_emsp)),
      displayText_emsp
    )

     val connector1 = Connector(
        "1",
        ConnectorTypeEnum.`IEC-62196-T2`,
        ConnectorFormatEnum.Cable,
        price_schemes = Some(List(priceScheme1,priceScheme2)),
        CurrentTypeEnum.AC3Phases,
        220,
        16,
        None
     )

    val connector2 = Connector(
      "2",
      ConnectorTypeEnum.`IEC-62196-T2`,
      ConnectorFormatEnum.Socket,
      price_schemes = Some(List(priceScheme1)),
      CurrentTypeEnum.AC3Phases,
      220,
      16,
      None
    )

    val evse1 = Evse(
      "BE-BEC-E041503001",
      "LOC1",
      ConnectorStatusEnum.Available,
      Some(List("RESERVABLE")),
      List(connector1,connector2),
      floor_level = Some("-1"),
      physical_number = Some("1")
    )

    val evse2 = Evse(
      "BE-BEC-E041503002",
      "LOC2",
      ConnectorStatusEnum.Available,
      Some(List("RESERVABLE")),
      List(connector1),
      floor_level = Some("-1"),
      physical_number = Some("1")
    )

    val evse3 = Evse(
      "BE-BEC-E041503003",
      "LOC2",
      ConnectorStatusEnum.Available,
      Some(List("RESERVABLE")),
      List(connector1),
      floor_level = Some("-1"),
      physical_number = Some("1")
    )

    val excp_open_begin = formatter.parseDateTime("2014-06-21T09:00:00+02:00")
    val excp_open_end = formatter.parseDateTime("2014-06-21T12:00:00+02:00")
    val excp_close_begin = formatter.parseDateTime("2014-06-24T00:00:00+02:00")
    val excp_close_end = formatter.parseDateTime("2014-06-25T00:00:00+02:00")

    val hours1 = Hours(
      regular_hours = List(
        RegularHours(1, "08:00", "20:00"),
        RegularHours(2, "08:00", "20:00"),
        RegularHours(3, "08:00", "20:00"),
        RegularHours(4, "08:00", "20:00"),
        RegularHours(5, "08:00", "20:00")
      ),
      twentyfourseven = false,
      exceptional_openings = List(
        ExceptionalPeriod(excp_open_begin, excp_open_end)),
      exceptional_closings = List(
        ExceptionalPeriod(excp_close_begin, excp_close_end)
      )
    )

    val location1 = Location(
      "LOC1",
      `type` = LocationTypeEnum.OnStreet,
      Some("Gent Zuid"),
      address = "F.Rooseveltlaan 3A",
      city = "Gent",
      postal_code = "9000",
      country = "BE",
      coordinates = GeoLocation("3.72994", "51.04759"),
      evses = Some(List(evse1)),
      directions = Some("left, left, left, right, left"), None,
      opening_times = Some(hours1),  None, None

    )

    val location2 = Location(
      "LOC2",
      `type` = LocationTypeEnum.OnStreet,
      Some("Gent Zuid"),
      address = "F.Rooseveltlaan 30",
      city = "Gent",
      postal_code = "9000",
      country = "BE",
      coordinates = GeoLocation("3.72995", "51.04760"),
      evses = Some(List(evse2,evse3)),
      directions = Some("left, left, left, right, left"), None, None, None, None

    )

    val locationResp1 = LocationsData(
      locations = List(location1,location2)
    )

     val power1 = Power(Some(CurrentTypeEnum.AC3Phases), 16, 220)





    val geoLocationJson1 =
      s"""
         |{
         |  "latitude": "3.72994",
         |  "longitude": "51.04759"
         |}
     """.stripMargin.parseJson

    val geoLocationJson2 =
      s"""
         |{
         |  "latitude": "3.72995",
         |  "longitude": "51.04760"
         |}
     """.stripMargin.parseJson



    val evseJson1 =
      s"""
         |    {
         |      "id": "BE-BEC-E041503001",
         |      "location_id": "LOC1",
         |      "status": "AVAILABLE",
         |      "capabilities": [
         |        "RESERVABLE"
         |      ],
         |      "connectors": [
         |        {
         |          "id": "1",
         |          "standard": "IEC-62196-T2",
         |          "format": "CABLE",
         |          "power_type": "AC_3_PHASE",
         |          "voltage": 220,
         |          "amperage": 16,
         |          "price_schemes": [
         |            {
         |              "price_scheme_id": 1,
         |              "expiry_date": "2020-12-31T23:59:59Z",
         |              "start_date": "2010-01-01T00:00:00Z",
         |              "tariff": [
         |                {
         |                  "currency": "EUR",
         |                  "price_untaxed": 0.1936,
         |                  "pricing_unit": "kwhtoev",
         |                  "tariff_id": "kwrate",
         |                  "display_text": [
         |                    {
         |                      "language": "nl",
         |                      "text": "Standaard Tarief"
         |                    },
         |                    {
         |                      "language": "en",
         |                      "text": "Standard Tariff"
         |                    }
         |                  ]
         |                }
         |              ],
         |              "display_text": [
         |                {
         |                  "language": "nl",
         |                  "text": "Standaard Tarief"
         |                },
         |                {
         |                  "language": "en",
         |                  "text": "Standard Tariff"
         |                }
         |              ]
         |            },
         |            {
         |              "price_scheme_id": 2,
         |              "expiry_date": "2020-12-31T23:59:59Z",
         |              "start_date": "2010-01-01T00:00:00Z",
         |              "tariff": [
         |                {
         |                  "currency": "EUR",
         |                  "price_untaxed": 0.1536,
         |                  "pricing_unit": "kwhtoev",
         |                  "tariff_id": "kwrate",
         |                  "display_text": [
         |                    {
         |                      "language": "nl",
         |                      "text": "eMSP Tarief"
         |                    },
         |                    {
         |                      "language": "en",
         |                      "text": "eMSP Tariff"
         |                    }
         |                  ]
         |                }
         |              ],
         |              "display_text": [
         |                {
         |                  "language": "nl",
         |                  "text": "eMSP Tarief"
         |                },
         |                {
         |                  "language": "en",
         |                  "text": "eMSP Tariff"
         |                }
         |              ]
         |            }
         |          ]
         |        },
         |        {
         |          "id": "2",
         |          "standard": "IEC-62196-T2",
         |          "format": "SOCKET",
         |          "power_type": "AC_3_PHASE",
         |          "voltage": 220,
         |          "amperage": 16,
         |          "price_schemes": [
         |            {
         |              "price_scheme_id": 1,
         |              "expiry_date": "2020-12-31T23:59:59Z",
         |              "start_date": "2010-01-01T00:00:00Z",
         |              "tariff": [
         |                {
         |                  "currency": "EUR",
         |                  "price_untaxed": 0.1936,
         |                  "pricing_unit": "kwhtoev",
         |                  "tariff_id": "kwrate",
         |                  "display_text": [
         |                    {
         |                      "language": "nl",
         |                      "text": "Standaard Tarief"
         |                    },
         |                    {
         |                      "language": "en",
         |                      "text": "Standard Tariff"
         |                    }
         |                  ]
         |                }
         |              ],
         |              "display_text": [
         |                {
         |                  "language": "nl",
         |                  "text": "Standaard Tarief"
         |                },
         |                {
         |                  "language": "en",
         |                  "text": "Standard Tariff"
         |                }
         |              ]
         |            }
         |          ]
         |        }
         |      ],
         |      "physical_number": "1",
         |      "floor_level": "-1"
         |    }
   """.stripMargin.parseJson




    val evseJson2 =
      s"""
         |    {
         |      "id": "BE-BEC-E041503002",
         |      "location_id": "LOC2",
         |      "status": "AVAILABLE",
         |      "capabilities": [
         |        "RESERVABLE"
         |      ],
         |      "connectors": [
         |        {
         |          "id": "1",
         |          "standard": "IEC-62196-T2",
         |          "format": "CABLE",
         |          "power_type": "AC_3_PHASE",
         |          "voltage": 220,
         |          "amperage": 16,
         |          "price_schemes": [
         |            {
         |              "price_scheme_id": 1,
         |              "expiry_date": "2020-12-31T23:59:59Z",
         |              "start_date": "2010-01-01T00:00:00Z",
         |              "tariff": [
         |                {
         |                  "currency": "EUR",
         |                  "price_untaxed": 0.1936,
         |                  "pricing_unit": "kwhtoev",
         |                  "tariff_id": "kwrate",
         |                  "display_text": [
         |                    {
         |                      "language": "nl",
         |                      "text": "Standaard Tarief"
         |                    },
         |                    {
         |                      "language": "en",
         |                      "text": "Standard Tariff"
         |                    }
         |                  ]
         |                }
         |              ],
         |              "display_text": [
         |                {
         |                  "language": "nl",
         |                  "text": "Standaard Tarief"
         |                },
         |                {
         |                  "language": "en",
         |                  "text": "Standard Tariff"
         |                }
         |              ]
         |            },
         |            {
         |              "price_scheme_id": 2,
         |              "expiry_date": "2020-12-31T23:59:59Z",
         |              "start_date": "2010-01-01T00:00:00Z",
         |              "tariff": [
         |                {
         |                  "currency": "EUR",
         |                  "price_untaxed": 0.1536,
         |                  "pricing_unit": "kwhtoev",
         |                  "tariff_id": "kwrate",
         |                  "display_text": [
         |                    {
         |                      "language": "nl",
         |                      "text": "eMSP Tarief"
         |                    },
         |                    {
         |                      "language": "en",
         |                      "text": "eMSP Tariff"
         |                    }
         |                  ]
         |                }
         |              ],
         |              "display_text": [
         |                {
         |                  "language": "nl",
         |                  "text": "eMSP Tarief"
         |                },
         |                {
         |                  "language": "en",
         |                  "text": "eMSP Tariff"
         |                }
         |              ]
         |            }
         |          ]
         |        }
         |      ],
         |      "physical_number": "1",
         |      "floor_level": "-1"
         |    }
   """.stripMargin.parseJson

    val evseJson3 =
      s"""
         |    {
         |      "id": "BE-BEC-E041503003",
         |      "location_id": "LOC2",
         |      "status": "AVAILABLE",
         |      "capabilities": [
         |        "RESERVABLE"
         |      ],
         |      "connectors": [
         |        {
         |          "id": "1",
         |          "standard": "IEC-62196-T2",
         |          "format": "CABLE",
         |          "power_type": "AC_3_PHASE",
         |          "voltage": 220,
         |          "amperage": 16,
         |          "price_schemes": [
         |            {
         |              "price_scheme_id": 1,
         |              "expiry_date": "2020-12-31T23:59:59Z",
         |              "start_date": "2010-01-01T00:00:00Z",
         |              "tariff": [
         |                {
         |                  "currency": "EUR",
         |                  "price_untaxed": 0.1936,
         |                  "pricing_unit": "kwhtoev",
         |                  "tariff_id": "kwrate",
         |                  "display_text": [
         |                    {
         |                      "language": "nl",
         |                      "text": "Standaard Tarief"
         |                    },
         |                    {
         |                      "language": "en",
         |                      "text": "Standard Tariff"
         |                    }
         |                  ]
         |                }
         |              ],
         |              "display_text": [
         |                {
         |                  "language": "nl",
         |                  "text": "Standaard Tarief"
         |                },
         |                {
         |                  "language": "en",
         |                  "text": "Standard Tariff"
         |                }
         |              ]
         |            },
         |            {
         |              "price_scheme_id": 2,
         |              "expiry_date": "2020-12-31T23:59:59Z",
         |              "start_date": "2010-01-01T00:00:00Z",
         |              "tariff": [
         |                {
         |                  "currency": "EUR",
         |                  "price_untaxed": 0.1536,
         |                  "pricing_unit": "kwhtoev",
         |                  "tariff_id": "kwrate",
         |                  "display_text": [
         |                    {
         |                      "language": "nl",
         |                      "text": "eMSP Tarief"
         |                    },
         |                    {
         |                      "language": "en",
         |                      "text": "eMSP Tariff"
         |                    }
         |                  ]
         |                }
         |              ],
         |              "display_text": [
         |                {
         |                  "language": "nl",
         |                  "text": "eMSP Tarief"
         |                },
         |                {
         |                  "language": "en",
         |                  "text": "eMSP Tariff"
         |                }
         |              ]
         |            }
         |          ]
         |        }
         |      ],
         |      "physical_number": "1",
         |      "floor_level": "-1"
         |    }
   """.stripMargin.parseJson




    val locationJson1 =
      s"""
         |    {
         |      "id": "LOC1",
         |      "type": "on_street",
         |      "name": "Gent Zuid",
         |      "address": "F.Rooseveltlaan 3A",
         |      "city": "Gent",
         |      "postal_code": "9000",
         |      "country": "BEL",
         |      "coordinates": $geoLocationJson1,
         |      "directions": "left, left, left, right, left",
         |      "opening_times": {
         |		    "regular_hours": [
         |		      {
         |		        "weekday": 1,
         |		        "period_begin": "08:00",
         |		        "period_end": "20:00"
         |		      },
         |		      {
         |		        "weekday": 2,
         |		        "period_begin": "08:00",
         |		        "period_end": "20:00"
         |		      },
         |		      {
         |		        "weekday": 3,
         |		        "period_begin": "08:00",
         |		        "period_end": "20:00"
         |		      },
         |		      {
         |		        "weekday": 4,
         |		        "period_begin": "08:00",
         |		        "period_end": "20:00"
         |		      },
         |		      {
         |		        "weekday": 5,
         |		        "period_begin": "08:00",
         |		        "period_end": "20:00"
         |		      }
         |		    ],
         |        "twentyfourseven": false,
         |		    "exceptional_openings": [
         |		      {
         |		        "period_begin": "2014-06-21T07:00:00Z",
         |		        "period_end": "2014-06-21T10:00:00Z"
         |		      }
         |		    ],
         |		    "exceptional_closings": [
         |		      {
         |		        "period_begin": "2014-06-23T22:00:00Z",
         |		        "period_end": "2014-06-24T22:00:00Z"
         |		      }
         |		    ]
         |		  }
         |    }
   """.stripMargin.parseJson

    val locationJson2 =
      s"""
         |    {
         |      "id": "LOC2",
         |      "type": "on_street",
         |      "name": "Gent Zuid",
         |      "address": "F.Rooseveltlaan 30",
         |      "city": "Gent",
         |      "postal_code": "9000",
         |      "country": "BE",
         |      "coordinates": $geoLocationJson2,
         |      "directions": "left, left, left, right, left"
         |    }
   """.stripMargin.parseJson

    val locationRespJson1 =
      s"""
         |{
         |  "locations": [$locationJson1,$locationJson2],
         |  "evses": [$evseJson1,$evseJson2,$evseJson3]
         |}
       """.stripMargin.parseJson

  }

  val operatorJsonWithNulls1 =
    s"""
       |{
       |  "identifier": null,
       |  "phone": "+31253621489",
       |  "url": null
       |}
    """.stripMargin.parseJson

  val operatorJsonNoNulls1 =
    s"""
       |{
       |  "phone": "+31253621489"
       |}
    """.stripMargin.parseJson

  val operator1 = Operator(None, Some("+31253621489"), None)

  val powerJson1 =
    s"""
       |{
       |  "current": "AC_3_PHASE",
       |  "amperage": 16,
       |  "voltage": 220
       |}
     """.stripMargin.parseJson

  val power1 = Power(Some(AC3Phases), 16, 220)


}
