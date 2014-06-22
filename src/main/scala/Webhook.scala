package bintry

object Webhook {
  sealed trait Method {
    def name: String
  }
  object Method {
    abstract class Value(val name: String) extends Method
    object POST extends Value("post")
    object PUT extends Value("put")
    object GET extends Value("get")
  }
}
