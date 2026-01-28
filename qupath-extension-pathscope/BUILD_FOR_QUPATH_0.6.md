# 为 QuPath 0.6 构建插件

## 问题说明

QuPath 0.6 使用 Java 21 运行环境，但当前开发环境使用 Java 25 编译插件。编译出的 JAR 文件包含 Java 25 字节码（class file version 69.0），无法在 Java 21 环境中运行。

## 解决方案

### 方案1：使用 Java 21 编译（推荐）

1. **下载并安装 JDK 21**
   - 从 [Oracle](https://www.oracle.com/java/technologies/downloads/#java21) 或 [Adoptium](https://adoptium.net/) 下载 JDK 21

2. **设置环境变量**
   ```bash
   # Windows (PowerShell)
   $env:JAVA_HOME="C:\Path\To\JDK21"
   $env:PATH="$env:JAVA_HOME\bin;$env:PATH"

   # Linux/Mac
   export JAVA_HOME=/path/to/jdk21
   export PATH=$JAVA_HOME/bin:$PATH
   ```

3. **清理并重新编译**
   ```bash
   cd qupath-extension-pathscope
   ../gradlew clean fatJar
   ```

4. **使用生成的 JAR**
   - JAR 文件位于: `qupath-extension-pathscope/build/libs/qupath-extension-pathscope-0.7.0-SNAPSHOT-all.jar`
   - 将此文件复制到 QuPath 0.6 的 extensions 目录

### 方案2：字节码降级（高级）

使用 ASM 或其他工具将现有 JAR 的字节码从版本 69 降级到 65：

```bash
# 使用 Recaf (图形界面工具)
# 或使用命令行工具
java -jar asm-downgrade-tool.jar \
  --input qupath-extension-pathscope-0.7.0-SNAPSHOT-all.jar \
  --output qupath-extension-pathscope-0.7.0-SNAPSHOT-all-java21.jar \
  --target 21
```

## 验证 JAR 版本

检查 class 文件版本：
```bash
javap -v ApiIntegrationExtension.class | grep "major version"
# Java 21 = major version 65
# Java 25 = major version 69
```

## 当前状态

已完成的修改：
- ✅ 统一使用 all_wsi.json 缓存文件
- ✅ 保存 TaskFile.status 字段到缓存
- ✅ 移除 wsi_list.json 重复缓存文件
- ✅ 优化 task 选择和更新逻辑
- ⚠️ 需要用 Java 21 重新编译才能在 QuPath 0.6 上运行
