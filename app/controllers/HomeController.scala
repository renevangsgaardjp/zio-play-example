package controllers

import models.UserRepository
import zio.interop.play.{ZioComponents, ZioController}

import javax.inject._

/** This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomeController @Inject() (val zioComponents: ZioComponents) extends ZioController(zioComponents) {

  /** Create an Action to render an HTML page.
    *
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */
  def index() = Action.zio { _ =>
    UserRepository.getUser.map(Ok(_))
  }

}
