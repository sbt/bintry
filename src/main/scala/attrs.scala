package bintry

import java.util.Date
import org.json4s._
import org.json4s.JsonDSL._

import java.text.SimpleDateFormat

object Iso8601 {
  val FMT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
  def formatter = new SimpleDateFormat(FMT)
  def apply(d: Date) =
    formatter.format(d)
  def apply(str: String) =
    formatter.parse(str)
}

object AttrsSearchJson {
  def apply(pair: (String, AttrQuery[_])): (String, JValue) =
    pair match {
      case (name, query) =>
        (name -> (query match {
          case AttrOneOf(values)  =>  values.toList.map(AttrsToJson(_))
          case AttrIs(value)      =>  AttrsToJson.apply(value) :: Nil
        }))
    }

  def apply(qs: Iterable[(String, AttrQuery[_])]): JValue =
    qs.map(apply(_))
}

object AttrsToJson {
   def apply[A <: Attr[_]](a: A): JValue =
     a match {
       case StringAttr(value)  => JString(value)
       case IntAttr(value)     => JInt(value)
       case BooleanAttr(value) => JBool(value)
       case DateAttr(value)    => JString(Iso8601(value))
       case VersionAttr(value) => JString(value)
     }
   def apply[A <: Attr[_]](attrs: Iterable[(String, Iterable[A])]): JValue =
     attrs.map {
       case (name, values) =>
         (("name" -> name) ~ {
           val tpe = values.headOption.map(_.tpe).getOrElse("string")
           ("type" -> tpe) ~ ("values" ->
             values.map(apply(_)))
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
        case "date"    => for { JString(date) <- values } yield DateAttr(Iso8601(date)) // todo ( ISO8601 (yyyy-MM-dd'T'HH:mm:ss.SSSZ) )
        case "version" => for { JString(ver)  <- values } yield VersionAttr(ver)
        case "boolean" => for { JBool(bool)   <- values } yield BooleanAttr(bool)
        case _ => Nil
      }): Iterable[Attr[_]])).toMap
}

trait AttrQuery[A <: Attr[_]]

case class AttrIs[A <: Attr[_]](attr: A) extends AttrQuery[A]
case class AttrOneOf[A <: Attr[_]](attrs: Iterable[A]) extends AttrQuery[A]

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
