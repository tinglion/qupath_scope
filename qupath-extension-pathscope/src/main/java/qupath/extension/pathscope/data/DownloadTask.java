package qupath.extension.pathscope.data;

import javafx.beans.property.*;

/**
 * 下载任务模型，用于跟踪WSI文件下载的状态和进度
 */
public class DownloadTask {

    public enum Status {
        WAITING("等待中"),
        DOWNLOADING("下载中"),
        COMPLETED("已完成"),
        FAILED("失败");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private final TaskFile taskFile;
    private final Task task;
    private final String savePath;

    private final ObjectProperty<Status> status = new SimpleObjectProperty<>(Status.WAITING);
    private final DoubleProperty progress = new SimpleDoubleProperty(-1.0); // -1 = indeterminate
    private final LongProperty downloadedBytes = new SimpleLongProperty(0);
    private final LongProperty totalBytes = new SimpleLongProperty(-1);
    private final StringProperty wsiName = new SimpleStringProperty();
    private final StringProperty taskName = new SimpleStringProperty();
    private final StringProperty errorMessage = new SimpleStringProperty();

    public DownloadTask(TaskFile taskFile, String savePath, Task task) {
        this.taskFile = taskFile;
        this.savePath = savePath;
        this.task = task;

        if (taskFile.getWsi() != null) {
            this.wsiName.set(taskFile.getWsi().getName());
        } else {
            this.wsiName.set(taskFile.getId());
        }
        if (task != null) {
            this.taskName.set(task.getName());
        }
    }

    public TaskFile getTaskFile() {
        return taskFile;
    }

    public Task getTask() {
        return task;
    }

    public String getSavePath() {
        return savePath;
    }

    // --- status ---
    public ObjectProperty<Status> statusProperty() {
        return status;
    }

    public Status getStatus() {
        return status.get();
    }

    public void setStatus(Status status) {
        this.status.set(status);
    }

    // --- progress ---
    public DoubleProperty progressProperty() {
        return progress;
    }

    public double getProgress() {
        return progress.get();
    }

    public void setProgress(double progress) {
        this.progress.set(progress);
    }

    // --- downloadedBytes ---
    public LongProperty downloadedBytesProperty() {
        return downloadedBytes;
    }

    public long getDownloadedBytes() {
        return downloadedBytes.get();
    }

    public void setDownloadedBytes(long bytes) {
        this.downloadedBytes.set(bytes);
    }

    // --- totalBytes ---
    public LongProperty totalBytesProperty() {
        return totalBytes;
    }

    public long getTotalBytes() {
        return totalBytes.get();
    }

    public void setTotalBytes(long bytes) {
        this.totalBytes.set(bytes);
    }

    // --- wsiName ---
    public StringProperty wsiNameProperty() {
        return wsiName;
    }

    public String getWsiName() {
        return wsiName.get();
    }

    // --- taskName ---
    public StringProperty taskNameProperty() {
        return taskName;
    }

    public String getTaskName() {
        return taskName.get();
    }

    // --- errorMessage ---
    public StringProperty errorMessageProperty() {
        return errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage.get();
    }

    public void setErrorMessage(String message) {
        this.errorMessage.set(message);
    }

    /**
     * 格式化已下载大小的显示文本
     */
    public static String formatBytes(long bytes) {
        if (bytes < 0) return "-";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
