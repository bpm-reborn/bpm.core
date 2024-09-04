package bpm.client.font

import imgui.ImFont
import bpm.common.logging.KotlinLogging
import kotlin.math.*

data class Font(val type: FontType, private val sizedFonts: Map<Int, ImFont>) {

    /**
     * Represents a font family used for text rendering.
     *
     * This variable is used to specify the font family to be used for rendering text.
     * It should be set internally and not accessed directly from outside the scope of the class.
     *
     * @property family The font family to be used.
     */
    private lateinit var family: FontFamily

    /**
     * Represents the name of an object.
     * The name consists of the lowercase family name and lowercase type name, separated by a hyphen.
     *
     * @property family The family of the object.
     * @property type The type of the object.
     * @return The generated name string.
     */
    val name: String
        get() = "${this.family.name.lowercase()}-${type.name.lowercase()}"

    /**
     * Gets the header font with size 24.
     *
     * @return The [ImFont] object representing the header font.
     */
    val header: ImFont
        get() = this[24]

    /**
     * Represents the title font used in the application.
     *
     * This font is retrieved from an internal source and returned as an [ImFont] object.
     * The size of the font is specified as 20.
     *
     * @return The title font with a size of 20.
     */
    val title: ImFont
        get() = this[20]
    /**
     * Provides access to the `body` font in the ImFont collection.
     *
     * @return The `body` font as an ImFont object.
     */
    val body: ImFont
        get() = this[16]

    /**
     * A property representing a small font.
     *
     * @return The small font.
     */
    val small: ImFont
        get() = this[12]

    /**
     * Represents a tiny font.
     *
     * @property tiny Gets the tiny font.
     */
    val tiny: ImFont
        get() = this[10]


    /**
     * Set the font family for the font.
     *
     * @param family The font family to be set.
     * @return The modified Font object.
     */
    fun family(family: FontFamily): Font {
        this.family = family
        return this
    }

    /**
     * Retrieve the ImFont object for the specified font size.
     *
     * @param size The font size for which to retrieve the ImFont object.
     * @return The ImFont object corresponding to the specified font size.
     */
    operator fun get(size: Int): ImFont {
        if (size !in sizedFonts) {
            //find the closest font size
            val closest = sizedFonts.keys.minBy { abs(it - size) }
            return sizedFonts[closest]!!
        }
        return sizedFonts[size]!!
    }

    companion object {

        private val logger = KotlinLogging.logger { }
    }


}

