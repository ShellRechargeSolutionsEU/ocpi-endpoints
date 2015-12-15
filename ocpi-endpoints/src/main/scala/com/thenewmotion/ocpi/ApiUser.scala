package com.thenewmotion.ocpi

case class ApiUser(
  id: String,
  token: String,
  country_code: String,
  party_id: String
) {
  require(country_code.length == 2 && country_code.matches("""[A-Za-z]{2}"""), "Country code needs to conform to ISO 3166-1 alpha-2")
  require(party_id.length == 3 && party_id.matches("""\w{3}"""), "Party ID needs to conform to ISO 15118")
}