package com.github.borispristupa.onlynewwarningsplugin.analysis

import com.github.borispristupa.onlynewwarningsplugin.MyBundle
import com.github.borispristupa.onlynewwarningsplugin.document
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vcs.VcsBundle
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.checkin.CheckinHandlerUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.impl.source.tree.injected.changesHandler.range
import com.intellij.testFramework.LightVirtualFile

typealias Highlights = Collection<HighlightInfo>
typealias FileMap<T> = Map<VirtualFile, T>

fun findNewProblems(
  fileChanges: FileMap<Change>,
  project: Project
): FileMap<Highlights> {
  val task =
    object : Task.WithResult<FileMap<Highlights>, Exception>(
      project, MyBundle.message("progress.find.new.problems.title"), true
    ) {
      override fun compute(indicator: ProgressIndicator) = runReadAction {
        findNewProblems(fileChanges, project, indicator)
      }
    }

  return ProgressManager.getInstance().run(task)
}


private fun findNewProblems(
  fileChanges: FileMap<Change>,
  project: Project,
  indicator: ProgressIndicator
): FileMap<Highlights> {
  indicator.isIndeterminate = false

  val notGeneratedNotExcludedFiles =
    CheckinHandlerUtil.filterOutGeneratedAndExcludedFiles(fileChanges.keys, project).toSet()

  val fileChangesToAnalyze = fileChanges.filterKeys(notGeneratedNotExcludedFiles::contains)

  val createdFiles = mutableListOf<VirtualFile>()
  val oldFilesCurrentFiles = mutableListOf<Triple<VirtualFile, VirtualFile, RangeFixer>>()

  for ((file, change) in fileChangesToAnalyze) {
    if (change.afterRevision?.content == null) continue

    val beforeContent = change.beforeRevision?.content
    if (beforeContent == null) {
      createdFiles += file
    } else {
      val doc = file.document ?: continue

      val oldFile = file.lightCopyWithContent(beforeContent)
      val oldDoc = oldFile.document ?: continue

      oldFilesCurrentFiles += Triple(oldFile, file, RangeFixer(oldDoc, doc))
    }
  }

  val newProblemsInChanged =
    oldFilesCurrentFiles.mapIndexedNotNull { i, (oldFile, currentFile, rangeFixer) ->
      indicator.text = VcsBundle.message(
        "searching.for.code.smells.processing.file.progress.text",
        currentFile.presentableUrl
      )
      indicator.fraction = i.toDouble() / (oldFilesCurrentFiles.size + createdFiles.size)

      val currentProblems = findProblems(currentFile, project, indicator)
      val oldProblems = findProblems(oldFile, project, indicator)

      val newProblems = currentProblems.filterNot { currentProblem ->
        oldProblems.any { oldProblem ->
          currentProblem.range == rangeFixer.fixRange(oldProblem.range) &&
              currentProblem.description == oldProblem.description &&
              currentProblem.severity == oldProblem.severity
        }
      }

      (currentFile to newProblems).takeIf { newProblems.isNotEmpty() }
    }

  val newProblemsInCreated = createdFiles.mapIndexed { i, file ->
    indicator.text = VcsBundle.message(
      "searching.for.code.smells.processing.file.progress.text",
      file.presentableUrl
    )
    indicator.fraction =
      (i + oldFilesCurrentFiles.size).toDouble() / (oldFilesCurrentFiles.size + createdFiles.size)
    file to findProblems(file, project, indicator)
  }
  return (newProblemsInChanged + newProblemsInCreated).toMap()
}


private fun VirtualFile.lightCopyWithContent(content: String): VirtualFile =
  LightVirtualFile(
    this,
    StringUtil.convertLineSeparators(content),
    this.modificationStamp
  )
