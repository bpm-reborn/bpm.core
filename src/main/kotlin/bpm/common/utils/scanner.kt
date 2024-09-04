package bpm.common.utils

import bpm.common.logging.KotlinLogging
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URL
import java.nio.file.*
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarFile
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.*
import kotlin.reflect.typeOf

class ClassResourceScanner private constructor(private val builder: Builder) {

    private val logger = KotlinLogging.logger {}
    private val resourceIndex = ConcurrentHashMap<String, ResourceInfo>()
    private val classIndex = ConcurrentHashMap<String, ClassInfo>()

    interface Info

    data class ResourceInfo(val path: String, val size: Long) : Info

    data class ClassInfo(val qualifiedClassName: String, val packageName: String) : Info

    private val isWindows = System.getProperty("os.name").startsWith("Windows")

    data class ScanResults(
        val classLoader: ClassLoader,
        val resources: List<ResourceInfo>,
        val classes: List<ClassInfo>,
        val timeTaken: Long
    ) {

        private val resourceExtensionCache = ConcurrentHashMap<String, List<ResourceInfo>>()
        private val classPackageCache = ConcurrentHashMap<String, List<ClassInfo>>()
        private val resourceNameCache = ConcurrentHashMap<String, List<ResourceInfo>>()
        private val classNameCache = ConcurrentHashMap<String, List<ClassInfo>>()

        fun getResourcesWithExtension(extension: String): List<ResourceInfo> =
            resourceExtensionCache.getOrPut(extension) {
                resources.filter { it.path.endsWith(extension) }
            }


        fun getClassesInPackage(packageName: String): List<ClassInfo> = classPackageCache.getOrPut(packageName) {
            classes.filter { it.packageName == packageName || it.packageName.startsWith("$packageName.") }
        }

        fun findResourcesByName(name: String): List<ResourceInfo> = resourceNameCache.getOrPut(name) {
            resources.filter { it.path.contains(name) }
        }

        fun findClassesByName(name: String): List<ClassInfo> = classNameCache.getOrPut(name) {
            classes.filter { it.qualifiedClassName.contains(name) }
        }
    }

    class Builder {

        internal var classLoader: ClassLoader? = Thread.currentThread().contextClassLoader
        internal var scanClasspath: Boolean = true
        internal var scanSelf: Boolean = false
        internal var scanAdditionalPaths: List<String> = emptyList()

        fun withClassloader(classloader: ClassLoader) = apply { this.classLoader = classLoader }
        fun withSystemClassloader() = apply { this.classLoader = ClassLoader.getSystemClassLoader() }
        fun scanClasspath(scan: Boolean = true) = apply { this.scanClasspath = scan }
        fun scanSelf(scan: Boolean = true) = apply { this.scanSelf = scan }
        fun scanAdditionalPaths(vararg extraPaths: String) = apply { this.scanAdditionalPaths = extraPaths.toList() }

        fun scan(): ScanResults {
            val scanner = ClassResourceScanner(this)
            return scanner.performScan()
        }
    }

    private fun performScan(): ScanResults {
        val startTime = System.currentTimeMillis()
        logger.info { "Starting scan" }

        if (builder.scanClasspath) {
            val classPath = System.getProperty("java.class.path")
            classPath.split(File.pathSeparator).forEach { scanPath(it) }
        }

        builder.scanAdditionalPaths.forEach { scanPath(it) }


        if (builder.scanSelf) {
            val resourceUrl = ClassResourceScanner::class.java.protectionDomain.codeSource.location
            val path = normalizeUrl(resourceUrl)
            logger.info { "Scanning self at: $path" }
            scanPath(path)
        }

        builder.classLoader?.let { scanResources(it, "") }



        logger.info { "Scan completed, got ${resourceIndex.size} resources and ${classIndex.size} classes" }
        val endTime = System.currentTimeMillis()
        val timeTaken = endTime - startTime
        return ScanResults(builder.classLoader ?: ClassLoader.getSystemClassLoader(),
            resourceIndex.values.toList(),
            classIndex.values.map {
                ClassInfo(
                    it.qualifiedClassName.removePrefix("kotlin.main.").removePrefix("java.main."), it.packageName
                )
            }.toList().filter { !it.qualifiedClassName.endsWith("package-info") },
            timeTaken
        )
    }


