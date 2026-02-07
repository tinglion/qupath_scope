# PowerShell script to create a fat JAR for qupath-extension-pathscope

# Configuration
$PLUGIN_NAME = "qupath-extension-pathscope"
$VERSION = "0.7.1"
$PROJECT_DIR = Get-Location
$LIB_DIR = Join-Path $PROJECT_DIR "libs"
$PLUGIN_JAR = Join-Path $PROJECT_DIR "$PLUGIN_NAME\build\libs\$PLUGIN_NAME-$VERSION.jar"
$FAT_JAR = Join-Path $PROJECT_DIR "$PLUGIN_NAME-$VERSION-all.jar"
$TEMP_DIR = Join-Path $PROJECT_DIR "temp_fatjar"
$DIST_DIR = Join-Path $PROJECT_DIR "dist"

Write-Host "=== Creating fat JAR for $PLUGIN_NAME ==="

# Check if plugin JAR exists
if (-not (Test-Path $PLUGIN_JAR)) {
    Write-Host "ERROR: Plugin JAR not found at $PLUGIN_JAR!" -ForegroundColor Red
    Write-Host "Please build the plugin first with: ./gradlew :$PLUGIN_NAME:jar --no-daemon"
    exit 1
}

# Create directories
New-Item -ItemType Directory -Path $TEMP_DIR -Force | Out-Null
New-Item -ItemType Directory -Path $DIST_DIR -Force | Out-Null

# Step 1: Build the plugin if needed
Write-Host "1. Verifying plugin JAR..."
Write-Host "   鉁?Plugin JAR found: $PLUGIN_JAR"

# Step 2: Download dependencies if missing
Write-Host "2. Checking dependencies..."
$dependencies = @(
    @{ Name = "okhttp-4.12.0.jar"; Url = "https://repo1.maven.org/maven2/com/squareup/okhttp3/okhttp/4.12.0/okhttp-4.12.0.jar" },
    @{ Name = "okio-3.4.0.jar"; Url = "https://repo1.maven.org/maven2/com/squareup/okio/okio/3.4.0/okio-3.4.0.jar" },
    @{ Name = "kotlin-stdlib-1.9.20.jar"; Url = "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-stdlib/1.9.20/kotlin-stdlib-1.9.20.jar" },
    @{ Name = "kotlin-stdlib-common-1.9.20.jar"; Url = "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-stdlib-common/1.9.20/kotlin-stdlib-common-1.9.20.jar" },
    @{ Name = "gson-2.13.2.jar"; Url = "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.13.2/gson-2.13.2.jar" }
)

foreach ($dep in $dependencies) {
    $depPath = Join-Path $LIB_DIR $dep.Name
    if (-not (Test-Path $depPath)) {
        Write-Host "   Downloading $($dep.Name)..."
        Invoke-WebRequest -Uri $dep.Url -OutFile $depPath
    } else {
        Write-Host "   鉁?$($dep.Name) already exists"
    }
}

# Step 3: Create a simple fat JAR using Gradle's built-in capabilities
Write-Host "3. Creating fat JAR using Gradle..."

# Create a temporary Gradle script to build the fat JAR
$gradleScript = """./gradlew -b <(cat << 'EOF'
plugins {
    id('java')
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(files('$PLUGIN_JAR'))
    implementation('com.squareup.okhttp3:okhttp:4.12.0')
    implementation('com.google.code.gson:gson:2.13.2')
}

task fatJar(type: Jar) {
    archiveBaseName.set('$PLUGIN_NAME')
    archiveVersion.set('$VERSION')
    archiveClassifier.set('all')
    from sourceSets.main.output
    dependsOn configurations.runtimeClasspath
    from {
        configurations.runtimeClasspath.findAll { it.name.endsWith('jar') }.collect { zipTree(it) }
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

task build {
    dependsOn fatJar
}
EOF
) build --no-daemon"""

# Run the Gradle script
$tempGradleFile = Join-Path $PROJECT_DIR "temp-fatjar-build.gradle"
@"
plugins {
    id('java')
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(files('$PLUGIN_JAR'))
    implementation('com.squareup.okhttp3:okhttp:4.12.0')
    implementation('com.google.code.gson:gson:2.13.2')
}

task fatJar(type: Jar) {
    archiveBaseName.set('$PLUGIN_NAME')
    archiveVersion.set('$VERSION')
    archiveClassifier.set('all')
    from sourceSets.main.output
    dependsOn configurations.runtimeClasspath
    from {
        configurations.runtimeClasspath.findAll { it.name.endsWith('jar') }.collect { zipTree(it) }
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    destinationDirectory.set(file('$DIST_DIR'))
}

task build {
    dependsOn fatJar
}
"@ | Out-File -FilePath $tempGradleFile -Encoding UTF8

# Run the Gradle build
& ./gradlew -b $tempGradleFile build --no-daemon

# Clean up temporary file
Remove-Item -Path $tempGradleFile -Force

# Verify the fat JAR was created
$finalFatJar = Join-Path $DIST_DIR "$PLUGIN_NAME-$VERSION-all.jar"
if (Test-Path $finalFatJar) {
    Write-Host "4. Verifying fat JAR..."
    $size = (Get-Item $finalFatJar).Length / 1MB
    Write-Host "   鉁?Fat JAR created successfully!" -ForegroundColor Green
    Write-Host "   Size: $([math]::Round($size, 2)) MB"
    
    Write-Host "5. Checking contents..."
    Write-Host "   Checking for plugin classes..."
    & jar tf $finalFatJar | Select-String -Pattern "qupath/extension/pathscope" | Select-Object -First 3
    
    Write-Host "   Checking for okhttp3 classes..."
    & jar tf $finalFatJar | Select-String -Pattern "okhttp3" | Select-Object -First 3
    
    Write-Host "   Checking for gson classes..."
    & jar tf $finalFatJar | Select-String -Pattern "com/google/gson" | Select-Object -First 3
    
    Write-Host "" -ForegroundColor White
    Write-Host "=== SUCCESS! ===" -ForegroundColor Green
    Write-Host "Fat JAR created at: $finalFatJar" -ForegroundColor White
    Write-Host "To install: Copy this single JAR file to QuPath's extensions directory" -ForegroundColor White
    Write-Host "Example path (Windows): C:\Users\YourName\AppData\Roaming\QuPath\extensions" -ForegroundColor White
} else {
    Write-Host "ERROR: Fat JAR was not created!" -ForegroundColor Red
    exit 1
}
