package bintry

import dispatch._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.Printer.compact
import org.json4s.native.JsonMethods.render
import java.io.File

trait Methods { self: Requests =>
  import bintry.Util._
  /** All methods relating to a given repo */
  case class Repo(subject: String, repo: String) extends Client.Completion {

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
        request((apiHost / "packages" / subject / repo).POST <<
               compact(render(
                 ("name"     -> name) ~
                 ("desc"     -> _desc) ~
                 ("licenses" -> _licenses) ~
                 ("labels"   -> _labels) ~
                 ("vcs_url"  -> _vcs))))(handler)
    }

    /** Package methods */
    case class Package(name: String) extends Client.Completion {
      object Attrs {
        private def pkgAttrBase = apiHost / "packages" / subject / repo / name / "attributes"

        /** https://bintray.com/docs/api.html#_get_attributes */
        def apply(names: String*) =
          complete(if (names.isEmpty) pkgAttrBase else pkgAttrBase <<?
                   Map("names" -> names.mkString(",")))

        /** https://bintray.com/docs/api.html#_set_attributes */
        def set[A <: Attr[_]](attrs: (String, Iterable[A])*) =
          complete(pkgAttrBase.POST << compact(render(AttrsToJson(attrs))))

        /** https://bintray.com/docs/api.html#_update_attributes */
        def update[A <: Attr[_]](attrs: (String, Iterable[A])*) =
          complete(pkgAttrBase.PATCH << compact(render(AttrsToJson(attrs))))

        /** https://bintray.com/docs/api.html#_delete_attributes */
        def delete(names: String*) =
          complete(if (names.isEmpty) pkgAttrBase.DELETE else pkgAttrBase.DELETE <<?
                   Map("names" -> names.mkString(",")))
      }

      private[this] def publishPath(
        path: String, publish: Boolean, explode: Boolean) =
          "%s;publish=%s;explode=%s".format(
            path,
            if (publish) 1 else 0,
            if (explode) 1 else 0)

      /** Package version methods */
      case class Version(vers: String) extends Client.Completion {
        object Attrs {
          private def versionAttrBase =
            apiHost / "packages" / subject / repo / name / "versions" / vers / "attributes"

          /** https://bintray.com/docs/api.html#_get_attributes */
          def apply(names: String*) =
            complete(if (names.isEmpty) versionAttrBase else versionAttrBase <<?
                     Map("names" -> names.mkString(",")))

          /** https://bintray.com/docs/api.html#_set_attributes */
          def set[A <: Attr[_]](attrs: (String, Iterable[A])*) =
            complete(versionAttrBase.POST << compact(render(AttrsToJson(attrs))))

          /** https://bintray.com/docs/api.html#_update_attributes */
          def update[A <: Attr[_]](attrs: (String, Iterable[A])*) =
            complete(versionAttrBase.PATCH << compact(render(AttrsToJson(attrs))))

          /** https://bintray.com/docs/api.html#_delete_attributes */
          def delete(names: String*) =
            complete(if (names.isEmpty) versionAttrBase.DELETE else versionAttrBase.DELETE <<?
                     Map("names" -> names.mkString(",")))
        }

        private[this] def versionBase =
          apiHost / "packages" / subject / repo / name / "versions" / vers

        private[this] def contentBase = apiHost / "content" / subject / repo

        /** https://bintray.com/docs/api.html#_get_version */
        override def apply[T](handler: Client.Handler[T]) =
          request(versionBase)(handler)

        /** https://bintray.com/docs/api.html#_gpg_sign_a_version
         *  see also http://blog.bintray.com/2013/08/06/fight-crime-with-gpg/
         */
        def sign(passphrase: String) =
          complete(apiHost.POST / "gpg" / subject / repo / name / "versions" / vers <<
                   compact(render(("passphrase" -> passphrase))))

        /** https://bintray.com/docs/api.html#_sync_version_artifacts_to_maven_central
         *  see also http://blog.bintray.com/2014/02/11/bintray-as-pain-free-gateway-to-maven-central/
         */
        def sync(sonatypeUser: String, sonatypePassword: String, close: Boolean = false) =
          complete(apiHost.POST / "maven_central_sync" / subject / repo / name / "versions" / vers <<
                 compact(render(("username" -> sonatypeUser) ~
                                ("password" -> sonatypePassword) ~
                                ("close"    -> Some("1").filter(Function.const(close))))))

        /** https://bintray.com/docs/api.html#_delete_version */
        def delete =
          complete(versionBase.DELETE)

        /** https://bintray.com/docs/api.html#_update_version */
        def update(desc: String) =
          complete(versionBase.PATCH <<
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

      /** Logs interface */
      object Logs extends Client.Completion {
        private[this] def logsBase = apiHost / "packages" / subject / repo / name / "logs"
        def apply[T](handler: Client.Handler[T]) =
          request(logsBase)(handler)
        def log(name: String) =
          complete(logsBase / name)
      }

      private[this] def pkgBase = apiHost / "packages" / subject / repo / name

      /** https://bintray.com/docs/api.html#_get_package */
      override def apply[T](handler: Client.Handler[T]) =
        request(pkgBase)(handler)

      /** https://bintray.com/docs/api.html#_delete_package */
      def delete =
        complete(pkgBase.DELETE)

      /** https://bintray.com/docs/api.html#_update_package */
      def update(desc: String, labels: String*) =
        complete(pkgBase.PATCH / name <<
                 compact(render(
                   ("desc"   -> desc) ~
                   ("labels" -> labels.toList))))

      def attrs = Attrs

      def version(version: String = "_latest") =
        Version(version)

      /** https://bintray.com/docs/api.html#_create_version */
      def createVersion(
        version: String, notes: Option[String] = None,
        readme: Option[String] = None) =
        complete(pkgBase.POST / "versions" <<
                 compact(render(
                   ("name"          -> version) ~
                   ("release_notes" -> notes.map(JString(_)).getOrElse(JNothing)) ~
                   ("release_url"   -> readme.map(JString(_)).getOrElse(JNothing)))))

      /** https://bintray.com/docs/api.html#_maven_upload
       *  path should be in standard mvn format
       *  i.e. com/org/name/version/name-version.pom
       */
      def mvnUpload(
        path: String, content: File,
        publish: Boolean = false, explode: Boolean = false) =
        complete(appendPath(apiHost.PUT / "maven" / subject / repo / name,
                            publishPath(path, publish, explode)) <<< content)

      def logs = Logs
    }

    private[this] def base = apiHost / "repos" / subject / repo

    private[this] def packagesBase = apiHost / "packages" / subject / repo

    private[this] def linkBase = apiHost / "repository" / subject / repo / "links"

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

    /** https://bintray.com/docs/api.html#_gpg_sign_a_file
     *  see also http://blog.bintray.com/2013/08/06/fight-crime-with-gpg/
     */
    def sign(passphrase: String, path: String) =
      complete(appendPath(apiHost.POST / "gpg" / subject / repo, path) <<
               compact(render(("passphrase" -> passphrase))))      
  }

  /** User methods */
  case class User(user: String) extends Client.Completion {
    private[this] def userBase = apiHost / "users" / user

    /** https://bintray.com/docs/api.html#_get_user */
    override def apply[T](handler: Client.Handler[T]) =
      request(userBase)(handler)

    /** https://bintray.com/docs/api.html#_get_followers */
    def followers(pos: Int = 0) =
      complete(userBase / "followers" <<? Map("start_pos" -> pos.toString))
  }

  /** Webhook methods */
  case class Webhooks(subject: String, repo: Option[String] = None)
    extends Client.Completion {
    sealed trait Method {
      def name: String
    }
    object POST extends Method {
      val name = "post"
    }
    object PUT extends Method {
      val name = "put"
    }
    object GET extends Method {
      val name = "get"
    }

    private[this] def hookBase = {
      val hooks = apiHost / "webhooks" / subject
      repo.map(hooks / _).getOrElse(hooks)
    }

    /** https://bintray.com/docs/api.html#_get_webhooks */
    override def apply[T](handler: Client.Handler[T]) =
      request(hookBase)(handler)

     /** https://bintray.com/docs/api.html#_register_a_webhook */
     def create(pkg: String, url: String, method: Method) =
      complete(hookBase.POST / pkg << compact(render(
        ("url"    -> url) ~
        ("method" -> method.name))))

    /** https://bintray.com/docs/api.html#_delete_a_webhook */
    def delete(pkg: String) =
      complete(hookBase.DELETE / pkg)

    /** https://bintray.com/docs/api.html#_test_a_webhook */
    def test(pkg: String, version: String) =
      complete(hookBase.POST / pkg / version)
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
        extends Client.Completion {

        def is[A <: Attr[_]](name: String, attr: A) =
          copy(_queries = (name, AttrIs(attr)) +: _queries)

        def oneOf[A <: Attr[_]](name: String, attrs: A*) =
          copy(_queries = (name, AttrOneOf(attrs)) +: _queries)

        
        override def apply[T](handler: Client.Handler[T]) = {
          val query = compact(render(AttrsSearchJson(_queries)))
          request(endpoint.POST << query)(handler)
        }
      }

      def ofPackageVersions(subject: String, repo: String, pkg: String) =
        SearchTarget(attrSearchBase / subject / repo / pkg / "versions")

      def ofPackages(subject: String, repo: String) =
        SearchTarget(attrSearchBase / subject / repo)
    }

    /** https://bintray.com/docs/api.html#_repository_search */
    def repos(
      name: Option[String] = None, desc: Option[String] = None,
      pos: Int = 0) =
      complete(searchBase / "repos" <<?
               Map("start_pos"   -> pos.toString) ++
                 name.map("name" -> _) ++
                 desc.map("desc" -> _))

    /** https://bintray.com/docs/api.html#_package_search */
    def packages(
      name: Option[String] = None,
      desc: Option[String] = None,
      subject: Option[String] = None,
      repo: Option[String] = None,
      pos: Int = 0) =
      complete(searchBase / "packages" <<?
               Map("start_pos" -> pos.toString) ++
                 name.map("name" -> _) ++
                 desc.map("desc" -> _) ++
                 subject.map("subject" -> _) ++
                 repo.map("repo" -> _))

    /** https://bintray.com/docs/api.html#_file_search_by_name */
    def file(
      name: String,
      repo: Option[String] = None,
      pos: Int = 0) =
      complete(searchBase / "file" <<?
               Map("name" -> name, "start_pos" -> pos.toString) ++
                 repo.map(("repo" -> _)))

    /** https://bintray.com/docs/api.html#_file_search_by_checksum */
    def sha(
      sha: String,
      repo: Option[String] = None,
      pos: Int = 0) =
      complete(searchBase / "file" <<?
               Map("sha" -> sha, "start_pos" -> pos.toString) ++
                 repo.map(("repo" -> _)))

    /** https://bintray.com/docs/api.html#_user_search */
    def users(name: String, pos: Int = 0) =
      complete(searchBase / "users" <<?
               Map("name" -> name, "start_pos" -> pos.toString))

    /** https://bintray.com/docs/api.html#_attribute_search */
    def attributes = new AttributeSearch
  }

  /** https://bintray.com/docs/api.html#_get_repositories */
  def repos(subject: String) =
    complete(apiHost / "repos" / subject)

  def repo(subject: String, repo: String) =
    Repo(subject, repo)

  def user(name: String) =
    User(name)

  def webooks(subject: String, repo: Option[String] = None) =
    Webhooks(subject, repo)

  def search = Search
}
