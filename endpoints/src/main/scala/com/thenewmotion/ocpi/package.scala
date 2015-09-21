package com.thenewmotion

import org.slf4j.LoggerFactory

package object ocpi {
  def Logger(cls: Class[_]) = LoggerFactory.getLogger(cls)
}
