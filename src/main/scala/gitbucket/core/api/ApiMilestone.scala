package gitbucket.core.api

import java.util.Date

import gitbucket.core.model.Milestone
import gitbucket.core.util.RepositoryName

/**
 * https://developer.github.com/v3/issues/milestones/
 */
case class ApiMilestone(
  id: Int,
  number: Int,
  title: String,
  description: String,
  open_issues: Int,
  closed_issues: Int,
  state: String,
  closed_at: Date,
  due_on: Date
)(repositoryName: RepositoryName) {
  val url = ApiPath(s"/api/v3/repos/${repositoryName.fullName}/milestones/${id}")
  val html_url = ApiPath(s"/${repositoryName.fullName}/milestones/${title}")
}

object ApiMilestone {
  def apply(
    milestone: Milestone,
    repositoryName: RepositoryName,
    openIssues: Int,
    closedIssues: Int
  ): ApiMilestone =
    ApiMilestone(
      id = milestone.milestoneId,
      number = milestone.milestoneId,
      title = milestone.title,
      description = milestone.description.getOrElse(""),
      open_issues = openIssues,
      closed_issues = closedIssues,
      state = if (milestone.closedDate.isEmpty) { "open" } else { "closed" },
      closed_at = milestone.closedDate.getOrElse(null),
      due_on = milestone.dueDate.getOrElse(null)
    )(repositoryName)
}
