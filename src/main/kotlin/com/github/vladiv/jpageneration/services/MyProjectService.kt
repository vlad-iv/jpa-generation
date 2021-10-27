package com.github.vladiv.jpageneration.services

import com.intellij.openapi.project.Project
import com.github.vladiv.jpageneration.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
