package bintry

import com.ning.http.client.Response
import bintry.Util.appendPath
import dispatch.Req
import org.json4s.JValue
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods.{ compact, render }
import java.io.File

trait Methods { self: Requests =>

  object json {
    private[this] val Type = "application/json"
    private[this] val Encoding = "UTF-8"
    def content(r: Req) = r.setContentType(Type, Encoding)
    def str(jv: JValue) = compact(render(jv))
  }

  /** All methods relating to a given repo */
  case class Repo(subject: String, repo: String)
    extends Client.Completion[bintry.Repo] {

    case class PackageCreate(
      name: String,
      _desc: Option[String]   = None,
      _labels: List[String]   = Nil,
      _licenses: List[String] = Nil,
      _vcs: Option[String]    = None)
      extends Client.Completion[Response] {

      def desc(d: String) = copy(_desc = Some(d))
      def labels(ls: String*) = copy(_labels = ls.toList)
      def licenses(ls: String*) = copy(_licenses = ls.toList)
      def vcs(url: String) = copy(_vcs = Some(url))

      /** https://bintray.com/docs/api.html#_create_package */
      override def apply[T](handler: Client.Handler[T]) =
        request((json.content(apiHost / "packages" / subject / repo).POST) <<
               json.str(
                 ("name"     -> name) ~
                 ("desc"     -> _desc) ~
                 ("licenses" -> _licenses) ~
                 ("labels"   -> _labels) ~
                 ("vcs_url"  -> _vcs)))(handler)
    }

    /** Package methods */
    case class Package(name: String)
      extends Client.Completion[bintry.Package] {
      object Attrs {
        private def pkgAttrBase =
          (apiHost / "packages" /
           subject / repo / name / "attributes")

        /** https://bintray.com/docs/api.html#_get_attributes */
        def apply(names: String*) =
          complete[Response](
            (if (names.isEmpty) pkgAttrBase else pkgAttrBase)
              <<? Map("names" -> names.mkString(",")))

        /** https://bintray.com/docs/api.html#_set_attributes */
        def set[A <: Attr[_]](attrs: (String, Iterable[A])*) =
          complete[Response](
            json.content(pkgAttrBase.POST)
              << json.str(AttrsToJson(attrs)))

        /** https://bintray.com/docs/api.html#_update_attributes */
        def update[A <: Attr[_]](attrs: (String, Iterable[A])*) =
          complete[Response](
            json.content(pkgAttrBase.PATCH)
              << json.str(AttrsToJson(attrs)))

        /** https://bintray.com/docs/api.html#_delete_attributes */
        def delete(names: String*) =
          complete[Response](
            (if (names.isEmpty) pkgAttrBase.DELETE else pkgAttrBase.DELETE)
              <<? Map("names" -> names.mkString(",")))
      }

      private[this] def publishPath(
        path: String, publish: Boolean, explode: Boolean) =
          "%s;publish=%s;explode=%s".format(
            path,
            if (publish) 1 else 0,
            if (explode) 1 else 0)

      /** https://bintray.com/docs/api.html#_create_version */
      case class CreateVersion(
        version: String,
        _desc: Option[String]   = None,
        _vcsTag: Option[String] = None,
        _notes: Option[String]  = None,
        _readme: Option[String] = None)
        extends Client.Completion[Response] {
        def desc(d: String) = copy(_desc = Some(d))
        def vcsTag(tag: String) = copy(_vcsTag = Some(tag))
        def notes(n: String) = copy(_notes = Some(n))
        def readme(rm: String) = copy(_readme = Some(rm))
        def apply[T](handler: Client.Handler[T]) =
          request(json.content(pkgBase.POST) / "versions" <<
                  json.str(
                   ("name"          -> version) ~
                   ("desc"          -> _desc)   ~
                   ("release_notes" -> _notes)  ~
                   ("release_url"   -> _readme) ~
                   ("vcs_tag"       -> _vcsTag)))(handler)
      }

      /** Package version methods */
      case class Version(version: String)
        extends Client.Completion[Response] {
        /** version  attr interface */
        object Attrs {
          private def versionAttrBase =
            (apiHost / "packages" /
             subject / repo / name / "versions" / version / "attributes")

          /** https://bintray.com/docs/api.html#_get_attributes */
          def apply(names: String*) =
            complete[Response](
              (if (names.isEmpty) versionAttrBase else versionAttrBase)
                <<? Map("names" -> names.mkString(",")))

          /** https://bintray.com/docs/api.html#_set_attributes */
          def set[A <: Attr[_]](attrs: (String, Iterable[A])*) =
            complete[Response](
              json.content(versionAttrBase.POST) << json.str(AttrsToJson(attrs)))

          /** https://bintray.com/docs/api.html#_update_attributes */
          def update[A <: Attr[_]](attrs: (String, Iterable[A])*) =
            complete[Response](
              json.content(versionAttrBase.PATCH) << json.str(AttrsToJson(attrs)))

          /** https://bintray.com/docs/api.html#_delete_attributes */
          def delete(names: String*) =
            complete[Response](
              (if (names.isEmpty) versionAttrBase.DELETE else versionAttrBase.DELETE)
                <<? Map("names" -> names.mkString(",")))
        }

        /** version upload interface */
        case class Upload(
          _artifact: (String, File),
          _publish: Boolean = false,
          _explode: Boolean = false)
          extends Client.Completion[Response] {
          def artifact(path: String, content: File) = copy(
            _artifact = (path, content)
          )
          def publish(pub: Boolean) = copy(_publish = pub)
          def explode(expl: Boolean) = copy(_explode = expl)
          def apply[T](handler: Client.Handler[T]) =
            request(appendPath(
            contentBase.PUT,
            publishPath(_artifact._1, _publish, _explode)) <:< Map(
              "X-Bintray-Package" -> name,
              "X-Bintray-Version" -> version
            ) <<< _artifact._2)(handler)
        }

        private[this] def versionBase =
          (apiHost / "packages" /
           subject / repo / name / "versions" / version)

        private[this] def contentBase = apiHost / "content" / subject / repo

        /** https://bintray.com/docs/api.html#_get_version */
        override def apply[T](handler: Client.Handler[T]) =
          request(versionBase)(handler)

        /** https://bintray.com/docs/api.html#_gpg_sign_a_version
         *  see also http://blog.bintray.com/2013/08/06/fight-crime-with-gpg/
         *  see also https://bintray.com/docs/uploads/uploads_gpgsigning.html
         */
        def sign(passphrase: String) =
          complete[Response](
            json.content(apiHost.POST) / "gpg" / subject / repo / name / "versions" / version
              << json.str(("passphrase" -> passphrase)))

        /** https://bintray.com/docs/api.html#_sync_version_artifacts_to_maven_central
         *  see also http://blog.bintray.com/2014/02/11/bintray-as-pain-free-gateway-to-maven-central/
         *  see also https://docs.sonatype.org/display/Repository/Central+Sync+Requirements
         */
        def mavenCentralSync(sonatypeUser: String, sonatypePassword: String, close: Boolean = true) =
          complete[Response](
            json.content(apiHost.POST) / "maven_central_sync" / subject / repo / name / "versions" / version
              << json.str(("username" -> sonatypeUser) ~
                          ("password" -> sonatypePassword) ~
                          ("close"    -> Some("1").filter(Function.const(close)))))

        /** https://bintray.com/docs/api.html#_delete_version */
        def delete =
          complete[Response](versionBase.DELETE)

        /** https://bintray.com/docs/api.html#_update_version */
        def update(desc: String) =
          complete[Response](json.content(versionBase.PATCH) <<
                   json.str(("desc" -> desc)))
        
        def attrs = Attrs

        /** https://bintray.com/docs/api.html#_upload_content */
        def upload(path: String, content: File) = Upload((path, content))

        /** https://bintray.com/docs/api.html#_publish_discard_uploaded_content */
        def publish =
          complete[Response](contentBase.POST / name / version / "publish")

        /** https://bintray.com/docs/api.html#_publish_discard_uploaded_content */
        def discard =
          complete[Response](
            json.content(contentBase.POST) / name / version / "publish"
              << json.str("discard" -> true))
      }

      /** Logs interface
       *  see also http://blog.bintray.com/2013/11/18/its-your-content-claim-the-logs-for-it/ */
      object Logs extends Client.Completion[Response] {
        private[this] def logsBase =
          apiHost / "packages" / subject / repo / name / "logs"
        def apply[T](handler: Client.Handler[T]) =
          request(logsBase)(handler)
        def log(name: String) =
          complete[Response](logsBase / name)
      }

      private[this] def pkgBase =
        apiHost / "packages" / subject / repo / name

      /** https://bintray.com/docs/api.html#_get_package */
      override def apply[T](handler: Client.Handler[T]) =
        request(pkgBase)(handler)

      /** https://bintray.com/docs/api.html#_delete_package */
      def delete =
        complete[Response](pkgBase.DELETE)

      /** https://bintray.com/docs/api.html#_update_package */
      def update(desc: String, labels: String*) =
        complete[Response](json.content(pkgBase.PATCH) / name <<
                 json.str(
                   ("desc"   -> desc) ~
                   ("labels" -> labels.toList)))

      def attrs = Attrs

      def version(version: String) =
        Version(version)

      def latest = version("_latest")

      /** https://bintray.com/docs/api.html#_create_version */
      def createVersion(version: String) =
        CreateVersion(version)

      case class MvnUpload(
        _artifact: (String, File),
        _publish: Boolean = false,
        _exploded: Boolean = false)
        extends Client.Completion[Response] {
        def artifact(path: String, content: File) =
          copy(_artifact = (path, content))
        def publish(pub: Boolean) = copy(_publish = pub)
        def exploded(explode: Boolean) = copy(_exploded = explode)
        def apply[T](handler: Client.Handler[T]) =
          request(appendPath(
            apiHost.PUT / "maven" / subject / repo / name,
            publishPath(_artifact._1, _publish, _exploded)) <<< _artifact._2)(handler)
      }

      /** https://bintray.com/docs/api.html#_maven_upload
       *  path should be in standard mvn format
       *  i.e. com/org/name/version/name-version.pom
       */
      def mvnUpload(path: String, content: File) = MvnUpload((path, content))

      def logs = Logs
    }

    private[this] def base =
      apiHost / "repos" / subject / repo

    private[this] def linkBase =
      apiHost / "repository" / subject / repo / "links"

    override def apply[T](handler: Client.Handler[T]) =
      request(base)(handler)

    /** https://bintray.com/docs/api.html#_get_repository */
    def packages(pos: Int = 0, prefix: Option[String] = None) =
      complete[List[PackageSummary]](
        base / "packages" <<? Map("start_pos" -> pos.toString) ++ prefix.map(("start_name" -> _)))

    /** https://bintray.com/docs/api.html#_link_package */
    def link(subject: String, repo: String, pkg: String) = 
      complete[Response](linkBase.PUT / subject / repo / pkg)

    /** https://bintray.com/docs/api.html#_unlink_package */
    def unlink(subject: String, repo: String, pkg: String) =
      complete[Response](linkBase.DELETE / subject / repo / pkg)

    /** Reference to package interface */
    def get(pkg: String) =
      Package(pkg)

    /** https://bintray.com/docs/api.html#_create_package
     *  the provided licenses should be defined under Licenses.Names */
    def createPackage(name: String) =
      PackageCreate(name)

    /** https://bintray.com/docs/api.html#_gpg_sign_a_file
     *  see also http://blog.bintray.com/2013/08/06/fight-crime-with-gpg/
     *  see also https://bintray.com/docs/uploads/uploads_gpgsigning.html
     */
    def sign(passphrase: String, path: String) =
      complete[Response](
        appendPath(json.content(apiHost.POST) / "gpg" / subject / repo, path)
          << json.str(("passphrase" -> passphrase)))
  }

