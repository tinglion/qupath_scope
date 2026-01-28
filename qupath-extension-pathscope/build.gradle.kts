plugins {
  id("qupath.extension-conventions")
  id("qupath.javafx-conventions")
  id("qupath.publishing-conventions")
  `java-library`
  id("me.champeau.mrjar") version "0.1.1" apply false
}

extra["moduleName"] = "qupath.extension.pathscope"
base {
  archivesName = "qupath-extension-pathscope"
  description = "QuPath PathScope extension for task management and annotation submission."
}

dependencies {
  implementation(project(":qupath-gui-fx"))
  implementation(project(":qupath-core"))
  // Add HTTP client for API calls
  implementation(libs.okhttp)
  // JSON parsing
  implementation(libs.gson)
}

// Create a custom task to build a fat JAR
tasks.register<Jar>("fatJar") {
    group = "build"
    description = "Builds a fat JAR with all dependencies included"

    // Configure the output JAR
    archiveBaseName.set("qupath-extension-pathscope")
    archiveClassifier.set("all")

    // Include compiled classes
    from(sourceSets.main.get().output)

    // Include dependencies
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })

    // Exclude QuPath core modules (they will be provided by the QuPath application)
    exclude("qupath/core/**")
    exclude("qupath/lib/**")
    exclude("qupath/imagej/**")

    // Exclude duplicate files
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Use ASM to downgrade bytecode from Java 25 to Java 21
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.ow2.asm:asm:9.7")
        classpath("org.ow2.asm:asm-commons:9.7")
    }
}

tasks.register<JavaExec>("downgradeJar") {
    group = "build"
    description = "Downgrades JAR bytecode from Java 25 to Java 21"
    dependsOn("fatJar")

    val inputJar = tasks.named<Jar>("fatJar").get().archiveFile.get().asFile
    val outputJar = file("${buildDir}/libs/qupath-extension-pathscope-0.7.0-SNAPSHOT-all-java21.jar")

    doLast {
        ant.withGroovyBuilder {
            "taskdef"(
                "name" to "jarjar",
                "classname" to "com.tonicsystems.jarjar.JarJarTask",
                "classpath" to configurations.named("compileClasspath").get().asPath
            )
        }

        println("Note: Manual bytecode downgrade required.")
        println("Input JAR: $inputJar")
        println("Please use a tool like 'jarjar' or 'asm' to downgrade bytecode from v69 to v65")
        println("Or recompile in a Java 21 environment.")
    }
}

// Make the build task depend on our fatJar task
tasks.named<DefaultTask>("build") {
    dependsOn(tasks.named("fatJar"))
}