    private fun normalizeUrl(urlIn: URL): String {
        val prefixEnd = urlIn.path.indexOf(":/")
//        var url = if (prefixEnd != -1) urlIn.path.substring(prefixEnd) else urlIn.path
        var url = if (isWindows) {
            // On Windows, the URL path starts with a leading slash, which is not present on other platforms
            java.net.URLDecoder.decode(urlIn.path, "UTF-8").removePrefix("/")
        } else java.net.URLDecoder.decode(urlIn.path, "UTF-8")
        return normalizeUnionPath(url).removeSuffix("/").removeSuffix("!").removeSuffix("/")
    }


    private fun locateClassesDirectory(resourcesDirectory: Path): Path {
        //We can find the classes path by resolving the build folder, then drilling down to the classes directory
        val buildDirectory = resourcesDirectory.parent?.parent //We need to go up two levels to get to the build directory
        val classesDirectory = buildDirectory?.resolve("classes") ?: resourcesDirectory.resolve("classes")
        return classesDirectory
    }

    private fun scanPath(path: String) {
        logger.info { "Scanning path: $path" }
        val isJar = path.endsWith(".jar") || path.endsWith(".zip")
        if (isJar) {
            scanJar(path)
        } else {
            // We know it's a development environment, so we can scan the directory and then locate the classes directory to scan as well.
            val resourcesDirectory = Paths.get(path)
            if (Files.exists(resourcesDirectory)) {
                logger.info { "Scanning resources directory: $resourcesDirectory" }
                scanDirectory(resourcesDirectory)
                //Attempt to scan the classes directory
                val classesDirectory = locateClassesDirectory(resourcesDirectory)
                if (Files.exists(classesDirectory)) {
                    logger.info { "Scanning classes directory: $classesDirectory" }
                    scanDirectory(classesDirectory)
                }
            }

        }
    }

    private fun scanJar(jarPath: String) {
        logger.info { "Scanning JAR: $jarPath" }
        try {
            JarFile(jarPath).use { jar ->
                jar.entries().asSequence().forEach { entry ->
                    when {
                        entry.name.endsWith(".class") -> indexClass(entry.name)
                        !entry.isDirectory -> indexResource(entry.name, entry.size)
                    }
                }
            }
        } catch (e: IOException) {
            logger.error(e) { "Error scanning JAR: $jarPath" }
        }
    }


    // We know that we're in a development environment, so we can use the file system to scan the directory.
    // We are given a path, with neoforge, that points to our resources folder. We can use this to scan the directory.
    // We also can use that to scan the classes directory by offsetting the path.
    private fun scanDirectory(resourcesDirectory: Path) {


        logger.info { "Scanning directory: $resourcesDirectory" }
        if (!Files.exists(resourcesDirectory)) {
            logger.info { "Directory does not exist, skipping: $resourcesDirectory" }
            return
        }
        try {
            Files.walk(resourcesDirectory).forEach { path ->
                val relativePath = resourcesDirectory.relativize(path).toString().replace('\\', '/')
                when {
                    Files.isRegularFile(path) && path.toString().endsWith(".class") -> indexClass(relativePath)
                    Files.isRegularFile(path) -> indexResource(relativePath, Files.size(path))
                }
            }
        } catch (e: IOException) {
            logger.error(e) { "Error scanning directory: $resourcesDirectory" }
        }
    }

    private fun scanResources(classLoader: ClassLoader, directoryPath: String) {
        val resources = findResourceFiles(classLoader, directoryPath)
        resources.forEach { resourcePath ->
            indexResource(resourcePath, -1)
        }
    }

    private fun indexClass(classPath: String) {
        val className = classPath.removeSuffix(".class").replace('/', '.')
        val packageName = className.substringBeforeLast('.', "")
        classIndex[className] = ClassInfo(className, packageName.replace("kotlin.main.", "").replace("java.main.", ""))
    }

    private fun indexResource(resourcePath: String, size: Long) {
        if (resourcePath.endsWith(".class")) {
            indexClass(resourcePath)
            return
        }
        resourceIndex[resourcePath] = ResourceInfo(resourcePath, size)
    }


