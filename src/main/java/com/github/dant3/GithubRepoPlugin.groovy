package com.github.dant3

import org.gradle.api.Plugin
import org.gradle.api.Project

class GithubRepoPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        if (!project.repositories.metaClass.respondsTo(project.repositories, 'githubRepo', String, String, String)) {
            project.repositories.metaClass.githubRepo = { String org, String proj, String branch = "master" ->
                new GitHubRepo(org, proj, branch).setupOn(project)
            }
        }
    }
}
