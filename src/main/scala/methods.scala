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
        def values(name0: String, names: String*) =
          complete(base <<? Map("names" -> (name0 +: names).mkString(",")))
        def set[A <: Attr[_]](attrs: Map[String, Iterable[A]]) =
          complete(base.POST << compact(render(AttrsJson(attrs))))
        def update[A <: Attr[_]](attrs: Map[String, Iterable[A]]) =
          complete(base.PATCH << compact(render(AttrsJson(attrs))))
        def delete(name0: String, names: String*) =
          complete(base.DELETE <<? Map("names" -> (name0 +: names).mkString(",")))
      }

      case class Version(vers: String) extends Client.Completion {
        object Attrs {
          def base = apiHost / "packages" / sub / repo / name / "versions" / vers / "attributes"
          def values(name0: String, names: String*) =
            complete(base <<? Map("names" -> (name0 +: names).mkString(",")))
          def set[A <: Attr[_]](attrs: Map[String, Iterable[A]]) =
            complete(base.POST << compact(render(AttrsJson(attrs))))
          def update[A <: Attr[_]](attrs: Map[String, Iterable[A]]) =
            complete(base.PATCH << compact(render(AttrsJson(attrs))))
          def delete(name0: String, names: String*) =
            complete(base.DELETE <<? Map("names" -> (name0 +: names).mkString(",")))          
        }

        private def base = apiHost / "packages" / sub / repo / name / "versions" / vers

        private def contentBase = apiHost / "content" / sub / repo

        override def apply[T](handler: Client.Handler[T]) =
          request(base)(handler)

        def delete =
          complete(base.DELETE)

        def update(desc: String) =
          complete(base.PATCH <<
                   compact(render(("desc" -> desc))))

        def attrs = Attrs

        def upload(path: String, content: File,
                   publish: Boolean = false, explode: Boolean = false) =
                     complete(contentBase.PUT / "%s;publish=%s;explode=%s".format(
                       path,
                       if (publish) 1 else 0,
                       if (explode) 1 else 0) <:< Map(
                         "X-Bintray-Package" -> name,
                         "X-Bintray-Version" -> vers
                       ) <<< content)

        def publish =
          complete(contentBase.POST / name / vers / "publish")

        def discard =
          complete(contentBase.POST / name / vers / "discard")
      }

      def base = apiHost / "packages" / sub / repo / name

      override def apply[T](handler: Client.Handler[T]) =
        request(base)(handler)

      def delete =
        complete(base.DELETE)

      def update(desc: String, labels: String*) =
        complete(base.PATCH / name <<
                 compact(render(
                   ("desc" -> desc) ~
                   ("labels" -> labels.toList))))

      def attrs = Attrs

      def version(version: String = "_latest") =
        Version(version)

      def createVersion(version: String) =
        complete(base.POST / "versions" <<
                 compact(render(
                   ("name" -> version))))

      def mvnUpload(pkg: String, path: String, content: File, publish: Boolean = false, explode: Boolean = false) =
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

    def packages =
      complete(base / "packages")

    def get(pkg: String) =
      Package(pkg)

    def createPackage(name: String, desc: String, labels: String*) =
      complete(packagesBase.POST <<
               compact(render(
                 ("name" -> name) ~
                 ("desc" -> desc) ~
                 ("labels" -> labels.toList))))
  }

  case class User(user: String) extends Client.Completion {
    private def base = apiHost / "users" / user

    override def apply[T](handler: Client.Handler[T]) =
      request(base)(handler)

    def followers =
      complete(base / "followers")
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

    override def apply[T](handler: Client.Handler[T]) =
      request(base)(handler)

     def create(pkg: String, url: String, method: Method) =
      complete(base.POST / pkg << compact(render(
        ("url" -> url) ~
        ("method" -> (method match {
          case POST => "post"
          case PUT => "put"
          case GET => "get"
        })))))

    def delete(pkg: String) =
      complete(base.DELETE / pkg)

    def test(pkg: String, version: String) =
      complete(base.POST / pkg / version)
  }

  object Search {
    def searchBase = apiHost / "search"

    def repos(name: Option[String] = None, desc: Option[String] = None) =
      complete(searchBase / "repos" <<?
               Map.empty[String, String] ++ name.map(("name" -> _)) ++ desc.map("desc" -> _))

    def packages(name: Option[String] = None, desc: Option[String] = None,
                 subject: Option[String] = None, repo: Option[String] = None) =
     complete(searchBase / "packages" <<?
              Map.empty[String, String] ++ name.map(("name" -> _)) ++ desc.map(("desc" -> _)) ++
                subject.map(("subject" -> _)) ++ repo.map(("repo" -> _)))

    def file(name: String, repo: Option[String] = None) =
      complete(searchBase / "file" <<? Map("name" -> name) ++ repo.map(("repo" -> _)))

    def sha(sha: String, repo: Option[String] = None) =
      complete(searchBase / "file" <<? Map("sha" -> sha) ++ repo.map(("repo" -> _)))

    def users(name: String) =
      complete(searchBase / "users" <<? Map("name" -> name))
  }

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
