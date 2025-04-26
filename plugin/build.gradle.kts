import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly

plugins {
    id("java")
    kotlin("jvm") version "1.9.22"
    `maven-publish`
    id("io.github.goooler.shadow") version "8.1.7"
}

group = "dev.Marten_mrfcyt"
version = "0.7.0-alpha"

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
    maven("https://maven.enginehub.org/repo/")
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
    compileOnly("org.junit.jupiter:junit-jupiter-api:5.7.2")
    compileOnly("org.mockito:mockito-core:5.11.0")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.2")
    implementation("org.bstats:bstats-bukkit:3.1.0")
    implementation("mlib.api:MLib:0.0.1")
    implementation("com.h2database:h2:2.2.224") // Required for MySQL functionality
    implementation("com.mysql:mysql-connector-j:9.2.0") // Explicitly add MySQL connector
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

// Function to dynamically discover resources in src/main/resources
fun discoverResources(): Map<String, Set<String>> {
    val resourcesDir = file("src/main/resources")
    val result = mutableMapOf<String, MutableSet<String>>()
    
    // Initialize result categories
    result["criticalResources"] = mutableSetOf()
    result["languageFiles"] = mutableSetOf()
    result["pluginDescriptors"] = mutableSetOf()
    
    if (resourcesDir.exists()) {
        resourcesDir.walk().filter { it.isFile }.forEach { file ->
            val relativePath = file.relativeTo(resourcesDir).path.replace('\\', '/')
            
            when {
                // Plugin descriptors
                relativePath == "plugin.yml" || relativePath == "paper-plugin.yml" -> {
                    result["pluginDescriptors"]?.add(relativePath)
                }
                // Language files
                relativePath.startsWith("lang/") && (relativePath.endsWith(".yml") || relativePath.endsWith(".yaml")) -> {
                    result["languageFiles"]?.add(relativePath)
                }
                // Critical resources in root directory
                !relativePath.contains("/") && (relativePath.endsWith(".yml") || relativePath.endsWith(".yaml")) -> {
                    result["criticalResources"]?.add(relativePath)
                }
            }
        }
    }
    
    return result
}

// This is the key task for ensuring resources are properly processed
tasks.processResources {
    // Set UTF-8 as the input file encoding
    filteringCharset = "UTF-8"
    
    // Process plugin.yml for variable expansion
    filesMatching("plugin.yml") {
        expand(mapOf(
            "version" to version,
            "libraries" to centralDependencies
        ))
    }
    
    // Make sure resources are copied even if they already exist in the output
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    
    // Log what resources are being processed
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
    jvmToolchain(21)
}

// Task to create JAR with resources
tasks.jar {
    // Ensure resources are processed
    dependsOn("processResources")
    
    // Include resources at the root of the JAR (not in a resources/ directory)
    from("${layout.buildDirectory}/resources/main") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}

// ShadowJar configuration
tasks.withType<ShadowJar> {
    // Ensure resources are processed before creating the JAR
    dependsOn("processResources")
    
    // Don't add a suffix to the output file
    archiveClassifier.set("")
    
    // Handle duplicates
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    
    // Include resources directly (not in a resources/ directory)
    from("${layout.buildDirectory}/resources/main") {
        // This ensures resources don't go inside a 'resources' folder in the JAR
        into("")
    }

    minimize {
        exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
        // Don't minimize resources
        exclude("lang/**")
        exclude("*.yml")
    }
    
    // Merge service files
    mergeServiceFiles()
    
    // Validate the JAR after creation
    doLast {
        println("ShadowJar created at: ${archiveFile.get().asFile.absolutePath}")
        println("JAR size: ${archiveFile.get().asFile.length()} bytes")
        
        // This is to ensure resources aren't added under a resources/ directory
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

// Setup build task to depend on shadowJar
tasks.build {
    dependsOn("shadowJar")
}

// Create a task to move the JAR to the server plugins directory
tasks.register<Copy>("buildAndMove") {
    dependsOn("shadowJar")
    
    group = "build"
    description = "Builds the jar and moves it to the server folder"
    
    from("${layout.buildDirectory}/libs/${project.name}-${project.version}.jar")
    into("server/plugins")
    rename { "${project.name.capitalizeAsciiOnly()}-${project.version}.jar" }
    
    doFirst {
        val targetDir = file("server/plugins")
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        
        val existingJar = file("server/plugins/${project.name.capitalizeAsciiOnly()}-${project.version}.jar")
        if (existingJar.exists()) {
            existingJar.delete()
            println("Deleted existing JAR file: ${existingJar.absolutePath}")
        }
    }
    
    doLast {
        val destFile = file("server/plugins/${project.name.capitalizeAsciiOnly()}-${project.version}.jar")
        println("JAR successfully copied to: ${destFile.absolutePath}")
    }
}

// Create a separate task for validating JAR resources
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


// Utility function to validate resources in the JAR file
fun validateJarResources(jarFile: File) {
    println("\n=== Validating JAR Resources ===")
    
    if (!jarFile.exists()) {
        throw GradleException("JAR file not found at: ${jarFile.absolutePath}")
    }
    
    // Discover resources
    val discoveredResources = discoverResources()
    val criticalResources = discoveredResources["criticalResources"] ?: emptySet()
    val languageFiles = discoveredResources["languageFiles"] ?: emptySet()
    val pluginDescriptors = discoveredResources["pluginDescriptors"] ?: emptySet()
    
    // Get JAR contents
    val process = ProcessBuilder("jar", "-tf", jarFile.absolutePath)
        .redirectErrorStream(true)
        .start()
    
    val output = process.inputStream.bufferedReader().readText()
    val jarEntries = output.lines()
        .filter { it.isNotBlank() }
        .toList()
    
    // Extract YAML resources
    val yamlResources = jarEntries.filter { 
        it.endsWith(".yml") || it.endsWith(".yaml") 
    }
    
    println("Found ${yamlResources.size} YAML files in JAR:")
    yamlResources.forEach { println("  - $it") }
    
    // Extract key paths (root or resources/ prefix pattern)
    val resourcePattern = """(?:resources/)?(.+\.yml)""".toRegex()
    val normalizedResources = yamlResources.mapNotNull { path ->
        resourcePattern.find(path)?.groupValues?.get(1)
    }.toSet()
    
    println("\nNormalized resource paths:")
    normalizedResources.sorted().forEach { 
        println("  - $it") 
    }
    
    // Check for critical resources
    val missingResources = mutableListOf<String>()
    
    println("\nChecking for critical resources:")
    
    // Function to check if a resource exists in normalized resources
    fun isResourceInJar(resourcePath: String): Boolean {
        val normalizedPath = resourcePath.replace('\\', '/')
        return normalizedResources.any { 
            it == normalizedPath || it.endsWith("/$normalizedPath")
        }
    }
    
    // Check critical resources
    criticalResources.forEach { resource ->
        if (isResourceInJar(resource)) {
            println("  ✅ Found: $resource")
        } else {
            println("  ❌ Missing: $resource")
            missingResources.add(resource)
        }
    }
    
    // Check language files
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
    
    // Check for plugin.yml and paper-plugin.yml
    val foundDescriptors = pluginDescriptors.filter { isResourceInJar(it) }
    
    println("\nChecking for plugin descriptors:")
    if (foundDescriptors.isNotEmpty()) {
        foundDescriptors.forEach { println("  ✅ Found: $it") }
    } else if (!pluginDescriptors.isEmpty()) {
        println("  ❌ Missing plugin descriptors!")
        missingResources.add("Plugin descriptors (plugin.yml)")
    }
    
    // Final validation
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
