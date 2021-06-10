package com.github.borispristupa.onlynewwarningsplugin.settings

import com.github.borispristupa.onlynewwarningsplugin.BooleanProperty
import com.intellij.codeInsight.daemon.impl.SeverityRegistrar
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil

class MySettings(private val project: Project) {
  val severities
    get() = SeverityRegistrar.getSeverityRegistrar(project).allSeverities
      .filter {
        @Suppress("DEPRECATION") // we don't want a deprecated settings option
        it != HighlightSeverity.INFO && it > HighlightSeverity.INFORMATION
      }
      .map { SeverityOption(it) }
      .toSortedSet(
        Comparator.comparing(SeverityOption::severity).reversed()
          .thenComparing(SeverityOption::name)
      )
}

private fun prettifySeverityName(name: String) =
  StringUtil.capitalizeWords(StringUtil.toLowerCase(name), true)

class SeverityOption(val severity: HighlightSeverity) {
  val name = prettifySeverityName(severity.name)
  var isSelected: Boolean
      by BooleanProperty(name, severity > HighlightSeverity.WEAK_WARNING)
}