package zio.interop.play
import com.google.inject.Inject
import models.{Database, UserRepository}
import play.api.mvc._
import zio._
import zio.interop.play.ZioController.AppEnv

import scala.concurrent.ExecutionContext

abstract class ZioController @Inject() (protected val zc: ZioComponents)
    extends BaseController
    with ZioActionBuilderSyntax {
  val runtime: Runtime[AppEnv] = zc.runtime

  override protected def controllerComponents: ControllerComponents = zc.cc
}

object ZioController {
  type AppEnv = UserRepository.Service

  // Arguments should be what we are grabbing from Guice/Play
  def live(database: Database): ULayer[AppEnv] = {
    val dbLayer        = ZLayer.succeed(database)
    val userRepository = dbLayer >>> UserRepository.live
    ZLayer.make[AppEnv](userRepository)
  }
}

trait ZioActionBuilderSyntax {
  val runtime: Runtime[AppEnv]
  implicit final class ActionBuilderOps[+R[_], B](val actionBuilder: ActionBuilder[R, B]) {

    def zio[E](zioActionBody: R[B] => ZIO[AppEnv, E, Result]): Action[B] = actionBuilder.async { request =>
      Unsafe.unsafe(implicit unsafe => runtime.unsafe.runToFuture(ioToTask(zioActionBody(request))))
    }

    def zio[E, A](bp: BodyParser[A])(zioActionBody: R[A] => ZIO[AppEnv, E, Result]): Action[A] =
      actionBuilder(bp).async { request =>
        Unsafe.unsafe(implicit unsafe => runtime.unsafe.runToFuture(ioToTask(zioActionBody(request))))
      }
  }
  private def ioToTask[E, A](io: ZIO[AppEnv, E, A]) =
    io.mapError {
      case t: Throwable => t
      case s: String    => new Throwable(s)
      case e            => new Throwable("Error: " + e.toString)
    }
}

final case class ZioComponents @Inject() (
    database: Database,
    cc: ControllerComponents,
    ec: ExecutionContext
) extends ZioActionBuilderSyntax {
  val runtime: Runtime[AppEnv] = Unsafe.unsafe(implicit unsafe => Runtime.unsafe.fromLayer(ZioController.live(database)))
}
