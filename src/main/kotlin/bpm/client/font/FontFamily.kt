package bpm.client.font

import bpm.common.logging.KotlinLogging

data class FontFamily(val name: String, private val loadedFonts: MutableMap<FontType, Font> = mutableMapOf()) {


    /**
     * Represents the extra light font style.
     *
     * @return The font object with the extra light style.
     */
    val extraLight: Font
        get() = this[FontType.ExtraLight]

    /**
     * Represents a light font type.
     *
     * @property light The light font type.
     */
    val light: Font
        get() = this[FontType.Light]

    /**
     * Retrieves the regular Font from the specified FontType.
     *
     * @return The regular Font.
     */
    val regular: Font
        get() = this[FontType.Regular]

    /**
     * Returns the medium font from the set of available fonts.
     *
     * @return The medium font.
     */
    val medium: Font
        get() = this[FontType.Medium]

    /**
     * Represents a bold font style.
     *
     * @return The bold font style.
     */
    val bold: Font
        get() = this[FontType.Bold]

    /**
     * Represents a semi-bold font.
     *
     * @return The [Font] object for the semi-bold font type.
     */
    val semiBold: Font
        get() = this[FontType.SemiBold]

    /**
     * Represents an extra bold font.
     *
     * This variable provides access to the extra bold font associated with the current instance of `Font`.
     * It can be accessed using the `extraBold` property.
     *
     * @see Font
     * @see FontType
     */
    val extraBold: Font
        get() = this[FontType.ExtraBold]

    /**
     * Represents the black font in the application.
     * This font is retrieved using the [FontType.Black] key.
     *
     * @return The black font.
     */
    val black: Font
        get() = this[FontType.Black]


    /**
     * Retrieves the font of the specified type.
     *
     * @param type The type of the font to retrieve.
     * @return The font of the specified type.
     * @throws IllegalArgumentException if the specified type is Type.Regular and no Regular font is found for the font family.
     */
    operator fun get(type: FontType): Font {
        if (type !in loadedFonts && type == FontType.Regular) throw IllegalArgumentException("Regular font not found for font family $name")
        if (type !in loadedFonts) return get(FontType.Regular)
        return loadedFonts[type]!!
    }


    /**
     * Retrieves the element of the specified type from the collection.
     *
     * @param type the string representation of the type of the element
     * @return the element of the specified type
     * @throws IllegalArgumentException if the specified type is invalid or not found in the collection
     */
    operator

    fun get(type: String) = get(FontType.valueOf(type))

    /**
     * Determines whether the given type exists in the fonts collection.
     *
     * @param type The type to check for existence in the fonts collection.
     * @return `true` if the fonts collection contains the specified type, `false` otherwise.
     */
    operator fun contains(type: FontType) = loadedFonts.containsKey(type)
    /**
     * Executes the given closure on each element in the collection of loaded fonts.
     *
     * @param closure The closure to be executed on each font element. The closure takes two parameters:
     *                [FontType] - the type of the font, and [Font] - the font object itself.
     */
    fun forEach(closure: (Font) -> Unit) = loadedFonts.values.forEach(closure)


    companion object {

        private val logger = KotlinLogging.logger { }
    }
}