  /** User methods */
  case class User(user: String)
   extends Client.Completion[Response] {
    private[this] def userBase = apiHost / "users" / user

    /** https://bintray.com/docs/api.html#_get_user */
    override def apply[T](handler: Client.Handler[T]) =
      request(userBase)(handler)

    /** https://bintray.com/docs/api.html#_get_followers */
    def followers(pos: Int = 0) =
      complete[Response](userBase / "followers" <<? Map("start_pos" -> pos.toString))
  }

  /** Webhook methods */
  case class Webhooks(subject: String, repo: Option[String] = None)
    extends Client.Completion[Response] {

    private[this] def hookBase = {
      val hooks = apiHost / "webhooks" / subject
      repo.map(hooks / _).getOrElse(hooks)
    }

    /** https://bintray.com/docs/api.html#_get_webhooks */
    override def apply[T](handler: Client.Handler[T]) =
      request(hookBase)(handler)

     /** https://bintray.com/docs/api.html#_register_a_webhook */
     def create(pkg: String, url: String, method: Webhook.Method) =
      complete[Response](
        json.content(hookBase.POST) / pkg
          << json.str(("url"    -> url) ~
                      ("method" -> method.name)))

    /** https://bintray.com/docs/api.html#_delete_a_webhook */
    def delete(pkg: String) =
      complete[Response](hookBase.DELETE / pkg)

    /** https://bintray.com/docs/api.html#_test_a_webhook */
    def test(pkg: String, version: String) =
      complete[Response](hookBase.POST / pkg / version)
  }

