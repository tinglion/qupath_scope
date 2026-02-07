# QuPath PathScope 插件部署指南

## 概述

本文档提供了 QuPath PathScope 插件的详细部署指南，包括统一包（fat JAR）的使用方法和常见问题解决方案。

## 什么是统一包（Fat JAR）

统一包（Fat JAR）是一个包含了插件本身以及所有必需外部依赖的单个 JAR 文件。使用统一包可以大大简化插件部署过程，避免因缺少依赖导致的各种错误。

### 统一包包含的内容

- ✅ PathScope 插件核心代码
- ✅ OkHttp3（HTTP 客户端库）
- ✅ Gson（JSON 解析库）
- ✅ Kotlin 标准库
- ✅ Okio（IO 库）

## 部署步骤

### 方法一：使用统一包部署（推荐）

#### 1. 获取统一包

从构建好的项目中获取统一包文件：
- **文件路径**：`qupath-extension-pathscope/build/libs/qupath-extension-pathscope-0.7.1-all.jar`
- **文件大小**：约 149 MB（包含所有依赖）

#### 2. 找到 QuPath 插件目录

在目标机器上找到 QuPath 的 `extensions` 目录：

- **Windows**：`C:\Users\您的用户名\AppData\Roaming\QuPath\extensions`
- **macOS**：`~/Library/Application Support/QuPath/extensions`
- **Linux**：`~/.local/share/QuPath/extensions`

#### 3. 安装插件

将统一包 JAR 文件复制到上述 `extensions` 目录中。

#### 4. 重启 QuPath

关闭并重新启动 QuPath 软件，插件会自动加载。

#### 5. 验证安装

- 查看菜单栏中是否出现插件相关选项
- 检查日志输出，确认插件加载成功
- 尝试使用插件提供的功能

### 方法二：手动部署（不推荐）

如果您需要手动部署，需要同时复制插件 JAR 和所有依赖 JAR：

1. 复制插件 JAR：`qupath-extension-pathscope-0.7.1.jar`
2. 复制依赖 JAR：
   - `okhttp-4.12.0.jar`
   - `okio-3.4.0.jar`
   - `kotlin-stdlib-1.9.20.jar`
   - `kotlin-stdlib-common-1.9.20.jar`
   - `gson-2.13.2.jar`
3. 将所有 JAR 文件复制到 QuPath 的 `extensions` 目录
4. 重启 QuPath

## 构建统一包

如果您需要自己构建统一包，可以使用以下命令：

```bash
# 构建包含所有依赖的统一包
./gradlew :qupath-extension-pathscope:build --no-daemon
```

构建完成后，统一包将位于：
```
qupath-extension-pathscope/build/libs/qupath-extension-pathscope-0.7.1-all.jar
```

## 优势

使用统一包部署具有以下优势：

- **简化部署**：只需复制一个文件
- **减少错误**：避免因缺少依赖导致的 `NoClassDefFoundError`
- **版本一致性**：确保所有依赖版本兼容
- **跨平台兼容**：适用于所有 QuPath 支持的操作系统
- **易于管理**：方便版本控制和更新

## 常见问题

### 1. 插件加载失败，提示 `NoClassDefFoundError: okhttp3/OkHttpClient`

**解决方案**：
- 确保使用统一包（`-all.jar`）进行部署
- 检查是否将 JAR 文件复制到了正确的 `extensions` 目录
- 重启 QuPath 后再次尝试

### 2. 插件加载成功但功能无法使用

**解决方案**：
- 检查 QuPath 日志获取详细错误信息
- 确保 QuPath 版本与插件兼容
- 尝试重新构建统一包

### 3. 构建统一包失败

**解决方案**：
- 确保网络连接正常，能够访问 Maven 仓库
- 检查 Gradle 版本是否兼容（推荐使用 8.5+）
- 尝试清理构建缓存：`./gradlew clean --no-daemon`

## 验证

部署完成后，您可以通过以下方式验证插件是否正常工作：

1. **启动 QuPath**
2. **检查菜单栏**：查看菜单栏中是否出现 PathScope 相关选项
3. **检查工具栏**：查看工具栏中是否出现 PathScope 按钮（带有"PathScope"文字或图标）
4. **检查功能**：点击工具栏或菜单中的 PathScope 选项，打开插件功能
5. **验证登录**：尝试登录 PathScope API，检查是否能正常连接
6. **验证任务列表**：登录成功后，检查是否能正常获取任务列表

## 工具栏按钮说明

插件已配置为在 QuPath 工具栏上显示 PathScope 按钮，便于快速访问插件功能。

### 工具栏按钮功能
- **PathScope 按钮**：点击后会检查登录状态，未登录则显示登录对话框，登录成功后自动打开任务列表
- **按钮位置**：位于工具栏的右侧区域
- **提示信息**：鼠标悬停时显示提示"PathScope Integration"
- **文字显示**：按钮显示完整的"PathScope"文字，便于识别

### 工具栏按钮优势
- ✅ **快速访问**：无需通过多层菜单查找
- ✅ **直观识别**：明确的文字标识，易于识别
- ✅ **统一体验**：与其他 QuPath 功能保持一致的操作方式
- ✅ **减少点击**：一步直达插件功能，提高工作效率

## 版本信息

- 插件版本：0.7.1
- 支持 QuPath 版本：0.6.0+
- Java 版本要求：Java 21+

## 联系方式

如果您在部署过程中遇到任何问题，请联系开发团队获取支持。

---

**更新日期**：2026-01-26
**文档版本**：1.0
