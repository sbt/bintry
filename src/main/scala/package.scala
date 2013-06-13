package object bintry {
  import com.ning.http.client.{ AsyncHandler, ProgressAsyncHandler, Response }
  import dispatch.FunctionHandler

  object Noop extends FunctionHandler(identity)

  /*object as {
    object Progress {
      import AsyncHandler.STATE
      import STATE._
      def apply(onProgress: (Long, Long, Long) => Unit, onHeaderComplete: () => Unit = {()=>()}, onComplete: () => Unit = {()=>()}) =
        new ProgressAsyncHandler[Unit] {
          val state = CONTINUE

          def onHeaderWriteCompleted(): STATE = {
            onHeaderComplete()
            state
          }

          def onContentWriteCompleted(): STATE = {
            onComplete()
            state
          }

          def onContentWriteProgress(amount: Long, current: Long, total: Long): Unit = {
            onProgress(amount, current, total)
            state
          }
        }
    }
  }*/

  // use this when dropping 2.9.3 support
  //implicit class ImplicitFunctionHandler[T](f: Response => T) extends FunctionHandler(f)

  implicit def r2h[T](f: Response => T): Client.Handler[T] =
    new FunctionHandler(f)
}

