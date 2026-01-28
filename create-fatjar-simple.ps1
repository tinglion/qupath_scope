# Simple PowerShell script to create a fat JAR
# This script manually combines JAR files without relying on complex Gradle configurations

# Configuration
$PLUGIN_NAME = "qupath-extension-pathscope"
$VERSION = "0.7.0-SNAPSHOT"
$PROJECT_DIR = Get-Location
$OUTPUT_DIR = Join-Path $PROJECT_DIR "dist"

# Create output directory if it doesn't exist
New-Item -ItemType Directory -Path $OUTPUT_DIR -Force | Out-Null

# List of JAR files to include (in order of precedence)
$jarFiles = @(
    "$PROJECT_DIR\qupath-extension-pathscope\build\libs\$PLUGIN_NAME-$VERSION.jar",
    "$PROJECT_DIR\libs\okhttp-4.12.0.jar",
    "$PROJECT_DIR\libs\okio-3.4.0.jar",
    "$PROJECT_DIR\libs\kotlin-stdlib-1.9.20.jar",
    "$PROJECT_DIR\libs\kotlin-stdlib-common-1.9.20.jar",
    "$PROJECT_DIR\libs\gson-2.13.2.jar"
)

# Output fat JAR name
$fatJarName = "$PLUGIN_NAME-$VERSION-all.jar"
$fatJarPath = Join-Path $OUTPUT_DIR $fatJarName

Write-Host "=== Creating Fat JAR: $fatJarName ==="
Write-Host "Project Directory: $PROJECT_DIR"
Write-Host "Output Directory: $OUTPUT_DIR"
Write-Host ""

# Check if all required JARs exist
$missingJars = @()
foreach ($jar in $jarFiles) {
    if (-not (Test-Path $jar)) {
        $missingJars += $jar
    }
}

if ($missingJars.Count -gt 0) {
    Write-Host "ERROR: Missing required JAR files!" -ForegroundColor Red
    foreach ($jar in $missingJars) {
        Write-Host "   - $jar" -ForegroundColor Red
    }
    Write-Host ""
    Write-Host "Please ensure all JAR files are present and try again." -ForegroundColor Yellow
    exit 1
}

# Create temporary directory for extraction
$tempDir = Join-Path $PROJECT_DIR "temp_fatjar_build"
New-Item -ItemType Directory -Path $tempDir -Force | Out-Null

Write-Host "Step 1: Extracting all JAR files..."

# Extract each JAR file
foreach ($jar in $jarFiles) {
    $jarName = Split-Path $jar -Leaf
    Write-Host "   Extracting: $jarName"
    
    # Create a subdirectory for each JAR to avoid conflicts during extraction
    $jarTempDir = Join-Path $tempDir $jarName.Replace('.jar', '')
    New-Item -ItemType Directory -Path $jarTempDir -Force | Out-Null
    
    # Extract the JAR using jar command
    & jar xf $jar -C $jarTempDir
    
    # Copy extracted files to main temp directory, overwriting older files
    Get-ChildItem -Path $jarTempDir -Recurse | ForEach-Object {
        if (-not $_.PSIsContainer) {
            $destPath = Join-Path $tempDir $_.FullName.Substring($jarTempDir.Length + 1)
            $destDir = Split-Path $destPath -Parent
            New-Item -ItemType Directory -Path $destDir -Force | Out-Null
            Copy-Item -Path $_.FullName -Destination $destPath -Force
        }
    }
}

Write-Host ""
Write-Host "Step 2: Creating fat JAR file..."

# Create the fat JAR from the combined files
& jar cf $fatJarPath -C $tempDir .

# Check if fat JAR was created successfully
if (-not (Test-Path $fatJarPath)) {
    Write-Host "ERROR: Failed to create fat JAR!" -ForegroundColor Red
    exit 1
}

# Get file size
$fileSize = (Get-Item $fatJarPath).Length / 1MB
Write-Host "   ✓ Fat JAR created: $fatJarName"
Write-Host "   ✓ Size: $([math]::Round($fileSize, 2)) MB"

Write-Host ""
Write-Host "Step 3: Verifying fat JAR contents..."

# Check if plugin classes are included
Write-Host "   Checking for plugin classes..."
$pluginClasses = & jar tf $fatJarPath | Select-String -Pattern "qupath/extension/pathscope" | Measure-Object | Select-Object -ExpandProperty Count
if ($pluginClasses -gt 0) {
    Write-Host "   ✓ Plugin classes found: $pluginClasses"
} else {
    Write-Host "   ✗ Plugin classes not found!" -ForegroundColor Red
}

# Check if okhttp3 classes are included
Write-Host "   Checking for okhttp3 classes..."
$okhttpClasses = & jar tf $fatJarPath | Select-String -Pattern "okhttp3" | Measure-Object | Select-Object -ExpandProperty Count
if ($okhttpClasses -gt 0) {
    Write-Host "   ✓ OkHttp3 classes found: $okhttpClasses"
} else {
    Write-Host "   ✗ OkHttp3 classes not found!" -ForegroundColor Red
}

# Check if gson classes are included
Write-Host "   Checking for gson classes..."
$gsonClasses = & jar tf $fatJarPath | Select-String -Pattern "com/google/gson" | Measure-Object | Select-Object -ExpandProperty Count
if ($gsonClasses -gt 0) {
    Write-Host "   ✓ Gson classes found: $gsonClasses"
} else {
    Write-Host "   ✗ Gson classes not found!" -ForegroundColor Red
}

Write-Host ""
Write-Host "Step 4: Cleaning up temporary files..."
Remove-Item -Path $tempDir -Recurse -Force

Write-Host ""
Write-Host "=== BUILD COMPLETE! ===" -ForegroundColor Green
Write-Host ""
Write-Host "Fat JAR has been created successfully:"
Write-Host "   $fatJarPath" -ForegroundColor White
Write-Host ""
Write-Host "=== DEPLOYMENT INSTRUCTIONS ==="
Write-Host "1. Copy the fat JAR to QuPath's extensions directory:"
Write-Host "   Windows: C:\Users\YourName\AppData\Roaming\QuPath\extensions"
Write-Host "   macOS: ~/Library/Application Support/QuPath/extensions"
Write-Host "   Linux: ~/.local/share/QuPath/extensions"
Write-Host ""
Write-Host "2. Restart QuPath to load the plugin."
Write-Host ""
Write-Host "3. Verify the plugin is loaded by checking:"
Write-Host "   - Plugin-specific menu items in the QuPath menu bar"
Write-Host "   - No 'NoClassDefFoundError' errors in the QuPath log"
Write-Host ""
Write-Host "This single JAR file contains all required dependencies, including:"
Write-Host "   - OkHttp3 (for HTTP requests)"
Write-Host "   - Gson (for JSON parsing)"
Write-Host "   - Kotlin stdlib (required by OkHttp)"
Write-Host "   - Okio (required by OkHttp)"
Write-Host ""
Write-Host "=== END ==="