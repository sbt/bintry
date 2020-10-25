import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods
import verify._

object JsonVersionLibSpec extends BasicTestSuite {

  val jsonMethods = new JsonMethods {}
  test("Stringifying json should work all values") {
    println(s"Scala version tested is: ${scala.util.Properties.versionString}")
    val rendered = jsonMethods.compact(
      jsonMethods.render(
        ("name" -> "randomName") ~
          ("desc" -> Option("basicDescription")) ~
          ("licenses" -> List("Apache", "MIT")) ~
          ("labels" -> List("label1", "label2")) ~
          ("vcs_url" -> Option("vcs"))
      )
    )

    assert(
      rendered ==
        "{" +
          """"name":"randomName",""" +
          """"desc":"basicDescription",""" +
          """"licenses":["Apache","MIT"],""" +
          """"labels":["label1","label2"],""" +
          """"vcs_url":"vcs"""" +
          "}"
    )
  }

  test("Stringifying json should work with some values omitted") {
    val rendered = jsonMethods.compact(
      jsonMethods.render(
        ("name" -> "randomName") ~
          ("desc" -> (None: Option[String])) ~
          ("licenses" -> List[String]()) ~
          ("labels" -> List[String]()) ~
          ("vcs_url" -> (None: Option[String]))
      )
    )

    assert(
      rendered ==
        "{" +
          """"name":"randomName",""" +
          """"licenses":[],""" +
          """"labels":[]""" +
          "}"
    )
  }
}
