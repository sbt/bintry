package bintry

import java.util.Date
import org.json4s._
import org.json4s.JsonDSL._

object AttrsToJson {
   def apply[A <: Attr[_]](attrs: Iterable[(String, Iterable[A])]): JValue =
     attrs.map {
       case (name, values) =>
         (("name" -> name) ~ {
           val tpe = values.headOption.map(_.tpe).getOrElse("string")
           ("type" -> tpe) ~ ("values" -> 
             values.map(_ match {
               case StringAttr(value)  => JString(value)
               case IntAttr(value)     => JInt(value)
               case BooleanAttr(value) => JBool(value)
               case DateAttr(value)    => JString(value.toString) // todo: ISO8601 (yyyy-MM-dd'T'HH:mm:ss.SSSZ)
               case VersionAttr(value) => JString(value)
             }))
         })
     }
}

object AttrsFromJson {
  def apply(js: JValue): Map[String, Iterable[Attr[_]]] =
    (for {
      JArray(ary) <- js
      JObject(fs) <- ary
      ("name", JString(name)) <- fs
      ("type", JString(tpe)) <- fs
      ("values", JArray(values)) <- fs
    } yield
      (name, (tpe match {
        case "string"  => for { JString(str)  <- values } yield StringAttr(str)
        case "number"  => for { JInt(num)     <- values } yield IntAttr(num.toInt)
        case "date"    => for { JString(date) <- values } yield DateAttr(new Date()) // todo ( ISO8601 (yyyy-MM-dd'T'HH:mm:ss.SSSZ) )
        case "version" => for { JString(ver)  <- values } yield VersionAttr(ver)
        case "boolean" => for { JBool(bool)   <- values } yield BooleanAttr(bool)
        case _ => Nil
      }): Iterable[Attr[_]])).toMap
}

sealed trait Attr[T] {
  def tpe: String
  def value: T
}

case class StringAttr(value: String) extends Attr[String] {
  def tpe = "string"
}

case class DateAttr(value: Date) extends Attr[Date] {
  def tpe = "date"
}

case class IntAttr(value: Int) extends Attr[Int] {
  def tpe = "number"
}

case class BooleanAttr(value: Boolean) extends Attr[Boolean] {
  def tpe = "boolean"
}

case class VersionAttr(value: String) extends Attr[String] {
  def tpe = "version"
}
