import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods
import org.scalatest.{MustMatchers, WordSpec}

class JsonVersionLibSpec extends WordSpec with MustMatchers {

  val jsonMethods = new JsonMethods {}

  "Stringifying json" should {
    println(s"Scala version tested is: ${scala.util.Properties.versionString}")

    "should work all values" in {

      val rendered = jsonMethods.compact(
        jsonMethods.render(
          ("name"     -> "randomName") ~
          ("desc"     -> Option("basicDescription")) ~
          ("licenses" -> List("Apache", "MIT")) ~
          ("labels"   -> List("label1", "label2")) ~
          ("vcs_url"  -> Option("vcs"))
        )
      )

      rendered must be(
        "{" +
          """"name":"randomName",""" +
          """"desc":"basicDescription",""" +
          """"licenses":["Apache","MIT"],""" +
          """"labels":["label1","label2"],""" +
          """"vcs_url":"vcs"""" +
        "}"
      )
    }

    "should work with some values omitted" in {

      val rendered = jsonMethods.compact(
        jsonMethods.render(
          ("name"     -> "randomName") ~
            ("desc"     -> (None : Option[String])) ~
            ("licenses" -> List[String]()) ~
            ("labels"   -> List[String]()) ~
            ("vcs_url"  -> (None : Option[String]))
        )
      )

      rendered must be(
        "{" +
          """"name":"randomName",""" +
          """"licenses":[],""" +
          """"labels":[]""" +
          "}"
      )
    }
  }
}
