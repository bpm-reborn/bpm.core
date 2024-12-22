package bpm.common.workspace.graph

import bpm.common.memory.Buffer
import bpm.common.network.NetUtils
import bpm.common.property.*
import bpm.common.serial.Serial
import bpm.common.serial.Serialize
import java.util.*


data class Edge(override val properties: PropertyMap = Property.Object()) : PropertySupplier {

    val name: String by properties delegate { "Edge" }
    val uid: UUID by properties delegate { UUID.randomUUID() }
    var owner: UUID by properties delegate { NetUtils.DefaultUUID }
    val direction: String by properties delegate { "input" }
    val type: String by properties delegate { "exec" }
    val icon: Int by properties delegate { 0 }
    val description: String by properties delegate { "" }
    val value: PropertyMap get() = properties.getTyped("value")

    init {
        if (!properties.contains("value"))
            properties["value"] = Property.Object()
    }


    object EdgeSerializer : Serialize<Edge>(Edge::class) {

        /**
         * Deserializes the contents of the Buffer and returns an instance of type T.
         *
         * @return The deserialized object of type T.
         */
        override fun deserialize(buffer: Buffer): Edge {
            val properties = Serial.read<PropertyMap>(buffer) ?: error("Failed to read properties from buffer!")
            return Edge(properties)
        }

        /**
         * Serializes the provided Node instance into the specified buffer.
         *
         * @param buffer The buffer in which the Node will be serialized.
         * @param value The Node instance to be serialized.
         */
        override fun serialize(buffer: Buffer, value: Edge) {
            Serial.write(buffer, value.properties)
        }
    }

}