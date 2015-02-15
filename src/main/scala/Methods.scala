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
      extends Client.Completion[bintry.Package] {

      def desc(d: String) = copy(_desc = Some(d))
      def labels(ls: String*) = copy(_labels = ls.toList)
      def licenses(ls: String*) = copy(_licenses = ls.toList)
      def vcs(url: String) = copy(_vcs = Some(url))

      /** https://bintray.com/docs/api.html#_create_package */
      override def apply[T](handler: Client.Handler[T]) =
        request((json.content(apiHost / "packages" / subject / repo).POST)
                << body)(handler)

      def body = json.str(
        ("name"     -> name) ~
        ("desc"     -> _desc) ~
        ("licenses" -> _licenses) ~
        ("labels"   -> _labels) ~
        ("vcs_url"  -> _vcs))
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
          complete[Attr.AttrMap](
            (if (names.isEmpty) pkgAttrBase else pkgAttrBase)
              <<? Map("names" -> names.mkString(",")))

        /** https://bintray.com/docs/api.html#_set_attributes */
        def set[A <: Attr[_]](attrs: (String, Iterable[A])*) =
          complete[Attr.AttrMap](
            json.content(pkgAttrBase.POST)
              << json.str(AttrsToJson(attrs)))

        /** https://bintray.com/docs/api.html#_update_attributes */
        def update[A <: Attr[_]](attrs: (String, Iterable[A])*) =
          complete[Attr.AttrMap](
            json.content(pkgAttrBase.PATCH)
              << json.str(AttrsToJson(attrs)))

        /** https://bintray.com/docs/api.html#_delete_attributes */
        def delete(names: String*) =
          complete[Message](
            (if (names.isEmpty) pkgAttrBase.DELETE else pkgAttrBase.DELETE)
              <<? Map("names" -> names.mkString(",")))
      }

      private[this] def publishPath(
        path: String, publish: Boolean, replace: Boolean) =
          "%s;publish=%s;override=%s".format(
            path,
            if (publish) 1 else 0,
            if (replace) 1 else 0)

      /** https://bintray.com/docs/api.html#_create_version */
      case class CreateVersion(
        version: String,
        _desc: Option[String]                  = None,
        _vcsTag: Option[String]                = None,
        _ghNotesFile: Option[String]           = None,
        _useGhTagReleaseNotes: Option[Boolean] = None)
        extends Client.Completion[bintry.Version] {

        def desc(d: String) = copy(_desc = Some(d))

        def vcsTag(tag: String) = copy(_vcsTag = Some(tag))

        def githubNotesFile(n: String) = copy(_ghNotesFile = Some(n))

        def useGithubTagReleaseNotes(use: Boolean) = copy(_useGhTagReleaseNotes = Some(use))

        def apply[T](handler: Client.Handler[T]) =
          request(json.content(pkgBase.POST) / "versions"
                  << body)(handler)

        def body = json.str(
          ("name"          -> version) ~
          ("desc"          -> _desc)   ~
          ("github_release_notes_file"  -> _ghNotesFile)  ~
          ("github_use_tag_release_notes" -> _useGhTagReleaseNotes) ~
          ("vcs_tag"       -> _vcsTag))
      }

      /** Package version methods */
      case class Version(version: String)
        extends Client.Completion[bintry.Version] {

        /** version  attr interface */
        object Attrs {
          private def versionAttrBase =
            (apiHost / "packages" /
             subject / repo / name / "versions" / version / "attributes")

          /** https://bintray.com/docs/api.html#_get_attributes */
          def apply(names: String*) =
            complete[Attr.AttrMap](
              (if (names.isEmpty) versionAttrBase else versionAttrBase)
                <<? Map("names" -> names.mkString(",")))

          /** https://bintray.com/docs/api.html#_set_attributes */
          def set[A <: Attr[_]](attrs: (String, Iterable[A])*) =
            complete[Attr.AttrMap](
              json.content(versionAttrBase.POST) << json.str(AttrsToJson(attrs)))

          /** https://bintray.com/docs/api.html#_update_attributes */
          def update[A <: Attr[_]](attrs: (String, Iterable[A])*) =
            complete[Attr.AttrMap](
              json.content(versionAttrBase.PATCH) << json.str(AttrsToJson(attrs)))

          /** https://bintray.com/docs/api.html#_delete_attributes */
          def delete(names: String*) =
            complete[Message](
              (if (names.isEmpty) versionAttrBase.DELETE else versionAttrBase.DELETE)
                <<? Map("names" -> names.mkString(",")))
        }

        /** version upload interface
         *  https://bintray.com/docs/api/#_upload_content
         */
        case class Upload(
          _artifact: (String, File),
          _publish: Boolean = false,
          _replace: Boolean = false)
          extends Client.Completion[Response] { // todo: Rep

          def artifact(path: String, content: File) = copy(
            _artifact = (path, content)
          )

          def publish(pub: Boolean) = copy(_publish = pub)

          def replace(rep: Boolean) = copy(_replace = rep)

          def apply[T](handler: Client.Handler[T]) =
            request(appendPath(
            contentBase.PUT,
            publishPath(_artifact._1, _publish, _replace)) <:< Map(
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
            json.content(apiHost.POST)
              / "maven_central_sync" / subject
              / repo / name / "versions" / version
              << json.str(
                ("username" -> sonatypeUser) ~
                ("password" -> sonatypePassword) ~
                ("close"    -> Some("1").filter(Function.const(close)))))

        /** https://bintray.com/docs/api.html#_delete_version */
        def delete =
          complete[Message](versionBase.DELETE)

        // todo: add structure
        /** https://bintray.com/docs/api.html#_update_version */
        def update(desc: String) =
          complete[Message](json.content(versionBase.PATCH) <<
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
      object Logs extends Client.Completion[Response] { // todo: Rep
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

      // todo: add structure
      /** https://bintray.com/docs/api.html#_update_package */
      def update(desc: String, labels: String*) =
        complete[Message](
          json.content(pkgBase.PATCH)
          << json.str(
            ("desc"   -> desc) ~
            ("labels" -> labels.toList)))

      def attrs = Attrs

      def version(version: String) =
        Version(version)

      // fixme: this api seems flaky
      def latest = version("_latest")

      /** https://bintray.com/docs/api.html#_create_version */
      def createVersion(version: String) =
        CreateVersion(version)

      /** https://bintray.com/docs/api/#_maven_upload */
      case class MvnUpload(
        _artifact: (String, File),
        _publish: Boolean = false,
        _exploded: Boolean = false)
        extends Client.Completion[Response] { // todo: Rep

        def artifact(path: String, content: File) =
          copy(_artifact = (path, content))

        def publish(pub: Boolean) =
          copy(_publish = pub)

        def exploded(explode: Boolean) =
          copy(_exploded = explode)

        def apply[T](handler: Client.Handler[T]) =
          _artifact match {
            case (path, file) =>
              request(appendPath(
                apiHost.PUT / "maven" / subject / repo / name,
                publishPath(path, _publish, _exploded)) <<< file)(handler)
          }
      }

      /** https://bintray.com/docs/api/#_debian_upload */
      /*case class DebianUpload(
        _artifact: (String, File),
        _publish: Boolean            = false,
        _override: Boolean           = false,
        _distributions: List[String] = Nil,
        _components: List[String]    = Nil,
        _arches: List[String]        = Nil)
        extends Client.Completion[Response] { // todo: Rep
        def artifact(path: String, content: File) =
          copy(_artifact = (path, content))
        def publish(pub: Boolean) =
          copy(_publish = pub)
        def replace(replace: Boolean) =
          copy(_override = replace)
        def distributions(dx: String*) =
          copy(_distributions = dx.toList)
        def components(cx: String*) =
          copy(_components = cx.toList)
        def architectures(ax: String) =
          copy(_arches = ax.toList)

        def apply[T](handeler: Client.Handler[T]) =
          _artifact match {
            case (path, file) =>
              val headers = Map.empty[String, String]
                ++ _distributions.filter()
              request(appendPath(
                apiHost.PUT <:< headers / "content" / subject / repo / name,
                publishPath(path, _publish, _exploded)) <<< file)(handler)
            }
      }*/

        

      /** https://bintray.com/docs/api.html#_maven_upload
       *  path should be in standard mvn format
       *  i.e. com/org/name/version/name-version.pom
       */
      def mvnUpload(path: String, content: File) = MvnUpload((path, content))

      /** https://bintray.com/docs/api/#_debian_upload */
      //def debianUpload(path: String, content: File) = DebianUpload((path, content))

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
   extends Client.Completion[bintry.User] {
    private[this] def userBase = apiHost / "users" / user

    /** https://bintray.com/docs/api.html#_get_user */
    override def apply[T](handler: Client.Handler[T]) =
      request(userBase)(handler)

    /** https://bintray.com/docs/api.html#_get_followers */
    def followers(pos: Int = 0) =
      complete[List[Name]](userBase / "followers" <<? Map("start_pos" -> pos.toString))
  }

  /** Webhook methods */
  case class Webhooks(
    subject: String,
    repo: Option[String] = None)
    extends Client.Completion[Response] { // todo: Rep

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
        extends Client.Completion[Response] { // todo: Rep

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
      _pos: Int = 0) extends Client.Completion[Response] { // todo: Rep
      def name(n: String) = copy(_name = Some(n))
      def desc(d: String) = copy(_desc = Some(d))
      def startPos(start: Int) = copy(_pos = start)
      def apply[T](handler: Client.Handler[T]) =
        request(searchBase / "repos" <<? query)(handler)

      def query = Map("start_pos"   -> _pos.toString) ++
        _name.map("name" -> _) ++
        _desc.map("desc" -> _)
    }

    case class PackageSearch(
      _pos: Int = 0,
      _name: Option[String] = None,
      _desc: Option[String] = None,
      _subject: Option[String] = None,
      _repo: Option[String] = None) extends Client.Completion[List[bintry.Package]] {
      def name(pkg: String) = copy(_name = Some(pkg))
      def desc(d: String) = copy(_desc = Some(d))
      def subject(sub: String) = copy(_subject = Some(sub))
      def repo(r: String) = copy(_repo = Some(r))
      def startPos(start: Int) = copy(_pos = start)
      override def apply[T](handler: Client.Handler[T]) =
        request(searchBase / "packages" <<? query)(handler)

      def query = Map("start_pos" -> _pos.toString) ++
        _name.map("name" -> _) ++
        _desc.map("desc" -> _) ++
        _subject.map("subject" -> _) ++
        _repo.map("repo" -> _)
    }

    case class FileSearch(
      _file: String,
      _repo: Option[String] = None,
      _pos: Int = 0) extends Client.Completion[Response] { // todo: Rep
      def file(f: String) = copy(_file = f)
      def repo(r: String) = copy(_repo = Some(r))
      def startPos(start: Int) = copy(_pos = start)
      override def apply[T](handler: Client.Handler[T]) =
        request(searchBase / "file" <<? query)(handler)

      def query = Map(
        "name" -> _file,
        "start_pos" -> _pos.toString) ++
        _repo.map(("repo" -> _))
    }

    case class ShaSearch(
      _sha: String,
      _repo: Option[String] = None,
      _pos: Int = 0) extends Client.Completion[Response] { // todo: Rep
      def sha(s: String) = copy(_sha = s)
      def repo(r: String) = copy(_repo = Some(r))
      def startPos(start: Int) = copy(_pos = start)
      override def apply[T](handler: Client.Handler[T]) =
        request(searchBase / "file" <<? query)(handler)
      def query = Map(
        "sha" -> _sha,
        "start_pos" -> _pos.toString) ++
        _repo.map(("repo" -> _))
    }

    case class UserSearch(
      _name: String,
      _pos: Int = 0) extends Client.Completion[Response] { // todo: Rep
      def name(n: String) = copy(_name = n)
      def startPos(start: Int) = copy(_pos = start)
      override def apply[T](handler: Client.Handler[T]) =
        request(searchBase / "users" <<? query)(handler)

      def query = Map(
        "name" -> _name,
        "start_pos" -> _pos.toString)
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
