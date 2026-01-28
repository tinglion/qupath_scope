package qupath.extension.pathscope.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.images.servers.ImageServer;
import java.awt.image.BufferedImage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import qupath.extension.pathscope.data.Task;
import qupath.extension.pathscope.data.TaskFile;
import qupath.extension.pathscope.data.Wsi;
import qupath.extension.pathscope.data.Project;
import qupath.extension.pathscope.data.CacheManager;
import qupath.extension.pathscope.logic.ApiClient;
import qupath.extension.pathscope.logic.ApiClient.TaskListResult;
import qupath.extension.pathscope.logic.ApiClient.WsiListResult;

/**
 * 任务列表对话框，用于显示任务列表和WSI列表
 */
public class TaskListDialog extends Stage {

    private static final Logger logger = LoggerFactory.getLogger(TaskListDialog.class);

    private final QuPathGUI qupath;
    private final ApiClient apiClient;
    private final CacheManager cacheManager;

    private TableView<Task> taskTable;
    private TableView<TaskFile> wsiTable;
    private ObservableList<Task> tasks;
    private ObservableList<TaskFile> taskFiles;
    private Task selectedTask;

    // 分页相关变量
    private int taskCurrentPage = 1;
    private int taskPageSize = 10;
    private int taskTotalPages = 1;
    private int wsiCurrentPage = 1;
    private int wsiPageSize = 10;
    private int wsiTotalPages = 1;

    // 分页控件
    private Pagination taskPagination;
    private Pagination wsiPagination;

    // Total显示标签
    private Label taskTotalLabel;
    private Label wsiTotalLabel;

    public TaskListDialog(QuPathGUI qupath, ApiClient apiClient) {
        this.qupath = qupath;
        this.apiClient = apiClient;
        this.cacheManager = new CacheManager();

        initModality(Modality.NONE); // 设置为非模态，不阻塞主窗口
        setTitle("PathScope Task List");
        setWidth(1000);
        setHeight(700);

        createUI();
        loadTasks();
    }

    private void createUI() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Create task table
        taskTable = new TableView<>();
        tasks = FXCollections.observableArrayList();
        taskTable.setItems(tasks);
        taskTable.setFixedCellSize(25);
        taskTable.setMaxHeight(10 * 25 + 30); // 10 rows + header

        TableColumn<Task, String> taskIdColumn = new TableColumn<>("Task ID");
        taskIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        taskIdColumn.setPrefWidth(100);

        TableColumn<Task, String> taskNameColumn = new TableColumn<>("Task Name");
        taskNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        taskNameColumn.setPrefWidth(200);

        TableColumn<Task, String> projectColumn = new TableColumn<>("Project");
        projectColumn.setCellValueFactory(cellData -> {
            Project project = cellData.getValue().getProject();
            return project != null ? new SimpleStringProperty(project.getName()) : null;
        });
        projectColumn.setPrefWidth(150);

        TableColumn<Task, Integer> numWsiColumn = new TableColumn<>("WSI Count");
        numWsiColumn.setCellValueFactory(new PropertyValueFactory<>("numWsi"));
        numWsiColumn.setPrefWidth(100);

        TableColumn<Task, Integer> progressColumn = new TableColumn<>("Progress");
        progressColumn.setCellValueFactory(new PropertyValueFactory<>("progress"));
        progressColumn.setPrefWidth(100);

        taskTable.getColumns().addAll(taskIdColumn, taskNameColumn, projectColumn, numWsiColumn, progressColumn);

        // Create WSI table
        wsiTable = new TableView<>();
        taskFiles = FXCollections.observableArrayList();
        wsiTable.setItems(taskFiles);
        wsiTable.setFixedCellSize(25);
        wsiTable.setMaxHeight(10 * 25 + 30); // 10 rows + header

        TableColumn<TaskFile, String> wsiIdColumn = new TableColumn<>("WSI ID");
        wsiIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        wsiIdColumn.setPrefWidth(100);

        TableColumn<TaskFile, String> wsiNameColumn = new TableColumn<>("WSI Name");
        wsiNameColumn.setCellValueFactory(cellData -> {
            Wsi wsi = cellData.getValue().getWsi();
            return wsi != null ? new SimpleStringProperty(wsi.getName()) : null;
        });
        wsiNameColumn.setPrefWidth(200);

