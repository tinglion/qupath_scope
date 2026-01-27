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

        TableColumn<TaskFile, String> localStatusColumn = new TableColumn<>("Local Status");
        localStatusColumn.setCellValueFactory(cellData -> cellData.getValue().localStatusProperty());
        localStatusColumn.setPrefWidth(150);

        TableColumn<TaskFile, String> localPathColumn = new TableColumn<>("Local Path");
        localPathColumn.setCellValueFactory(new PropertyValueFactory<>("localPath"));
        localPathColumn.setPrefWidth(200);

        wsiTable.getColumns().addAll(wsiIdColumn, wsiNameColumn, localStatusColumn, sizeColumn, ptypeColumn,
                localPathColumn);

        // Create buttons
        Button updateTasksButton = new Button("Update Tasks");
        updateTasksButton.setOnAction(e -> loadTasks(true)); // 强制刷新，从接口获取最新数据

        Button updateTaskButton = new Button("Update Task");
        updateTaskButton.setOnAction(e -> {
            if (selectedTask != null) {
                loadTaskWsiList(selectedTask.getId(), true); // 强制刷新，从接口获取最新数据
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
                
                // 一次性获取全部任务（使用较大的pageSize）
                int pageSize = 1000; // 假设最大任务数不超过1000
                TaskListResult result = apiClient.getTaskList(userId, 1, pageSize);
                allTasks = result.getItems();
                totalTasks = result.getTotal();
                
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
                        updateWsiDisplay(allWsiList, totalWsi);
                        return;
                    }
                }

                // 缓存无数据或强制刷新，从接口获取全部数据
                logger.info("Loading all WSI from API for task {}", taskId);
                
                // 一次性获取全部WSI数据（使用较大的pageSize）
                int pageSize = 10000; // 假设最大WSI数不超过10000
                WsiListResult result = apiClient.getTaskWsiList(taskId, 1, pageSize);
                allWsiList = result.getItems();
                totalWsi = result.getTotal();
                
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
        // 计算当前页的数据范围
        int startIndex = (wsiCurrentPage - 1) * wsiPageSize;
        int endIndex = Math.min(startIndex + wsiPageSize, allWsiList.size());
        
        // 获取当前页的数据
        List<TaskFile> pageWsiList = allWsiList.subList(startIndex, endIndex);
        
        Platform.runLater(() -> {
            taskFiles.clear();
            taskFiles.addAll(pageWsiList);
            wsiTotalPages = total > 0 ? (total + wsiPageSize - 1) / wsiPageSize : 1;
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
            java.util.Map<String, TaskFile> cachedWsiMap = new java.util.HashMap<>();
            for (TaskFile cachedFile : cachedWsiList) {
                cachedWsiMap.put(cachedFile.getId(), cachedFile);
            }

            for (TaskFile apiFile : wsiList) {
                TaskFile cachedFile = cachedWsiMap.get(apiFile.getId());
                if (cachedFile != null) {
                    apiFile.setLocalStatus(cachedFile.getLocalStatus());
                    apiFile.setLocalPath(cachedFile.getLocalPath());
                    apiFile.setAnnotated(cachedFile.isAnnotated());
                    logger.debug("Merged local status for WSI {}: {}", apiFile.getId(), cachedFile.getLocalStatus());
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to merge local status: {}", e.getMessage());
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
                if (wsi != null) {
                    wsi.setStatus(Wsi.Status.DOWNLOADING);
                }
                boolean success = apiClient.downloadWsi(wsi.getDownloadUrl(), savePath);
                Platform.runLater(() -> {
                    if (success) {
                        selectedFile.setLocalPath(savePath);
                        selectedFile.setLocalStatus("downloaded");
                        if (wsi != null) {
                            wsi.setStatus(Wsi.Status.DOWNLOADED);
                        }
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
                        if (wsi != null) {
                            wsi.setStatus(Wsi.Status.DEFAULT);
                        }
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
                    if (wsi != null) {
                        wsi.setStatus(Wsi.Status.DEFAULT);
                    }
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
            if (wsi != null) {
                wsi.setStatus(Wsi.Status.ANNOTATED);
            }
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
