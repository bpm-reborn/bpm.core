package bpm.common.type

import com.charleskorn.kaml.*
import java.nio.file.*

data class NodeTypeMeta(
    /**
     * The path of the node type. (A yaml file with .node extension)
     */
    val path: Path,
    /**
     * The name of the node type. (The name of the yaml file without the .node extension) or the processed name
     */
    val name: String,
    /**
     * The group of the node type. (The name of the parent directory of the yaml file)
     */
    val group: String,

    /**
     * The type that we extend from. This may be empty up until the node is fully parsed bredth first.
     */
    val extends: String = "",
) {

    constructor(
        name: String,
        group: String,
        extends: String = "",
    ) : this(
        path = Paths.get(""),
        name = name,
        group = group,
        extends = extends,
    )

    /**
     * The yaml map of the node type
     */
    val yaml: YamlMap
        get() = Yaml.default.parseToYamlNode(Files.readString(path)).yamlMap

    val nodeTypeName: String = "$group/$name"

    /**
     * Returns the last modified timestamp of a file.
     *
     * This property retrieves the last modified timestamp of a file by obtaining the file's `Path`
     * and using the `Files.getLastModifiedTime()` method. The returned timestamp is in milliseconds.
     *
     * @return The last modified timestamp of the file, in milliseconds.
     */
    val lastModified: Long
        get() = Files.getLastModifiedTime(path).toMillis()

    /**
     * Returns a string representation of the object.
     *
     * This method overrides the default implementation of the `toString` method.
     *
     * @return a string representation of the object
     */
    override fun toString(): String = nodeTypeName
}