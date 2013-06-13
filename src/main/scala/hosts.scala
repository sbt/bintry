package bintry

import com.ning.http.client.RequestBuilder
import dispatch._

trait Hosts {
  def apiHost: RequestBuilder
}

trait DefaultHosts extends Hosts {
  def apiHost = :/("api.bintray.com").secure
}