        TableColumn<TaskFile, String> ptypeColumn = new TableColumn<>("PType");
        ptypeColumn.setCellValueFactory(cellData -> {
            Wsi wsi = cellData.getValue().getWsi();
            return wsi != null ? new SimpleStringProperty(wsi.getPtype()) : null;
        });
        ptypeColumn.setPrefWidth(100);

        TableColumn<TaskFile, Long> sizeColumn = new TableColumn<>("Size");
        sizeColumn.setCellValueFactory(cellData -> {
            Wsi wsi = cellData.getValue().getWsi();
            return wsi != null ? new javafx.beans.property.SimpleLongProperty(wsi.getSize()).asObject() : null;
        });
        sizeColumn.setPrefWidth(100);

        TableColumn<TaskFile, Integer> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getStatus()).asObject()
        );
        statusColumn.setPrefWidth(80);

        TableColumn<TaskFile, String> localStatusColumn = new TableColumn<>("Local Status");
        localStatusColumn.setCellValueFactory(cellData -> cellData.getValue().localStatusProperty());
        localStatusColumn.setPrefWidth(150);

        TableColumn<TaskFile, String> localPathColumn = new TableColumn<>("Local Path");
        localPathColumn.setCellValueFactory(new PropertyValueFactory<>("localPath"));
        localPathColumn.setPrefWidth(200);

        wsiTable.getColumns().addAll(wsiIdColumn, wsiNameColumn, statusColumn, localStatusColumn, sizeColumn,
                ptypeColumn, localPathColumn);

        // Create buttons
        Button updateTasksButton = new Button("Update Tasks");
        updateTasksButton.setOnAction(e -> loadTasks(true)); // 强制刷新，从接口获取最新数据

        Button updateTaskButton = new Button("Update Task");
        updateTaskButton.setOnAction(e -> {
            if (selectedTask != null) {
                updateSingleTask(selectedTask.getId()); // 更新task信息和WSI列表
            }
        });

        Button updateWsiButton = new Button("Update WSI");
        updateWsiButton.setOnAction(e -> {
            if (selectedTask != null) {
                loadTaskWsiList(selectedTask.getId(), true); // 强制刷新，从接口获取最新数据
            }
        });

        Button downloadButton = new Button("Download WSI");
        downloadButton.setOnAction(e -> downloadSelectedWsi());

        Button annotateButton = new Button("Annotate");
        annotateButton.setOnAction(e -> annotateSelectedWsi());

        Button submitButton = new Button("Submit Task");
        submitButton.setOnAction(e -> submitSelectedTask());

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.getChildren().addAll(updateTasksButton, updateTaskButton, updateWsiButton, downloadButton,
                annotateButton, submitButton);

        // Create task pagination
        taskPagination = new Pagination();
        taskPagination.setPageCount(1);
        taskPagination.setCurrentPageIndex(0);
        taskPagination.setMaxPageIndicatorCount(5);
        taskPagination.currentPageIndexProperty().addListener((obs, oldPage, newPage) -> {
            taskCurrentPage = newPage.intValue() + 1;
            loadTasks();
        });

        // Create wsi pagination
        wsiPagination = new Pagination();
        wsiPagination.setPageCount(1);
        wsiPagination.setCurrentPageIndex(0);
        wsiPagination.setMaxPageIndicatorCount(5);
        wsiPagination.currentPageIndexProperty().addListener((obs, oldPage, newPage) -> {
            wsiCurrentPage = newPage.intValue() + 1;
            if (selectedTask != null) {
                loadTaskWsiList(selectedTask.getId());
            }
        });

        // Create Total labels
        taskTotalLabel = new Label("Total: 0");
        wsiTotalLabel = new Label("Total: 0");

        // Create page size combo box for WSI
        ComboBox<Integer> wsiPageSizeComboBox = new ComboBox<>();
        wsiPageSizeComboBox.getItems().addAll(10, 20, 50, 100);
        wsiPageSizeComboBox.setValue(wsiPageSize);
        wsiPageSizeComboBox.setOnAction(e -> {
            int newPageSize = wsiPageSizeComboBox.getValue();
            if (newPageSize != wsiPageSize) {
                wsiPageSize = newPageSize;
                wsiCurrentPage = 1; // 重置到第一页
                if (selectedTask != null) {
                    loadTaskWsiList(selectedTask.getId());
                }
            }
        });

        // Create layout
        VBox taskBox = new VBox(10);
        HBox taskHeaderBox = new HBox(10);
        taskHeaderBox.getChildren().addAll(new Label("Tasks"), taskTotalLabel);
        taskBox.getChildren().addAll(taskHeaderBox, taskTable, taskPagination);

        VBox wsiBox = new VBox(10);
        HBox wsiHeaderBox = new HBox(10);
        wsiHeaderBox.getChildren().addAll(new Label("WSI Files"), wsiTotalLabel, new Label("Page Size:"), wsiPageSizeComboBox);
        wsiBox.getChildren().addAll(wsiHeaderBox, wsiTable, wsiPagination);

        BorderPane centerPane = new BorderPane();
        centerPane.setTop(taskBox);
        centerPane.setCenter(wsiBox);

        root.setCenter(centerPane);
        root.setBottom(buttonBox);

        // Add selection listener to task table
        taskTable.getSelectionModel().selectedItemProperty().addListener((obs, oldTask, newTask) -> {
            if (newTask != null) {
                selectedTask = newTask;
                // 切换task时重置到第一页
                wsiCurrentPage = 1;
                loadTaskWsiList(newTask.getId());
            }
        });

        Scene scene = new Scene(root);
        setScene(scene);
    }

    private void loadTasks() {
        loadTasks(false);
    }

    /**
     * 加载任务列表
     * 
     * @param forceRefresh 是否强制从接口刷新
     */
    private void loadTasks(boolean forceRefresh) {
        // 这里需要从登录信息中获取用户ID，暂时使用默认值
        String userId = "1";

        new Thread(() -> {
            try {
                List<Task> allTasks = new ArrayList<>();
                int totalTasks = 0;

                if (!forceRefresh) {
                    // 先尝试从本地缓存读取全部任务
                    allTasks = cacheManager.loadAllTasks();
                    totalTasks = allTasks.size();

                    if (!allTasks.isEmpty()) {
                        // 缓存有数据，使用缓存数据并进行分页显示
                        logger.info("Loaded all tasks from cache: {} tasks", allTasks.size());
                        updateTaskDisplay(allTasks, totalTasks);
                        return;
                    }
                }

                // 缓存无数据或强制刷新，从接口获取全部数据
                logger.info("Loading all tasks from API");

                // 先获取第一页以得到total值
                TaskListResult firstPageResult = apiClient.getTaskList(userId, 1, 1);
                totalTasks = firstPageResult.getTotal();

                logger.info("Total tasks count from API: {}", totalTasks);

                // 根据total值一次性获取全部任务
                TaskListResult result = apiClient.getTaskList(userId, 1, totalTasks);
                allTasks = result.getItems();

                logger.info("API returned {} tasks, total: {}", allTasks.size(), totalTasks);

                // 保存全部任务到缓存
                cacheManager.saveAllTasks(allTasks);
                logger.info("Saved all tasks to cache: {} tasks", allTasks.size());

                // 更新显示
                updateTaskDisplay(allTasks, totalTasks);
            } catch (IOException e) {
                logger.error("Failed to load tasks: {}", e.getMessage());
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Failed to load tasks");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }

    /**
     * 更新任务列表显示（支持分页）
     */
    private void updateTaskDisplay(List<Task> allTasks, int total) {
        // 计算当前页的数据范围
        int startIndex = (taskCurrentPage - 1) * taskPageSize;
        int endIndex = Math.min(startIndex + taskPageSize, allTasks.size());
        
        // 获取当前页的数据
        List<Task> pageTasks = allTasks.subList(startIndex, endIndex);
        
        Platform.runLater(() -> {
            tasks.clear();
            tasks.addAll(pageTasks);
            // 计算总页数
            taskTotalPages = total > 0 ? (total + taskPageSize - 1) / taskPageSize : 1;
            taskPagination.setPageCount(taskTotalPages);
            taskPagination.setCurrentPageIndex(taskCurrentPage - 1);
            // 更新total显示
            updateTaskTotalDisplay(total);
        });
    }

    /**
     * 更新单个任务的信息和WSI列表
     * 从接口获取最新数据，合并本地信息后保存到缓存
     *
     * @param taskId 任务ID
     */
    private void updateSingleTask(String taskId) {
        new Thread(() -> {
            try {
                logger.info("Updating task {} with latest data from API", taskId);

                // 1. 从缓存加载所有 tasks
                List<Task> cachedTasks = cacheManager.loadAllTasks();

                // 2. 找到要更新的 task
                Task taskToUpdate = null;
                int taskIndex = -1;
                for (int i = 0; i < cachedTasks.size(); i++) {
                    if (cachedTasks.get(i).getId().equals(taskId)) {
                        taskToUpdate = cachedTasks.get(i);
                        taskIndex = i;
                        break;
                    }
                }

                if (taskToUpdate == null) {
                    logger.warn("Task {} not found in cache", taskId);
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Warning");
                        alert.setHeaderText("Task not found");
                        alert.setContentText("Cannot find task in the cached task list.");
                        alert.showAndWait();
                    });
                    return;
                }

                // 3. 更新 WSI 列表（从接口获取并合并本地信息）
                WsiListResult result = apiClient.getTaskWsiList(taskId, 1, 1);
                int totalWsi = result.getTotal();
                logger.info("Total WSI count from API: {}", totalWsi);

                WsiListResult fullResult = apiClient.getTaskWsiList(taskId, 1, totalWsi);
                List<TaskFile> updatedWsiList = fullResult.getItems();
                logger.info("API returned {} WSI items, total: {}", updatedWsiList.size(), totalWsi);

                // 合并本地状态（不修改 local_status, local_path 等）
                mergeLocalStatus(updatedWsiList, taskId);

                // 4. 保存 WSI 列表到缓存
                cacheManager.saveAllTaskWsi(taskId, updatedWsiList);
                logger.info("Saved updated WSI list to cache for task {}", taskId);

                // 5. 更新 task 对象的 files 字段
                taskToUpdate.setFiles(updatedWsiList);

                // 6. 保存更新后的 task 到缓存
                if (taskIndex >= 0) {
                    cachedTasks.set(taskIndex, taskToUpdate);
                }
                cacheManager.saveAllTasks(cachedTasks);
                logger.info("Saved updated task info to cache for task {}", taskId);

                // 7. 刷新显示（重置到第一页）
                wsiCurrentPage = 1;
                updateWsiDisplay(updatedWsiList, totalWsi);

                Platform.runLater(() -> {
                    logger.info("Task {} updated successfully", taskId);
                });

            } catch (Exception e) {
                logger.error("Failed to update task {}: {}", taskId, e.getMessage(), e);
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Failed to update task");
                    alert.setContentText("Error: " + e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }

    private void loadTaskWsiList(String taskId) {
        loadTaskWsiList(taskId, false);
    }

    /**
     * 加载任务的WSI列表
     * 
     * @param taskId       任务ID
     * @param forceRefresh 是否强制从接口刷新
     */
    private void loadTaskWsiList(String taskId, boolean forceRefresh) {
        new Thread(() -> {
            try {
                List<TaskFile> allWsiList = new ArrayList<>();
                int totalWsi = 0;

                if (!forceRefresh) {
                    // 先尝试从本地缓存读取全部WSI数据
                    allWsiList = cacheManager.loadAllTaskWsi(taskId);
                    totalWsi = allWsiList.size();

                    if (!allWsiList.isEmpty()) {
                        // 缓存有数据，使用缓存数据并进行分页显示
                        logger.info("Loaded all WSI from cache for task {}: {} items", taskId, allWsiList.size());
                        // 不在这里重置页码，因为翻页操作需要保持当前页码
                        // 页码重置在切换task和update task时已经处理
                        updateWsiDisplay(allWsiList, totalWsi);
                        return;
                    }
                }

                // 缓存无数据或强制刷新，从接口获取全部数据
                logger.info("Loading all WSI from API for task {}", taskId);

                // 先获取第一页以得到total值
                WsiListResult firstPageResult = apiClient.getTaskWsiList(taskId, 1, 1);
                totalWsi = firstPageResult.getTotal();

                logger.info("Total WSI count from API: {}", totalWsi);

                // 根据total值一次性获取全部WSI数据
                WsiListResult result = apiClient.getTaskWsiList(taskId, 1, totalWsi);
                allWsiList = result.getItems();

                logger.info("API returned {} WSI items, total: {}", allWsiList.size(), totalWsi);

                // 合并本地状态
                mergeLocalStatus(allWsiList, taskId);

                // 保存全部WSI到缓存
                cacheManager.saveAllTaskWsi(taskId, allWsiList);
                logger.info("Saved all WSI to cache for task {}: {} items", taskId, allWsiList.size());

                // 更新显示
                updateWsiDisplay(allWsiList, totalWsi);
            } catch (Exception e) {
                logger.error("Failed to load WSI list: {}", e.getMessage(), e);
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Failed to load WSI list");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }

    /**
     * 更新WSI列表显示（支持分页）
     * 
     * @param allWsiList 全部WSI列表
     * @param total      总数
     */
    private void updateWsiDisplay(List<TaskFile> allWsiList, int total) {
        // 计算总页数
        int totalPages = total > 0 ? (total + wsiPageSize - 1) / wsiPageSize : 1;

        // 检查并调整当前页码，避免超出范围
        if (wsiCurrentPage > totalPages) {
            logger.warn("Current page {} exceeds total pages {}, adjusting to last page", wsiCurrentPage, totalPages);
            wsiCurrentPage = Math.max(1, totalPages);
        }
        if (wsiCurrentPage < 1) {
            logger.warn("Current page {} is less than 1, adjusting to page 1", wsiCurrentPage);
            wsiCurrentPage = 1;
        }

        // 计算当前页的数据范围
        int startIndex = (wsiCurrentPage - 1) * wsiPageSize;
        int endIndex = Math.min(startIndex + wsiPageSize, allWsiList.size());

        // 获取当前页的数据
        List<TaskFile> pageWsiList = allWsiList.subList(startIndex, endIndex);

        Platform.runLater(() -> {
            taskFiles.clear();
            taskFiles.addAll(pageWsiList);
            wsiTotalPages = totalPages;
            wsiPagination.setPageCount(wsiTotalPages);
            wsiPagination.setCurrentPageIndex(wsiCurrentPage - 1);
            updateWsiTotalDisplay(total);
        });
    }

    /**
     * 合并本地状态到WSI列表
     *
     * @param wsiList WSI列表
     * @param taskId  任务ID
     */
    private void mergeLocalStatus(List<TaskFile> wsiList, String taskId) {
        try {
            // 从新的全量缓存中读取本地状态
            List<TaskFile> cachedWsiList = cacheManager.loadAllTaskWsi(taskId);
            logger.info("Merging local status: loaded {} cached WSI items for task {}", cachedWsiList.size(), taskId);

            if (cachedWsiList.isEmpty()) {
                logger.info("No cached data found, skipping merge");
                return;
            }

            java.util.Map<String, TaskFile> cachedWsiMap = new java.util.HashMap<>();
            for (TaskFile cachedFile : cachedWsiList) {
                cachedWsiMap.put(cachedFile.getId(), cachedFile);
                logger.debug("Cached WSI {}: local_status={}, local_path={}",
                    cachedFile.getId(), cachedFile.getLocalStatus(), cachedFile.getLocalPath());
            }

            int mergedCount = 0;
            for (TaskFile apiFile : wsiList) {
                TaskFile cachedFile = cachedWsiMap.get(apiFile.getId());
                if (cachedFile != null) {
                    String beforeStatus = apiFile.getLocalStatus();
                    // 批量合并时不触发缓存更新，因为之后会统一保存整个列表
                    apiFile.setLocalStatus(cachedFile.getLocalStatus(), false);
                    apiFile.setLocalPath(cachedFile.getLocalPath(), false);
                    apiFile.setAnnotated(cachedFile.isAnnotated());
                    String afterStatus = apiFile.getLocalStatus();
                    logger.info("Merged WSI {}: status changed from '{}' to '{}'",
                        apiFile.getId(), beforeStatus, afterStatus);
                    mergedCount++;
                } else {
                    logger.debug("WSI {} not found in cache, keeping default status", apiFile.getId());
                }
            }
            logger.info("Merge completed: {}/{} WSI items merged", mergedCount, wsiList.size());
        } catch (IOException e) {
            logger.warn("Failed to merge local status: {}", e.getMessage(), e);
        }
    }

    private void downloadSelectedWsi() {
        TaskFile selectedFile = wsiTable.getSelectionModel().getSelectedItem();
        if (selectedFile == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setHeaderText("No WSI selected");
            alert.setContentText("Please select a WSI to download");
            alert.showAndWait();
            return;
        }

        // Check if WSI is already downloading or downloaded
        String currentStatus = selectedFile.getLocalStatus();
        if (currentStatus != null && (currentStatus.equals("downloading") || currentStatus.equals("downloaded"))) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setHeaderText("WSI already processed");
            if (currentStatus.equals("downloading")) {
                alert.setContentText("WSI is currently being downloaded. Please wait for completion.");
            } else {
                alert.setContentText("WSI has already been downloaded. No need to download again.");
            }
            alert.showAndWait();
            return;
        }

        Wsi wsi = selectedFile.getWsi();
        if (wsi == null || wsi.getDownloadUrl() == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setHeaderText("Invalid WSI");
            alert.setContentText("Selected WSI has no download URL");
            alert.showAndWait();
            return;
        }

        // Create save directory
        String saveDir = System.getProperty("user.home") + "/.qupath/pathscope/wsi/" + selectedTask.getId();
        String savePath = saveDir + "/" + wsi.getName();

        new Thread(() -> {
            try {
                selectedFile.setLocalStatus("downloading");
                boolean success = apiClient.downloadWsi(wsi.getDownloadUrl(), savePath);
                Platform.runLater(() -> {
                    if (success) {
                        selectedFile.setLocalPath(savePath);
                        selectedFile.setLocalStatus("downloaded");
                        try {
                            // 将当前显示的WSI列表设置到selectedTask中，确保缓存保存最新状态
                            selectedTask.setFiles(new ArrayList<>(taskFiles));
                            cacheManager.saveTask(selectedTask);
                        } catch (IOException e) {
                            logger.error("Failed to save task: {}", e.getMessage());
                        }
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Success");
                        alert.setHeaderText("WSI downloaded successfully");
                        alert.setContentText("WSI saved to: " + savePath);
                        alert.showAndWait();
                    } else {
                        selectedFile.setLocalStatus("default");
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("Failed to download WSI");
                        alert.showAndWait();
                    }
                });
            } catch (IOException e) {
                logger.error("Failed to download WSI: {}", e.getMessage());
                Platform.runLater(() -> {
                    selectedFile.setLocalStatus("default");
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Failed to download WSI");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }

    private void annotateSelectedWsi() {
        TaskFile selectedFile = wsiTable.getSelectionModel().getSelectedItem();
        if (selectedFile == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setHeaderText("No WSI selected");
            alert.setContentText("Please select a WSI to annotate");
            alert.showAndWait();
            return;
        }

        if (!selectedFile.isDownloaded()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setHeaderText("WSI not downloaded");
            alert.setContentText("Please download the WSI first");
            alert.showAndWait();
            return;
        }

        Wsi wsi = selectedFile.getWsi();
        // Open the WSI in QuPath for annotation
        try {
            // Get the base path without extension
            String localPath = selectedFile.getLocalPath();
            String basePath = localPath;
            if (localPath.contains(".")) {
                basePath = localPath.substring(0, localPath.lastIndexOf('.'));
            }

            // Try different formats - handle both cases: .svs as tiff and .svs as actual
            // svs
            String[] formats = { ".tiff", ".tif" };
            String bestPath = localPath;

            logger.debug("Checking WSI formats for base path: {}", basePath);
            logger.debug("Original local path: {}", localPath);

            // First try with tiff/tif extensions (handles .svs that's actually tiff)
            boolean foundTiffFormat = false;
            for (String format : formats) {
                String testPath = basePath + format;
                java.io.File testFile = new java.io.File(testPath);
                logger.debug("Checking format {}: {} (exists: {})", format, testPath, testFile.exists());

                if (testFile.exists()) {
                    bestPath = testPath;
                    logger.info("Opening WSI in {} format: {}", format.substring(1), testPath);
                    foundTiffFormat = true;
                    break;
                }
            }

            // If no tiff format found, use original path (could be actual .svs format)
            if (!foundTiffFormat) {
                bestPath = localPath;
                logger.info("Opening WSI in original format (may be actual .svs format): {}", localPath);
            }

            String finalPath = bestPath;
            logger.info("Final WSI path: {}", finalPath);

            // Open the image in the current viewer
            logger.info("Opening image: {}", finalPath);
            logger.info("File extension: {}", localPath.substring(localPath.lastIndexOf('.') + 1).toLowerCase());
            
            // Use QuPath's built-in method which handles auto pyramid dialog
            // QuPath will automatically show auto pyramid dialog for large images
            // This works for all file formats including .svs
            // Pass prompt=true to ensure auto pyramid dialog is shown for large images
            qupath.openImage(qupath.getViewer(), finalPath, true, false);
            
            logger.info("Image opening process initiated - QuPath will handle auto pyramid dialog for large images");
            logger.info("For .svs files: QuPath will check if auto pyramid is needed based on image size");

            // Set to maximum resolution layer (highest resolution, level 0)
            QuPathViewer viewer = qupath.getViewer();
            if (viewer != null) {
                logger.info("Viewer obtained: {}", viewer);

                ImageServer<BufferedImage> server = viewer.getServer();
                if (server != null) {
                    logger.info("Server obtained: {}", server);
                    logger.info("Server type: {}", server.getClass().getName());
                    int resolutionCount = server.nResolutions();
                    logger.info("WSI resolution layers: {}", resolutionCount);

                    // Print detailed layer information
                    for (int i = 0; i < resolutionCount; i++) {
                        double downsample = server.getDownsampleForResolution(i);
                        logger.info("Layer {}: downsample factor = {}", i, downsample);
                    }

                    if (resolutionCount > 1) {
                        // Get the downsample factor for the highest resolution (level 0)
                        double maxResolutionDownsample = server.getDownsampleForResolution(0);
                        logger.info("Max resolution downsample factor (level 0): {}", maxResolutionDownsample);

                        // Set the viewer to use this downsample factor
                        logger.info("Setting viewer to maximum resolution layer...");
                        viewer.setDownsampleFactor(maxResolutionDownsample, -1, -1);
                        logger.info("Set WSI to maximum resolution layer (level 0) with downsample factor: {}",
                                maxResolutionDownsample);
                    } else {
                        logger.info("WSI has only one resolution layer");
                    }
                    
                    // Fit image to window size
                    logger.info("Fitting image to window size...");
                    viewer.zoomToFit();
                    logger.info("Image fitted to window size");
                } else {
                    logger.error("Failed to get server from viewer");
                }
            } else {
                logger.error("Failed to get viewer");
            }

            // Open annotation dialog
            AnnotationDialog annotationDialog = new AnnotationDialog(qupath, apiClient, selectedFile);
            annotationDialog.showAndWait();

            // Update status to annotated after annotation dialog is closed
            selectedFile.setLocalStatus("annotated");
            try {
                cacheManager.saveTask(selectedTask);
            } catch (IOException e) {
                logger.error("Failed to save task: {}", e.getMessage());
            }
        } catch (Exception e) {
            logger.error("Failed to open WSI: {}", e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to open WSI");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void submitSelectedTask() {
        if (selectedTask == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setHeaderText("No task selected");
            alert.setContentText("Please select a task to submit");
            alert.showAndWait();
            return;
        }

        new Thread(() -> {
            try {
                boolean success = apiClient.submitTaskCompletion(selectedTask.getId(), 1);
                Platform.runLater(() -> {
                    if (success) {
                        selectedTask.setStatus(1);
                        try {
                            cacheManager.saveTask(selectedTask);
                        } catch (IOException e) {
                            logger.error("Failed to save task: {}", e.getMessage());
                        }
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Success");
                        alert.setHeaderText("Task submitted successfully");
                        alert.showAndWait();
                    } else {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("Failed to submit task");
                        alert.showAndWait();
                    }
                });
            } catch (IOException e) {
                logger.error("Failed to submit task: {}", e.getMessage());
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Failed to submit task");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }

    /**
     * 更新任务列表的总数显示
     */
    private void updateTaskTotalDisplay(int total) {
        taskTotalLabel.setText("Total: " + total);
    }

    /**
     * 更新WSI列表的总数显示
     */
    private void updateWsiTotalDisplay(int total) {
        wsiTotalLabel.setText("Total: " + total);
    }
}
