package bintry

import dispatch._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.Printer.compact
import org.json4s.native.JsonMethods.render
import java.io.File

trait Methods { self: Requests =>
  import bintry.Util._
  case class Repo(sub: String, repo: String) extends Client.Completion {

    case class PackageCreate(
      name: String,
      _desc: Option[String] = None,
      _labels: List[String] = Nil,
      _licenses: List[String] = Nil,
      _vcs: Option[String] = None)
      extends Client.Completion {
      def desc(d: String) = copy(_desc = Some(d))
      def labels(ls: String*) = copy(_labels = ls.toList)
      def licences(ls: String*) = copy(_licenses = ls.toList)
      def vcs(url: String) = copy(_vcs = Some(url))

      /** https://bintray.com/docs/api.html#_create_package */
      override def apply[T](handler: Client.Handler[T]) =
        request((apiHost / "packages" / sub / repo).POST <<
               compact(render(
                 ("name"     -> name) ~
                 ("desc"     -> _desc) ~
                 ("licenses" -> _licenses) ~
                 ("labels"   -> _labels) ~
                 ("vcs_url"  -> _vcs))))(handler)
    }

    case class Package(name: String) extends Client.Completion {
      object Attrs {
        private def base = apiHost / "packages" / sub / repo / name / "attributes"

        /** https://bintray.com/docs/api.html#_get_attributes */
        def apply(names: String*) =
          complete(if (names.isEmpty) base else base <<?
                   Map("names" -> names.mkString(",")))

        /** https://bintray.com/docs/api.html#_set_attributes */
        def set[A <: Attr[_]](attrs: (String, Iterable[A])*) =
          complete(base.POST << compact(render(AttrsToJson(attrs))))

        /** https://bintray.com/docs/api.html#_update_attributes */
        def update[A <: Attr[_]](attrs: (String, Iterable[A])*) =
          complete(base.PATCH << compact(render(AttrsToJson(attrs))))

        /** https://bintray.com/docs/api.html#_delete_attributes */
        def delete(names: String*) =
          complete(if (names.isEmpty) base.DELETE else base.DELETE <<?
                   Map("names" -> names.mkString(",")))
      }

      private[this] def publishPath(
        path: String, publish: Boolean, explode: Boolean) =
          "%s;publish=%s;explode=%s".format(
            path,
            if (publish) 1 else 0,
            if (explode) 1 else 0)

      case class Version(vers: String) extends Client.Completion {
        object Attrs {
          private def base =
            apiHost / "packages" / sub / repo / name / "versions" / vers / "attributes"

          /** https://bintray.com/docs/api.html#_get_attributes */
          def apply(names: String*) =
            complete(if (names.isEmpty) base else base <<?
                     Map("names" -> names.mkString(",")))

          /** https://bintray.com/docs/api.html#_set_attributes */
          def set[A <: Attr[_]](attrs: (String, Iterable[A])*) =
            complete(base.POST << compact(render(AttrsToJson(attrs))))

          /** https://bintray.com/docs/api.html#_update_attributes */
          def update[A <: Attr[_]](attrs: (String, Iterable[A])*) =
            complete(base.PATCH << compact(render(AttrsToJson(attrs))))

          /** https://bintray.com/docs/api.html#_delete_attributes */
          def delete(names: String*) =
            complete(if (names.isEmpty) base.DELETE else base.DELETE <<?
                     Map("names" -> names.mkString(",")))
        }

        private[this] def base =
          apiHost / "packages" / sub / repo / name / "versions" / vers

        private[this] def contentBase = apiHost / "content" / sub / repo

        /** https://bintray.com/docs/api.html#_get_version */
        override def apply[T](handler: Client.Handler[T]) =
          request(base)(handler)

        /** https://bintray.com/docs/api.html#_gpg_sign_a_version */
        def sign(passphrase: String) =
          complete(apiHost.POST / "gpg" / sub / repo / name / "versions" / vers <<
                   compact(render(("passphrase" -> passphrase))))

        /** https://bintray.com/docs/api.html#_sync_version_artifacts_to_maven_central */
        def sync(sonatypeUser: String, sonatypePassword: String, close: Boolean = false) =
          complete(apiHost.POST / "maven_central_sync" / sub / repo / name / "versions" / vers <<
                 compact(render(("username" -> sonatypeUser) ~
                                ("password" -> sonatypePassword) ~
                                ("close"    -> Some("1").filter(Function.const(close))))))

        /** https://bintray.com/docs/api.html#_delete_version */
        def delete =
          complete(base.DELETE)

        /** https://bintray.com/docs/api.html#_update_version */
        def update(desc: String) =
          complete(base.PATCH <<
                   compact(render(("desc" -> desc))))
        
        def attrs = Attrs

        /** https://bintray.com/docs/api.html#_upload_content */
        def upload(
          path: String, content: File,
          publish: Boolean = false, explode: Boolean = false) =
            complete(appendPath(
              contentBase.PUT,
              publishPath(path, publish, explode)) <:< Map(
              "X-Bintray-Package" -> name,
              "X-Bintray-Version" -> vers
            ) <<< content)

        /** https://bintray.com/docs/api.html#_publish_discard_uploaded_content */
        def publish =
          complete(contentBase.POST / name / vers / "publish")

        /** https://bintray.com/docs/api.html#_publish_discard_uploaded_content */
        def discard =
          complete(contentBase.POST / name / vers / "publish" << compact(render("discard" -> true)))
      }

      object Logs extends Client.Completion {
        private[this] def logsBase = apiHost / "packages" / sub / repo / name / "logs"
        def apply[T](handler: Client.Handler[T]) =
          request(logsBase)(handler)
        def log(name: String) =
          complete(logsBase / name)
      }

      private[this] def base = apiHost / "packages" / sub / repo / name

      /** https://bintray.com/docs/api.html#_get_package */
      override def apply[T](handler: Client.Handler[T]) =
        request(base)(handler)

      /** https://bintray.com/docs/api.html#_delete_package */
      def delete =
        complete(base.DELETE)

      /** https://bintray.com/docs/api.html#_update_package */
      def update(desc: String, labels: String*) =
        complete(base.PATCH / name <<
                 compact(render(
                   ("desc" -> desc) ~
                   ("labels" -> labels.toList))))

      def attrs = Attrs

      def version(version: String = "_latest") =
        Version(version)

      /** https://bintray.com/docs/api.html#_create_version */
      def createVersion(
        version: String, notes: Option[String] = None,
        readme: Option[String] = None) =
        complete(base.POST / "versions" <<
                 compact(render(
                   ("name" -> version) ~
                   ("release_notes" -> notes.map(JString(_)).getOrElse(JNothing)) ~
                   ("release_url" -> readme.map(JString(_)).getOrElse(JNothing)))))

      /** https://bintray.com/docs/api.html#_maven_upload
       *  path should be in standard mvn format
       *  i.e. com/org/name/version/name-version.pom
       */
      def mvnUpload(
        path: String, content: File,
        publish: Boolean = false, explode: Boolean = false) =
        complete(appendPath(apiHost.PUT / "maven" / sub / repo / name,
                            publishPath(path, publish, explode)) <<< content)

      def logs =
        Logs
    }

    private[this] def base = apiHost / "repos" / sub / repo

    private[this] def packagesBase = apiHost / "packages" / sub / repo

    private[this] def linkBase = apiHost / "repository" / sub / repo / "links"

    override def apply[T](handler: Client.Handler[T]) =
      request(base)(handler)

    /** https://bintray.com/docs/api.html#_get_repository */
    def packages(pos: Int = 0) =
      complete(base / "packages" <<? Map("start_pos" -> pos.toString))

    /** https://bintray.com/docs/api.html#_link_package */
    def link(subject: String, repo: String, pkg: String) = 
      complete(linkBase.PUT / subject / repo / pkg)

    /** https://bintray.com/docs/api.html#_unlink_package */
    def unlink(subject: String, repo: String, pkg: String) =
      complete(linkBase.DELETE / subject / repo / pkg)

    def get(pkg: String) =
      Package(pkg)

    /** https://bintray.com/docs/api.html#_create_package
     *  the provided licenses should be defined under Licenses.Names */
    def createPackage(name: String) =
      PackageCreate(name)

    /** https://bintray.com/docs/api.html#_gpg_sign_a_file */
    def sign(passphrase: String, path: String) =
      complete(appendPath(apiHost.POST / "gpg" / sub / repo, path) <<
               compact(render(("passphrase" -> passphrase))))      
  }

