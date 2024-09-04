package bpm.common.type

import bpm.common.property.*


data class NodeType(
    val meta: NodeTypeMeta,
    val properties: PropertyMap,
) : Property.Object(properties.get()) {

    fun isAbstract(): Boolean = meta.group == "Base"
}
