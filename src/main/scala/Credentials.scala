package bintry

import dispatch.Req

sealed trait Credentials {
  def sign(req: Req): Req
}

case class BasicAuth(user: String, pass: String) extends Credentials {
  def sign(req: Req) =
    req.as_!(user, pass)
}
