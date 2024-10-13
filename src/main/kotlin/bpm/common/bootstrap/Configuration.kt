package bpm.common.bootstrap

import bpm.common.property.*
import bpm.common.logging.KotlinLogging
import com.charleskorn.kaml.*
import java.nio.file.Path
import java.nio.file.Files
import org.joml.*

class Configuration(private val configPath: Path) {

    private val logger = KotlinLogging.logger {}
    private val yaml = Yaml.default
    private var yamlMap: YamlMap = YamlMap(emptyMap(), YamlPath.root)
    private val root: PropertyMap = Property.Object()


    init {
        load()
    }

    fun load() {
        if (!Files.exists(configPath)) {
            logger.warn { "Configuration file not found at $configPath. Creating a default configuration." }
            return
        }
        val yamlContent = Files.readString(configPath)
        if (yamlContent.isEmpty()) {
            logger.warn { "Configuration file is empty at $configPath. Creating a default configuration." }
            return
        }
        yamlMap = yaml.parseToYamlNode(yamlContent).yamlMap
        parseYamlMap(yamlMap)
        logger.info { "Configuration loaded from $configPath" }
    }

    fun save() {
        val yamlString = propertyMapToYamlString(root)
        Files.write(configPath, yamlString.toByteArray())
        logger.info { "Configuration saved to $configPath" }
    }


    private fun parseYamlMap(map: YamlMap) {
        for ((key, value) in map.entries) {
            this[key.content] = parseYamlNode(value, key.path)
        }
    }

    private fun parseYamlNode(node: YamlNode, path: YamlPath): Property<*> {
        return when (node) {
            is YamlScalar -> parseScalar(node, path)
            is YamlMap -> parseYamlMapToProperty(node, path)
            is YamlList -> parseYamlListToProperty(node, path)
            is YamlNull -> Property.Null
            is YamlTaggedNode -> parseYamlNode(node.innerNode, path) // You might want to handle tags differently
            else -> throw IllegalArgumentException("Unsupported YAML node type: ${node::class.simpleName}")
        }
    }

    private fun parseScalar(scalar: YamlScalar, path: YamlPath): Property<*> {
        return try {
            when {
                scalar.content.equals("true", ignoreCase = true) -> Property.Boolean(true)
                scalar.content.equals("false", ignoreCase = true) -> Property.Boolean(false)
                scalar.content.toIntOrNull() != null -> Property.Int(scalar.toInt())
                scalar.content.toLongOrNull() != null -> Property.Long(scalar.toLong())
                scalar.content.toFloatOrNull() != null -> Property.Float(scalar.toFloat())
                scalar.content.toDoubleOrNull() != null -> Property.Double(scalar.toDouble())
                scalar.content.startsWith("#") || scalar.content.startsWith("rgb(") || scalar.content.startsWith("rgba(") -> Property.Vec4i(
                    parseColor(scalar.content)
                )

                else -> Property.String(scalar.content)
            }
        } catch (e: Exception) {
            logger.warn { "Failed to parse scalar value '${scalar.content}' at ${path.toHumanReadableString()}: ${e.message}. Falling back to string." }
            Property.String(scalar.content)
        }
    }

    private fun parseYamlMapToProperty(map: YamlMap, path: YamlPath): Property.Object {
        val obj = Property.Object()
        for ((key, value) in map.entries) {
            val newPath = path.withMapElementKey(key.content, key.location)
            obj[key.content] = parseYamlNode(value, newPath)
        }
        return obj
    }

    private fun parseYamlListToProperty(list: YamlList, path: YamlPath): PropertyList {
        val propertyList = Property.List()
        list.items.forEachIndexed { index, item ->
            val newPath = path.withListEntry(index, item.location)
            propertyList.add(parseYamlNode(item, newPath))
        }
        return propertyList
    }

    private fun parseColor(colorString: String): Vector4i {
        return when {
            colorString.startsWith("#") -> parseHexColor(colorString)
            colorString.startsWith("rgb(") -> parseRgbColor(colorString)
            colorString.startsWith("rgba(") -> parseRgbaColor(colorString)
            else -> throw IllegalArgumentException("Invalid color format: $colorString")
        }
    }

