package qupath.extension.pathscope.ui;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import qupath.extension.pathscope.data.DownloadTask;
import qupath.extension.pathscope.logic.DownloadManager;

/**
 * 下载任务列表窗口，非阻塞显示所有下载任务的实时进度
 */
public class DownloadListDialog extends Stage {

    private static DownloadListDialog instance;

    private TableView<DownloadTask> table;

    private DownloadListDialog() {
        initModality(Modality.NONE);
        setTitle("下载任务列表");
        setWidth(700);
        setHeight(400);

        createUI();
    }

    public static DownloadListDialog getInstance() {
        if (instance == null) {
            instance = new DownloadListDialog();
        }
        return instance;
    }

    /**
     * 显示窗口（如果已显示则置前）
     */
    public void showDialog() {
        if (!isShowing()) {
            show();
        }
        toFront();
    }

    @SuppressWarnings("unchecked")
    private void createUI() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        table = new TableView<>();
        table.setItems(DownloadManager.getInstance().getDownloadTasks());
        table.setPlaceholder(new Label("暂无下载任务"));

        // WSI名称列
        TableColumn<DownloadTask, String> wsiNameCol = new TableColumn<>("WSI名称");
        wsiNameCol.setCellValueFactory(new PropertyValueFactory<>("wsiName"));
        wsiNameCol.setPrefWidth(180);

        // 任务名称列
        TableColumn<DownloadTask, String> taskNameCol = new TableColumn<>("任务");
        taskNameCol.setCellValueFactory(new PropertyValueFactory<>("taskName"));
        taskNameCol.setPrefWidth(120);

        // 状态列
        TableColumn<DownloadTask, DownloadTask.Status> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        statusCol.setPrefWidth(80);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(DownloadTask.Status item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.getLabel());
                    switch (item) {
                        case DOWNLOADING:
                            setStyle("-fx-text-fill: #2196F3;");
                            break;
                        case COMPLETED:
                            setStyle("-fx-text-fill: #4CAF50;");
                            break;
                        case FAILED:
                            setStyle("-fx-text-fill: #F44336;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });

        // 进度列（ProgressBar）
        TableColumn<DownloadTask, Double> progressCol = new TableColumn<>("进度");
        progressCol.setCellValueFactory(cellData -> cellData.getValue().progressProperty().asObject());
        progressCol.setPrefWidth(150);
        progressCol.setCellFactory(col -> new TableCell<>() {
            private final ProgressBar progressBar = new ProgressBar();
            {
                progressBar.setMaxWidth(Double.MAX_VALUE);
            }

            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    progressBar.setProgress(item);
                    setGraphic(progressBar);
                }
            }
        });

        // 已下载大小列
        TableColumn<DownloadTask, String> sizeCol = new TableColumn<>("已下载");
        sizeCol.setPrefWidth(120);
        sizeCol.setCellValueFactory(cellData -> {
            DownloadTask dt = cellData.getValue();
            return Bindings.createStringBinding(
                    () -> {
                        String downloaded = DownloadTask.formatBytes(dt.getDownloadedBytes());
                        if (dt.getTotalBytes() > 0) {
                            return downloaded + " / " + DownloadTask.formatBytes(dt.getTotalBytes());
                        }
                        return downloaded;
                    },
                    dt.downloadedBytesProperty(),
                    dt.totalBytesProperty()
            );
        });

        table.getColumns().addAll(wsiNameCol, taskNameCol, statusCol, progressCol, sizeCol);

        root.setCenter(table);

        // 底部按钮
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(10, 0, 0, 0));

        Button clearBtn = new Button("清除已完成");
        clearBtn.setOnAction(e -> DownloadManager.getInstance().clearFinished());

        buttonBar.getChildren().add(clearBtn);
        root.setBottom(buttonBar);

        setScene(new Scene(root));
    }
}
