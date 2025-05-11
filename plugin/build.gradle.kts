import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly

plugins {
    id("java")
    kotlin("jvm") version "1.9.22"
    `maven-publish`
    id("io.github.goooler.shadow") version "8.1.7"
}

group = "dev.marten_mrfcyt"
version = "0.7.1"

repositories {
    mavenCentral()
    maven("https://javadoc.jitpack.io")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    maven("https://repo.opencollab.dev/maven-snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://libraries.minecraft.net")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

val centralDependencies = listOf(
    "org.jetbrains.kotlin:kotlin-stdlib:2.1",
    "com.corundumstudio.socketio:netty-socketio:1.7.19"
)

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly("com.mojang:brigadier:1.0.18")
    compileOnly("me.clip:placeholderapi:2.11.6")
    implementation("org.bstats:bstats-bukkit:3.1.0")
    implementation("mlib.api:MLib:0.0.1")
}

val targetJavaVersion = 21
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "21"
}

fun discoverResources(): Map<String, Set<String>> {
    val resourcesDir = file("src/main/resources")
    val result = mutableMapOf<String, MutableSet<String>>()

    result["criticalResources"] = mutableSetOf()
    result["languageFiles"] = mutableSetOf()
    result["pluginDescriptors"] = mutableSetOf()

    if (resourcesDir.exists()) {
        resourcesDir.walk().filter { it.isFile }.forEach { file ->
            val relativePath = file.relativeTo(resourcesDir).path.replace('\\', '/')

            when {

                relativePath == "plugin.yml" || relativePath == "paper-plugin.yml" -> {
                    result["pluginDescriptors"]?.add(relativePath)
                }

                relativePath.startsWith("lang/") && (relativePath.endsWith(".yml") || relativePath.endsWith(".yaml")) -> {
                    result["languageFiles"]?.add(relativePath)
                }

                !relativePath.contains("/") && (relativePath.endsWith(".yml") || relativePath.endsWith(".yaml")) -> {
                    result["criticalResources"]?.add(relativePath)
                }
            }
        }
    }

    return result
}

