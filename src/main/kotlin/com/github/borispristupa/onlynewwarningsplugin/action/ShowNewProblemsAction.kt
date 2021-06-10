package com.github.borispristupa.onlynewwarningsplugin.action

import com.github.borispristupa.onlynewwarningsplugin.MyBundle
import com.github.borispristupa.onlynewwarningsplugin.analysis.findNewProblems
import com.github.borispristupa.onlynewwarningsplugin.analysis.showProblems
import com.github.borispristupa.onlynewwarningsplugin.filterKeysNotNull

import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.notification.SingletonNotificationManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager

class ShowNewProblemsAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    try {
      val changes = ChangeListManager.getInstance(project).allChanges
        .groupBy { it.virtualFile }
        .mapValues { it.value.first() }
        .filterKeysNotNull()

      val problems = findNewProblems(changes, project)
      showProblems(problems, project)

      if (problems.values.any { it.isNotEmpty() }) {
        showProblems(problems, project)
      } else {
        notifyOnNoNewProblems(project)
      }
    } catch (e: ProcessCanceledException) {
      notifyOnCancellation(project)
    } catch (e: Exception) {
      notifyOnFailure(project, e)
      throw e
    }
  }

  private fun notifyOnNoNewProblems(project: Project) {
    val group = NotificationGroup.balloonGroup("notification.group.onlynewwarningsplugin")

    SingletonNotificationManager(group, NotificationType.INFORMATION)
      .notify(MyBundle.message("action.show.new.problems.nothing.found.notification.text"), project)
  }

  private fun notifyOnCancellation(project: Project) {
    val group = NotificationGroup.balloonGroup("notification.group.onlynewwarningsplugin")

    SingletonNotificationManager(group, NotificationType.INFORMATION)
      .notify(MyBundle.message("action.show.new.problems.cancellation.notification.text"), project)
  }

  private fun notifyOnFailure(project: Project, exception: Exception) {
    val group = NotificationGroup.balloonGroup("notification.group.onlynewwarningsplugin")

    SingletonNotificationManager(group, NotificationType.ERROR)
      .notify(
        MyBundle.message("action.show.new.problems.exception.notification.text", exception),
        project
      )
  }
}