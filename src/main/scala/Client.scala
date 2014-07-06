package bintry

import com.ning.http.client.{ AsyncHandler, Response }
import dispatch.{ FunctionHandler, Http, Req }
import scala.concurrent.{ ExecutionContext, Future }

object Client {
  type Handler[T] = AsyncHandler[T]
  trait Completion {
    def apply(): Future[Response] =
      apply(new FunctionHandler(identity))
    def apply[T](f: Response => T): Future[T] =
      apply(new FunctionHandler(f))
    def apply[T]
      (handler: Client.Handler[T]): Future[T]
  }
}

abstract class Requests(
  credentials: Credentials, http: Http)
 (implicit ec: ExecutionContext)
  extends DefaultHosts
  with Methods {

  def request[T]
    (req: Req)
    (handler: Client.Handler[T]): Future[T] =
    http(credentials.sign(req) > handler)

  def complete(req: Req): Client.Completion =
    new Client.Completion {
      override def apply[T]
        (handler: Client.Handler[T]) =
        request(req)(handler)
    }
}

case class Client(
  user: String, token: String, private val http: Http = Http)
  (implicit ec: ExecutionContext)
  extends Requests(BasicAuth(user, token), http) {
  /** releases http resources. once closed, this client may no longer be used */
  def close(): Unit = http.shutdown()
}
