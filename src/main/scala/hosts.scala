package bintry

import dispatch._

trait Hosts {
  def apiHost: Req
}

trait DefaultHosts extends Hosts {
  def apiHost = :/("api.bintray.com").secure
}
