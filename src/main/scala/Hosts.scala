package bintry

import dispatch.{ Req, :/ }

trait Hosts {
  protected def apiHost: Req
}

trait DefaultHosts extends Hosts {
  protected def apiHost = :/("api.bintray.com").secure
}
