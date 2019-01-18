package com.couchbase.client.scala.document

import com.couchbase.client.core.error.CouchbaseException
import upickle.default.read

import scala.concurrent.duration.Duration
import scala.language.dynamics
import scala.reflect.runtime.universe._
import upickle.default._

trait Convertable {
  def contentAs[T](path: PathElements): T
  def exists(path: PathElements): Boolean
}

class PathNotFound extends CouchbaseException

sealed trait PathElement
case class PathObjectOrField(name: String) extends PathElement {
  override def toString: String = name
}
case class PathArray(name: String, index: Int) extends PathElement {
  override def toString: String = name + "[" + index + "]"
}

case class PathElements(paths: List[PathElement]) {
  def add(elem: PathElement) = copy(paths :+ elem)

  override def toString: String = {
    paths.map(_.toString).mkString(".")
  }
}

// TODO much more implementation required
case class GetSelecter(private val result: Convertable, path: PathElements) extends Dynamic {
  def selectDynamic(name: String): GetSelecter = GetSelecter(result, path.add(PathObjectOrField(name)))
  def applyDynamic(name: String)(index: Int): GetSelecter = GetSelecter(result, path.add(PathArray(name, index)))

  def exists: Boolean = result.exists(path)
  def getString: String = result.contentAs[String](path)
  // TODO see what Jackson transcoding produces in terms of ints, longs, floats, doubles
  def getInt: Int = result.contentAs[Int](path)
  def getObject: JsonObject = result.contentAs[JsonObject](path)
  def getAs[T]: T = result.contentAs[T](path)
}

class GetResult(val id: String,
                private val _content: Array[Byte],
                val cas: Long,
                val expiry: Option[Duration]) extends Dynamic with Convertable {

  def contentAsObject: JsonObject = contentAs[JsonObject]

  def contentAsObject(path: String): JsonObject = contentAs[JsonObject](path)

  def contentAsArray: JsonArray = contentAs[JsonArray]

  def contentAsArray(path: String): JsonArray = contentAs[JsonArray](path)

  def content: JsonType = ???

  def contentAsBytes: Array[Byte] = _content

  def contentAsStr: String = {
    import upickle.default._

    read[String](_content)
  }

  def content(idx: Int): JsonType = ???

  def content(path: String): JsonType = ???

  def contentAsUjson = {

//    ByteArrayParser.transform(_content, new BaseRenderer)
//    transform(Readable.fromByteArray(_content), BytesRenderer())
    read[ujson.Obj](_content)
  }

  def contentAs[T]: T = ???
//  (implicit tt: TypeTag[T]): T = {
//    read[ujson.Obj](_content)
//  }

  def contentAs[T](path: String): T = ???

  def contentAs[T](path: String, decoder: Array[Byte] => T): T = ???

  // TODO MVP decide: nope, far too easy to get this wrong, and drops you into GetSelector.  Must do .dyn or something first.
//  def selectDynamic(name: String): GetSelecter = GetSelecter(this, PathElements(List(PathObjectOrField(name))))
//  def applyDynamic(name: String)(index: Int): GetSelecter = GetSelecter(this, PathElements(List(PathArray(name, index))))

  override def exists(path: PathElements): Boolean = ???

  override def contentAs[T](path: PathElements): T = ???
}

object GetResult {
  def unapply[T](document: GetResult): Option[(String, JsonObject, Long, Option[Duration])] = {
    Some(document.id, document.contentAsObject, document.cas, document.expiry)
  }

  // TODO can get this working along with unapply
//    def unapplySeq(doc: ReadResult): Option[Seq[Any]] = null
}