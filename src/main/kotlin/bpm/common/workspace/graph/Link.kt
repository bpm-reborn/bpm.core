package bpm.common.workspace.graph

import bpm.common.network.NetUtils
import bpm.common.property.Property
import bpm.common.property.PropertyMap
import bpm.common.property.PropertySupplier
import bpm.common.property.delegate
import java.util.*

/**
 * Represents a Connection between edges of 2 nodes.
 *
 * @property properties The property map to save the state of the link.
 */
data class Link(override val properties: PropertyMap = Property.Object()) : PropertySupplier {

    /**
     * The Unique Identifier for this Link.
     *
     * This value is generated using [UUID.randomUUID] when created.
     */
    val uid: UUID by properties delegate { UUID.randomUUID() }

    /**
     * The Unique Identifier for the input edge.
     *
     * This value is defaulted to [NetUtils.DefaultUUID] on creation,
     * and later set to the actual uid of the edge.
     */
    val from: UUID by properties delegate { NetUtils.DefaultUUID }

    /**
     * The Unique Identifier for the output edge.
     *
     * This value is defaulted to [NetUtils.DefaultUUID] on creation,
     * and later set to the actual uid of the edge.
     */
    val to: UUID by properties delegate { NetUtils.DefaultUUID }
}