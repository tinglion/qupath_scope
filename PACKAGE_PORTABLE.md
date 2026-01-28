# QuPath Portable 软件包构建指南

本文档说明如何将QuPath（包含pathscope扩展）打包成可直接发布的portable软件包。

## 打包机制说明

QuPath使用jpackage工具将应用程序打包成自包含的可执行程序，包括：
- 完整的Java运行时环境（JRE）
- QuPath应用程序及所有依赖
- 所有已注册的扩展（包括pathscope）

### pathscope扩展自动包含

pathscope扩展已在项目配置中注册：
- 在 `settings.gradle.kts` 第60行已注册
- 在 `build.gradle.kts` 第165-167行自动包含所有子项目
- 打包时会自动包含，无需额外配置

## 打包类型

QuPath支持多种打包类型，通过 `-P package=<type>` 参数指定：

### 1. app-image（推荐用于portable发布）
```bash
./gradlew jpackage -P package=image
```
- 创建一个可以直接解压运行的应用程序目录
- **Windows**: 生成 `build/dist/QuPath-<version>` 文件夹
- **Linux**: 生成 `build/dist/QuPath` 文件夹
- **macOS**: 生成 `build/dist/QuPath-<version>.app`
- 最适合制作portable版本（打包成zip即可发布）

### 2. installer（安装程序）
```bash
./gradlew jpackage -P package=installer
```
- **Windows**: 生成 `.msi` 安装程序
- **Linux**: 生成 `.deb` 或 `.rpm` 安装包
- **macOS**: 生成 `.pkg` 安装程序
- 适合需要系统安装的场景

### 3. 特定格式
```bash
# Windows MSI安装程序
./gradlew jpackage -P package=msi

# macOS PKG安装程序
./gradlew jpackage -P package=pkg

# macOS DMG镜像
./gradlew jpackage -P package=dmg
```

## Windows快速打包（推荐）

### 使用批处理脚本

项目根目录提供了 `package-portable.bat` 脚本，一键完成打包：

```cmd
package-portable.bat
```

脚本会自动：
1. 清理之前的构建
2. 编译整个项目（包括pathscope扩展）
3. 创建portable应用程序包
4. 压缩成zip文件
5. 在文件资源管理器中显示结果

### 手动打包步骤

如果不使用脚本，可以手动执行：

```cmd
# 1. 清理并编译项目
gradlew.bat clean build

# 2. 创建portable应用程序包
gradlew.bat jpackage -P package=image

# 3. 打包结果位于
# build\dist\QuPath-<version>\
```

## Linux打包

```bash
# 创建portable版本（会自动打包成tar.xz）
./gradlew clean build jpackage -P package=image

# 结果：build/dist/QuPath-v<version>-Linux.tar.xz
```

## macOS打包

```bash
# 创建.app包
./gradlew clean build jpackage -P package=image

# 创建.pkg安装程序
./gradlew clean build jpackage -P package=pkg

# 结果：
# build/dist/QuPath-<version>-arm64.app  (Apple Silicon)
# build/dist/QuPath-<version>-x64.app    (Intel)
```

## 输出目录

所有打包结果都在 `build/dist/` 目录：

```
build/dist/
├── QuPath-<version>/              # Windows portable应用目录
│   ├── bin/
│   │   └── QuPath.exe            # 主程序
│   ├── lib/                      # 所有JAR文件（包括pathscope）
│   └── runtime/                  # 内置Java运行时
├── QuPath-<version>.zip          # 压缩后的portable包（需手动创建或用脚本）
└── checksums/                    # SHA512校验和（如果生成）
```

## 验证pathscope扩展已包含

打包完成后，检查扩展是否包含：

1. 打开 `build/dist/QuPath-<version>/lib/` 目录
2. 查找 `qupath-extension-pathscope-<version>.jar`
3. 该文件存在即表示扩展已包含

或者运行打包后的程序：
1. 双击 `QuPath.exe`
2. 菜单栏：Extensions → Installed Extensions
3. 应该能看到 "PathScope" 扩展

## 发布流程建议

### Windows Portable版本

1. 运行打包脚本：
   ```cmd
   package-portable.bat
   ```

2. 得到 `QuPath-<version>-portable.zip`

3. 发布zip文件，用户下载后：
   - 解压到任意目录
   - 运行 `QuPath.exe`
   - 无需安装，直接使用

### 完整发布（多平台）

参考GitHub Actions工作流 `.github/workflows/jpackage.yml`：
- 在不同平台上自动构建
- 生成Windows MSI、Linux tar.xz、macOS PKG
- 自动计算校验和

## 注意事项

### Java版本
- 打包后的程序**包含完整Java 25运行时**
- 用户无需安装Java
- 打包的程序可以在**任何Windows 10/11**上运行
- **解决了Java 21/25兼容性问题** - 因为运行时已内置

### 文件大小
- Portable包约300-500 MB（包含Java运行时和所有依赖）
- 如果需要减小体积，可以：
  - 使用 `--compress zip-9` 选项（已在配置中启用）
  - 只包含必要的Java模块（已优化）

### Windows Defender
- 首次运行时Windows Defender可能会扫描
- 建议对发布的exe进行代码签名（需要证书）

### macOS代码签名
- macOS发布需要Apple Developer账号进行签名
- 未签名的app在macOS上会提示"来自未知开发者"

## 故障排除

### 问题1：gradlew命令找不到
```cmd
# 确保在项目根目录执行命令
cd f:\bingli\qupath
```

### 问题2：Java版本错误
```cmd
# 检查Java版本（需要Java 21或更高）
java --version

# 如果版本不对，设置JAVA_HOME
set JAVA_HOME=E:\programs\Java\jdk-21
```

### 问题3：内存不足
```cmd
# 增加Gradle内存
set GRADLE_OPTS=-Xmx4g
```

### 问题4：依赖下载失败
```cmd
# 清理并重试
gradlew.bat clean --refresh-dependencies
gradlew.bat jpackage -P package=image
```

## 高级选项

### 生成校验和
```cmd
gradlew.bat jpackage -P package=image createChecksums
```

### 包含Git提交信息
```cmd
gradlew.bat jpackage -P package=image -P git-commit=true
```

### 禁用Gradle守护进程（如遇到问题）
```cmd
gradlew.bat jpackage -P package=image --no-daemon
```

## 参考文档

- [QuPath jpackage配置](buildSrc/src/main/kotlin/qupath.jpackage-conventions.gradle.kts)
- [GitHub Actions打包工作流](.github/workflows/jpackage.yml)
- [Badass Runtime Plugin文档](https://badass-runtime-plugin.beryx.org/)
- [jpackage官方文档](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jpackage.html)
