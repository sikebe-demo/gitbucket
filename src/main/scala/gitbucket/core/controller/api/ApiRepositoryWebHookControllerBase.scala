package gitbucket.core.controller.api
import gitbucket.core.api._
import gitbucket.core.controller.ControllerBase
import gitbucket.core.model.{CommitState, RepositoryWebHook, WebHook, WebHookContentType}
import gitbucket.core.service.{AccountService, WebHookService}
import gitbucket.core.service.RepositoryService.RepositoryInfo
import gitbucket.core.util.Implicits._
import gitbucket.core.util.{JGitUtil, OwnerAuthenticator, RepositoryName}

import java.util.Date
import gitbucket.core.settings.html.options
import org.slf4j.LoggerFactory

trait ApiRepositoryWebHookControllerBase extends ControllerBase {
  self: AccountService with WebHookService with OwnerAuthenticator =>

  private val logger = LoggerFactory.getLogger(classOf[ApiRepositoryWebHookControllerBase])

  /*
   * i. Create a hook
   * https://developer.github.com/v3/repos/hooks/#create-a-hook
   */
  post("/api/v3/repos/:owner/:repository/hooks")(ownerOnly { repository =>
    (for {
      data <- extractFromJsonBody[CreateAWebHook]
    } yield {
      addWebHook(
        repository.owner,
        repository.name,
        data.config.url,
        //data.events.getOrElse(Set(WebHook.Push)),
        Set(WebHook.Push),
        WebHookContentType.valueOf(data.config.content_type.getOrElse("form")),
        Some(data.config.secret.getOrElse(""))
      )
      getWebHook(repository.owner, repository.name, data.config.url) match {
        case Some((webhook, events)) =>
          JsonFormat(
            ApiRepositoryWebHook(
              webhook,
              events,
              data.config.insecure_ssl,
              data.active.getOrElse(true),
              RepositoryName(repository)
            )
          )
        case None => NotFound()
      }
    }) getOrElse NotFound()
  })
}

case class ApiRepositoryWebHook(
  config: ApiWebHookConfig,
  `type`: String,
  id: Int,
  name: String,
  active: Boolean,
  events: Set[WebHook.Event],
  updated_at: Date,
  created_at: Date
)

object ApiRepositoryWebHook {
  def apply(
    webhook: RepositoryWebHook,
    events: Set[WebHook.Event],
    insecure_ssl: Option[String],
    active: Boolean,
    repositoryName: RepositoryName
  ): ApiRepositoryWebHook = ApiRepositoryWebHook(
    config = new ApiWebHookConfig(webhook.url, Some(webhook.ctype.code), webhook.token, insecure_ssl),
    `type` = "Repository",
    id = 0, // dummy id
    name = "web",
    active = active,
    events = events,
    updated_at = new Date,
    created_at = new Date
  )
}

case class ApiWebHookConfig(
  url: String,
  content_type: Option[String],
  secret: Option[String],
  insecure_ssl: Option[String]
)

case class CreateAWebHook(
  name: Option[String],
  config: ApiWebHookConfig,
  events: Option[Seq[String]],
  active: Option[Boolean]
)
