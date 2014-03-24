package bintry

import dispatch.{ Req, :/ }

trait Hosts {
  def apiHost: Req
}

trait DefaultHosts extends Hosts {
  def apiHost = :/("api.bintray.com").secure
}
