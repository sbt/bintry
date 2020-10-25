import bintry.Licenses
import dispatch._
import org.json4s._
import org.json4s.native.JsonMethods._
import verify._

object LicensesSpec extends BasicTestSuite {
  val bintrayOssLicensesUrl = url("https://api.bintray.com/licenses/oss_licenses")

  testAsync("Licenses in bintry should equal licenses available in Bintray") {
    import scala.concurrent.ExecutionContext.Implicits._

    Http
      .default(bintrayOssLicensesUrl OK as.String)
      .map(parse(_))
      .map { json =>
        {
          for {
            JObject(license) <- json
            JField("name", JString(name)) <- license
          } yield name
        }.toSet
      }
      .map { names =>
        assert(
          names.diff(Licenses.Names).isEmpty,
          "not all licenses available in Bintray are in bintry"
        )
        assert(
          Licenses.Names.diff(names).isEmpty,
          "not all licenses in bintry are available in Bintray"
        )
      }
  }
}
