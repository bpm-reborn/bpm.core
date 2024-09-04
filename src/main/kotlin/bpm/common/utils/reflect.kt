package bpm.common.utils

import io.github.classgraph.ClassGraph
import io.github.classgraph.ClassInfoList
import io.github.classgraph.ScanResult
import bpm.common.logging.KotlinLogging
import java.lang.RuntimeException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.math.min
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance


/**
 * This class provides reflection-related utility methods.
 *
 * @property logger The logger used for logging messages.
 */
object Reflection {

    private val logger = KotlinLogging.logger { }

    /**
     * Filters and retrieves a list of Kotlin classes that meet the provided filter criteria.
     *
     * @param filter A lambda function that takes a [ScanResult] parameter and returns a [ClassInfoList].
     *     This filter will be applied to the scan result to determine which classes to include in the result.
     *
     * @return A list of [KClass] objects representing the filtered Kotlin classes.
     *     The returned list contains only non-abstract classes that meet the filter criteria.
     *
     * Example Usage:
     * ```
     * val classes = findClassesFiltered { scanResult ->
     *    scanResult.getClassesImplementing(type.qualifiedName)
     * }
     *
     *    */
    fun findClassesFiltered(filter: (ScanResult) -> ClassInfoList): List<KClass<*>> {
        val timeStart = System.currentTimeMillis()
        try {

            ClassGraph().enableClassInfo().scan().use {
                val results = filter(it)
                val classes = results.filter { !it.isAbstract }.loadClasses().map { it.kotlin }
                val timeTaken = (System.currentTimeMillis() - timeStart) / 1000.0
                logger.info { "Found ${classes.size} classes that met the filter criteria in $timeTaken seconds" }
                return classes
            }
        } catch (e: Exception) {
            logger.error { "Error occurred while scanning classes: ${e.message}" }

        }
        return emptyList()
    }
    /**
     * Returns a list of subclasses of the given type.
     *
     * @param type The type for which to retrieve the subclasses.
     * @return A list of subclasses of the given type.
     *
     * @throws ClassNotFoundException If the type is not found.
     *
     * @suppress("UNCHECKED_CAST")
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> findSubclasses(type: KClass<out T>): List<KClass<out T>> =
        findClassesFiltered { it.getSubclasses(type.qualifiedName) }.map { it as KClass<T> }


    /**
     * Returns a list of subclasses of the given type.
     *
     * @param type The type for which to retrieve the subclasses.
     * @return A list of subclasses of the given type.
     *
     * @throws ClassNotFoundException If the type is not found.
     *
     * @suppress("UNCHECKED_CAST")
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> findImplementations(type: KClass<out T>): List<KClass<out T>> =
        findClassesFiltered { it.getClassesImplementing(type.qualifiedName) }.map { it as KClass<T> }


    /**
     * Retrieves the list of classes of the same type as the given type.
     *
     * @return a list of [KClass] objects representing the types of the same type as [T]
     * @reified T the type to retrieve the types of
     *
     * @see [findImplementations]
     */
    inline fun <reified T : Any> findImplementations(): List<KClass<out T>> = findImplementations(T::class)

    /**
     * Returns a list of instances of the specified type.
     *
     * @param type the type of instances to retrieve
     * @return a list of instances of the specified type
     */
    fun <T : Any> findAndCreateInstanceOfType(type: KClass<T>): List<T> =
        if (type.java.isInterface) findImplementations(type).map { it.instantiate }
        else if (type.isAbstract) findSubclasses(type).map { it.instantiate }
        else throw IllegalArgumentException("Cannot find instances of type ${type.simpleName}")

    /**
     * Returns a list of instances that are of type [T].
     *
     * @return a list of instances of type [T]
     */
    inline fun <reified T : Any> findAndCreateInstanceOfType(): List<T> = findAndCreateInstanceOfType(T::class)
}


/**
 * Interface for a class that can be used to instantiate objects of type [T].
 */
