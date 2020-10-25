package bintry

import java.util.Date
import org.json4s._
import org.json4s.JsonDSL._
import java.text.SimpleDateFormat
import java.util.{ Date => JDate }

object Iso8601 {
  val FMT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
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
          case AttrOneOf(values) => values.toList.map(AttrsToJson(_))
          case AttrIs(value)     => AttrsToJson.apply(value) :: Nil
        }))
    }

  def apply(qs: Iterable[(String, AttrQuery[_])]): JValue =
    qs.map(apply(_))
}

object AttrsToJson {
  def apply[A <: Attr[_]](a: A): JValue =
    a match {
      case Attr.String(value)  => JString(value)
      case Attr.Number(value)  => JInt(value)
      case Attr.Boolean(value) => JBool(value)
      case Attr.Date(value)    => JString(Iso8601(value))
      case Attr.Version(value) => JString(value)
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

trait AttrQuery[A <: Attr[_]]

case class AttrIs[A <: Attr[_]](attr: A) extends AttrQuery[A]
case class AttrOneOf[A <: Attr[_]](attrs: Iterable[A]) extends AttrQuery[A]

sealed trait Attr[T] {
  def tpe: String = getClass.getSimpleName.stripSuffix("$")
  def value: T
}

object Attr {
  import java.lang.{ String => JString, Boolean => JBoolean }
  type AttrMap = Map[JString, Iterable[Attr[_]]]
  case class String(value: JString) extends Attr[JString]
  case class Date(value: JDate) extends Attr[JDate]
  case class Number(value: scala.Int) extends Attr[scala.Int]
  case class Boolean(value: JBoolean) extends Attr[JBoolean]
  case class Version(value: JString) extends Attr[JString]
}
