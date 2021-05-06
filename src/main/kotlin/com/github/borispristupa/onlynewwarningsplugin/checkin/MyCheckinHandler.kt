package com.github.borispristupa.onlynewwarningsplugin.checkin

import com.github.borispristupa.onlynewwarningsplugin.BooleanProperty
import com.github.borispristupa.onlynewwarningsplugin.MyBundle
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.changes.CommitExecutor
import com.intellij.openapi.vcs.changes.ui.BooleanCommitOption
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.ui.RefreshableOnComponent
import com.intellij.util.PairConsumer
import com.intellij.util.ui.UIUtil

class MyCheckinHandler(private val panel: CheckinProjectPanel) : CheckinHandler() {
  private val project = panel.project

  companion object {
    var enabled: Boolean by BooleanProperty("checkin_enabled", true)
  }

  override fun getBeforeCheckinConfigurationPanel(): RefreshableOnComponent =
    BooleanCommitOption(
      panel, MyBundle.message("checkin.handler.option.text"),
      true, ::enabled
    )

  override fun beforeCheckin(executor: CommitExecutor?, p: PairConsumer<Any, Any>?): ReturnResult {
    if (!enabled) return ReturnResult.COMMIT

    if (DumbService.getInstance(project).isDumb) {
      return if (shouldCommitOnDumb()) {
        ReturnResult.COMMIT
      } else ReturnResult.CANCEL
    }

    return try {
      runCodeAnalysis()
    } catch (e: ProcessCanceledException) {
      ReturnResult.CANCEL
    } catch (e: Exception) {
      e.printStackTrace()

      if (shouldCommitOnException(e)) {
        ReturnResult.COMMIT
      } else ReturnResult.CANCEL
    }
  }

  private fun runCodeAnalysis(): ReturnResult {
    return ReturnResult.COMMIT
  }


  private fun shouldCommitOnDumb() = Messages.CANCEL == Messages.showOkCancelDialog(
    project,
    MyBundle.message("checkin.handler.dumb.dialog.message"),
    MyBundle.message("checkin.handler.dumb.dialog.title"),
    MyBundle.message("checkin.handler.dumb.dialog.wait"), // Messages.OK
    MyBundle.message("checkin.handler.dumb.dialog.commit"), // Messages.CANCEL
    UIUtil.getWarningIcon()
  )

  private fun shouldCommitOnException(e: Exception): Boolean {
    val message = e.message?.let { msg ->
      MyBundle.message("checkin.handler.exception.dialog.withMessage", e.javaClass.name, msg)
    } ?: MyBundle.message("checkin.handler.exception.dialog.withoutMessage", e.javaClass.name)

    return Messages.OK == Messages.showOkCancelDialog(
      project, message,
      MyBundle.message("checkin.handler.exception.dialog.title"),
      MyBundle.message("checkin.handler.exception.dialog.commit"), // Messages.OK
      MyBundle.message("checkin.handler.exception.dialog.cancel"), // Messages.CANCEL
      UIUtil.getErrorIcon()
    )
  }
}