    // Integrated methods from the provided code
    private fun findResourceFiles(classLoader: ClassLoader, directoryPath: String): List<String> {
        logger.info { "Starting findResourceFiles with directoryPath: $directoryPath" }
        val resource = classLoader.getResource(directoryPath)
        if (resource == null) {
            logger.warn { "Resource not found for directoryPath: $directoryPath" }
            return emptyList()
        }
        logger.info { "Loading resources from $directoryPath. ${resource.file} with protocol ${resource.protocol}" }
        return when (resource.protocol) {
            "file" -> findFilesInFileSystem(resource.toURI())
            "jar" -> findFilesInJar(resource.toString())
            "union" -> findFilesInUnion(resource.toString())
            else -> {
                logger.warn { "Unknown protocol: ${resource.protocol}" }
                emptyList()
            }
        }
    }

    private fun findFilesInSpecificJar(jarPath: String, internalPath: String): List<String> {
        logger.info { "Finding files in specific JAR: $jarPath, internal path: $internalPath" }
        return try {
            val jarUri = URI("jar:file", null, "/${jarPath.replace('\\', '/')}", null)
            FileSystems.newFileSystem(jarUri, mapOf("create" to "false")).use { fs ->
                val root = fs.getPath(internalPath)
                Files.walk(root).filter { Files.isRegularFile(it) }.map { it.toString().removePrefix("/") }.toList()
            }
        } catch (e: Exception) {
            logger.error(e) { "Error finding files in specific JAR: $jarPath" }
            emptyList()
        }
    }

    private fun scanUnionPath(normalized: String): List<String> {
        //If it's a jar, we need to find the jar and the internal path. We do this by finding .jar, and if it's suffiexed by our own path, we know it's the jar.
        val jarIndex = normalized.lastIndexOf(".jar")
        //We now need to make sure it's not the last part of the path, as it could be a directory.
        if (jarIndex != -1 && jarIndex != normalized.length - 4) {
            val jarPath = normalized.substring(0, jarIndex + 4)
            val internalPath = normalized.substring(jarIndex + 5)
            return findFilesInSpecificJar(jarPath, internalPath)
        }
        return emptyList()
    }

    private fun normalizeUnionPath(resourcePath: String): String {
        val parts = resourcePath.split("/")
        return parts.map { part ->
            val hashIndex = part.indexOf('#')
            val bangIndex = part.indexOf('!')
            if (hashIndex != -1 && bangIndex != -1 && hashIndex < bangIndex) {
                part.substring(0, hashIndex) + part.substring(bangIndex)
            } else {
                part
            }
        }.filter { it != "!" }.joinToString("/")
    }


    private fun findFilesInUnion(resourcePath: String): List<String> {
        // Remove the "union:" prefix and decode URL-encoded characters
        val decodedPath = java.net.URLDecoder.decode(resourcePath.removePrefix("union:/"), "UTF-8")
        //The normalizied path.
        val normalizedPath = normalizeUnionPath(decodedPath)
        logger.info { "Normalized path: $normalizedPath" }
        //Attempt to load from jar
        val unionFiles = scanUnionPath(normalizedPath)
        if (unionFiles.isNotEmpty()) return unionFiles
        //Otherwise, load from file system
        return try {
            Files.walk(Paths.get(normalizedPath)).filter { Files.isRegularFile(it) }.map { it.toString() }.toList()
        } catch (e: IOException) {
            emptyList()
        }
    }


    private fun findFilesInFileSystem(uri: URI): List<String> {
        logger.info { "Finding files in file system: $uri" }
        return try {
            Files.walk(Paths.get(uri)).filter { Files.isRegularFile(it) }.map { it.toString() }.toList()
        } catch (e: IOException) {
            logger.error(e) { "Error finding files in file system" }
            emptyList()
        }
    }

    private fun findFilesInJar(resourcePath: String): List<String> {
        logger.info { "Finding files in JAR: $resourcePath" }
        val jarPath = resourcePath.substringBefore("!")
        val directoryPath = resourcePath.substringAfter("!/")
        return try {
            JarFile(jarPath.removePrefix("jar:file:")).use { jarFile ->
                jarFile.entries().asSequence().filter { it.name.startsWith(directoryPath) && !it.isDirectory }
                    .map { it.name }.toList()
            }
        } catch (e: IOException) {
            logger.error(e) { "Error finding files in JAR" }
            emptyList()
        }
    }


    companion object {

        fun create() = Builder()
    }
}
// Extension properties for easy access
val ClassResourceScanner.ScanResults.fromClasses get() = QueryBuilder(this, this.classes)

inline infix fun <reified Return : Any> ClassResourceScanner.ScanResults.fromClasses(query: QueryBuilder<ClassResourceScanner.ClassInfo>.() -> Return): Return =
    QueryBuilder(this, this.classes).query()


