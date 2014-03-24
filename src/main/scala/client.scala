package bintry

import com.ning.http.client.AsyncHandler
import dispatch.{ Http, Req }
import scala.concurrent.{ ExecutionContext, Future }

object Client {
  type Handler[T] = AsyncHandler[T]
  trait Completion {
    def apply[T](handler: Client.Handler[T])(implicit ec: ExecutionContext): Future[T]
  }
}

abstract class Requests(
  credentials: Credentials, http: Http = Http)
  extends DefaultHosts
  with Methods {

  def request[T]
    (req: Req)
    (handler: Client.Handler[T])
    (implicit ec: ExecutionContext): Future[T] =
    http(credentials.sign(req) > handler)

  def complete(req: Req): Client.Completion =
    new Client.Completion {
      override def apply[T](handler: Client.Handler[T])(implicit ec: ExecutionContext) =
        request(req)(handler)
    }
}

case class Client(user: String, token: String, http: Http = Http)
  extends Requests(BasicAuth(user, token))
