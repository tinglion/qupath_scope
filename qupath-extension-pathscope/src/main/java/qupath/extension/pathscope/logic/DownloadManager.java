package qupath.extension.pathscope.logic;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.extension.pathscope.data.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 下载管理器，管理所有WSI下载任务的队列和并发执行
 */
public class DownloadManager {

    private static final Logger logger = LoggerFactory.getLogger(DownloadManager.class);

    private static DownloadManager instance;

    private final ApiClient apiClient;
    private final CacheManager cacheManager;
    private final ObservableList<DownloadTask> downloadTasks = FXCollections.observableArrayList();
    private ExecutorService executorService;
    private int maxConcurrent;

    private DownloadManager(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.cacheManager = new CacheManager();
        this.maxConcurrent = Configuration.getInstance().getMaxConcurrentDownloads();
        this.executorService = Executors.newFixedThreadPool(maxConcurrent);
    }

    /**
     * 初始化单例（首次调用时需要传入ApiClient）
     */
    public static synchronized DownloadManager init(ApiClient apiClient) {
        if (instance == null) {
            instance = new DownloadManager(apiClient);
        }
        return instance;
    }

    /**
     * 获取单例
     */
    public static synchronized DownloadManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("DownloadManager not initialized. Call init(apiClient) first.");
        }
        return instance;
    }

    /**
     * 获取下载任务列表（用于UI绑定）
     */
    public ObservableList<DownloadTask> getDownloadTasks() {
        return downloadTasks;
    }

    /**
     * 添加下载任务
     * @return 如果任务已存在返回false，否则返回true
     */
    public boolean addDownload(TaskFile taskFile, String savePath, Task task) {
        // 检查是否已在下载列表中
        for (DownloadTask dt : downloadTasks) {
            if (dt.getTaskFile().getId().equals(taskFile.getId())) {
                DownloadTask.Status status = dt.getStatus();
                if (status == DownloadTask.Status.WAITING || status == DownloadTask.Status.DOWNLOADING) {
                    logger.info("Download task already exists for WSI: {}", taskFile.getId());
                    return false;
                }
            }
        }

        DownloadTask downloadTask = new DownloadTask(taskFile, savePath, task);
        Platform.runLater(() -> downloadTasks.add(downloadTask));

        taskFile.setLocalStatus("downloading");

        executorService.submit(() -> executeDownload(downloadTask));
        return true;
    }

    private void executeDownload(DownloadTask downloadTask) {
        Platform.runLater(() -> downloadTask.setStatus(DownloadTask.Status.DOWNLOADING));

        try {
            boolean success = apiClient.downloadWsiWithProgress(
                    downloadTask.getTaskFile().getWsi().getDownloadUrl(),
                    downloadTask.getSavePath(),
                    (bytesRead, totalBytes) -> {
                        Platform.runLater(() -> {
                            downloadTask.setDownloadedBytes(bytesRead);
                            downloadTask.setTotalBytes(totalBytes);
                            if (totalBytes > 0) {
                                downloadTask.setProgress((double) bytesRead / totalBytes);
                            }
                        });
                    }
            );

            Platform.runLater(() -> {
                if (success) {
                    downloadTask.setStatus(DownloadTask.Status.COMPLETED);
                    downloadTask.setProgress(1.0);
                    downloadTask.getTaskFile().setLocalPath(downloadTask.getSavePath());
                    downloadTask.getTaskFile().setLocalStatus("downloaded");
                    saveTaskCache(downloadTask);
                } else {
                    downloadTask.setStatus(DownloadTask.Status.FAILED);
                    downloadTask.setErrorMessage("下载返回失败");
                    downloadTask.getTaskFile().setLocalStatus("default");
                }
            });
        } catch (IOException e) {
            logger.error("Failed to download WSI: {}", e.getMessage());
            Platform.runLater(() -> {
                downloadTask.setStatus(DownloadTask.Status.FAILED);
                downloadTask.setErrorMessage(e.getMessage());
                downloadTask.getTaskFile().setLocalStatus("default");
            });
        }
    }

    private void saveTaskCache(DownloadTask downloadTask) {
        try {
            Task task = downloadTask.getTask();
            if (task != null) {
                cacheManager.saveTask(task);
            }
        } catch (IOException e) {
            logger.error("Failed to save task cache: {}", e.getMessage());
        }
    }

    /**
     * 清除已完成和失败的任务
     */
    public void clearFinished() {
        Platform.runLater(() -> {
            downloadTasks.removeIf(dt ->
                    dt.getStatus() == DownloadTask.Status.COMPLETED ||
                    dt.getStatus() == DownloadTask.Status.FAILED);
        });
    }

    /**
     * 关闭下载管理器
     */
    public void shutdown() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }
}
