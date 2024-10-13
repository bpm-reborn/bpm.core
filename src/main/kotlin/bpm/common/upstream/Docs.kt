package bpm.client.docs

import bpm.common.bootstrap.BpmIO
import bpm.common.logging.KotlinLogging
import bpm.common.network.Listener
import bpm.common.packets.Packet
import org.commonmark.node.Node
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.readText

class Docs : Listener {

    private val logger = KotlinLogging.logger { }
    private val docTree: MutableMap<String, Any> = mutableMapOf()
    private val documentCache: MutableMap<String, Node> = mutableMapOf()
    private val parser = Parser.builder().build()
    private var renderer: HtmlRenderer = HtmlRenderer.builder().build()

    override fun onInstall() {
        // Load the documentation
        loadDocTree(BpmIO.docsPath)
        logger.info { "Loaded documentation" }
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
                        val document = parser.parse(path.readText())
                        documentCache[path.name] = document
                        result[path.name] = renderer.render(document)
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
        BpmIO.loadDocs() // This will update the repository if necessary
        loadDocTree(BpmIO.docsPath)
        logger.info { "Reloaded documentation" }
    }

    // This is kept in case we need to handle any client-side packets in the future
    override fun onPacket(packet: Packet, from: UUID) {
        // No packet handling needed for client-side docs
    }
}