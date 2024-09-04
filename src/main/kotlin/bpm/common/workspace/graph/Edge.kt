package bpm.common.workspace.graph

import bpm.common.network.NetUtils
import bpm.common.property.*
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

}