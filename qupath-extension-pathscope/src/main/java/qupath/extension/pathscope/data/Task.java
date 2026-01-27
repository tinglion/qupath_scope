package qupath.extension.pathscope.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务类，存储任务信息
 */
public class Task {

    private String id;
    private String name;
    private String description;
    private Project project;
    private int numWsi;
    private int progress;
    private int status;
    private List<TaskFile> files;

    /**
     * 初始化任务对象
     */
    public Task(String id, String name, String description, Project project, int numWsi, int progress, int status, List<TaskFile> files) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.project = project;
        this.numWsi = numWsi;
        this.progress = progress;
        this.status = status;
        this.files = files != null ? files : new ArrayList<>();
    }

    /**
     * 从JsonObject创建任务对象
     */
    public static Task fromJson(JsonObject json, CacheManager cacheManager) {
        String id = json.has("id") && !json.get("id").isJsonNull() ? json.get("id").getAsString() : "";
        String name = json.has("name") && !json.get("name").isJsonNull() ? json.get("name").getAsString() : "";
        String description = json.has("description") && !json.get("description").isJsonNull() ? json.get("description").getAsString() : "";
        Project project = null;
        
        if (json.has("project_obj") && !json.get("project_obj").isJsonNull()) {
            project = Project.fromJson(json.getAsJsonObject("project_obj"));
        }
        
        int numWsi = json.has("num_wsi") && !json.get("num_wsi").isJsonNull() ? json.get("num_wsi").getAsInt() : 0;
        int progress = json.has("progress") && !json.get("progress").isJsonNull() ? json.get("progress").getAsInt() : 0;
        int status = json.has("status") && !json.get("status").isJsonNull() ? json.get("status").getAsInt() : 0;
        List<TaskFile> files = new ArrayList<>();
        
        if (json.has("files") && !json.get("files").isJsonNull()) {
            JsonArray filesArray = json.getAsJsonArray("files");
            for (int i = 0; i < filesArray.size(); i++) {
                if (!filesArray.get(i).isJsonNull()) {
                    TaskFile taskFile = TaskFile.fromJson(filesArray.get(i).getAsJsonObject(), null, cacheManager);
                    files.add(taskFile);
                }
            }
        }

        Task task = new Task(id, name, description, project, numWsi, progress, status, files);
        
        // 为每个文件设置task引用
        for (TaskFile file : task.files) {
            file.setTask(task);
        }
        
        return task;
    }

    /**
     * 统计文件的status_local的downloaded和annotated比例
     */
    public FileStatusRatio calculateFileStatusRatio() {
        int totalFiles = files.size();
        if (totalFiles == 0) {
            return new FileStatusRatio(0, 0.0, 0, 0.0, 0);
        }

        int downloadedCount = 0;
        int annotatedCount = 0;
        
        for (TaskFile file : files) {
            if ("downloaded".equals(file.getLocalStatus())) {
                downloadedCount++;
            } else if ("annotated".equals(file.getLocalStatus())) {
                annotatedCount++;
            }
        }

        double downloadedRatio = (double) downloadedCount / totalFiles;
        double annotatedRatio = (double) annotatedCount / totalFiles;

        return new FileStatusRatio(downloadedCount, downloadedRatio, annotatedCount, annotatedRatio, totalFiles);
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public int getNumWsi() {
        return numWsi;
    }

    public void setNumWsi(int numWsi) {
        this.numWsi = numWsi;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public List<TaskFile> getFiles() {
        return files;
    }

    public void setFiles(List<TaskFile> files) {
        this.files = files;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", numWsi=" + numWsi +
                ", progress=" + progress +
                ", status=" + status +
                '}';
    }

    /**
     * 文件状态比例类
     */
    public static class FileStatusRatio {
        private final int downloadedCount;
        private final double downloadedRatio;
        private final int annotatedCount;
        private final double annotatedRatio;
        private final int totalFiles;

        public FileStatusRatio(int downloadedCount, double downloadedRatio, int annotatedCount, double annotatedRatio, int totalFiles) {
            this.downloadedCount = downloadedCount;
            this.downloadedRatio = downloadedRatio;
            this.annotatedCount = annotatedCount;
            this.annotatedRatio = annotatedRatio;
            this.totalFiles = totalFiles;
        }

        public int getDownloadedCount() {
            return downloadedCount;
        }

        public double getDownloadedRatio() {
            return downloadedRatio;
        }

        public int getAnnotatedCount() {
            return annotatedCount;
        }

        public double getAnnotatedRatio() {
            return annotatedRatio;
        }

        public int getTotalFiles() {
            return totalFiles;
        }
    }
}
