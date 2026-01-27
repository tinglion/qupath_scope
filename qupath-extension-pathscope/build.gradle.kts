import org.gradle.api.attributes.java.TargetJvmEnvironment

plugins {
  id("qupath.extension-conventions")
  id("qupath.javafx-conventions")
  id("qupath.publishing-conventions")
  `java-library`
}

extra["moduleName"] = "qupath.extension.pathscope"
base {
  archivesName = "qupath-extension-pathscope"
  description = "QuPath PathScope extension for task management and annotation submission."
}

// Use Java 25 compatibility settings to match other modules
tasks.withType<JavaCompile> {
    // Remove the release flag to use the default settings from the convention plugin
    options.compilerArgs = options.compilerArgs.filter { it != "--release" && it != "21" }
}

// Configure dependency resolution to accept higher Java version dependencies
configurations.all {
    resolutionStrategy {
        // Force accept dependencies regardless of their Java version
        eachDependency {
            // Always use the requested version if available
            requested.version?.let { useVersion(it) }
        }
    }
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
    // Don't exclude extension packages - they might be needed for other extensions
    // but our own pathscope extension will be included anyway
    
    // Exclude duplicate files
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Make the build task depend on our fatJar task
tasks.named<DefaultTask>("build") {
    dependsOn(tasks.named("fatJar"))
}
