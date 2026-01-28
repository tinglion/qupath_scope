#

## 运行项目

gradlew需要访问外网

```bash
gradlew clean --no-daemon
gradlew jpackage --no-daemon

gradlew :qupath-extension-pathscope:compileJava --no-daemon

gradlew run
```

## 插件打包发布

### 构建插件

```bash
# 构建整个项目
./gradlew build

# 仅构建pathscope插件
./gradlew :qupath-extension-pathscope:build

# 仅构建插件jar包（跳过测试和文档）
./gradlew :qupath-extension-pathscope:jar
```

### 解决Java版本兼容性问题

1. **重新构建插件**：
   ```bash
   # 构建插件，跳过守护进程
   ./gradlew :qupath-extension-pathscope:build --no-daemon
   
   # 或者只构建jar包（跳过测试和文档）
   ./gradlew :qupath-extension-pathscope:jar --no-daemon
   ```

2. **验证生成的jar包**：
   ```bash
   # 查看生成的jar包
   ls qupath-extension-pathscope/build/libs/
   ```

### 常见问题排查

1. **编译错误：未命名变量不支持**
   - 错误信息：`-source 21 中不支持 未命名变量`
   - 解决方案：将所有下划线`_`变量名替换为标准变量名

2. **编译错误：找不到ColorScheme类**
   - 错误信息：`找不到符号: 类 ColorScheme`
   - 解决方案：确保JavaFX版本至少为23.0.1

3. **构建缓慢或失败**
   - 确保网络连接稳定，能够访问所有依赖仓库
   - 尝试添加`--no-build-cache`参数清理构建缓存
   - 检查Gradle版本是否兼容（推荐使用8.5+）

4. **依赖冲突**
   - 错误信息：`Conflicts found for the following modules`
   - 解决方案：在`qupath.java-conventions.gradle.kts`中添加依赖强制约束

### 构建成功验证

构建成功后，您将看到类似输出：
```
BUILD SUCCESSFUL in Xm Ys
X actionable tasks: X executed, X up-to-date
```

生成的插件文件将位于 `qupath-extension-pathscope/build/libs/` 目录下：
- 主插件jar包：`qupath-extension-pathscope-0.7.0-SNAPSHOT.jar`
- 源码jar包：`qupath-extension-pathscope-0.7.0-SNAPSHOT-sources.jar`
- 文档jar包：`qupath-extension-pathscope-0.7.0-SNAPSHOT-javadoc.jar`

### 构建产物

构建成功后，插件文件生成在 `qupath-extension-pathscope/build/libs/` 目录下：
- 主插件jar包：`qupath-extension-pathscope-0.7.0-SNAPSHOT.jar`
- 源码jar包：`qupath-extension-pathscope-0.7.0-SNAPSHOT-sources.jar`
- 文档jar包：`qupath-extension-pathscope-0.7.0-SNAPSHOT-javadoc.jar`

### 发布方式

1. **本地使用**：将主jar包复制到QuPath的`extensions`目录下，重启QuPath即可加载
2. **远程发布**：项目已配置`qupath.publishing-conventions`插件，可发布到Maven仓库

### 在其他机器上安装插件

#### 步骤1：获取插件文件
从构建好的项目中获取插件主jar文件：
- 开发机器路径：`qupath-extension-pathscope/build/libs/qupath-extension-pathscope-0.7.0-SNAPSHOT.jar`
- 通过网络传输或U盘等方式将该文件复制到目标机器

#### 步骤2：找到QuPath插件目录
在目标机器上找到QuPath的`extensions`目录：
- **Windows**：`C:\Users\您的用户名\AppData\Roaming\QuPath\extensions`
- **macOS**：`~/Library/Application Support/QuPath/extensions`
- **Linux**：`~/.local/share/QuPath/extensions`

#### 步骤3：安装插件
将插件jar文件复制到上述`extensions`目录中

#### 步骤4：重启QuPath
关闭并重新启动QuPath软件，插件会自动加载

#### 步骤5：验证插件安装
- 查看菜单栏中是否出现插件相关选项
- 检查日志输出，确认插件加载成功
- 尝试使用插件提供的功能（如配置对话框）

#### 注意事项
- 确保插件版本与QuPath软件版本兼容
- 避免安装多个冲突的插件版本
- 如插件无法加载，可检查QuPath日志获取详细错误信息

## 参考链接

Combining Cellpose and StarDist detections into cells
https://forum.image.sc/t/combining-cellpose-and-stardist-detections-into-cells/78225
