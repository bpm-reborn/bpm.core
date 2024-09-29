package bpm.client.docs

import bpm.common.logging.KotlinLogging
import bpm.common.network.Listener
import bpm.common.packets.Packet
import bpm.common.upstream.GitLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.readText

class Docs(private val path: Path) : Listener {

    private val logger = KotlinLogging.logger { }
    private val gitDocLoader = GitLoader("https://github.com/meng-devs/bpm.docs.git", "main", path)
    private val docTree: MutableMap<String, Any> = mutableMapOf()

    override fun onInstall() {
        // Clone or pull the repository and load the documentation
        val docsPath = gitDocLoader.cloneOrPull()
        loadDocTree(docsPath)
        logger.info { "Loaded documentation from Git repository" }
    }

    private fun loadDocTree(rootPath: Path) {
        docTree.clear()
        docTree.putAll(loadDirectory(rootPath))
    }

    private fun loadDirectory(dir: Path): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        Files.list(dir).use { paths ->
            paths.forEach { path ->
                when {
                    path.isDirectory() -> {
                        result[path.name] = loadDirectory(path)
                    }
                    path.toString().endsWith(".md") -> {
                        result[path.name] = path.readText()
                    }
                }
            }
        }
        return result
    }

    fun getDocContent(path: String): String? {
        val parts = path.split("/")
        var current: Any = docTree
        for (part in parts) {
            current = (current as? Map<*, *>)?.get(part) ?: return null
        }
        return current as? String
    }

    fun getDocTree(): Map<String, Any> = docTree

    fun reloadDocs() {
        val docsPath = gitDocLoader.cloneOrPull()
        loadDocTree(docsPath)
        logger.info { "Reloaded documentation from Git repository" }
    }

    // This is kept in case we need to handle any client-side packets in the future
    override fun onPacket(packet: Packet, from: UUID) {
        // No packet handling needed for client-side docs
    }
}