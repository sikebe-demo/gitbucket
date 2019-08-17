package gitbucket.core.service

import gitbucket.core.model.Milestone
import gitbucket.core.model.Profile._
import gitbucket.core.model.Profile.profile.blockingApi._
import gitbucket.core.model.Profile.dateColumnType

trait MilestonesService {

  def createMilestone(
    owner: String,
    repository: String,
    title: String,
    description: Option[String],
    dueDate: Option[java.util.Date]
  )(implicit s: Session): Unit =
    Milestones insert Milestone(
      userName = owner,
      repositoryName = repository,
      title = title,
      description = description,
      dueDate = dueDate,
      closedDate = None
    )

  def updateMilestone(milestone: Milestone)(implicit s: Session): Unit =
    Milestones
      .filter(t => t.byPrimaryKey(milestone.userName, milestone.repositoryName, milestone.milestoneId))
      .map(t => (t.title, t.description, t.dueDate, t.closedDate))
      .update(milestone.title, milestone.description, milestone.dueDate, milestone.closedDate)

  def openMilestone(milestone: Milestone)(implicit s: Session): Unit =
    updateMilestone(milestone.copy(closedDate = None))

  def closeMilestone(milestone: Milestone)(implicit s: Session): Unit =
    updateMilestone(milestone.copy(closedDate = Some(currentDate)))

  def deleteMilestone(owner: String, repository: String, milestoneId: Int)(implicit s: Session): Unit = {
    Issues.filter(_.byMilestone(owner, repository, milestoneId)).map(_.milestoneId.?).update(None)
    Milestones.filter(_.byPrimaryKey(owner, repository, milestoneId)).delete
  }

  def getMilestone(owner: String, repository: String, milestoneId: Int)(implicit s: Session): Option[Milestone] =
    Milestones.filter(_.byPrimaryKey(owner, repository, milestoneId)).firstOption

  def getMilestonesWithIssueCount(owner: String, repository: String)(
    implicit s: Session
  ): List[(Milestone, Int, Int)] = {
    val counts = Issues
      .filter { t =>
        t.byRepository(owner, repository) && (t.milestoneId.? isDefined)
      }
      .groupBy { t =>
        t.milestoneId -> t.closed
      }
      .map { case (t1, t2) => t1._1 -> t1._2 -> t2.length }
      .list
      .toMap

    getMilestones(owner, repository).map { milestone =>
      (
        milestone,
        counts.getOrElse((milestone.milestoneId, false), 0),
        counts.getOrElse((milestone.milestoneId, true), 0)
      )
    }
  }

  def getMilestones(owner: String, repository: String)(implicit s: Session): List[Milestone] =
    Milestones
      .filter(_.byRepository(owner, repository))
      .sortBy(t => (t.dueDate.asc, t.closedDate.desc, t.milestoneId.desc))
      .list
}

object MilestonesService {
  import javax.servlet.http.HttpServletRequest
  case class MilestoneSearchCondition(
    state: String = "open",
    sort: String = "due_on",
    direction: String = "asc"
  )
  object MilestoneSearchCondition {

    private def param(request: HttpServletRequest, name: String, allow: Seq[String] = Nil): Option[String] = {
      val value = request.getParameter(name)
      if (value == null || value.isEmpty || (allow.nonEmpty && !allow.contains(value))) None else Some(value)
    }

    /**
     * Restores MilestoneSearchCondition instance from request parameters.
     */
    def apply(request: HttpServletRequest): MilestoneSearchCondition =
      MilestoneSearchCondition(
        param(request, "state", Seq("open", "closed", "all")).getOrElse("open"),
        param(request, "sort", Seq("due_on", "completeness")).getOrElse("due_on"),
        param(request, "direction", Seq("asc", "desc")).getOrElse("asc")
      )
  }
}
