package bpm.common.workspace

import bpm.common.property.*
import bpm.common.memory.Buffer
import bpm.common.serial.Serialize
import org.joml.*
import kotlin.math.*

/**
 * The GraphSettings class represents the settings for a graph.
 *
 * @property properties The map of properties for the graph settings.
 * @property backgroundColor The background color of the graph.
 * @property gridColor The color of the grid lines in the graph.
 * @property gridSubColor The color of the sub grid lines in the graph.
 * @property scrolled The position of the view in the graph.
 * @property zoom The zoom level of the graph.
 * @constructor Creates a new instance of GraphSettings with the specified properties.
 */
data class WorkspaceSettings(
    private val properties: PropertyMap = Property.Object()
) {

    /**
     * A property delegate representing the background color.
     *
     * The background color is defined as a `Vector4i` object representing the RGBA values. The default background color is
     * set to (84, 84, 84, 255) which represents a dark gray color.
     *
     * @property backgroundColor The background color represented as a `Vector4i` object with RGBA values.
     * @since 1.0.0
     */
    var backgroundColor by properties to { Vector4i(84, 84, 84, 255) }

    /**
     * The color of the grid lines.
     *
     * This variable represents the color of the grid lines as a Vector4i object.
     * The Vector4i object stores the red, green, blue, and alpha components
     * of the color in the range of 0 to 255.
     * The default value of the grid color is (178, 178, 178, 233).
     *
     * @see Vector4i
     */
    var gridColor by properties to { Vector4i(178, 178, 178, 233) }

    /**
     * The color of the grid sub lines.
     *
     * This variable represents the color of the grid sub lines. It is used to define the RGBA values of the color.
     *
     * @property gridSubColor
     * @type Vector4i
     * @defaultValue Vector4i(138, 138, 138, 244)
     */
    var gridSubColor by properties to { Vector4i(138, 138, 138, 244) }

    /**
     * The current position in a grid view.
     *
     * The [scrolled] represents the position of an element within a grid view.
     * It is implemented as a delegated property which defaults to `Vector2i(0, 0)`.
     * This means that if no position is explicitly set, the default value will be `(0, 0)`.
     *
     * @property scrolled The current position in the grid view.
     *
     * @see Vector2i
     */
    var scrolled by properties to { Vector2f(0f, 0f) }

    /**
     * The variable bounds is a mutable property that represents a set of 4D vector bounds.
     *
     * @property bounds The current 4D vector bounds.
     */
    val bounds by properties to { Vector4f() }

    /**
     * Variable `extents` represents the extents of an object in 3D space.
     *
     * The extents are defined as a Vector4f object, which holds four components: x, y, z, and w.
     * Here, x, y, and z represent the dimensions of the object in the corresponding axes,
     * while w represents an additional property of the extents.
     *
     * By default, the `extents` variable is initialized with a Vector4f object created using
     * the default constructor. The default constructor sets all components to 0.0.
     *
     * You can assign a different Vector4f object to the `extents` variable to update the extents.
     *
     * @property extents The extents of the object in 3D space.
     * @see Vector4f
     */
    val extents by properties to { Vector4f() }

    /**
     * Property delegate for the grid zoom level.
     *
     * The default value is 1.0f.
     *
     * @property zoom the property representing the grid zoom level
     */
    var zoom by properties to { 1.0f }

    /**
     * Returns the fontSize value based on the gridZoom value.
     *
     * The fontSize value is calculated by multiplying the gridZoom value by 16 and rounding it to the nearest integer.
     *
     * @return The calculated fontSize value.
     */
    val fontSize: Int get() = max(round(16f * zoom).roundToInt(), 8)

    /**
     * Calculates the header font size based on the current grid zoom level.
     *
     * The header font size is calculated by rounding the result of the product
     * of 24 and the grid zoom level, and then rounding it to the nearest integer value.
     *
     * @return The calculated header font size.
     */
    val fontHeaderSize: Int get() = max(round(18f * zoom).roundToInt(), 12)


    /**
     * Serializer class for serializing and deserializing objects of type [WorkspaceSettings].
     *
     * This class implements the [Serialize] interface, which allows for custom serialization and deserialization logic.
     * The serialized format is based on the [Buffer] implementation.
     *
     * @param T The type of objects that can be serialized/deserialized.
     * @property targetClass The class of objects that can be serialized/deserialized.
     * @constructor Creates a new instance of the Serializer class.
     */
    object Serializer : Serialize<WorkspaceSettings>(WorkspaceSettings::class) {

        override fun deserialize(buffer: Buffer): WorkspaceSettings =
            WorkspaceSettings(PropertySerializer.read(buffer).cast())

        override fun serialize(buffer: Buffer, value: WorkspaceSettings) =
            PropertySerializer.write(buffer, value.properties)

    }

}