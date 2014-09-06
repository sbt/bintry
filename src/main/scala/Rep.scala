package bintry

import com.ning.http.client.Response
import dispatch.as
import org.json4s._
import org.json4s.JsonDSL._

case class PackageSummary(name: String, linked: Boolean)

case class Package(
  name: String,
  repo: String,
  owner: String,
  desc: String,
  labels: List[String],
  attrNames: List[String],
  followers: Int,
  created: String,
  updated: String,
  web: Option[String],
  issueTracker: Option[String],
  gitRepo: Option[String],
  vcs: Option[String],
  githubReleaseNotes: String,
  publicDownloadNumbers: Boolean,
  links: List[String],
  versions: List[String],
  latestVersion: Option[String],
  rating: Int,
  systemIds: List[String]  
)

/** Type class for representing a response as a given type */
trait Rep[T] {
  def map: Response => T
}

object Rep {
  private trait Common {
    def str(jv: JValue) = (for {
      JString(s) <- jv
    } yield s).headOption

    def strs(jv: JValue) = for {
      JArray(xs) <- jv
      JString(s) <- xs
    } yield s
  }
  implicit val Identity: Rep[Response] =
    new Rep[Response] {
      def map = identity(_)
    }

  implicit val Nada: Rep[Unit] =
    new Rep[Unit] {
      def map = _ => ()
    }

  implicit val PackageDetails: Rep[Package] =
    new Rep[Package] with Common {    
      def map = as.json4s.Json andThen { js => (for {
        JObject(pkg)                      <- js
        ("name", JString(name))           <- pkg
        ("repo", JString(repo))           <- pkg
        ("owner", JString(owner))         <- pkg
        ("desc", JString(desc))           <- pkg
        ("labels", labels)                <- pkg
        ("attribute_names", attrs)        <- pkg
        ("followers_count", JInt(followers)) <- pkg
        ("created", JString(created))     <- pkg
        ("website_url", web)              <- pkg
        ("issue_tracker_url", issues)     <- pkg
        ("github_repo", git)              <- pkg
        ("github_release_notes", JString(releaseNotes)) <- pkg
        ("public_download_numbers", JBool(downloadNums)) <- pkg
        ("linked_to_repos", links)        <- pkg
        ("versions", versions)            <- pkg
        ("latest_version", latestVersion) <- pkg
        ("updated", JString(updated))     <- pkg
        ("rating_count", JInt(rating))    <- pkg
        ("system_ids", sysIds)            <- pkg
        ("vcs", vcs)                      <- pkg
      } yield Package(
        name, repo, owner, desc,
        strs(labels), strs(attrs),
        followers.toInt,
        created, updated,
        str(web), str(issues), str(git), str(vcs),
        releaseNotes, downloadNums,
        strs(links), strs(versions), str(latestVersion),
        rating.toInt, strs(sysIds))).head
     }
  }

  implicit val PackageSummaries: Rep[List[PackageSummary]] =
    new Rep[List[PackageSummary]] {
      def map = as.json4s.Json andThen(for {
        JArray(pkgs)              <- _
        JObject(pkg)              <- pkgs
        ("name", JString(name))   <- pkg
        ("linked", JBool(linked)) <- pkg
      } yield PackageSummary(name, linked))
    }
}
