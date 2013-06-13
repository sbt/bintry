package bintry

import com.ning.http.client.RequestBuilder
import dispatch._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.Printer.compact
import org.json4s.native.JsonMethods.render
import java.io.File

trait Methods { self: Requests =>

  case class Repo(sub: String, repo: String) extends Client.Completion {

    case class Package(name: String) extends Client.Completion { 
      object Attrs {
        private def base = apiHost / "packages" / sub / repo / name / "attributes"

        /** https://bintray.com/docs/rest/api.html#_get_attributes */
        def apply(names: String*) =
          complete(if (names.isEmpty) base else base <<?
                   Map("names" -> names.mkString(",")))

        /** https://bintray.com/docs/rest/api.html#_set_attributes */
        def set[A <: Attr[_]](attrs: Map[String, Iterable[A]]) =
          complete(base.POST << compact(render(AttrsJson(attrs))))

        /** https://bintray.com/docs/rest/api.html#_update_attributes */
        def update[A <: Attr[_]](attrs: Map[String, Iterable[A]]) =
          complete(base.PATCH << compact(render(AttrsJson(attrs))))

        /** https://bintray.com/docs/rest/api.html#_delete_attributes */
        def delete(names: String*) =
          complete(if (names.isEmpty) base.DELETE else base.DELETE <<?
                   Map("names" -> names.mkString(",")))
      }

      case class Version(vers: String) extends Client.Completion {
        object Attrs {
          private def base = apiHost / "packages" / sub / repo / name / "versions" / vers / "attributes"

          /** https://bintray.com/docs/rest/api.html#_get_attributes */
          def apply(names: String*) =
            complete(if (names.isEmpty) base else base <<? Map("names" -> names.mkString(",")))

          /** https://bintray.com/docs/rest/api.html#_set_attributes */
          def set[A <: Attr[_]](attrs: Map[String, Iterable[A]]) =
            complete(base.POST << compact(render(AttrsJson(attrs))))

          /** https://bintray.com/docs/rest/api.html#_update_attributes */
          def update[A <: Attr[_]](attrs: Map[String, Iterable[A]]) =
            complete(base.PATCH << compact(render(AttrsJson(attrs))))

          /** https://bintray.com/docs/rest/api.html#_delete_attributes */
          def delete(names: String*) =
            complete(if (names.isEmpty) base.DELETE else base.DELETE <<?
                     Map("names" -> names.mkString(",")))          
        }

        private def base = apiHost / "packages" / sub / repo / name / "versions" / vers

        private def contentBase = apiHost / "content" / sub / repo

        /** https://bintray.com/docs/rest/api.html#_get_version */
        override def apply[T](handler: Client.Handler[T]) =
          request(base)(handler)

        /** https://bintray.com/docs/rest/api.html#_delete_version */
        def delete =
          complete(base.DELETE)

        /** https://bintray.com/docs/rest/api.html#_update_version */
        def update(desc: String) =
          complete(base.PATCH <<
                   compact(render(("desc" -> desc))))

        def attrs = Attrs

        /** https://bintray.com/docs/rest/api.html#_upload_content */
        def upload(path: String, content: File,
                   publish: Boolean = false, explode: Boolean = false) =
                     complete(contentBase.PUT / "%s;publish=%s;explode=%s".format(
                       path,
                       if (publish) 1 else 0,
                       if (explode) 1 else 0) <:< Map(
                         "X-Bintray-Package" -> name,
                         "X-Bintray-Version" -> vers
                       ) <<< content)

        /** https://bintray.com/docs/rest/api.html#_publish_discard_uploaded_content */
        def publish =
          complete(contentBase.POST / name / vers / "publish")

        /** https://bintray.com/docs/rest/api.html#_publish_discard_uploaded_content */
        def discard =
          complete(contentBase.POST / name / vers / "publish" << compact(render("discard" -> true)))
      }

      private def base = apiHost / "packages" / sub / repo / name

      /** https://bintray.com/docs/rest/api.html#_get_package */
      override def apply[T](handler: Client.Handler[T]) =
        request(base)(handler)

      /** https://bintray.com/docs/rest/api.html#_delete_package */
      def delete =
        complete(base.DELETE)

      /** https://bintray.com/docs/rest/api.html#_update_package */
      def update(desc: String, labels: String*) =
        complete(base.PATCH / name <<
                 compact(render(
                   ("desc" -> desc) ~
                   ("labels" -> labels.toList))))

      def attrs = Attrs

      def version(version: String = "_latest") =
        Version(version)

      /** https://bintray.com/docs/rest/api.html#_create_version */
      def createVersion(version: String) =
        complete(base.POST / "versions" <<
                 compact(render(
                   ("name" -> version))))

      /** https://bintray.com/docs/rest/api.html#_maven_upload */
      def mvnUpload(pkg: String, path: String, content: File,
                    publish: Boolean = false, explode: Boolean = false) =
                      complete(apiHost.PUT / "maven" / sub / repo / pkg /
                               "%s;publish=%s;explode=%s"
                                 .format(path,
                                         if (publish) 1 else 0,
                                         if (explode) 1 else 0) <<< content)
    }

    private def base = apiHost / "repos" / sub / repo

    private def packagesBase = apiHost / "packages" / sub / repo

    override def apply[T](handler: Client.Handler[T]) =
      request(base)(handler)

    /** https://bintray.com/docs/rest/api.html#_get_repository */
    def packages(pos: Int = 0) =
      complete(base / "packages" <<? Map("start_pos" -> pos.toString))

    def get(pkg: String) =
      Package(pkg)

    /** https://bintray.com/docs/rest/api.html#_create_package */
    def createPackage(name: String, desc: String, labels: String*) =
      complete(packagesBase.POST <<
               compact(render(
                 ("name" -> name) ~
                 ("desc" -> desc) ~
                 ("labels" -> labels.toList))))
  }

