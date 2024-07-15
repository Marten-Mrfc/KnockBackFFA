import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly

plugins {
    id("java")
    kotlin("jvm") version "1.9.22"
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "dev.Marten_mrfcyt"
version = "0.1-dev"

repositories {
    mavenCentral()
    maven("https://javadoc.jitpack.io")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    maven("https://repo.opencollab.dev/maven-snapshots/")
    maven ("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://libraries.minecraft.net")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}
val centralDependencies = listOf(
    "org.jetbrains.kotlin:kotlin-stdlib:1.9.22",
    "org.jetbrains.kotlin:kotlin-reflect:1.9.22",
    "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1",
    "com.corundumstudio.socketio:netty-socketio:1.7.19", // Keep this on a lower version as the newer version breaks the ping
)

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly("net.kyori:adventure-text-minimessage:4.16.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk9:1.6.0")
    compileOnly("com.mojang:brigadier:1.0.18")
    compileOnly("net.kyori:adventure-text-minimessage:4.13.1")
    implementation("com.github.marten-mrfc:LirandAPI:621cd466ce")
    compileOnly("me.clip:placeholderapi:2.11.3")
    compileOnly("org.junit.jupiter:junit-jupiter-api:5.7.2")
    compileOnly("org.mockito:mockito-core:5.11.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.2")
}

val targetJavaVersion = 21
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "21"
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand("version" to version, "libraries" to centralDependencies)
    }
}

kotlin {
    jvmToolchain(21)
}

task<ShadowJar>("buildAndMove") {
    dependsOn("shadowJar")

    group = "build"
    description = "Builds the jar and moves it to the server folder"

    // Move the jar from the build/libs folder to the server/plugins folder
    doLast {
        val jar = file("build/libs/%s-%s-all.jar".format(project.name, version))
        val server = file("server/plugins/%s-%s.jar".format(project.name.capitalizeAsciiOnly(), version))

        // Delete the old file if it exists
        if (server.exists()) {
            server.delete()
        }

        // Copy the new file to the server
        jar.copyTo(server, overwrite = true)
    }
}