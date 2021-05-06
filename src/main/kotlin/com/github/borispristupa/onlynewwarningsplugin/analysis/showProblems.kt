package com.github.borispristupa.onlynewwarningsplugin.analysis

import com.github.borispristupa.onlynewwarningsplugin.document
import com.intellij.codeInsight.CodeSmellInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.CodeSmellDetector
import com.intellij.psi.impl.source.tree.injected.changesHandler.range

fun showProblems(highlightsMap: FileMap<Highlights>, project: Project) {
  val smells = highlightsMap.flatMap { (file, highlights) ->
    val document = file.document

    if (document != null)
      highlightsToSmells(highlights, document)
    else emptyList()
  }

  CodeSmellDetector.getInstance(project).showCodeSmellErrors(smells)
}

private fun highlightsToSmells(
  highlights: Collection<HighlightInfo>,
  document: Document
): Collection<CodeSmellInfo> =
  highlights.map { info ->
    CodeSmellInfo(document, info.description, info.range, info.severity)
  }