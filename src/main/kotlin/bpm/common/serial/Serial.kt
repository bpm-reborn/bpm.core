package bpm.common.serial

import bpm.common.logging.KotlinLogging
import bpm.common.memory.*
import bpm.common.property.PropertySerializer
import bpm.common.utils.simpleClassName
import bpm.common.utils.toFormattedString
import bpm.common.workspace.Workspace
import bpm.common.workspace.WorkspaceSettings
import bpm.common.workspace.graph.Graph
import bpm.common.workspace.graph.Node
import bpm.common.workspace.graph.User
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass

/**
 * A registry class for managing serializers.
 */
object Serial {

    private val logger = KotlinLogging.logger { }

    private val serializers: MutableMap<Class<*>, Serialize<*>> = mutableMapOf()

    private val cachedSuperSerializers: MutableMap<Class<*>, Serialize<*>> = mutableMapOf()

    /**
     * Registers a serializer with the specified type.
     *
     * @param serializer the serializer to register
     */
    fun register(type: Class<*>, serializer: Serialize<*>): Serial {
        serializers[type] = serializer
        return this
    }


    /**
     * Registers a serializer for the given type [T].
     *
     * @param serializer The serializer to register for the type [T].
     * @throws IllegalArgumentException if the type [T] is not serializable.
     */
    inline fun <reified T : Any> register(serializer: Serialize<T>): Serial =
        register(T::class.java, serializer)

    /**
     * Retrieves the serializer associated with the specified type.
     *
     * @param type The type of object for which the serializer is requested.
     * @return The serializer associated with the specified type.
     * @throws NoSuchElementException if no serializer is found for the given type.
     */
    operator fun <T : Any> get(type: Class<T>): Serialize<T> {
        var serializer = (serializers[type] as? Serialize<T>) ?: cachedSuperSerializers[type] as? Serialize<T>
        if (serializer == null) {
            for ((key, value) in serializers) {
                if (type.isAssignableFrom(key)) {
                    cachedSuperSerializers[type] = value
                    serializer = value as? Serialize<T>
                    break
                }
            }
        }
        if (serializer == null) throw NoSuchElementException("No serializer found for type $type")
        return serializer
    }

    /**
     * Serializes the given value of type [T] into the provided [buffer].
     *
     * @param buffer The buffer to write serialized data into.
     * @param value The value to be serialized.
     */
    inline fun <reified T : Any> write(buffer: Buffer, value: T) =
        get(T::class.java).serialize(buffer, value)

    inline fun <reified T : Any> write(path: Path, value: T) {
        val buffer = Buffer.allocate()
        write(buffer, value)
        val bytes = buffer.finish()
        val folder = path.parent
        if (!Files.exists(folder)) Files.createDirectories(folder)
        Files.write(path, bytes)
    }

    /**
     * Read method to deserialize an object of type T from a file at the specified path.
     *
     * @param type The class of the object to be deserialized
     * @param path The file path of the file to be read
     * @return The deserialized object of type T, or null if reading was unsuccessful
     */
    fun <T : Any> read(type: KClass<T>, path: Path): T? {
        val bytes = Files.readAllBytes(path)
        val buffer = Buffer.wrap(bytes)
        return get(type.java).deserialize(buffer)
    }

    /**
     * Reads the content of a file at the specified path and deserializes it into an object of type [T].
     *
     * @param path The path to the file to read.
     * @return The deserialized object of type [T], or `null` if the file could not be read or deserialized.
     */
    inline fun <reified T : Any> read(path: Path): T? = read(T::class, path)

    /**
     * Reads a resource file from classpath and deserializes it into the specified type.
     *
     * @param type The type to deserialize the resource file into.
     * @param path The path of the resource file.
     * @return The deserialized object of the specified type, or null if the resource file was not found or failed to read.
     */
    fun <T : Any> read(type: KClass<T>, path: String): T? {
        //Attempts to read from classpath resources
        val bytes = Serial::class.java.getResourceAsStream(path)?.readBytes()
        if (bytes == null) {
            logger.warn { "Failed to read resource $path" }
            return null
        }
        val buffer = Buffer.wrap(bytes)
        return get(type.java).deserialize(buffer)
    }

    /**
     * Reads an object of type [T] from the specified [path].
     *
     * @param path The path from which to read the object.
     * @return The object of type [T] read from the path, or `null` if the object could not be read.
     *
     * @throws IOException If an I/O error occurs while reading the object.
     */
    inline fun <reified T : Any> read(path: String): T? = read(T::class, path)


    /**
     * Deserializes the given buffer into an object of type [T].
     *
     * @param buffer The buffer containing the serialized data.
     * @return The deserialized object of type [T], or null if deserialization fails.
     */
    inline fun <reified T : Any> read(buffer: Buffer): T? =
        get(T::class.java).deserialize(buffer)


    /**
     * Retrieves a serializer for the specified type [T].
     *
     * @return The serializer for the specified type [T].
     *
     * @param T The type for which the serializer is to be retrieved.
     *
     * @throws NoSuchElementException If a serializer for the specified type [T] is not found.
     */
    inline fun <reified T : Any> get(): Serialize<T> = get(T::class.java)


    /**
     * Registers all serializers.
     *
     * This method registers the serializers for Node and Property classes.
     * It is recommended to call this method before using any serialization operation on these classes.
     */
    fun registerSerializers(): Serial {
        logger.info { "Registering all serializers" }
//        val serializers = Reflection.findAndCreateInstanceOfType<Serialize<*>>()
        //Manually list out all serializers
        PropertySerializer.register()
        Workspace.Serializer.register()
        Graph.Serializer.register()
        Node.NodeSerializer.register()
        User.Serializer.register()
        WorkspaceSettings.Serializer.register()
        //Register all serializers
//        for (serializer in serializers) serializer.register()
        val serializerNames = serializers.map { it.simpleClassName }.toList().toFormattedString()
        logger.info { "Registered ${serializers.size} serializers:\n\t$serializerNames" }
        return this
    }


}