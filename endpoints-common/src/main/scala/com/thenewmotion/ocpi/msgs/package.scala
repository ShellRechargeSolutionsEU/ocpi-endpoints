package com.thenewmotion.ocpi

import akka.http.scaladsl.model.Uri
import scala.language.implicitConversions

package object msgs {
  implicit def urlToUri(url: Url): Uri = Uri(url.value)
}
