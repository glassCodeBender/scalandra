package com.nodeta.scalandra.map

import com.nodeta.scalandra._

import org.apache.cassandra.thrift.InvalidRequestException

class UnsupportedActionException(s : String) extends Exception(s) {}

trait ColumnFamily[A] extends CassandraMap[String, A] { this : Base[_, _, _] =>
  protected val path : Path[_, _]
  
  protected def build(key : String) : A
  
  protected sealed class KeyIterator extends Iterator[String] {
    var start : Option[String] = None
    var buffer : Iterator[String] = Iterator.empty
    var end = false
    
    private def updateBuffer() {
      if (end) return
      val keys = client.keys(path.columnFamily, start, None, 100)
      if (keys.isEmpty || keys.size < 100) end = true
      buffer = keys.iterator
      if (!keys.isEmpty) start = Some(keys.last + ' ')
    }
    
    def hasNext : Boolean = {
      if (buffer.hasNext) return true
      updateBuffer()
      buffer.hasNext
    }
    def next : String = buffer.next
  }

  def iterator : Iterator[(String, A)] = keys.iterator.map(k => (k -> build(k)))
  override def keys : Iterable[String] = new Iterable[String] { def iterator = new KeyIterator() }
  
  def get(key : String) = Some(build(key))
  override def size = { toList.size }

}

class StandardColumnFamily[A, B, C](protected val path : Path[A, B], protected val client : Client[A, B, C]) extends ColumnFamily[StandardRecord[A, B, C]] with Base[A, B, C] {
  def this(columnFamily : String, client : Client[A, B, C]) = this(client.Path(columnFamily), client)
  
  protected def build(key : String) = {
    new StandardRecord(key, path / None, client)
  }
  
  sealed protected trait ListPredicate extends StandardColumnFamily[A, B, C] {
    def constraint : Iterable[String]
    override def keys = constraint
  }

  sealed protected trait RangePredicate extends StandardColumnFamily[A, B, C] {
    def constraint : Range[String]
    override def keys = {
      this.client.get(path, this.client.StandardSlice(Nil), this.constraint.start, this.constraint.finish, this.constraint.count).keys
    }
  }

  def slice(r : Range[String]) = {
    new StandardColumnFamily(path, client) with RangePredicate {
      val constraint = r
    }
  }
  
  def slice(r : Iterable[String]) = {
    new StandardColumnFamily(path, client) with ListPredicate {
      val constraint = r
    }
  }
  
  def map(column : B) : Map[String, C] = {
    multiget(path / column)
  }
  
  protected def multiget(x : ColumnPath[A, B]) : Map[String, C] = {
    val r = client.get(path, client.StandardSlice(List(x.column)), None, None, 20000000)
    scala.collection.immutable.ListMap() ++ r.flatMap { case(key, value) =>
      value.get(x.column) match {
        case Some(column) => List((key, column))
        case None => Nil
      }
    }
  }
  
  def remove(key : String) = {
    client.remove(key, path)
    this
  }
  
  def update(key : String, value : StandardRecord[A, B, C]) = {
    client(key, path / None) = value
    this
  }
  def update(key : String, value : Iterable[(B, C)]) = {
    client(key, path / None) = value
    this
  }
}

class SuperColumnFamily[A, B, C](protected val path : Path[A, B], protected val client : Client[A, B, C]) extends ColumnFamily[SuperRecord[A, B, C]] with Base[A, B, C] {
  def this(columnFamily : String, client : Client[A, B, C]) = this(client.Path(columnFamily), client)
  protected def build(key : String) = {
    val parent = this
    new SuperRecord[A, B, C](key, path, client)
  }

  sealed protected trait ListPredicate extends SuperColumnFamily[A, B, C] {
    def constraint : Iterable[String]
    override def keys = constraint
  }
  
  sealed protected trait RangePredicate extends SuperColumnFamily[A, B, C] {
    def constraint : Range[String]
    override def keys = {
      this.client.get(path, this.client.SuperSlice(Nil), this.constraint.start, this.constraint.finish, this.constraint.count).keys
    }
  }
  


  def slice(r : Range[String]) = {
    new SuperColumnFamily(path, client) with RangePredicate {
      val constraint = r
    }
  }
  
  def slice(r : Iterable[String]) = {
    new SuperColumnFamily(path, client) with ListPredicate {
      val constraint = r
    }
  }

  def remove(key : String) = {
    client.remove(key, path)
    this
  }

  def update(key : String, value : SuperRecord[A, B, C]) = {
    client(key, path) = value
    this
  }
  def update(key : String, value : Iterable[(A, Iterable[(B, C)])]) = {
    client(key, path) = value
    this
  }
}
