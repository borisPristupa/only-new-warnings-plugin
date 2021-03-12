package com.github.borispristupa.onlynewwarningsplugin.services

import com.github.borispristupa.onlynewwarningsplugin.MyBundle
import com.intellij.openapi.project.Project

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
