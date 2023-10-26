import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.10"
    id("io.papermc.paperweight.userdev") version "1.5.8"
    id("idea")
}

group = "de.zweistein2.plugins"
version = "2.0.3"
java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://nexus.scarsz.me/content/groups/public/")
    maven("https://maven.elmakers.com/repository/")
}

dependencies {
    // PaperMC Dependency
    paperweight.paperDevBundle("1.20.1-R0.1-SNAPSHOT")

    // Plugin Dependencies
    implementation("net.kyori:adventure-text-serializer-legacy:4.14.0")
    compileOnly("com.discordsrv:discordsrv:1.27.0-SNAPSHOT")
    compileOnly("ru.tehkode:PermissionsEx:1.23") {
        exclude("net.gravitydevelopment.updater", "updater")
    }
    testCompileOnly("com.discordsrv:discordsrv:1.27.0-SNAPSHOT")
    testCompileOnly("ru.tehkode:PermissionsEx:1.23") {
        exclude("net.gravitydevelopment.updater", "updater")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(JavaVersion.VERSION_17.majorVersion))
    }
}

project.sourceSets {
    main {
        java.srcDirs("/src/main/java")
        kotlin.srcDirs("/src/main/kotlin")
        resources.srcDirs("/src/main/resources")
    }
    test {
        java.srcDirs("/src/main/java")
        kotlin.srcDirs("/src/main/kotlin")
        resources.srcDirs("/src/main/resources")
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = JavaVersion.VERSION_17.majorVersion
    }
}

tasks.compileKotlin {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = JavaVersion.VERSION_17.majorVersion
    }
}

tasks.compileTestKotlin {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = JavaVersion.VERSION_17.majorVersion
    }
}

tasks.withType<Copy>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.assemble {
    dependsOn(tasks.reobfJar)
}

tasks.withType<Jar> {
    // To avoid the duplicate handling strategy error
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // To add all of the dependencies
    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}