package qupath.extension.pathscope.data;

import com.google.gson.JsonObject;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 任务文件类，存储任务相关的文件信息
 */
public class TaskFile {

    private String id;
    private String type;
    private String taskId;
    private Wsi wsi;
    private String localPath;
    private boolean annotated;
    private Map<String, Object> annotation;
    private StringProperty localStatus;
    private Task task;
    private CacheManager cacheManager;

    /**
     * 初始化任务文件对象
     */
    public TaskFile(String id, String type, String taskId, Wsi wsi, String localPath, boolean annotated,
                    Map<String, Object> annotation, String localStatus, Task task, CacheManager cacheManager) {
        this.id = id;
        this.type = type;
        this.taskId = taskId;
        this.wsi = wsi;
        this.localPath = localPath;
        this.annotated = annotated;
        this.annotation = annotation != null ? annotation : new HashMap<>();
        this.localStatus = new SimpleStringProperty(localStatus != null ? localStatus : "default");
        this.task = task;
        this.cacheManager = cacheManager;
    }

    /**
     * 从JsonObject创建任务文件对象
     */
    public static TaskFile fromJson(JsonObject json, Task task, CacheManager cacheManager) {
        String id = json.has("id") && !json.get("id").isJsonNull() ? json.get("id").getAsString() : "";
        String type = json.has("type") && !json.get("type").isJsonNull() ? json.get("type").getAsString() : "";
        String taskId = json.has("task_id") && !json.get("task_id").isJsonNull() ? json.get("task_id").getAsString() : "";
        Wsi wsi = null;
        
        if (json.has("project_wsi_obj") && !json.get("project_wsi_obj").isJsonNull()) {
            wsi = Wsi.fromSrc(json.getAsJsonObject("project_wsi_obj"));
        } else if (json.has("wsi") && !json.get("wsi").isJsonNull()) {
            wsi = Wsi.fromJson(json.getAsJsonObject("wsi"));
        }
        
        String localPath = json.has("local_path") && !json.get("local_path").isJsonNull() ? json.get("local_path").getAsString() : "";
        boolean annotated = json.has("annotated") && !json.get("annotated").isJsonNull() ? json.get("annotated").getAsBoolean() : false;
        Map<String, Object> annotation = new HashMap<>();
        if (json.has("annotation") && !json.get("annotation").isJsonNull()) {
            // 这里需要根据实际的JSON结构来解析annotation
        }
        
        // 确定localStatus值
        String localStatus = "default";
        // 检查是否有本地状态字段（从缓存加载时）
        if (json.has("status_local") && !json.get("status_local").isJsonNull()) {
            // 从缓存加载时，使用缓存中的本地状态
            localStatus = json.get("status_local").getAsString();
        } else if (json.has("local_status") && !json.get("local_status").isJsonNull()) {
            // 兼容旧缓存格式
            localStatus = json.get("local_status").getAsString();
        }
        // 从接口获取信息时，不使用接口返回的local_status，保持默认值"default"
        // 这样可以确保本地状态只在本地操作时更新，不受接口数据影响

        return new TaskFile(id, type, taskId, wsi, localPath, annotated, annotation, localStatus, task, cacheManager);
    }

    /**
     * 获取标注数据
     */
    public Map<String, Object> getAnnotation() {
        return annotation;
    }

    /**
     * 设置标注数据，并自动更新缓存
     */
    public void setAnnotation(Map<String, Object> annotation) {
        this.annotation = annotation != null ? annotation : new HashMap<>();

        // 自动更新对应task的缓存
        if (this.task != null && this.cacheManager != null) {
            try {
                this.cacheManager.saveTask(this.task);
            } catch (Exception e) {
                // 记录错误
            }
        }
    }

    /**
     * 获取本地文件状态
     */
    public String getLocalStatus() {
        return localStatus.get();
    }

    /**
     * 获取本地文件状态属性
     */
    public StringProperty localStatusProperty() {
        return localStatus;
    }

    /**
     * 设置本地文件状态，并更新对应task的缓存
     */
    public boolean setLocalStatus(String status) {
        return setLocalStatus(status, true);
    }

    /**
     * 设置本地文件状态
     *
     * @param status          新状态
     * @param updateCache     是否更新缓存
     * @return 是否设置成功
     */
    public boolean setLocalStatus(String status, boolean updateCache) {
        String[] validStatuses = {"default", "downloading", "downloaded", "annotated"};
        boolean isValid = false;
        for (String validStatus : validStatuses) {
            if (validStatus.equals(status)) {
                isValid = true;
                break;
            }
        }

        if (!isValid) {
            return false;
        }

        this.localStatus.set(status);

        // 如果需要更新缓存，使用高效的单个 WSI 更新方法
        if (updateCache) {
            if (this.taskId != null && this.id != null && this.cacheManager != null) {
                return this.cacheManager.updateWsiLocalStatus(this.taskId, this.id, status);
            } else if (this.task != null && this.id != null && this.cacheManager != null) {
                // 如果没有 taskId，但有 task 对象，从 task 获取 ID
                return this.cacheManager.updateWsiLocalStatus(this.task.getId(), this.id, status);
            }
        }

        return true;
    }

    /**
     * 检查文件是否已下载到本地（文件实际存在）
     */
    public boolean isDownloaded() {
        if (localPath == null) {
            return false;
        }
        File file = new File(localPath);
        return file.exists();
    }

    /**
     * 删除本地文件
     */
    public boolean deleteLocalFile() {
        if (localPath == null) {
            return false;
        }

        File file = new File(localPath);
        if (!file.exists()) {
            // 即使文件不存在，也重置状态
            this.localPath = null;
            this.setLocalStatus("default");
            return true;
        }

        try {
            if (file.delete()) {
                // 重置本地路径和状态
                this.localPath = null;
                this.setLocalStatus("default");
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            // 记录错误
            return false;
        }
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Wsi getWsi() {
        return wsi;
    }

    public void setWsi(Wsi wsi) {
        this.wsi = wsi;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        setLocalPath(localPath, true);
    }

    /**
     * 设置本地文件路径
     *
     * @param localPath   本地路径
     * @param updateCache 是否更新缓存
     */
    public void setLocalPath(String localPath, boolean updateCache) {
        this.localPath = localPath;

        // 如果需要更新缓存，自动更新缓存中的本地路径
        if (updateCache) {
            if (this.taskId != null && this.id != null && this.cacheManager != null) {
                this.cacheManager.updateWsiLocalPath(this.taskId, this.id, localPath);
            } else if (this.task != null && this.id != null && this.cacheManager != null) {
                this.cacheManager.updateWsiLocalPath(this.task.getId(), this.id, localPath);
            }
        }
    }

    public boolean isAnnotated() {
        return annotated;
    }

    public void setAnnotated(boolean annotated) {
        this.annotated = annotated;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public String toString() {
        return "TaskFile{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", wsi=" + wsi +
                ", localStatus='" + localStatus + '\'' +
                '}';
    }
}
