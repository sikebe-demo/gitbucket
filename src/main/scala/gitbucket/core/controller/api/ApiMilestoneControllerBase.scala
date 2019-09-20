package gitbucket.core.controller.api

import gitbucket.core.api._
import gitbucket.core.controller.ControllerBase
import gitbucket.core.model.Milestone
import gitbucket.core.service.MilestonesService.MilestoneSearchCondition
import gitbucket.core.service.{AccountService, MilestonesService}
import gitbucket.core.util.{ReferrerAuthenticator, RepositoryName}
import gitbucket.core.util.Implicits._

trait ApiMilestoneControllerBase extends ControllerBase {
  self: AccountService with MilestonesService with ReferrerAuthenticator =>
  /*
   * i. List milestones for a repository
   * https://developer.github.com/v3/issues/milestones/#list-milestones-for-a-repository
   */
  get("/api/v3/repos/:owner/:repository/milestones")(referrersOnly { repository =>
    val condition = MilestoneSearchCondition(request)
    val milestones: List[(Milestone, Int, Int)] =
      getMilestonesWithIssueCount(repository.owner, repository.name, condition)

    JsonFormat(milestones.map {
      case (milestone, openIssues, closedIssue) =>
        ApiMilestone(
          milestone,
          RepositoryName(repository),
          openIssues,
          closedIssue
        )
    })
  })
}
