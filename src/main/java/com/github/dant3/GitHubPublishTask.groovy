package com.github.dant3

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.gradle.api.internal.AbstractTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

public class GitHubPublishTask extends AbstractTask {
    @Input File gitDir;
    @Input String branch;

    @TaskAction
    def publish() {
        def gitRepo = Grgit.open(dir: gitDir)

        def head = getHead(gitRepo)
        gitRepo.add(patterns: ['.'])
        gitRepo.commit(message: "published artifacts for ${project.getGroup()} ${project.version}")
        try {
            gitRepo.push {
                remote = "origin"
                refsOrSpecs = ["$branch:refs/heads/$branch"]
            }
        } catch (Exception ex) {
            if (head != null) {
                gitRepo.checkout {
                    startPoint = head
                    branch = branch
                }
            }
        }
    }

    private Commit getHead(Grgit gitRepo) {
        try {
            return gitRepo.head()
        } catch (Exception ex) {
            return null
        }
    }
}