  case class User(user: String) extends Client.Completion {
    private def base = apiHost / "users" / user

    /** https://bintray.com/docs/rest/api.html#_get_user */
    override def apply[T](handler: Client.Handler[T]) =
      request(base)(handler)

    /** https://bintray.com/docs/rest/api.html#_get_followers */
    def followers(pos: Int = 0) =
      complete(base / "followers" <<? Map("start_pos" -> pos.toString))
  }

  case class Webhooks(sub: String, repo: Option[String] = None) extends Client.Completion {
    sealed trait Method
    object POST extends Method
    object PUT extends Method
    object GET extends Method

    private def base = {
      val hooks = apiHost / "webhooks" / sub
      repo.map(hooks / _).getOrElse(hooks)
    }

    /** https://bintray.com/docs/rest/api.html#_get_webhooks */
    override def apply[T](handler: Client.Handler[T]) =
      request(base)(handler)

     /** https://bintray.com/docs/rest/api.html#_register_a_webhook */
     def create(pkg: String, url: String, method: Method) =
      complete(base.POST / pkg << compact(render(
        ("url" -> url) ~
        ("method" -> (method match {
          case POST => "post"
          case PUT => "put"
          case GET => "get"
        })))))

    /** https://bintray.com/docs/rest/api.html#_delete_a_webhook */
    def delete(pkg: String) =
      complete(base.DELETE / pkg)

    /** https://bintray.com/docs/rest/api.html#_test_a_webhook */
    def test(pkg: String, version: String) =
      complete(base.POST / pkg / version)
  }

  object Search {
    private def base = apiHost / "search"

    /** https://bintray.com/docs/rest/api.html#_repository_search */
    def repos(
      name: Option[String] = None, desc: Option[String] = None,
      pos: Int = 0) =
      complete(base / "repos" <<?
               Map("start_pos" -> pos.toString) ++
                 name.map("name" -> _) ++
                 desc.map("desc" -> _))

    /** https://bintray.com/docs/rest/api.html#_package_search */
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

    /** https://bintray.com/docs/rest/api.html#_file_search_by_name */
    def file(
      name: String, repo: Option[String] = None,
      pos: Int = 0) =
        complete(base / "file" <<?
                 Map("name" -> name, "start_pos" -> pos.toString) ++
                     repo.map(("repo" -> _)))

    /** https://bintray.com/docs/rest/api.html#_file_search_by_checksum */
    def sha(
      sha: String, repo: Option[String] = None, pos: Int = 0) =
        complete(base / "file" <<?
                 Map("sha" -> sha, "start_pos" -> pos.toString) ++
                     repo.map(("repo" -> _)))

    /** https://bintray.com/docs/rest/api.html#_user_search */
    def users(name: String, pos: Int = 0) =
      complete(base / "users" <<?
               Map("name" -> name, "start_pos" -> pos.toString))
  }

  /** https://bintray.com/docs/rest/api.html#_get_repositories */
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
