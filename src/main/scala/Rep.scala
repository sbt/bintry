package bintry

import org.asynchttpclient.Response
import dispatch.as
import org.json4s._

object Message {
  val empty = Message("")
}

case class Message(message: String)

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
  desc: Option[String],
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
  desc: Option[String],
  pkg: String,
  repo: String,
  owner: String,
  labels: List[String],
  attrNames: List[String],
  created: String,
  updated: String,
  released: String,
  ordinal: Int,
  vcsTag: Option[String]
)

case class User(
  name: String,
  fullName: String,
  gravatar: String,
  repos: List[String],
  organizations: List[String],
  followerCount: Int,
  registered: String,
  bytesUsed: Long
)

case class Name(name: String)

/** Type class for representing a response as a given type */
trait Rep[+T] {
  def map(r: Response): T
}

object Rep {
  private def str(jv: JValue): Option[String] =
    (for {
      JString(s) <- jv
    } yield s).headOption

  private def strs(jv: JValue): List[String] =
    for {
      JArray(xs) <- jv
      JString(s) <- xs
    } yield s

  implicit val Identity: Rep[Response] =
    new Rep[Response] {
      def map(r: Response): Response = r
    }

  implicit val nada: Rep[Unit] =
    new Rep[Unit] {
      def map(r: Response): Unit = ()
    }

  trait JsonRep[T] extends Rep[T] {
    def map(r: Response): T =
      map(as.json4s.Json(r))
    def map(js: JValue): T
  }

  implicit val messages: Rep[Message] =
    new JsonRep[Message] {
      def map(json: JValue) = (for {
        JObject(msg)                  <-  json
        ("message", JString(message)) <-  msg
      } yield Message(message)).head
    }

  implicit val attributes: Rep[Attr.AttrMap] =
    new JsonRep[Attr.AttrMap] {
      def map(json: JValue): Attr.AttrMap =
        (for {
          JArray(ary)                <- json
          JObject(fs)                <- ary
          ("name", JString(name))    <- fs
          ("type", JString(tpe))     <- fs
          ("values", JArray(values)) <- fs
        } yield
          (name, (tpe match {
            case "string"  => for { JString(str)  <- values } yield Attr.String(str)
            case "number"  => for { JInt(num)     <- values } yield Attr.Number(num.toInt)
            case "date"    => for { JString(date) <- values } yield Attr.Date(Iso8601(date))
            case "version" => for { JString(ver)  <- values } yield Attr.Version(ver)
            case "boolean" => for { JBool(bool)   <- values } yield Attr.Boolean(bool)
            case _ => Nil
          }): Iterable[Attr[_]])).toMap
     }

  implicit val repoSummaries: Rep[List[RepoSummary]] =
    new JsonRep[List[RepoSummary]] {
      def map(json: JValue): List[RepoSummary] = for {
        JArray(repos)             <- json
        JObject(repo)             <- repos
        ("name", JString(name))   <- repo
        ("owner", JString(owner)) <- repo
      } yield RepoSummary(name, owner)
    }

  implicit val repoDetails: Rep[Repo] =
    new JsonRep[Repo] {
      def map(json: JValue): Repo =
        (for {
          JObject(repo)                     <- json
          ("name", JString(name))           <- repo
          ("owner", JString(owner))         <- repo
          ("desc", JString(desc))           <- repo
          ("labels", labels)                <- repo
          ("created", JString(created))     <- repo
          ("package_count", JInt(pkgCount)) <- repo
        } yield Repo(
          name,
          owner,
          desc,
          strs(labels),
          created,
          pkgCount.toInt)
       ).head
    }

  implicit val packages: Rep[List[Package]] =
    new JsonRep[List[Package]] {
      def map(json: JValue): List[Package] = for {
        JArray(pkgs) <- json
        pkg          <- pkgs
      } yield packageDetails.one(pkg).get
    }

