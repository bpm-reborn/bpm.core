package bpm.common.workspace.graph

import bpm.common.network.NetUtils
import bpm.common.property.Property
import bpm.common.property.PropertyMap
import bpm.common.property.PropertySupplier
import bpm.common.property.delegate
import java.util.*

/**
 * Represents a Connection between two entities.
 *
 * This class provides a way to create and manage connections by generating and accessing universally unique identifier (UUID) values.
 *
 * @property properties The map of properties associated with the connection.
 */
data class Link(override val properties: PropertyMap = Property.Object()) : PropertySupplier {

    /**
     * Universally unique identifier (UUID) for identifying entities.
     *
     * This variable uses a properties delegate to generate a new random UUID
     * using [UUID.randomUUID] each time it is accessed.
     */
    val uid: UUID by properties delegate { UUID.randomUUID() }

    /**
     * A delegated property that generates a new random UUID when accessed.
     * The UUID is generated using the [UUID.randomUUID] function.
     *
     * @property from The property that holds the generated UUID value.
     */
    val from: UUID by properties delegate { NetUtils.DefaultUUID }

    /**
     * Represents a delegated property for generating and accessing a universally unique identifier (UUID) value.
     * The generated UUID is assigned to the property 'to' upon its first access.
     *
     * @property to The delegated property for the UUID value.
     */
    val to: UUID by properties delegate { NetUtils.DefaultUUID }
}