inline fun <reified Return : Any?> ClassResourceScanner.ScanResults.fromClassesInPackage(
    packageName: String,
    query: QueryBuilder<ClassResourceScanner.ClassInfo>.() -> Return
): Return =
    QueryBuilder(this, this.getClassesInPackage(packageName)).query()


val ClassResourceScanner.ScanResults.fromResources get() = QueryBuilder(this, this.resources)

inline infix fun ClassResourceScanner.ScanResults.fromResources(query: QueryBuilder<ClassResourceScanner.ResourceInfo>.() -> Unit) =
    QueryBuilder(this, this.resources).apply(query)

// Extension functions for SQL-like queries
inline infix fun <reified T : ClassResourceScanner.Info> ClassResourceScanner.ScanResults.from(crossinline selector: (ClassResourceScanner.ScanResults) -> List<T>): QueryBuilder<*> =
    QueryBuilder(this, selector(this))

class QueryBuilder<T : ClassResourceScanner.Info>(
    val results: ClassResourceScanner.ScanResults, val items: List<T>
) {

    fun where(predicate: (T) -> Boolean) = QueryBuilder(results, items.filter(predicate))
    fun <R> select(transformer: (T) -> R) = items.map(transformer)
    fun <R> selectDistinct(transformer: (T) -> R) = items.map(transformer).distinct()
    fun count() = items.size
    fun first() = items.firstOrNull()
    fun take(n: Int) = QueryBuilder(results, items.take(n))
    fun drop(n: Int) = QueryBuilder(results, items.drop(n))
    fun <R : Comparable<R>> orderBy(selector: (T) -> R?) = QueryBuilder(results, items.sortedBy(selector))
    fun <R : Comparable<R>> orderByDescending(selector: (T) -> R?) =
        QueryBuilder(results, items.sortedByDescending(selector))
}


// Extension functions for specific queries
inline fun <reified T : Any> ClassResourceScanner.ScanResults.classesImplementing(): List<KClass<out T>> =
    fromClasses {
        where { classInfo ->
            try {
                val kClass = classInfo.qualifiedClassName.toKClass(classLoader)
                kClass.isSubclassOf(T::class) && !kClass.isAbstract
            } catch (e: Exception) {
                false
            }
        }.select { it.qualifiedClassName.toKClass(classLoader) as KClass<out T> }
    }

inline fun <reified T : Any> ClassResourceScanner.ScanResults.classesImplementingInPackage(packageName: String): List<KClass<out T>> =
    fromClassesInPackage(packageName) {
        where { classInfo ->
            try {
                val kClass = classInfo.qualifiedClassName.toKClass(classLoader)
                kClass.isSubclassOf(T::class) && !kClass.isAbstract
            } catch (e: Exception) {
                false
            }
        }.select { it.qualifiedClassName.toKClass(classLoader) as KClass<out T> }
    }


inline fun <reified T : Annotation> ClassResourceScanner.ScanResults.classesAnnotatedWith(): List<KClass<*>> =
    fromClasses {
        where { classInfo ->
            try {
                val kClass = classInfo.qualifiedClassName.toKClass(classLoader)
                kClass.hasAnnotation<T>()
            } catch (e: Exception) {
                false
            }
        }.select { it.qualifiedClassName.toKClass(classLoader) }
    }

inline fun <reified T : Any> ClassResourceScanner.ScanResults.propertiesOfType(): List<KProperty<T>> =
    fromClasses {
        select { classInfo ->
            try {
                val kClass = classInfo.qualifiedClassName.toKClass(classLoader)
                kClass.memberProperties.filter { prop -> prop.returnType.isSubtypeOf(typeOf<T>()) }
            } catch (e: Exception) {
                emptyList()
            }
        }.flatten().filterIsInstance<KProperty<T>>()
    }

fun ClassResourceScanner.ScanResults.functionsWithAnnotation(annotationKClass: KClass<out Annotation>) =
    fromClasses {
        select { classInfo ->
            try {
                val kClass = classInfo.qualifiedClassName.toKClass(classLoader)
                kClass.functions.filter { func -> func.annotations.any { anno -> annotationKClass.isInstance(anno) } }
            } catch (e: Exception) {
                emptyList()
            }
        }.flatten()
    }


