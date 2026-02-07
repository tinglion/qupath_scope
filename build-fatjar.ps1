# PowerShell script to build a fat JAR for the qupath-extension-pathscope plugin

# Configuration
$VERSION_FILE = "VERSION"
$PLUGIN_NAME = "qupath-extension-pathscope"
$BUILD_DIR = "build"
$LIB_DIR = "libs"
$OUTPUT_DIR = Join-Path $BUILD_DIR "fatjars"
$DIST_DIR = "dist"

# Read version from file
$VERSION = Get-Content -Path $VERSION_FILE -Raw -Encoding UTF8
$VERSION = $VERSION.Trim()

# Create directories
New-Item -ItemType Directory -Path $OUTPUT_DIR -Force | Out-Null
New-Item -ItemType Directory -Path $LIB_DIR -Force | Out-Null
New-Item -ItemType Directory -Path $DIST_DIR -Force | Out-Null

Write-Host "=== Building fat JAR for $PLUGIN_NAME ==="

# Step 1: Build the original plugin JAR
Write-Host "Step 1: Building original plugin JAR..." -ForegroundColor Cyan
& .\gradlew :$PLUGIN_NAME:jar --no-daemon
if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to build original plugin JAR!" -ForegroundColor Red
    exit 1
}

# Step 2: Download dependencies if not already present
Write-Host "Step 2: Downloading dependencies..." -ForegroundColor Cyan

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
        Write-Host "Downloading $($dep.Name)..."
        Invoke-WebRequest -Uri $dep.Url -OutFile $depPath
    } else {
        Write-Host "$($dep.Name) already exists, skipping download..."
    }
}

# Step 3: Create temporary directory for JAR assembly
$TEMP_DIR = Join-Path $BUILD_DIR "temp_fatjar"
New-Item -ItemType Directory -Path $TEMP_DIR -Force | Out-Null

# Step 4: Extract all JARs to temporary directory
Write-Host "Step 3: Extracting JAR files..." -ForegroundColor Cyan

# Extract plugin JAR
$pluginJarPath = Join-Path $PLUGIN_NAME "build/libs/$PLUGIN_NAME-0.7.1.jar"
Write-Host "Extracting plugin JAR: $pluginJarPath"
& jar xf $pluginJarPath -C $TEMP_DIR

# Extract dependency JARs
Get-ChildItem -Path $LIB_DIR -Filter "*.jar" | ForEach-Object {
    $depJar = $_.FullName
    Write-Host "Extracting dependency: $($_.Name)"
    & jar xf $depJar -C $TEMP_DIR
}

# Step 5: Create fat JAR
Write-Host "Step 4: Creating fat JAR..." -ForegroundColor Cyan
$fatJarName = "$PLUGIN_NAME-0.7.1-all.jar"
$fatJarPath = Join-Path $OUTPUT_DIR $fatJarName
& jar cf $fatJarPath -C $TEMP_DIR .

# Step 6: Clean up temporary files
Write-Host "Step 5: Cleaning up temporary files..." -ForegroundColor Cyan
Remove-Item -Path $TEMP_DIR -Recurse -Force

# Step 7: Copy to distribution directory
$distJarPath = Join-Path $DIST_DIR $fatJarName
Copy-Item -Path $fatJarPath -Destination $distJarPath -Force

# Step 8: Verify the fat JAR contains all dependencies
Write-Host "Step 6: Verifying fat JAR contents..." -ForegroundColor Cyan
Write-Host "Checking if okhttp3 classes are included..."
& jar tf $fatJarPath | Select-String -Pattern "okhttp3" -Quiet
if ($LASTEXITCODE -eq 0) {
    Write-Host "鉁?okhttp3 classes found" -ForegroundColor Green
} else {
    Write-Host "鉁?okhttp3 classes missing!" -ForegroundColor Red
}

Write-Host "Checking if gson classes are included..."
& jar tf $fatJarPath | Select-String -Pattern "com/google/gson" -Quiet
if ($LASTEXITCODE -eq 0) {
    Write-Host "鉁?gson classes found" -ForegroundColor Green
} else {
    Write-Host "鉁?gson classes missing!" -ForegroundColor Red
}

# Final summary
Write-Host "" -ForegroundColor White
Write-Host "=== Build Complete! ===" -ForegroundColor Green
Write-Host "Fat JAR created: $fatJarPath" -ForegroundColor White
Write-Host "Distribution copy: $distJarPath" -ForegroundColor White
Write-Host ""
Write-Host "To install: Copy $fatJarName to QuPath's extensions directory" -ForegroundColor Cyan
