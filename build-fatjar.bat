@echo off
setlocal enabledelayedexpansion

REM éç½®åé
set VERSION_FILE=VERSION
set PLUGIN_NAME=qupath-extension-pathscope
set BUILD_DIR=build
set LIB_DIR=libs
set OUTPUT_DIR=%BUILD_DIR%\fatjars
set MAIN_JAR=%PLUGIN_NAME%-%VERSION%-%VERSION%-SNAPSHOT.jar

REM è¯»åçæ¬å?set /p VERSION=<%VERSION_FILE%
set VERSION=%VERSION:~0,-1% REM ç§»é¤æ¢è¡ç¬?
REM åå»ºè¾åºç®å½
mkdir %OUTPUT_DIR% 2>nul
mkdir %LIB_DIR% 2>nul

REM 1. æå»ºåå§æä»¶JAR
echo "Step 1: æå»ºåå§æä»¶JAR..."
call gradlew :%PLUGIN_NAME%:jar --no-daemon
if %ERRORLEVEL% neq 0 goto error

REM 2. ä¸è½½ä¾èµ

echo "Step 2: ä¸è½½ä¾èµ..."

REM okhttp3
echo "Downloading okhttp..."
curl -L -o %LIB_DIR%\okhttp-4.12.0.jar https://repo1.maven.org/maven2/com/squareup/okhttp3/okhttp/4.12.0/okhttp-4.12.0.jar
if %ERRORLEVEL% neq 0 goto error

REM okio
echo "Downloading okio..."
curl -L -o %LIB_DIR%\okio-3.4.0.jar https://repo1.maven.org/maven2/com/squareup/okio/okio/3.4.0/okio-3.4.0.jar
if %ERRORLEVEL% neq 0 goto error

REM kotlin-stdlib
echo "Downloading kotlin-stdlib..."
curl -L -o %LIB_DIR%\kotlin-stdlib-1.9.20.jar https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-stdlib/1.9.20/kotlin-stdlib-1.9.20.jar
if %ERRORLEVEL% neq 0 goto error

REM kotlin-stdlib-common
echo "Downloading kotlin-stdlib-common..."
curl -L -o %LIB_DIR%\kotlin-stdlib-common-1.9.20.jar https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-stdlib-common/1.9.20/kotlin-stdlib-common-1.9.20.jar
if %ERRORLEVEL% neq 0 goto error

REM gson
echo "Downloading gson..."
curl -L -o %LIB_DIR%\gson-2.13.2.jar https://repo1.maven.org/maven2/com/google/code/gson/gson/2.13.2/gson-2.13.2.jar
if %ERRORLEVEL% neq 0 goto error

REM 3. åå»ºä¸´æ¶ç®å½ç¨äºç»è£JAR
set TEMP_DIR=%BUILD_DIR%\temp_fatjar
mkdir %TEMP_DIR% 2>nul

REM 4. è§£åææJARå°ä¸´æ¶ç®å½?echo "Step 3: è§£åJARæä»¶..."

REM è§£åæä»¶JAR
jar xf %PLUGIN_NAME%\build\libs\%PLUGIN_NAME%-0.7.1.jar -C %TEMP_DIR%

REM è§£åä¾èµJAR
for %%f in (%LIB_DIR%\*.jar) do (
    echo "Extracting %%~nf..."
    jar xf %%f -C %TEMP_DIR%
)

REM 5. åå»ºfat JAR
echo "Step 4: åå»ºfat JAR..."
jar cf %OUTPUT_DIR%\%PLUGIN_NAME%-0.7.1-all.jar -C %TEMP_DIR% .

REM 6. æ¸çä¸´æ¶æä»¶
echo "Step 5: æ¸çä¸´æ¶æä»¶..."
rmdir /s /q %TEMP_DIR%

REM 7. å¤å¶å°ååç®å½?mkdir dist 2>nul
copy %OUTPUT_DIR%\%PLUGIN_NAME%-0.7.1-all.jar dist\ 2>nul

echo ""
echo "æå»ºå®æï¼?
echo "è¾åºæä»¶ï¼?OUTPUT_DIR%\%PLUGIN_NAME%-0.7.1-all.jar"
echo "ååæä»¶ï¼dist\%PLUGIN_NAME%-0.7.1-all.jar"
goto end

error:
echo "æå»ºå¤±è´¥ï¼?
exit /b 1

end:
endlocal
