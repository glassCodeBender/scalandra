package com.nodeta.scalandra.map

import com.nodeta.scalandra._
import serializer.Serializer

trait Base[A, B ,C] {
  protected val client : Client[A, B, C]
}
