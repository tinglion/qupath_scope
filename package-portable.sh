#!/bin/bash
# ============================================================================
# QuPath Portable Package Builder (Linux/macOS)
# ============================================================================
# This script builds a portable QuPath package with pathscope extension
# The output will be in build/dist/ directory
# ============================================================================

set -e  # Exit on error

echo ""
echo "============================================================================"
echo "QuPath Portable Package Builder"
echo "============================================================================"
echo ""

# Check if we're in the right directory
if [ ! -f "gradlew" ]; then
    echo "ERROR: gradlew not found!"
    echo "Please run this script from the QuPath project root directory."
    exit 1
fi

# Make gradlew executable
chmod +x gradlew

# Get QuPath version
if [ -f "VERSION" ]; then
    QUPATH_VERSION=$(cat VERSION | tr -d '\r\n')
    echo "QuPath Version: $QUPATH_VERSION"
else
    echo "WARNING: VERSION file not found, using default"
    QUPATH_VERSION="0.7.0-SNAPSHOT"
fi

# Detect platform
PLATFORM=$(uname -s)
if [ "$PLATFORM" = "Darwin" ]; then
    PLATFORM_NAME="Mac"
    ARCH=$(uname -m)
    if [ "$ARCH" = "arm64" ]; then
        DIST_NAME="QuPath-${QUPATH_VERSION}-arm64.app"
    else
        DIST_NAME="QuPath-${QUPATH_VERSION}-x64.app"
    fi
else
    PLATFORM_NAME="Linux"
    DIST_NAME="QuPath"
fi

echo ""
echo "Step 1/5: Cleaning previous builds..."
echo "----------------------------------------------------------------------------"
./gradlew clean

echo ""
echo "Step 2/5: Building QuPath with pathscope extension..."
echo "----------------------------------------------------------------------------"
echo "This may take several minutes on first run..."
./gradlew build -x test

echo ""
echo "Step 3/5: Creating portable application package..."
echo "----------------------------------------------------------------------------"
echo "This will create a self-contained application with Java runtime..."
./gradlew jpackage -P package=image

echo ""
echo "Step 4/5: Verifying pathscope extension..."
echo "----------------------------------------------------------------------------"
DIST_DIR="build/dist/$DIST_NAME"
if [ "$PLATFORM_NAME" = "Mac" ]; then
    LIB_DIR="$DIST_DIR/Contents/app"
else
    LIB_DIR="$DIST_DIR/lib"
fi

if ls $LIB_DIR/qupath-extension-pathscope-*.jar 1> /dev/null 2>&1; then
    echo "[OK] PathScope extension found in package:"
    ls $LIB_DIR/qupath-extension-pathscope-*.jar
else
    echo "[WARNING] PathScope extension JAR not found"
    echo "Please check $LIB_DIR directory"
fi

echo ""
echo "Step 5/5: Creating archive..."
echo "----------------------------------------------------------------------------"
if [ -d "$DIST_DIR" ]; then
    pushd build/dist > /dev/null

    if [ "$PLATFORM_NAME" = "Mac" ]; then
        # Create DMG for macOS
        ARCHIVE_NAME="QuPath-${QUPATH_VERSION}-${ARCH}.dmg"
        if command -v hdiutil &> /dev/null; then
            echo "Creating DMG image..."
            hdiutil create -volname "QuPath" -srcfolder "$DIST_NAME" -ov -format UDZO "../../$ARCHIVE_NAME"
            echo "[OK] Created: $ARCHIVE_NAME"
        else
            # Fallback to zip
            ARCHIVE_NAME="QuPath-${QUPATH_VERSION}-${ARCH}.zip"
            echo "Creating ZIP archive..."
            zip -r "../../$ARCHIVE_NAME" "$DIST_NAME"
            echo "[OK] Created: $ARCHIVE_NAME"
        fi
    else
        # Create tar.xz for Linux (matching GitHub Actions)
        ARCHIVE_NAME="QuPath-v${QUPATH_VERSION}-Linux.tar.xz"
        echo "Creating tar.xz archive..."
        tar -c "$DIST_NAME" | xz > "../../$ARCHIVE_NAME"
        echo "[OK] Created: $ARCHIVE_NAME"
    fi

    popd > /dev/null
else
    echo "[WARNING] Distribution directory not found: $DIST_DIR"
fi

echo ""
echo "============================================================================"
echo "Build Complete!"
echo "============================================================================"
echo ""
echo "Output location:"
echo "  Directory: $DIST_DIR"
if [ "$PLATFORM_NAME" = "Mac" ]; then
    echo "  Executable: $DIST_DIR/Contents/MacOS/QuPath"
else
    echo "  Executable: $DIST_DIR/bin/QuPath"
fi
[ -f "$ARCHIVE_NAME" ] && echo "  Archive: $ARCHIVE_NAME"
echo ""
echo "To test the build:"
if [ "$PLATFORM_NAME" = "Mac" ]; then
    echo "  1. Open $DIST_DIR"
    echo "  2. Check Extensions > Installed Extensions for PathScope"
else
    echo "  1. Navigate to $DIST_DIR"
    echo "  2. Run bin/QuPath"
    echo "  3. Check Extensions > Installed Extensions for PathScope"
fi
echo ""
echo "To distribute:"
[ -f "$ARCHIVE_NAME" ] && echo "  Share the $ARCHIVE_NAME file"
echo "  Users can extract and run without installing Java"
echo ""
