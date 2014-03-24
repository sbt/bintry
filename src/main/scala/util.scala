package bintry

import dispatch.Req

object Util {
  private[bintry] def appendPath(to: Req, path: String) =
    (to /: path.split('/')) {
      case (req, seg) => if (seg.isEmpty) req else req / seg
    }
}
