package bpm.common.bootstrap

import bpm.common.logging.KotlinLogging
import bpm.common.property.Property
import bpm.common.property.cast
import bpm.common.property.value
import bpm.common.serial.Serial
import bpm.common.upstream.GitLoader
import bpm.common.workspace.Workspace
import bpm.pipe.PipeNetManagerState
import net.minecraft.server.level.ServerLevel
import net.neoforged.fml.loading.FMLPaths
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeCommand
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.merge.MergeStrategy
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

object BpmIO {

    private val logger = KotlinLogging.logger {}
    private val rootPath: Path = Path.of(FMLPaths.GAMEDIR.get().toString(), "bpm")
    val dataPath: Path = rootPath.resolve("data")
    val worldDataPath: Path = dataPath.resolve("world_data")
    val workspaceDataPath: Path = dataPath.resolve("workspace_data")
    val schemasPath: Path = rootPath.resolve("schemas")
    val docsPath: Path = rootPath.resolve("docs")
    private var config: Configuration

    init {
        createDirectories()
        config = createConfig()
        loadSchemas()
        loadDocs()
    }

    //Create our directories if they don't exist
    private fun createDirectories() {
        listOf(rootPath, dataPath, worldDataPath, workspaceDataPath, schemasPath, docsPath).forEach {
            Files.createDirectories(it)
        }
    }


    //Create our configuration object
    private fun createConfig(): Configuration {
        val configPath = rootPath.resolve("config.yaml")
        val config = Configuration(configPath)
        if (!Files.exists(configPath)) {
            initializeConfiguration(config)
            config.save()
        } else {
            config.load()
        }
        return config
    }

    //Initialize our configuration with the default values if we don't have one
    private fun initializeConfiguration(configuration: Configuration) {
        configuration["upstream.auto_update"] = Property.of(true)

        configuration["upstream.schemas.repo"] = Property.of("https://github.com/bpm-reborn/bpm.nodes.git")
        configuration["upstream.schemas.branch"] = Property.of("main")
        configuration["upstream.schemas.path"] = Property.of(schemasPath.toString())
        configuration["upstream.schemas.tag"] = Property.Null

        configuration["upstream.docs.repo"] = Property.of("https://github.com/bpm-reborn/bpm.docs.git")
        configuration["upstream.docs.branch"] = Property.of("main")
        configuration["upstream.docs.path"] = Property.of(docsPath.toString())
        configuration["upstream.docs.tag"] = Property.Null


        configuration["workspace.tick_rate"] = Property.of(20)
        configuration["workspace.auto_save"] = Property.of(true)
        configuration["workspace.auto_save_interval"] = Property.of(600)
    }


    fun savePipeNetState(level: ServerLevel, state: PipeNetManagerState) {
        val levelFileName = "${level.name}.dat"
        val filePath = worldDataPath.resolve(levelFileName)
        Serial.write(filePath, state)
    }

    fun loadPipeNetState(level: ServerLevel): PipeNetManagerState? {
        val levelFileName = "${level.name}.dat"
        val filePath = worldDataPath.resolve(levelFileName)
        if (!Files.exists(filePath)) return null
        return Serial.read(filePath)
    }


    fun saveWorkspace(workspace: Workspace) {
        val path = workspaceDataPath.resolve("${workspace.uid}.dat")
        Serial.write(path, workspace)
    }

    fun loadWorkspace(workspaceUid: UUID): Workspace? {
        val path = workspaceDataPath.resolve("$workspaceUid.dat")
        if (!Files.exists(path)) return null
        return Serial.read(path)
    }

    fun listWorkspaces(): List<UUID> {
        return Files.list(workspaceDataPath).map {
            UUID.fromString(it.fileName.toString().removeSuffix(".dat"))
        }.toList()
    }

