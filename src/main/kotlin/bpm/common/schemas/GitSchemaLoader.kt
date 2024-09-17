package bpm.common.schemas

import org.eclipse.jgit.api.Git
import java.nio.file.Path
import java.nio.file.Files
import bpm.common.logging.KotlinLogging

class GitSchemaLoader(private val repoUrl: String, private val branch: String, private val localPath: Path) {

    private val logger = KotlinLogging.logger {}

    fun cloneOrPull(): Path {
        //Simply creates a temp dir and clones the repo
        if (!Files.exists(localPath)) {
            cloneRepo()
        } else {
            pullRepo()
        }
        return localPath
    }

    private fun cloneRepo() {
        logger.info { "Cloning repository from $repoUrl" }
        Git.cloneRepository()
            .setURI(repoUrl)
            .setDirectory(localPath.toFile())
            .setBranch(branch)
            .call().use { git ->
                logger.info { "Repository cloned successfully" }
            }
    }

    private fun pullRepo() {
        logger.info { "Pulling latest changes from $repoUrl" }
        Git.open(localPath.toFile()).use { git ->
            git.pull()
                .setRemoteBranchName(branch)
                .call()
            logger.info { "Repository pulled successfully" }
        }
    }
}