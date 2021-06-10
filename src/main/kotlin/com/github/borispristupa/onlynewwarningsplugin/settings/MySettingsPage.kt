package com.github.borispristupa.onlynewwarningsplugin.settings

import com.github.borispristupa.onlynewwarningsplugin.MyBundle
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.layout.panel

class MySettingsPage(private val project: Project) : BoundConfigurable(MyBundle.message("configurable.com.github.borispristupa.onlynewwarningsplugin.SettingsPage.displayName")) {

  override fun createPanel(): DialogPanel = panel {
    titledRow(MyBundle.message("configurable.com.github.borispristupa.onlynewwarningsplugin.SettingsPage.severity.selector.title")) {
      for (severity in project.service<MySettings>().severities) {
        row { checkBox(StringUtil.pluralize(severity.name), severity::isSelected) }
      }
    }
  }
}