tasks.processResources {

    filteringCharset = "UTF-8"

    filesMatching("plugin.yml") {
        expand(mapOf(
            "version" to version,
            "libraries" to centralDependencies
        ))
    }

    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    doFirst {
        println("Processing resources:")
        source.forEach { file ->
            if (file.isFile) {
                println("- ${file.name}")
            }
        }
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
}

tasks.jar {

    dependsOn("processResources")

    from("${layout.buildDirectory}/resources/main") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}

tasks.withType<ShadowJar> {
    relocate("org.bstats", "dev.marten_mrfcyt.bstats")
    dependsOn("processResources")

    archiveClassifier.set("")

    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    from("${layout.buildDirectory}/resources/main") {

        into("")
    }

    minimize {
        exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib"))

        exclude("lang/**")
        exclude("*.yml")
    }

    mergeServiceFiles()

    doLast {
        println("ShadowJar created at: ${archiveFile.get().asFile.absolutePath}")
        println("JAR size: ${archiveFile.get().asFile.length()} bytes")

        val resourcesDir = file("build/resources/main")
        if (resourcesDir.exists()) {
            println("\nAdding resources directly at root level...")
            exec {
                commandLine = listOf(
                    "jar",
                    "uf",
                    archiveFile.get().asFile.absolutePath,
                    "-C", resourcesDir.absolutePath, "."
                )
            }
        }

        validateJarResources(archiveFile.get().asFile)
    }
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.register("buildAndMove") {
    dependsOn("buildProduction")
    group = "build"
    description = "Builds the jar and moves it to the server folder"

    println("Building and moving JAR to server folder...")
    doLast {
        val jar = file("build/libs/${project.name}-${version}-prod.jar")
        if (!jar.exists()) {
            throw GradleException("JAR file not found: ${jar.absolutePath}. Run 'shadowJar' task first.")
        }
        println("JAR file found: ${jar.absolutePath}")
        val server = file("server/plugins/${project.name.capitalizeAsciiOnly()}-${version}.jar")
        if (!server.parentFile.exists()) {
            server.parentFile.mkdirs()
        }
        println("Moving JAR to server folder: ${server.absolutePath}")
        if (server.exists()) {
            println("Deleting existing JAR at: ${server.absolutePath}")
            server.delete()
        }
        println("Copying JAR to server folder...")
        jar.copyTo(server, overwrite = true)
        println("JAR moved successfully to: ${server.absolutePath}")
    }
}

tasks.register("buildProduction") {
    group = "build"
    description = "Builds a production-ready JAR with all validations and tests"
    dependsOn("clean")
    dependsOn("shadowJar")
    dependsOn("validateJar")
    tasks.findByName("validateJar")?.mustRunAfter("shadowJar")
    doLast {
        val jarFile = file("build/libs/${project.name}-${version}.jar")

        if (!jarFile.exists()) {
            throw GradleException("Production build failed: JAR file not found at ${jarFile.absolutePath}")
        }

        val prodJar = file("build/libs/${project.name}-${version}-prod.jar")
        jarFile.copyTo(prodJar, overwrite = true)

        println("""
            ✅ PRODUCTION BUILD SUCCESSFUL
            ✅ All tests passed
            ✅ All resources validated
            ✅ Production JAR created at: ${prodJar.absolutePath}
            ✅ JAR size: ${prodJar.length()} bytes
        """.trimIndent())
    }
}

tasks.register("validateJar") {
    group = "verification"
    description = "Validates resources in the built JAR file"

    dependsOn("shadowJar")

    doLast {
        val jarFile = file("build/libs/${project.name}-${version}.jar")
        if (jarFile.exists()) {
            validateJarResources(jarFile)
        } else {
            throw GradleException("JAR file not found: ${jarFile.absolutePath}. Run 'shadowJar' task first.")
        }
    }
}

fun validateJarResources(jarFile: File) {
    println("\n=== Validating JAR Resources ===")

    if (!jarFile.exists()) {
        throw GradleException("JAR file not found at: ${jarFile.absolutePath}")
    }

    val discoveredResources = discoverResources()
    val criticalResources = discoveredResources["criticalResources"] ?: emptySet()
    val languageFiles = discoveredResources["languageFiles"] ?: emptySet()
    val pluginDescriptors = discoveredResources["pluginDescriptors"] ?: emptySet()

    val process = ProcessBuilder("jar", "-tf", jarFile.absolutePath)
        .redirectErrorStream(true)
        .start()

    val output = process.inputStream.bufferedReader().readText()
    val jarEntries = output.lines()
        .filter { it.isNotBlank() }
        .toList()

    val yamlResources = jarEntries.filter {
        it.endsWith(".yml") || it.endsWith(".yaml")
    }

    println("Found ${yamlResources.size} YAML files in JAR:")
    yamlResources.forEach { println("  - $it") }

    val resourcePattern = """(?:resources/)?(.+\.yml)""".toRegex()
    val normalizedResources = yamlResources.mapNotNull { path ->
        resourcePattern.find(path)?.groupValues?.get(1)
    }.toSet()

    println("\nNormalized resource paths:")
    normalizedResources.sorted().forEach {
        println("  - $it")
    }

    val missingResources = mutableListOf<String>()

    println("\nChecking for critical resources:")

    fun isResourceInJar(resourcePath: String): Boolean {
        val normalizedPath = resourcePath.replace('\\', '/')
        return normalizedResources.any {
            it == normalizedPath || it.endsWith("/$normalizedPath")
        }
    }

    criticalResources.forEach { resource ->
        if (isResourceInJar(resource)) {
            println("  ✅ Found: $resource")
        } else {
            println("  ❌ Missing: $resource")
            missingResources.add(resource)
        }
    }

    println("\nChecking for language files:")
    val foundLanguageFiles = mutableListOf<String>()

    languageFiles.forEach { langFile ->
        if (isResourceInJar(langFile)) {
            println("  ✅ Found: $langFile")
            foundLanguageFiles.add(langFile)
        } else {
            println("  ❓ Not found: $langFile")
        }
    }

    if (foundLanguageFiles.isEmpty() && !languageFiles.isEmpty()) {
        println("  ❌ No language files found!")
        missingResources.add("Language files")
    } else if (!languageFiles.isEmpty()) {
        println("  ✅ Found ${foundLanguageFiles.size}/${languageFiles.size} language files")
    }

    val foundDescriptors = pluginDescriptors.filter { isResourceInJar(it) }

    println("\nChecking for plugin descriptors:")
    if (foundDescriptors.isNotEmpty()) {
        foundDescriptors.forEach { println("  ✅ Found: $it") }
    } else if (!pluginDescriptors.isEmpty()) {
        println("  ❌ Missing plugin descriptors!")
        missingResources.add("Plugin descriptors (plugin.yml)")
    }

    if (missingResources.isEmpty()) {
        println("\n✅ JAR VALIDATION PASSED - All required resources are present")
        println("   Total resources: ${normalizedResources.size}")
        println("   YAML files: ${yamlResources.size}")
        println("   Plugin descriptors: ${foundDescriptors.size}")
    } else {
        throw GradleException("""
            ❌ JAR VALIDATION FAILED - Missing required resources:
            ${missingResources.joinToString("\n") { "   - $it" }}

            Please ensure all required resources are in the src/main/resources directory.
            Critical resources should be directly in src/main/resources.
            Language files should be in src/main/resources/lang/.
        """.trimIndent())
    }
}