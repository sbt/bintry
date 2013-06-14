package object bintry {
  import com.ning.http.client.{ AsyncCompletionHandler, AsyncHandler, ProgressAsyncHandler, Response }
  import dispatch.FunctionHandler

  object Noop extends FunctionHandler(identity)

  object having {
    import AsyncHandler.STATE
    import STATE._

    case class Step(amount: Long, current: Long, total: Long)

    /** companion builder for the common case of Progress */
    object Progress {
      def apply(onProgress: Step => Unit): Progress[Unit] =
        Progress(onProgress, () => (), () => (), _ => ())
    }

    /** a completion handler an progres listener all in one */
    case class Progress[T](
      _onProgress: Step => Unit,
      _onHeaderComplete: () => Unit,
      _onComplete: () => Unit,
      _response: Response => T)
      extends AsyncCompletionHandler[T] with ProgressAsyncHandler[T] {

      @volatile private var state = CONTINUE

      def onComplete(complete: () => Unit) =
        Progress(_onProgress, _onHeaderComplete, complete, _response)

      def onHeaderComplete(complete: () => Unit) =
        Progress(_onProgress, complete, _onComplete, _response)

      def response[T](response: Response => T) =
        Progress(_onProgress, _onHeaderComplete, _onComplete, response)

      final def onCompleted(response: Response) =
        _response(response)

      final override def onHeaderWriteCompleted(): STATE = {
        _onHeaderComplete()
        state
      }

      final override def onContentWriteCompleted(): STATE = {
        _onComplete()
        state
      }

      final override def onContentWriteProgress(amount: Long, current: Long, total: Long): STATE = {
        _onProgress(Step(amount, current, total))
        state
      }

      final def stop() {
        state = ABORT
      }
    }
  }

  // use this when dropping 2.9.3 support
  //implicit class ImplicitFunctionHandler[T](f: Response => T) extends FunctionHandler(f)

  implicit def r2h[T](f: Response => T): Client.Handler[T] =
    new FunctionHandler(f)
}

