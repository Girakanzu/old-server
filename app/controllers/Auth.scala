package controllers

import entice.server._

import play.api._
import play.api.mvc._

import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import java.util.UUID

import scala.concurrent.Future


/** Authentication controller. You need to be logged in for most other functionality. */
object Auth extends EnticeController {

  sealed trait AuthMessage
  case class AuthForm(email: String, password: String) extends AuthMessage


  val authForm = Form(mapping(
    "email" -> email,
    "password" -> text(minLength = 4, maxLength = 32)
  )(AuthForm.apply)(AuthForm.unapply))


  def loginGet = Action { implicit request =>
    authorize match {
      case NotAuthorized => Ok(views.html.login(authForm))
      case _             => Redirect(routes.Application.index()).flashing("message" -> """Already logged in. <a href="/logout">...log out.<a>""")
    }
  }

  def logoutGet = Action { implicit request =>
    authorize match {
      case NotAuthorized => Redirect(routes.Application.index()).flashing("message" -> """Already logged out. <a href="/login">...log back in.<a>""")
      case _             => Ok(views.html.logout())
    }
  }

  def loginPost = Action.async { implicit request =>
    authForm.bindFromRequest.fold(
      formWithErrors => { Future.successful(BadRequest(views.html.login(formWithErrors))) },
      authData => {
        Global.authenticate(authData.email, authData.password).map { optClient =>
          optClient match {
            case None => replyUnauthorized
            case Some(client) =>
              Logger.info(s"User logged in: ${authData.email} - ${client.authToken}")
              Redirect(routes.Application.index())
                .flashing("message" -> """Successfully logged in. <a href="/logout">...log out.<a>""")
                .withSession {
                  request.session + ("authToken" -> client.authToken)
                }
          }
        }
      })
  }

  def logoutPost = Action { implicit request =>
    authorize match {
      case NotAuthorized             => Redirect(routes.Application.index()).withNewSession
      case ctx: AuthorizationContext =>
        Global.deauthenticate(ctx.authToken)
        Logger.info(s"User logged out: ${ctx.authToken}")
        Redirect(routes.Application.index())
          .flashing("message" -> """Successfully logged out. <a href="/login">...log in.<a>""")
          .withNewSession
    }
  }
}
