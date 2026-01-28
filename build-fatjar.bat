@echo off
setlocal enabledelayedexpansion

REM 配置变量
set VERSION_FILE=VERSION
set PLUGIN_NAME=qupath-extension-pathscope
set BUILD_DIR=build
set LIB_DIR=libs
set OUTPUT_DIR=%BUILD_DIR%\fatjars
set MAIN_JAR=%PLUGIN_NAME%-%VERSION%-%VERSION%-SNAPSHOT.jar

REM 读取版本号
set /p VERSION=<%VERSION_FILE%
set VERSION=%VERSION:~0,-1% REM 移除换行符

REM 创建输出目录
mkdir %OUTPUT_DIR% 2>nul
mkdir %LIB_DIR% 2>nul

REM 1. 构建原始插件JAR
echo "Step 1: 构建原始插件JAR..."
call gradlew :%PLUGIN_NAME%:jar --no-daemon
if %ERRORLEVEL% neq 0 goto error

REM 2. 下载依赖

echo "Step 2: 下载依赖..."

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

REM 3. 创建临时目录用于组装JAR
set TEMP_DIR=%BUILD_DIR%\temp_fatjar
mkdir %TEMP_DIR% 2>nul

REM 4. 解压所有JAR到临时目录
echo "Step 3: 解压JAR文件..."

REM 解压插件JAR
jar xf %PLUGIN_NAME%\build\libs\%PLUGIN_NAME%-0.7.0-SNAPSHOT.jar -C %TEMP_DIR%

REM 解压依赖JAR
for %%f in (%LIB_DIR%\*.jar) do (
    echo "Extracting %%~nf..."
    jar xf %%f -C %TEMP_DIR%
)

REM 5. 创建fat JAR
echo "Step 4: 创建fat JAR..."
jar cf %OUTPUT_DIR%\%PLUGIN_NAME%-0.7.0-SNAPSHOT-all.jar -C %TEMP_DIR% .

REM 6. 清理临时文件
echo "Step 5: 清理临时文件..."
rmdir /s /q %TEMP_DIR%

REM 7. 复制到分发目录
mkdir dist 2>nul
copy %OUTPUT_DIR%\%PLUGIN_NAME%-0.7.0-SNAPSHOT-all.jar dist\ 2>nul

echo ""
echo "构建完成！"
echo "输出文件：%OUTPUT_DIR%\%PLUGIN_NAME%-0.7.0-SNAPSHOT-all.jar"
echo "分发文件：dist\%PLUGIN_NAME%-0.7.0-SNAPSHOT-all.jar"
goto end

error:
echo "构建失败！"
exit /b 1

end:
endlocal