fun interface InstanceProvider<T : Any> {

    /**
     * Retrieve the value of type T.
     *
     * @return The value of type T.
     */
    fun provide(): T

    /**
     * Executes the `invoke` method.
     *
     * @return The result of calling the `provide` function.
     */
    operator fun invoke(): T = provide()
}

private val cachedInstanceProviders: ConcurrentMap<KClass<*>, InstanceProvider<*>> = ConcurrentHashMap()

/**
 * Retrieves the constructor for the given Kotlin class.
 *
 * @return the constructor for the given Kotlin class.
 *
 * @throws [RuntimeException] if the constructor is not found and the type is not an object.
 */
val <T : Any> KClass<T>.instantiate: T
    get() {
        if (cachedInstanceProviders.containsKey(this)) return cachedInstanceProviders[this]!!.provide() as T
        @Suppress("UNCHECKED_CAST") val provider: InstanceProvider<T> = cachedInstanceProviders.getOrPut(this) {
            InstanceProvider {
                if (this.isAbstract) throw RuntimeException("Cannot instantiate abstract class ${this.simpleName}")
                if (this.objectInstance != null) this.objectInstance!!
                else this.createInstance()
            }
        } as InstanceProvider<T>
        return provider.provide()
    }

/**
 * Instantiates an object of the given [KClass] and applies the provided lambda to configure its properties.
 *
 * @param apply The lambda function to apply on the instantiated object
 * @return The instantiated and configured object
 *
 * @param T The type of the object to be instantiated
 * @receiver The [KClass] representing the class of the object to be instantiated
 */
inline fun <T : Any> KClass<T>.instantiate(apply: T.() -> Unit): T = instantiate.apply(apply)


/**
 * Instantiates an object of the specified class with the provided arguments.
 *
 * @param args the arguments to be passed to the constructor
 * @return the instantiated object of type T
 * @throws IllegalArgumentException if no constructor is found with the same number of parameters as the args array,
 * or if the argument types do not match the constructor parameters
 */
fun <T : Any> KClass<T>.instantiateWith(vararg args: Any): T {
    if (args.isEmpty()) return instantiate
    if (cachedInstanceProviders.containsKey(this)) return cachedInstanceProviders[this]!!.provide() as T
    //Locates the correct constructor based on the provided arguments
    val constructor = this.constructors.firstOrNull { it.parameters.size == args.size }
        ?: throw IllegalArgumentException("No constructor found with ${args.size} parameters")
    //Checks the types against the constructor parameters

    return constructor.call(*args)
}

/**
 * Retrieves the fully qualified name of the class represented by this [KClass] object.
 *
 * @return The fully qualified class name.
 */
val <T : Any> T.className: String get() = this::class.qualifiedName!!

/**
 * Gets the abbreviated name of the given [KClass].
 *
 * The abbreviated name is obtained by splitting the simple name of the [KClass] using a positive look-ahead regex
 * pattern that matches any uppercase letter. Each word is then truncated to a maximum of 4 characters, and the
 * truncated words are joined together to form the abbreviated name.
 *
 * @return the abbreviated name of the [KClass].
 */
val KClass<*>.abbreviatedName: String
    get() = this.simpleName!!.split("(?=\\p{Upper})".toRegex()).map {
        it.take(min(4, it.length))
    }.joinToString("")
/**
 * Returns the simple class name of a given Kotlin class.
 *
 * @return The simple class name of the Kotlin class.
 */
val <T : Any> T.simpleClassName: String
    get() = if (this::class.java.enclosingClass == null) this::class.simpleName!!
    else {
        val enclosing = this::class.java.enclosingClass.kotlin.abbreviatedName
        "${enclosing}.${this::class.simpleName!!}"
    }

val <T : Any> KClass<T>.shortName: String
    get() = if (this.java.enclosingClass != null) "${this.java.enclosingClass.kotlin.abbreviatedName}.${
        this.simpleName!!
    }" else this.simpleName!!