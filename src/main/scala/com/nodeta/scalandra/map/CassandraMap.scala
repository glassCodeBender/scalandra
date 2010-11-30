package com.nodeta.scalandra.map

import com.nodeta.scalandra._

trait CassandraMap[A, B] extends scala.collection.Map[A, B] {
  def slice(r : Range[A]) : CassandraMap[A, B]

  def slice(l : Iterable[A]) : CassandraMap[A, B]

  def remove(key : A) : CassandraMap[A, B]

  def update(key : A, value : B) : CassandraMap[A, B]

  def +[B1 >: B](kv: (A, B1)) = throw new UnsupportedOperationException
  def -(key: A) = throw new UnsupportedOperationException
}
