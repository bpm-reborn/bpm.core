package bpm.client.font

import imgui.*
import bpm.common.logging.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

object Fonts {


    private val unloadedFontFamilies: MutableMap<String, UnloadedFontFamily> = mutableMapOf()
    private val fontFamilies: ConcurrentHashMap<String, FontFamily> = ConcurrentHashMap()
    private val logger = KotlinLogging.logger {}
    /**
     * Registers a font family with the given parameters.
     *
     * @param name The name of the font family.
     * @param sizes The range of font sizes supported by the font family. Default value is 8..16.
     * @param types The list of font types supported by the font family. Default value is FontType.entries.
     * @param fontPath The path pattern of the font files for the font family. Default value is "$*/
    fun register(
        name: String,
        sizes: IntRange = 8..16,
        types: List<FontType> = FontType.entries,
        glyphRange: Pair<Short, Short>? = null,
        fontPath: String = "/fonts/$name-%t.ttf",
        merge: Boolean = false
    ) {
        val unloadedFontFamily = UnloadedFontFamily(name,
            sizes,
            types.associateWith { FontDataLoader.loadFromResources(fontPath.replace("%t", it.name), glyphRange, merge) })
//            types.associateWith { FontDataLoader.loadFromResources(fontPath.replace("%t", it.name), glyphRange, merge) })
        unloadedFontFamilies[name] = unloadedFontFamily
        logger.info { "Registered font family: $name, $sizes, $types" }
    }

    /**
     * Method to load font families that were previously unloaded.
     */
    fun registerFonts() {
        if (unloadedFontFamilies.isEmpty()) return
        unloadedFontFamilies.forEach { (_, unloadedFontFamily) ->
            loadFontFamily(unloadedFontFamily)
        }
        val first = unloadedFontFamilies.keys.firstOrNull() ?: return
        // set default font size
        ImGui.getIO().setFontDefault(getFamily(first).regular.body)
        unloadedFontFamilies.clear()

    }

    /**
     * This method allows us to load fonts with varied sizes. This is mainly for the purpose of loading fonts with different sizes for different DPIs.
     * It also has the added benefit of allowing us a way to simulate font scaling without having to actually scale the font.
     * Graph rendering benefits from this because it allows us to zoom in and out without having to scale the font.
     *
     * @param unloadedFontData The font data to be loaded.
     */
    private fun loadFontFamily(unloadedFontData: UnloadedFontFamily) {
        val loadedFonts = mutableMapOf<FontType, Font>()
        // Iterate over each font type to load the font into the ImFont collection
        unloadedFontData.data.forEach { (type, fontData) ->
            val sizedFonts = mutableMapOf<Int, ImFont>()
            for (i in unloadedFontData.sizes) {
                val fontConfig = ImFontConfig()
                if (fontData.merge && fontData.fontRange != null) {
                    fontConfig.mergeMode = fontData.merge
                    fontConfig.sizePixels = i.toFloat()
                    fontConfig.glyphRanges = fontData.fontRange
                    val imFont = ImGui.getIO().fonts.addFontFromMemoryTTF(
                        fontData.bytes, i.toFloat(), fontConfig, fontData.fontRange
                    )
                    sizedFonts[i] = imFont
                } else {
                    fontConfig.mergeMode = true
                    fontConfig.sizePixels = i.toFloat()

                    val imFont = ImGui.getIO().fonts.addFontFromMemoryTTF(
                        fontData.bytes, i.toFloat()
                    )
                    sizedFonts[i] = imFont
                }
            }
            loadedFonts[type] = Font(type, sizedFonts)
        }
        // Create the font family
        val family = FontFamily(unloadedFontData.name, loadedFonts)
        fontFamilies[unloadedFontData.name] = family
        // Set the family for each font
        family.forEach {
            it.family(family)
        }

        logger.info { "Loaded font family: ${unloadedFontData.name}" }
    }

    /**
     * Checks if the given [name] is present in the font families.
     *
     * @param name The font name to check for presence in the font families.
     * @return True if the [name] is present in the font families, false otherwise.
     */
    operator fun contains(name: String) = fontFamilies.containsKey(name)

    /**
     * Retrieves the font family associated with the given name.
     *
     * @param name The name of the font family to retrieve.
     * @throws IllegalArgumentException If the font family with the given name is not found.
     * @return The font family associated with the given name.
     */
    fun getFamily(name: String) = fontFamilies[name] ?: throw IllegalArgumentException("Font family $name not found")

    /**
     * Finds the font with the given name and size.
     */
    operator fun get(name: String): ImFont {
        if (!name.contains("-")) return getFamily(name).regular.body
        val split = name.split("-")
        val family = getFamily(split[0])
        val type = FontType.valueOf(split[1])
        val size = split[2].toInt()
        return family[type][size]
    }

    /**
     * Applies a specific font to the current ImGui context and executes a block of code.
     *
     * @param name The name of the font to use. Must match the name of a loaded font.
     * @param block The code block to be executed with the specified font applied.
     */
    inline fun with(name: String, block: () -> Unit) {
        val font = get(name)
        ImGui.pushFont(font)
        block()
        ImGui.popFont()
    }

    private data class UnloadedFontFamily(
        val name: String, val sizes: IntRange, val data: Map<FontType, FontData>,
    )
}