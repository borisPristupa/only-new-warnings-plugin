package com.github.borispristupa.onlynewwarningsplugin.checkin

import com.github.borispristupa.onlynewwarningsplugin.BooleanProperty
import com.github.borispristupa.onlynewwarningsplugin.MyBundle
import com.github.borispristupa.onlynewwarningsplugin.analysis.FileMap
import com.github.borispristupa.onlynewwarningsplugin.analysis.Highlights
import com.github.borispristupa.onlynewwarningsplugin.analysis.findNewProblems
import com.github.borispristupa.onlynewwarningsplugin.analysis.showProblems
import com.github.borispristupa.onlynewwarningsplugin.settings.MySettingsPage
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.changes.CommitExecutor
import com.intellij.openapi.vcs.changes.ui.BooleanCommitOption
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.ui.RefreshableOnComponent
import com.intellij.ui.HyperlinkLabel
import com.intellij.util.PairConsumer
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class MyCheckinHandler(private val panel: CheckinProjectPanel) : CheckinHandler() {
  private val project = panel.project

  companion object {
    var enabled: Boolean by BooleanProperty("checkin_enabled", true)
  }

  override fun getBeforeCheckinConfigurationPanel(): RefreshableOnComponent =
    object : BooleanCommitOption(
      panel, MyBundle.message("checkin.handler.option.text"),
      true, ::enabled
    ) {
      override fun getComponent(): JComponent {
        val configureLabel = HyperlinkLabel(MyBundle.message("checkin.handler.option.configure")).apply {
          addHyperlinkListener {
            ShowSettingsUtil.getInstance()
              .showSettingsDialog(project, MySettingsPage::class.java)
          }
        }

        return JPanel(BorderLayout(4, 0)).apply {
          add(checkBox, BorderLayout.WEST)
          add(configureLabel, BorderLayout.CENTER)
        }
      }
    }

  override fun beforeCheckin(executor: CommitExecutor?, p: PairConsumer<Any, Any>?): ReturnResult {
    if (!enabled) return ReturnResult.COMMIT

    if (DumbService.getInstance(project).isDumb) {
      return if (shouldCommitOnDumb()) {
        ReturnResult.COMMIT
      } else ReturnResult.CANCEL
    }

    return try {
      runCodeAnalysis(executor)
    } catch (e: ProcessCanceledException) {
      ReturnResult.CANCEL
    } catch (e: Exception) {
      e.printStackTrace()

      if (shouldCommitOnException(e)) {
        ReturnResult.COMMIT
      } else ReturnResult.CANCEL
    }
  }

  private fun runCodeAnalysis(commitExecutor: CommitExecutor?): ReturnResult {
    val fileChanges = panel.selectedChanges
      .mapNotNull { it.virtualFile?.to(it) }
      .toMap()

    val newProblems = findNewProblems(fileChanges, project)

    return if (newProblems.isNotEmpty()) {
      processNewProblems(newProblems, commitExecutor)
    } else ReturnResult.COMMIT
  }


  private fun processNewProblems(problems: FileMap<Highlights>, executor: CommitExecutor?): ReturnResult {
    val fileNum = problems.keys.size
    val problemNum = problems.values.flatten().size

    return whatToDoOnProblemsFound(executor, fileNum, problemNum).also { answer ->
      if (answer == ReturnResult.CLOSE_WINDOW) {
        showProblems(problems, project)
      }
    }
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


  private fun whatToDoOnProblemsFound(
    executor: CommitExecutor?,
    fileNum: Int,
    problemNum: Int
  ): ReturnResult {
    var commitButtonText = executor?.actionText ?: panel.commitActionName
    commitButtonText = StringUtil.trimEnd(commitButtonText, "...")

    val message =
      if (fileNum == 1)
        MyBundle.message("checkin.handler.file.contains.problems", problemNum)
      else
        MyBundle.message("checkin.handler.files.contain.problems", fileNum, problemNum)

    val answer: Int = Messages.showYesNoCancelDialog(
      project, message,
      MyBundle.message("checkin.handler.commit.dialog.title"),
      MyBundle.message("checkin.handler.commit.dialog.review"),
      commitButtonText,
      MyBundle.message("checkin.handler.commit.dialog.cancel"),
      UIUtil.getWarningIcon()
    )

    return when(answer) {
      Messages.YES -> ReturnResult.CLOSE_WINDOW
      Messages.CANCEL -> ReturnResult.CANCEL
      else -> ReturnResult.COMMIT
    }
  }
}
