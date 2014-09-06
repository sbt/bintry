package bintry

import com.ning.http.client.Response
import dispatch.as
import org.json4s._
import org.json4s.JsonDSL._

case class RepoSummary(name: String, owner: String)

case class Repo(
  name: String,
  owner: String,
  desc: String,
  labels: List[String],
  created: String,
  packages: Int
)

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
  githubRepo: Option[String],
  vcs: Option[String],
  githubReleaseNotes: String,
  publicDownloadNumbers: Boolean,
  links: List[String],
  versions: List[String],
  latestVersion: Option[String],
  rating: Int,
  systemIds: List[String]  
)

case class Version(
  name: String,
  desc: String,
  pkg: String,
  repo: String,
  owner: String,
  labels: List[String],
  attrNames: List[String],
  created: String,
  updated: String,
  released: String,
  ordinal: Int
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

  implicit val RepoSummaries: Rep[List[RepoSummary]] =
    new Rep[List[RepoSummary]] {
      def map = as.json4s.Json andThen(for {
        JArray(repos) <- _
        JObject(repo) <- repos
        ("name", JString(name)) <- repo
        ("owner", JString(owner)) <- repo
      } yield RepoSummary(name, owner))
    }

  implicit val RepoDetails: Rep[Repo] =
    new Rep[Repo] with Common {
      def map = as.json4s.Json andThen { js =>
        (for {
          JObject(repo)                     <- js
          ("name", JString(name))           <- repo
          ("owner", JString(owner))         <- repo
          ("desc", JString(desc))           <- repo
          ("labels", labels)                <- repo
          ("created", JString(created))     <- repo
          ("package_count", JInt(packages)) <- repo
        } yield Repo(
          name, owner, desc, strs(labels), created, packages.toInt)).head
      }
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
        ("github_repo", github)           <- pkg
        ("github_release_notes", JString(releaseNotes)) <- pkg
        ("public_download_numbers", JBool(downloadNums)) <- pkg
        ("linked_to_repos", links)        <- pkg
        ("versions", versions)            <- pkg
        ("latest_version", latestVersion) <- pkg
        ("updated", JString(updated))     <- pkg
        ("rating_count", JInt(rating))    <- pkg
        ("system_ids", sysIds)            <- pkg
        ("vcs_url", vcs)                  <- pkg
      } yield Package(
        name, repo, owner, desc,
        strs(labels), strs(attrs),
        followers.toInt,
        created, updated,
        str(web), str(issues), str(github), str(vcs),
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

  /*implicit val VersionDetails: Rep[Version] =
    new Repo[Version] {
      def map = as.json4s.Json andThen { js =>
        (for {

        } yield ...).head
      }
    }*/
}