  case class User(user: String) extends Client.Completion {
    private def base = apiHost / "users" / user

    /** https://bintray.com/docs/api.html#_get_user */
    override def apply[T](handler: Client.Handler[T]) =
      request(base)(handler)

    /** https://bintray.com/docs/api.html#_get_followers */
    def followers(pos: Int = 0) =
      complete(base / "followers" <<? Map("start_pos" -> pos.toString))
  }

  case class Webhooks(sub: String, repo: Option[String] = None)
    extends Client.Completion {
    sealed trait Method
    object POST extends Method
    object PUT extends Method
    object GET extends Method

    private def base = {
      val hooks = apiHost / "webhooks" / sub
      repo.map(hooks / _).getOrElse(hooks)
    }

    /** https://bintray.com/docs/api.html#_get_webhooks */
    override def apply[T](handler: Client.Handler[T]) =
      request(base)(handler)

     /** https://bintray.com/docs/api.html#_register_a_webhook */
     def create(pkg: String, url: String, method: Method) =
      complete(base.POST / pkg << compact(render(
        ("url" -> url) ~
        ("method" -> (method match {
          case POST => "post"
          case PUT => "put"
          case GET => "get"
        })))))

    /** https://bintray.com/docs/api.html#_delete_a_webhook */
    def delete(pkg: String) =
      complete(base.DELETE / pkg)

    /** https://bintray.com/docs/api.html#_test_a_webhook */
    def test(pkg: String, version: String) =
      complete(base.POST / pkg / version)
  }

