package com.github.borispristupa.onlynewwarningsplugin.analysis

import com.github.borispristupa.onlynewwarningsplugin.document
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzerSettings
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.codeInsight.daemon.impl.DaemonProgressIndicator
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Document
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.AbstractProgressIndicatorExBase
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ex.ProgressIndicatorEx
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

fun findProblems(
  file: VirtualFile,
  project: Project,
  indicator: ProgressIndicator
): Highlights {
  val daemonIndicator: ProgressIndicator = DaemonProgressIndicator()
  (indicator as ProgressIndicatorEx).addStateDelegate(object : AbstractProgressIndicatorExBase() {
    override fun cancel() {
      super.cancel()
      daemonIndicator.cancel()
    }
  })

  val psiFile = PsiManager.getInstance(project).findFile(file) ?: return emptyList()
  val document = file.document ?: return emptyList()

  return ProgressManager.getInstance().runProcess(Computable {
    filterInfos(
      doFindProblems(project, psiFile, document, daemonIndicator)
    )
  }, daemonIndicator)
}


private fun doFindProblems(
  project: Project,
  file: PsiFile,
  document: Document,
  indicator: ProgressIndicator
): Collection<HighlightInfo> {
  val settings = DaemonCodeAnalyzerSettings.getInstance()
  val dumbService = DumbService.getInstance(project)
  val oldDelay = settings.autoReparseDelay
  try {
    settings.autoReparseDelay = 0
    return dumbService.runReadActionInSmartMode(Computable {
      DaemonCodeAnalyzerEx.getInstanceEx(project).runMainPasses(file, document, indicator)
    })
  } finally {
    settings.autoReparseDelay = oldDelay
  }
}


private fun filterInfos(infos: Collection<HighlightInfo>): Highlights = infos.filter { info ->
  info.severity in listOf(
    HighlightSeverity.ERROR,
    HighlightSeverity.WARNING,
    HighlightSeverity.WEAK_WARNING
  )
}