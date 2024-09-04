package bpm.client.font

import java.nio.file.*

object FontDataLoader {

    private val fontData: MutableMap<String, FontData> = mutableMapOf()

    /**
     * Loads the font data from the specified path and returns the FontData object.
     * If the font data has already been loaded, it is returned from the cache.
     *
     * @param path The path of the font data file.
     * @return The FontData object.
     * @throws Exception If the font data cannot be loaded from the specified path.
     */
    fun load(path: String): FontData {
        val fontData = fontData.getOrPut(path) {
            FontDataLoader::class.java.getResourceAsStream(path).use { stream ->
                val bytes = stream?.readBytes() ?: throw Exception("Could not load font data from $path")
                val fontData = FontData(bytes)
                FontDataLoader.fontData[path] = fontData
                fontData
            }
        }
        return fontData
    }

    fun loadFromResources(file: String, glyphs: Pair<Short, Short>?, merge: Boolean = false): FontData {
        val bytes = FontDataLoader::class.java.getResourceAsStream(file).use {
            it?.readBytes() ?: throw Exception("Could not load font data from $file")
        }
        return fontData.getOrPut(file) {
            val fontData = if (glyphs == null) FontData(bytes) else FontData(
                bytes, shortArrayOf(glyphs.first, glyphs.second)
            )
            FontDataLoader.fontData[file] = fontData
            fontData.merge = merge
            fontData
        }
    }


    fun loadFromFile(file: String, glyphs: Pair<Short, Short>?, merge: Boolean = false): FontData {
        val path = Paths.get(file)
        return fontData.getOrPut(file) {
            val bytes = Files.readAllBytes(path)
            val fontData = if (glyphs == null) FontData(bytes) else FontData(
                bytes, shortArrayOf(glyphs.first, glyphs.second)
            )
            fontData.merge = merge
            FontDataLoader.fontData[file] = fontData
            fontData
        }
    }


}