  object Search {
    private def base = apiHost / "search"

    class AttributeSearch {
      val base = apiHost / "search" / "attributes"
      case class SearchTarget(
        endpoint: Req,
        _queries: Seq[(String, AttrQuery[_])] =
          Seq.empty[(String, AttrQuery[_])])
        extends Client.Completion {
        def is[A <: Attr[_]](name: String, attr: A) =
          copy(_queries = (name, AttrIs(attr)) +: _queries)
        def oneOf[A <: Attr[_]](name: String, attrs: A*) =
          copy(_queries = (name, AttrOneOf(attrs)) +: _queries)
        override def apply[T](handler: Client.Handler[T]) = {
          val query = compact(render(AttrsSearchJson(_queries)))
          println("sending query %s" format query)
          request(endpoint.POST << query)(handler)
        }
      }
      def ofPackageVersions(sub: String, repo: String, pkg: String) =
        SearchTarget(base / sub / repo / pkg / "versions")
      def ofPackages(sub: String, repo: String) =
        SearchTarget(base / sub / repo)
    }

    /** https://bintray.com/docs/api.html#_repository_search */
    def repos(
      name: Option[String] = None, desc: Option[String] = None,
      pos: Int = 0) =
      complete(base / "repos" <<?
               Map("start_pos" -> pos.toString) ++
                 name.map("name" -> _) ++
                 desc.map("desc" -> _))

    /** https://bintray.com/docs/api.html#_package_search */
    def packages(
      name: Option[String] = None, desc: Option[String] = None,
      subject: Option[String] = None, repo: Option[String] = None,
      pos: Int = 0) =
        complete(base / "packages" <<?
                 Map("start_pos" -> pos.toString) ++
                   name.map("name" -> _) ++
                   desc.map("desc" -> _) ++
                   subject.map("subject" -> _) ++
                   repo.map("repo" -> _))

    /** https://bintray.com/docs/api.html#_file_search_by_name */
    def file(
      name: String, repo: Option[String] = None,
      pos: Int = 0) =
        complete(base / "file" <<?
                 Map("name" -> name, "start_pos" -> pos.toString) ++
                     repo.map(("repo" -> _)))

    /** https://bintray.com/docs/api.html#_file_search_by_checksum */
    def sha(
      sha: String, repo: Option[String] = None, pos: Int = 0) =
        complete(base / "file" <<?
                 Map("sha" -> sha, "start_pos" -> pos.toString) ++
                     repo.map(("repo" -> _)))

    /** https://bintray.com/docs/api.html#_user_search */
    def users(name: String, pos: Int = 0) =
      complete(base / "users" <<?
               Map("name" -> name, "start_pos" -> pos.toString))

    /** https://bintray.com/docs/api.html#_attribute_search */
    def attributes = new AttributeSearch
  }

  /** https://bintray.com/docs/api.html#_get_repositories */
  def repos(sub: String) =
    complete(apiHost / "repos" / sub)

  def repo(sub: String, repo: String) =
    Repo(sub, repo)

  def user(name: String) =
    User(name)

  def webooks(sub: String, repo: Option[String] = None) =
    Webhooks(sub, repo)

  def search = Search
}
