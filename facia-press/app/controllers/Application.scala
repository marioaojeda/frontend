package controllers

import common.ExecutionContexts
import conf.Configuration
import conf.Switches.FaciaPressOnDemand
import frontpress.{DraftFapiFrontPress, LiveFapiFrontPress}
import model.NoCache
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller, Result}
import services.ConfigAgent

import scala.concurrent.Future

object Application extends Controller with ExecutionContexts {
  def index = Action {
    NoCache(Ok("Hello, I am the Facia Press."))
  }

  def showCurrentConfig = Action {
    NoCache(Ok(ConfigAgent.contentsAsJsonString).withHeaders("Content-Type" -> "application/json"))
  }

  def generateFrontJson() = Action.async { request =>
    LiveFapiFrontPress.generateFrontJsonFromFapiClient()
      .map(Json.prettyPrint)
      .map(Ok.apply(_))
      .map(NoCache.apply)
      .fold(
        apiError => InternalServerError(apiError.message),
        successJson => successJson
      )}

  def generateLivePressedFor(path: String) = Action.async { request =>
    LiveFapiFrontPress.getPressedFrontForPath(path)
      .map(Json.toJson(_))
      .map(Json.prettyPrint)
      .map(Ok.apply(_))
      .map(NoCache.apply)
      .fold(
        apiError => InternalServerError(apiError.message),
        successJson => successJson.withHeaders("Content-Type" -> "application/json")
      )}

  private def handlePressRequest(path: String, liveOrDraft: String)(f: (String) => Future[_]): Future[Result] =
    if (FaciaPressOnDemand.isSwitchedOn) {
      val stage = Configuration.facia.stage.toUpperCase
      f(path)
        .map(_ => NoCache(Ok(s"Successfully pressed $path on $liveOrDraft for $stage")))
        .recover { case t => NoCache(InternalServerError(t.getMessage))}}
    else {
      Future.successful(NoCache(ServiceUnavailable))}

  def pressLiveForPath(path: String) = Action.async {
    handlePressRequest(path, "live")(LiveFapiFrontPress.pressByPathId)
  }

  def pressDraftForPath(path: String) = Action.async {
    handlePressRequest(path, "draft")(DraftFapiFrontPress.pressByPathId)
  }
}
