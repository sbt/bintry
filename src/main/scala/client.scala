package bintry

import com.ning.http.client.{ AsyncHandler, RequestBuilder }
import dispatch._, dispatch.Defaults._
import scala.concurrent.Future

object Client {
  type Handler[T] = AsyncHandler[T]
  trait Completion {
    def apply[T](handler: Client.Handler[T]): Future[T]
  }
}

abstract class Requests(
  credentials: Credentials, http: Http = Http)
  extends DefaultHosts
  with Methods {

  def request[T](req: RequestBuilder)
                (handler: Client.Handler[T]): Future[T] =
    http(credentials.sign(req) > handler)

  def complete(req: RequestBuilder): Client.Completion =
    new Client.Completion {
      override def apply[T](handler: Client.Handler[T]) =
        request(req)(handler)
    }
}

case class Client(user: String, token: String, http: Http = Http)
  extends Requests(BasicAuth(user, token))
