package bpm.common.workspace.graph

import bpm.common.memory.Buffer
import bpm.common.property.*
import bpm.common.serial.Serial
import bpm.common.serial.Serialize
import org.joml.Vector4f
import org.openjdk.nashorn.internal.ir.FunctionNode
import java.util.*

/**
 * Represents a function node that contains its own subgraph of nodes.
 * A function has input and output edges, and a body containing other nodes.
 */
class Function(override val properties: PropertyMap = Property.Object()) : PropertySupplier {

    /**
     * The unique identifier of the function
     */
    val uid: UUID by properties delegate { UUID.randomUUID() }

    /**
     * Input edges of the function
     */
    val inputs: MutableList<Property<UUID>> by properties delegate { mutableListOf() }

    /**
     * Output edges of the function
     */
    val outputs: MutableList<Property<UUID>> by properties delegate { mutableListOf() }

    /**
     * The color of the function
     */
    var color: Vector4f by properties delegate { Vector4f(0.0f, 0.0f, 0.0f, 1.0f) }

    /**
     * The body of the function, containing nodes
     */
    val nodes: MutableList<Property<UUID>> by properties delegate { mutableListOf() }

    /**
     * The name of the function
     */
    var name: String by properties delegate { UUID.randomUUID().toString() }

    /**
     * The type of the function
     */
    val type: String by properties delegate { "Function" }

    /**
     * The icon of the function
     */
    val icon: Int by properties delegate { 0x1F4D6 }
    /**
     * The x-coordinate of the function
     */
    var x: Float by properties delegate { 0.0f }

    /**
     * The y-coordinate of the function
     */
    var y: Float by properties delegate { 0.0f }

    /**
     * The width of the function
     */
    var width: Float by properties delegate { 0.0f }

    /**
     * The height of the function
     */
    var height: Float by properties delegate { 0.0f }

    /**
     * Whether the function is minimized
     */
    var minimized: Boolean by properties delegate { false }


    object FunctionSerializer : Serialize<Function>(Function::class) {

        /**
         * Deserializes the contents of the Buffer and returns an instance of type T.
         *
         * @return The deserialized object of type T.
         */
        override fun deserialize(buffer: Buffer): Function {
            val properties = Serial.read<PropertyMap>(buffer) ?: error("Failed to read properties from buffer!")
            return Function(properties)
        }

        /**
         * Serializes the provided Node instance into the specified buffer.
         *
         * @param buffer The buffer in which the Node will be serialized.
         * @param value The Node instance to be serialized.
         */
        override fun serialize(buffer: Buffer, value: Function) {
            Serial.write(buffer, value.properties)
        }
    }


}