  implicit object packageDetails extends JsonRep[Package] {
    def one(js: JValue): Option[Package] = (for {
      JObject(pkg)                      <- js
      ("name", JString(name))           <- pkg
      ("repo", JString(repo))           <- pkg
      ("owner", JString(owner))         <- pkg
      ("desc", desc)                    <- pkg
      ("labels", labels)                <- pkg
      ("attribute_names", attrs)        <- pkg
      ("followers_count", JInt(followers)) <- pkg
      ("created", JString(created))     <- pkg
      ("website_url", web)              <- pkg
      ("issue_tracker_url", issues)     <- pkg
      ("github_repo", github)           <- pkg
      ("github_release_notes_file", JString(releaseNotes)) <- pkg
      ("public_download_numbers", JBool(downloadNums)) <- pkg
      ("linked_to_repos", links)        <- pkg
      ("versions", versions)            <- pkg
      ("latest_version", latestVersion) <- pkg
      ("updated", JString(updated))     <- pkg
      ("rating_count", JInt(rating))    <- pkg
      ("system_ids", sysIds)            <- pkg
      ("vcs_url", vcs)                  <- pkg
    } yield Package(
      name,
      repo,
      owner,
      str(desc),
      strs(labels),
      strs(attrs),
      followers.toInt,
      created,
      updated,
      str(web),
      str(issues),
      str(github),
      str(vcs),
      releaseNotes,
      downloadNums,
      strs(links),
      strs(versions),
      str(latestVersion),
      rating.toInt,
      strs(sysIds))).headOption

    def map(json: JValue): Package = one(json).get
  }

  implicit val packageSummaries: Rep[List[PackageSummary]] =
    new JsonRep[List[PackageSummary]] {
      def map(json: JValue): List[PackageSummary] = for {
        JArray(pkgs)              <- json
        JObject(pkg)              <- pkgs
        ("name", JString(name))   <- pkg
        ("linked", JBool(linked)) <- pkg
      } yield PackageSummary(name, linked)
    }

  implicit val versions: JsonRep[Version] =
    new JsonRep[Version] {
      def map(json: JValue): Version = (for {
        JObject(ver)                    <- json
        ("name", JString(name))         <- ver
        ("desc", desc)                  <- ver
        ("package", JString(pkg))       <- ver
        ("repo", JString(repo))         <- ver
        ("owner", JString(owner))       <- ver
        ("labels", labels)              <- ver
        ("attribute_names", attrs)      <- ver
        ("created", JString(created))   <- ver
        ("updated", JString(updated))   <- ver
        ("released", JString(released)) <- ver
        ("ordinal", JDecimal(ord))       <- ver
        ("vcs_tag", tag)                <- ver
      } yield Version(
        name,
        str(desc),
        pkg,
        repo,
        owner,
        strs(labels),
        strs(attrs),
        created,
        updated,
        released,
        ord.toInt,
        str(tag))).head
    }

  implicit val names: Rep[List[Name]] =
    new JsonRep[List[Name]] {
      def map(json: JValue): List[Name] = for {
        JArray(xs)           <- json
        JObject(name)        <- xs
        ("name", JString(n)) <- name
      } yield Name(n)
    }

  implicit val user: Rep[User] =
    new JsonRep[User] {
      def map(json: JValue): User = (for {
        JObject(u)                           <- json
        ("name", JString(name))              <- u
        ("full_name", JString(fullname))     <- u
        ("gravatar_id", JString(gravatar))   <- u
        ("repos", repos)                     <- u
        ("organizations", orgs)              <- u
        ("followers_count", JInt(followers)) <- u
        ("registered", JString(reg))         <- u
        ("quota_used_bytes", JInt(bytes))    <- u
      } yield User(
        name,
        fullname,
        gravatar,
        strs(repos),
        strs(orgs),
        followers.toInt,
        reg,
        bytes.toLong
      )).head
    }
}
