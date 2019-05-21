package com.github.dant3

import org.ajoberstar.grgit.Grgit
import org.eclipse.jgit.api.errors.NoHeadException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.publish.Publication
import org.gradle.api.publish.PublishingExtension

import java.nio.file.Paths

class GitHubRepo {
    private final String owner;
    private final String repository;
    private final String branchName;

    public GitHubRepo(String owner, String repository, String branch) {
        this.owner = owner;
        this.repository = repository;
        this.branchName = branch;
    }

    String remote() {
        return "git@github.com:$owner/${repository}.git"
    }

    File directory(Project project) {
        def gitRoot = Paths.get(project.rootDir.absolutePath, ".gitRepos")
        return project.file(gitRoot.resolve(owner).resolve(repository).resolve(branchName))
    }

    def setupOn(Project project) {
        cloneOrPull(project)
        project.repositories.maven {
            it.url directory(project).absolutePath
        }
        setupPublishing(project)
    }

    def cloneOrPull(Project project) {
        def directory = directory(project)
        def repository = getGitRepo(directory, project)
        if (!project.hasProperty("offline")) {
            if (isRepositoryInitialized(repository)) { // if repository is empty, just don't bother
                repository.checkout {
                    branch = this.branchName
                }
                repository.pull()
            }
        }
    }

    private def isRepositoryInitialized(Grgit repository) {
        try {
            return !repository.log({
                maxCommits = 1
            }).isEmpty()
        } catch(NoHeadException ex) {
            return false;
        }
    }

    private Grgit getGitRepo(File directory, Project project) {
        if (directory.isDirectory() || project.hasProperty("offline")) {
            return Grgit.open {
                dir = directory
            }
        } else {
            return Grgit.clone {
                dir = directory
                uri = remote()
                refToCheckout = branchName
            }
        }
    }

    private def setupPublishing(Project project) {
        Task commitAndPush = project.tasks.create("pushArtifactsToGithub", GitHubPublishTask.class) {
            gitDir directory(project)
            branch branchName
        }
        def hasPublications = project.getPluginManager().hasPlugin("maven-publish")
        if (hasPublications) {
            // add a publishToGithub task
            def publicationsExt = project.getExtensions().getByType(PublishingExtension.class)
            publicationsExt.repositories {
                it.maven {
                    it.name = "github"
                    it.url = directory(project)
                }
            }

            project.afterEvaluate {
                publicationsExt.publications.forEach { publication ->
                    def taskName = getPublishTaskName(publication)
                    def publishTask = project.tasks.findByName(taskName)
                    println("For task name $taskName we found task $publishTask")
                    if (publishTask != null) {
                        commitAndPush.dependsOn(publishTask)
                        publishTask.finalizedBy(commitAndPush)
                    }
                }
            }
        }
    }

    private def getPublishTaskName(Publication publication) {
        return "publish${toCamelNamePart(publication.name)}PublicationToGithubRepository"
    }

    private def toCamelNamePart(String name) {
        return name.charAt(0).toUpperCase().toString() + name.substring(1)
    }
}