    private fun parseHexColor(hex: String): Vector4i {
        val color = hex.removePrefix("#")
        return when (color.length) {
            6 -> Vector4i(
                color.substring(0, 2).toInt(16), color.substring(2, 4).toInt(16), color.substring(4, 6).toInt(16), 255
            )

            8 -> Vector4i(
                color.substring(2, 4).toInt(16),
                color.substring(4, 6).toInt(16),
                color.substring(6, 8).toInt(16),
                color.substring(0, 2).toInt(16)
            )

            else -> throw IllegalArgumentException("Invalid hex color format: $hex")
        }
    }

    private fun propertyMapToYamlString(propertyMap: PropertyMap, indent: String = ""): String {
        val sb = StringBuilder()
        for ((key, value) in propertyMap.get()) {
            sb.append("$indent$key: ")
            sb.append(propertyToYamlString(value, "$indent  "))
            sb.append("\n")
        }
        return sb.toString()
    }

    private fun propertyToYamlString(property: Property<*>, indent: String): String {
        return when (property) {
            is Property.Boolean, is Property.Int, is Property.Long, is Property.Float, is Property.Double ->
                property.get().toString()

            is Property.String -> "'${property.get()}'"
            is Property.Vec4i -> "#${
                String.format(
                    "%02X%02X%02X%02X",
                    property.get().x,
                    property.get().y,
                    property.get().z,
                    property.get().w
                )
            }"

            is PropertyMap -> "\n" + propertyMapToYamlString(property, "$indent  ")
            is PropertyList -> listToYamlString(property, indent)
            is Property.Null -> "null"
            else -> property.toString()
        }
    }

    private fun listToYamlString(list: PropertyList, indent: String): String {
        val sb = StringBuilder("\n")
        for (item in list.get()) {
            sb.append("$indent- ${propertyToYamlString(item, "$indent  ")}\n")
        }
        return sb.toString().trimEnd()
    }


    private fun parseRgbColor(rgb: String): Vector4i {
        val values = rgb.removePrefix("rgb(").removeSuffix(")").split(",").map { it.trim().toFloat() }
        if (values.size != 3) throw IllegalArgumentException("Invalid RGB color format: $rgb")

        val (r, g, b) = values.map { (it * if (it <= 1) 255 else 1).toInt() }
        return Vector4i(r, g, b, 255)
    }

    private fun parseRgbaColor(rgba: String): Vector4i {
        val values = rgba.removePrefix("rgba(").removeSuffix(")").split(",").map { it.trim().toFloat() }
        if (values.size != 4) throw IllegalArgumentException("Invalid RGBA color format: $rgba")

        val (r, g, b, a) = values.map { (it * if (it <= 1) 255 else 1).toInt() }
        return Vector4i(r, g, b, a)
    }


    operator fun get(path: String): Property<*>? {
        val segments = path.split('.')
        var current: Property<*> = root
        for (segment in segments) {
            when (current) {
                is Property.Object -> current = current[segment] ?: return null
                is PropertyList -> {
                    val index = segment.toIntOrNull() ?: return null
                    if (index < 0 || index >= current.get().size) return null
                    current = current[index]
                }

                else -> return null
            }
        }
        return current
    }

    operator fun set(path: String, value: Property<*>) {
        val segments = path.split('.')
        var current: Property<*> = root
        for (i in 0 until segments.size - 1) {
            val segment = segments[i]
            when (current) {
                is PropertyMap -> {
                    if (!current.get().containsKey(segment)) {
                        current[segment] = Property.Object()
                    }
                    current = current[segment]!!
                }

                is PropertyList -> {
                    val index = segment.toIntOrNull() ?: throw IllegalArgumentException("Invalid list index: $segment")
                    if (index < 0 || index >= current.get().size) {
                        throw IndexOutOfBoundsException("List index out of bounds: $index")
                    }
                    current = current[index]
                }

                else -> throw IllegalArgumentException("Cannot set property at path: $path")
            }
        }
        when (current) {
            is PropertyMap -> current[segments.last()] = value
            is PropertyList -> {
                val size = current.get().size
                val index = segments.last().toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid list index: ${segments.last()}")
                if (index < 0 || index > size) {
                    throw IndexOutOfBoundsException("List index out of bounds: $index")
                }
                if (index == size) {
                    current.add(value)
                } else {
                    current[index] = value
                }
            }

            else -> throw IllegalArgumentException("Cannot set property at path: $path")
        }
    }

}