inline fun <reified T : Any> ClassResourceScanner.ScanResults.objectInstances(): List<T> =
    fromClasses {
        where { classInfo ->
            try {
                val kClass = classInfo.qualifiedClassName.toKClass(classLoader)
                kClass.objectInstance is T
            } catch (e: Exception) {
                false
            }
        }.select { it.qualifiedClassName.toKClass(classLoader).objectInstance as T }
    }


fun ClassResourceScanner.ScanResults.resourcesWithContent(predicate: (String) -> Boolean): List<Pair<ClassResourceScanner.ResourceInfo, String>> =
    from { it.resources }.where { resourceInfo ->
        if (resourceInfo is ClassResourceScanner.ResourceInfo) {
            val content = classLoader.getResourceAsStream(
                resourceInfo
                    .path
            )?.bufferedReader()?.use { it.readText() }
            content?.let(predicate) ?: false
        } else false

    }.select { resourceInfo ->
        if (resourceInfo is ClassResourceScanner.ResourceInfo) {
            val content = classLoader.getResourceAsStream(resourceInfo.path)?.bufferedReader()
                ?.use { it.readText() }
            Pair(resourceInfo, content!!)
        } else null
    }.filterIsInstance<Pair<ClassResourceScanner.ResourceInfo, String>>()


fun ClassResourceScanner.ScanResults.findResourcesByPattern(pattern: String): QueryBuilder<ClassResourceScanner.ResourceInfo> =
    from { it.resources }.where { resourceInfo ->
        if (resourceInfo is ClassResourceScanner.ResourceInfo) {
            resourceInfo.path.matches(Regex(pattern))
        } else false
    }.cast()

inline fun <reified T : ClassResourceScanner.Info> QueryBuilder<*>.cast() = this as QueryBuilder<T>

private val cachedClasses = ConcurrentHashMap<String, KClass<*>>()

// Helper extension function to convert String to KClass
fun String.toKClass(classLoader: ClassLoader): KClass<*> = cachedClasses.getOrPut(this) {
    classLoader.loadClass(this).kotlin
}

private val logger = KotlinLogging.logger {}


fun ClassResourceScanner.ScanResults.fromPackages(vararg packageNames: String): ClassResourceScanner.ScanResults {
    return ClassResourceScanner.ScanResults(
        classLoader,
        resources,
        classes.filter { classInfo ->
            packageNames.any { classInfo.packageName.startsWith(it) }
        },
        timeTaken
    )
}

fun ClassResourceScanner.ScanResults.readResourcesToByteArrayMap(): Map<String, ByteArray> {
    return resources.associate { resourceInfo ->
        try {
            val resourceName = resourceInfo.path.substringAfterLast('/')
            val resourceContent = classLoader.getResourceAsStream(resourceInfo.path)?.use { it.readBytes() }

            if (resourceContent != null) {
                resourceName to resourceContent
            } else {
                logger.warn { "Failed to read resource: ${resourceInfo.path}" }
                null
            }
        } catch (e: IOException) {
            logger.error(e) { "Error reading resource: ${resourceInfo.path}" }
            null
        } ?: return emptyMap()
    }
}

fun ClassResourceScanner.ScanResults.readResourceToByteArray(resourceName: String): ByteArray? {
    val resourceInfo = resources.find { it.path.endsWith(resourceName) }
    return resourceInfo?.let {
        try {
            classLoader.getResourceAsStream(it.path)?.use { it.readBytes() }
        } catch (e: IOException) {
            logger.error(e) { "Error reading resource: ${it.path}" }
            null
        }
    }
}


fun ClassResourceScanner.ScanResults.withExtension(extension: String): ClassResourceScanner.ScanResults {
    return ClassResourceScanner.ScanResults(
        classLoader,
        resources.filter { it.path.endsWith(extension) },
        classes,
        timeTaken
    )
}


fun QueryBuilder<ClassResourceScanner.ResourceInfo>.readResourcesToByteArrayMap(): Map<String, ByteArray> {
    return items.associate { resourceInfo ->
        try {
            val resourceName = resourceInfo.path.substringAfterLast('/')
            val resourceContent = results.classLoader.getResourceAsStream(resourceInfo.path)?.use { it.readBytes() }

            if (resourceContent != null) {
                resourceName to resourceContent
            } else {
                logger.warn { "Failed to read resource: ${resourceInfo.path}" }
                null
            }
        } catch (e: IOException) {
            logger.error(e) { "Error reading resource: ${resourceInfo.path}" }
            null
        } ?: return emptyMap()
    }
}


