package bintry

import java.util.Date
import org.json4s._
import org.json4s.JsonDSL._

object AttrsJson {
   def apply[A <: Attr[_]](attrs: Map[String, Iterable[A]]): JValue =
     (JObject() /: attrs) {
       case (js, (name, values)) =>
         js ~ (("name" -> name) ~ {
           val tpe = values.headOption.map(_.tpe).getOrElse("string")
           ("type" -> tpe) ~ ("values" -> 
             values.map(_ match {
               case StringAttr(value)  => JString(value)
               case IntAttr(value)     => JInt(value)
               case BooleanAttr(value) => JBool(value)
               case DateAttr(value)    => JString(value.toString) // todo: format
               case VersionAttr(value) => JString(value)
             }))
         })
     }
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
