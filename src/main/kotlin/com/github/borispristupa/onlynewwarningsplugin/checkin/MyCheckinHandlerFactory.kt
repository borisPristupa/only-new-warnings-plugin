package com.github.borispristupa.onlynewwarningsplugin.checkin

import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory

class MyCheckinHandlerFactory : CheckinHandlerFactory() {
  override fun createHandler(panel: CheckinProjectPanel, ignored: CommitContext) =
    MyCheckinHandler(panel)
}