  /** Search methods */
  object Search {
    private[this] def searchBase = apiHost / "search"

    class AttributeSearch {
      private[this] def attrSearchBase = apiHost / "search" / "attributes"

      case class SearchTarget(
        endpoint: Req,
        _queries: Seq[(String, AttrQuery[_])] =
          Seq.empty[(String, AttrQuery[_])])
        extends Client.Completion[Response] {

        def is[A <: Attr[_]](name: String, attr: A) =
          copy(_queries = (name, AttrIs(attr)) +: _queries)

        def oneOf[A <: Attr[_]](name: String, attrs: A*) =
          copy(_queries = (name, AttrOneOf(attrs)) +: _queries)
        
        override def apply[T](handler: Client.Handler[T]) =
          request(json.content(endpoint.POST) <<
                  json.str(AttrsSearchJson(_queries)))(handler)
      }

      def ofPackageVersions(subject: String, repo: String, pkg: String) =
        SearchTarget(attrSearchBase / subject / repo / pkg / "versions")

      def ofPackages(subject: String, repo: String) =
        SearchTarget(attrSearchBase / subject / repo)
    }

    case class RepoSearch(
      _name: Option[String] = None,
      _desc: Option[String] = None,
      _pos: Int = 0) extends Client.Completion[Response] {
      def name(n: String) = copy(_name = Some(n))
      def desc(d: String) = copy(_desc = Some(d))
      def startPos(start: Int) = copy(_pos = start)
      def apply[T](handler: Client.Handler[T]) =
        request(searchBase / "repos" <<?
                Map("start_pos"   -> _pos.toString) ++
                _name.map("name" -> _) ++
                _desc.map("desc" -> _))(handler)
    }