    fun loadSchemas() {
        val repoUrl: String = config["upstream.schemas.repo"]?.value() ?: let {
            logger.error { "No upstream repo specified in config!" }
            return
        }
        val tag: String? = config["upstream.schemas.tag"]?.value()
        val branch: String? = config["upstream.schemas.branch"]?.value()
        if (tag != null && branch != null) {
            logger.error { "Both tag and branch are specified in config, please specify only one!" }
            return
        }
        val path: String = config["upstream.schemas.path"]?.value() ?: let {
            logger.warn { "No path specified in config, defaulting to $schemasPath" }
            schemasPath.toString()
        }

        val autoUpdate: Boolean = config["upstream.auto_update"]?.value() ?: let {
            logger.warn { "No auto_update value specified in config, defaulting to true" }
            true
        }

        val repoPath = Path.of(path)

        if (!Files.exists(repoPath) || !Files.exists(repoPath.resolve(".git"))) {
            cloneRepo(repoUrl, branch ?: tag ?: "main", repoPath)
        } else if (autoUpdate) {
            updateRepo(repoPath, branch ?: tag ?: "main")
        }
    }

    fun loadDocs() {
        val repoUrl: String = config["upstream.docs.repo"]?.value() ?: let {
            logger.error { "No upstream docs repo specified in config!" }
            return
        }
        val tag: String? = config["upstream.docs.tag"]?.value()
        val branch: String? = config["upstream.docs.branch"]?.value()
        if (tag != null && branch != null) {
            logger.error { "Both tag and branch are specified in config for docs, please specify only one!" }
            return
        }
        val path: String = config["upstream.docs.path"]?.value() ?: let {
            logger.warn { "No path specified in config for docs, defaulting to $docsPath" }
            docsPath.toString()
        }

        val autoUpdate: Boolean = config["upstream.auto_update"]?.value() ?: let {
            logger.warn { "No auto_update value specified in config, defaulting to true" }
            true
        }

        val repoPath = Path.of(path)

        if (!Files.exists(repoPath) || !Files.exists(repoPath.resolve(".git"))) {
            cloneRepo(repoUrl, branch ?: tag ?: "main", repoPath)
        } else if (autoUpdate) {
            updateRepo(repoPath, branch ?: tag ?: "main")
        }
    }

    private fun cloneRepo(repoUrl: String, branchOrTag: String, localPath: Path) {
        logger.info { "Cloning repository from $repoUrl" }
        try {
            Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(localPath.toFile())
                .setBranch(branchOrTag)
                .call().use { git ->
                    logger.info { "Repository cloned successfully" }
                }
        } catch (e: GitAPIException) {
            logger.error(e) { "Failed to clone repository" }
        }
    }

    private fun updateRepo(localPath: Path, branchOrTag: String) {
        logger.info { "Updating repository at $localPath" }
        try {
            Git.open(localPath.toFile()).use { git ->
                // Check for local changes
                val status = git.status().call()
                if (!status.isClean) {
                    // Stash local changes
                    val stashCreateCommand = git.stashCreate()
                    val stashId = stashCreateCommand.call()
                    if (stashId != null) {
                        logger.info { "Stashed local changes" }
                    } else {
                        logger.warn { "No local changes to stash" }
                    }
                }

                // Pull latest changes
                val pullResult = git.pull()
                    .setRemoteBranchName(branchOrTag)
                    .setFastForward(MergeCommand.FastForwardMode.FF_ONLY)
                    .setStrategy(MergeStrategy.THEIRS)
                    .call()

                if (pullResult.isSuccessful) {
                    logger.info { "Repository updated successfully" }
                } else {
                    // If fast-forward fails, do a force update
                    git.fetch().setForceUpdate(true).call()
                    git.reset().setMode(ResetCommand.ResetType.HARD)
                        .setRef("origin/$branchOrTag").call()
                    logger.info { "Repository force updated to origin/$branchOrTag" }
                }

                // Inform about stashed changes
                val stashList = git.stashList().call()
                if (stashList.isNotEmpty()) {
                    logger.info { "Local changes are stashed. Use 'git stash apply' to recover them if needed." }
                }
            }
        } catch (e: GitAPIException) {
            logger.error(e) { "Failed to update repository" }
        }
    }


    private val ServerLevel.name: String
        get() = this.toString().removePrefix("ServerLevel[").removeSuffix("]")

}