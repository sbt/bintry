package object bintry {
  import com.ning.http.client.Response
  import dispatch.FunctionHandler


  object Noop extends FunctionHandler(identity)

  // use this when dropping 2.9.3 support
  //implicit class ImplicitFunctionHandler[T](f: Response => T) extends FunctionHandler(f)

  implicit def r2h[T](f: Response => T): Client.Handler[T] =
    new FunctionHandler(f)
}