    case class PackageSearch(
      _pos: Int = 0,
      _name: Option[String] = None,
      _desc: Option[String] = None,
      _subject: Option[String] = None,
      _repo: Option[String] = None) extends Client.Completion[Response] {
      def name(pkg: String) = copy(_name = Some(pkg))
      def desc(d: String) = copy(_desc = Some(d))
      def subject(sub: String) = copy(_subject = Some(sub))
      def repo(r: String) = copy(_repo = Some(r))
      def startPos(start: Int) = copy(_pos = start)
      override def apply[T](handler: Client.Handler[T]) =
        request(searchBase / "packages" <<?
                Map("start_pos" -> _pos.toString) ++
                _name.map("name" -> _) ++
                _desc.map("desc" -> _) ++
                _subject.map("subject" -> _) ++
                _repo.map("repo" -> _))(handler)
    }

    case class FileSearch(
      _file: String,
      _repo: Option[String] = None,
      _pos: Int = 0) extends Client.Completion[Response] {
      def file(f: String) = copy(_file = f)
      def repo(r: String) = copy(_repo = Some(r))
      def startPos(start: Int) = copy(_pos = start)
      override def apply[T](handler: Client.Handler[T]) =
        request(searchBase / "file" <<?
                Map("name" -> _file,
                    "start_pos" -> _pos.toString) ++
                _repo.map(("repo" -> _)))(handler)
    }

    case class ShaSearch(
      _sha: String,
      _repo: Option[String] = None,
      _pos: Int = 0) extends Client.Completion[Response] {
      def sha(s: String) = copy(_sha = s)
      def repo(r: String) = copy(_repo = Some(r))
      def startPos(start: Int) = copy(_pos = start)
      override def apply[T](handler: Client.Handler[T]) =
        request(searchBase / "file" <<?
                Map("sha" -> _sha,
                    "start_pos" -> _pos.toString) ++
                _repo.map(("repo" -> _)))(handler)
    }

    case class UserSearch(
      _name: String,
      _pos: Int = 0) extends Client.Completion[Response] {
      def name(n: String) = copy(_name = n)
      def startPos(start: Int) = copy(_pos = start)
      override def apply[T](handler: Client.Handler[T]) =
        request(searchBase / "users" <<?
                Map("name" -> _name,
                    "start_pos" -> _pos.toString))(handler)
    }

    /** https://bintray.com/docs/api.html#_repository_search */
    def repos = RepoSearch()

    /** https://bintray.com/docs/api.html#_package_search */
    // todo break out into case class interface.
    def packages = PackageSearch()

    /** https://bintray.com/docs/api.html#_file_search_by_name */
    def file(name: String) = FileSearch(name)

    /** https://bintray.com/docs/api.html#_file_search_by_checksum */
    def sha(sha: String) = ShaSearch(sha)

    /** https://bintray.com/docs/api.html#_user_search */
    def users(name: String) = UserSearch(name)

    /** https://bintray.com/docs/api.html#_attribute_search */
    def attributes = new AttributeSearch
  }

  /** https://bintray.com/docs/api.html#_get_repositories */
  def repos(subject: String) =
    complete[List[RepoSummary]](apiHost / "repos" / subject)

  def repo(subject: String, repo: String) =
    Repo(subject, repo)

  def user(name: String) =
    User(name)

  def webooks(subject: String, repo: Option[String] = None) =
    Webhooks(subject, repo)

  def search = Search
}
