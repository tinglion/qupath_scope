@echo off
REM ============================================================================
REM QuPath Portable Package Builder
REM ============================================================================
REM This script builds a portable QuPath package with pathscope extension
REM The output will be in build\dist\ directory
REM ============================================================================

echo.
echo ============================================================================
echo QuPath Portable Package Builder
echo ============================================================================
echo.

REM Check if we're in the right directory
if not exist "gradlew.bat" (
    echo ERROR: gradlew.bat not found!
    echo Please run this script from the QuPath project root directory.
    echo.
    pause
    exit /b 1
)

REM Get QuPath version
if exist "VERSION" (
    set /p QUPATH_VERSION=<VERSION
    echo QuPath Version: %QUPATH_VERSION%
) else (
    echo WARNING: VERSION file not found, using default
    set QUPATH_VERSION=0.7.0-SNAPSHOT
)

echo.
echo Step 1/5: Cleaning previous builds...
echo ----------------------------------------------------------------------------
call gradlew.bat clean
if errorlevel 1 (
    echo ERROR: Clean failed!
    pause
    exit /b 1
)

echo.
echo Step 2/5: Building QuPath with pathscope extension...
echo ----------------------------------------------------------------------------
echo This may take several minutes on first run...
call gradlew.bat build -x test
if errorlevel 1 (
    echo ERROR: Build failed!
    pause
    exit /b 1
)

echo.
echo Step 3/5: Creating portable application package...
echo ----------------------------------------------------------------------------
echo This will create a self-contained application with Java runtime...
call gradlew.bat jpackage -P package=image
if errorlevel 1 (
    echo ERROR: jpackage failed!
    pause
    exit /b 1
)

echo.
echo Step 4/5: Verifying pathscope extension...
echo ----------------------------------------------------------------------------
set DIST_DIR=build\dist\QuPath-%QUPATH_VERSION%
if exist "%DIST_DIR%\lib\qupath-extension-pathscope-*.jar" (
    echo [OK] PathScope extension found in package
    dir /b "%DIST_DIR%\lib\qupath-extension-pathscope-*.jar"
) else (
    echo [WARNING] PathScope extension JAR not found
    echo Please check build\dist\QuPath-%QUPATH_VERSION%\lib\ directory
)

echo.
echo Step 5/5: Creating ZIP archive...
echo ----------------------------------------------------------------------------
if exist "%DIST_DIR%" (
    REM Create a zip file name
    set ZIP_NAME=QuPath-%QUPATH_VERSION%-portable.zip

    REM Check if 7-Zip is available
    where 7z >nul 2>nul
    if %ERRORLEVEL% EQU 0 (
        echo Using 7-Zip to create archive...
        pushd build\dist
        7z a -tzip "../../!ZIP_NAME!" "QuPath-%QUPATH_VERSION%"
        popd
        echo [OK] Created: %ZIP_NAME%
    ) else (
        REM Check if PowerShell Compress-Archive is available
        powershell -Command "Get-Command Compress-Archive" >nul 2>nul
        if !ERRORLEVEL! EQU 0 (
            echo Using PowerShell to create archive...
            powershell -Command "Compress-Archive -Path '%DIST_DIR%' -DestinationPath '!ZIP_NAME!' -Force"
            echo [OK] Created: %ZIP_NAME%
        ) else (
            echo [SKIP] Neither 7-Zip nor PowerShell Compress-Archive found
            echo Please manually zip the folder: %DIST_DIR%
        )
    )
) else (
    echo [WARNING] Distribution directory not found: %DIST_DIR%
)

echo.
echo ============================================================================
echo Build Complete!
echo ============================================================================
echo.
echo Output location:
echo   Directory: %DIST_DIR%
echo   Executable: %DIST_DIR%\bin\QuPath.exe
if exist "%ZIP_NAME%" echo   Archive: %ZIP_NAME%
echo.
echo To test the build:
echo   1. Navigate to %DIST_DIR%
echo   2. Run bin\QuPath.exe
echo   3. Check Extensions ^> Installed Extensions for PathScope
echo.
echo To distribute:
if exist "%ZIP_NAME%" (
    echo   Share the %ZIP_NAME% file
) else (
    echo   Manually zip %DIST_DIR% and share
)
echo   Users can extract and run without installing Java
echo.

REM Ask if user wants to open the output directory
echo.
choice /C YN /M "Open output directory in Explorer?"
if errorlevel 2 goto :end
if errorlevel 1 (
    if exist "%DIST_DIR%" (
        explorer "build\dist"
    )
)

:end
echo.
pause
