/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tallison.tika.app.fx;

import java.io.IOException;
import java.util.Optional;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tallison.tika.app.fx.ctx.AppContext;
import org.tallison.tika.app.fx.status.MutableStatus;
import org.tallison.tika.app.fx.status.StatusUpdater;
import org.tallison.tika.app.fx.tools.BatchProcess;
import org.tallison.tika.app.fx.tools.BatchProcessConfig;
import org.tallison.tika.app.fx.tools.ConfigItem;

import org.apache.tika.utils.StringUtils;

public class TikaController extends ControllerBase {

    static AppContext APP_CONTEXT = AppContext.getInstance();
    private static Logger LOGGER = LogManager.getLogger(TikaController.class);
    @FXML
    private Label welcomeText;

    @FXML
    private Button runButton;

    @FXML
    private Button cancelButton;

    @FXML
    private Button statusButton;

    @FXML
    private Button configureInput;

    @FXML
    private Button configureOutput;

    @FXML
    private Button configureAdvanced;


    @FXML
    private Label inputLabel;

    @FXML
    private Label outputLabel;

    @FXML
    private ProgressIndicator batchProgressIndicator;

    @FXML
    public void initialize() {
        if (APP_CONTEXT.getBatchProcessConfig().isEmpty()) {
            LOGGER.warn("batch process config must not be null");
            return;
        }
        inputLabel.textProperty().bind(APP_CONTEXT.getBatchProcessConfig().get().getFetcherLabel());
        outputLabel.textProperty().bind(APP_CONTEXT.getBatchProcessConfig().get().getEmitterLabel());
        //batchProgress.setVisible(false);
        //if (APP_CONTEXT.getBatchProcess().isPresent()) {
          //  batchProgressIndicator.progressProperty().bind(APP_CONTEXT.getBatchProcess().get()
        //  .progressProperty());
        //}
        updateButtons(BatchProcess.STATUS.READY);
    }


    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to Apache Tika Demo App v2!");
    }

    public void handleAboutAction(ActionEvent actionEvent) {
    }

    public void handleKeyInput(KeyEvent keyEvent) {

    }

    public ProgressIndicator getBatchProgressIndicator() {
        return batchProgressIndicator;
    }

    public void resetState(ActionEvent actionEvent) {
        APP_CONTEXT.reset();
    }


    @FXML
    public void configureInput(ActionEvent actionEvent) throws IOException {
        FXMLLoader fxmlLoader =
                new FXMLLoader(TikaApplication.class.getResource("batch-input-view.fxml"));
        VBox dragTarget = new VBox();
        StackPane root = new StackPane();
        root.getChildren().add(dragTarget);
        TabPane tabPane = fxmlLoader.load();
        int selected = 0;

        if (APP_CONTEXT.getBatchProcessConfig().isPresent()) {
            selected = APP_CONTEXT.getBatchProcessConfig().get().getInputSelectedTab();
        }

        tabPane.getSelectionModel().select(selected);

        Scene scene = new Scene(tabPane);

        final Stage stage = new Stage();
        stage.setTitle("Select Input");
        stage.setScene(scene);
        stage.showAndWait();
        updateButtons(BatchProcess.STATUS.READY);
    }

    @FXML
    public void configureAdvanced(ActionEvent actionEvent) throws IOException {
        FXMLLoader fxmlLoader =
                new FXMLLoader(TikaApplication.class.getResource("batch-advanced-view.fxml"));
        VBox dragTarget = new VBox();
        StackPane root = new StackPane();
        root.getChildren().add(dragTarget);
        Scene scene = new Scene(fxmlLoader.load());

        final Stage stage = new Stage();
        final AdvancedBatchController advancedBatchController = fxmlLoader.getController();
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent we) {
                advancedBatchController.saveState();
                updateButtons(BatchProcess.STATUS.READY);
            }
        });
        stage.show();

        stage.setTitle("Advanced View");
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    public void configureOutput(ActionEvent actionEvent) throws IOException {
        FXMLLoader fxmlLoader =
                new FXMLLoader(TikaApplication.class.getResource("batch-output-view.fxml"));
        VBox dragTarget = new VBox();
        StackPane root = new StackPane();
        root.getChildren().add(dragTarget);
        TabPane tabPane = fxmlLoader.load();
        Scene scene = new Scene(tabPane);
        int selected = 0;
        if (APP_CONTEXT.getBatchProcessConfig().isPresent()) {
            selected = APP_CONTEXT.getBatchProcessConfig().get().getOutputSelectedTab();
        }
        tabPane.getSelectionModel().select(selected);

        final Stage stage = new Stage();
        stage.setTitle("Select Output");
        stage.setScene(scene);
        stage.showAndWait();
        updateButtons(BatchProcess.STATUS.READY);
    }

    public void updateButtons(BatchProcess.STATUS status) {

        if (status == BatchProcess.STATUS.READY) {
            statusButton.setDisable(true);
        }
        if (status == BatchProcess.STATUS.RUNNING) {
            runButton.setDisable(true);
            cancelButton.setDisable(false);
            statusButton.setDisable(false);
            configureInput.setDisable(true);
            configureOutput.setDisable(true);
            configureAdvanced.setDisable(true);
            return;
        }
        configureInput.setDisable(false);
        configureOutput.setDisable(false);
        configureAdvanced.setDisable(false);
        cancelButton.setDisable(true);

        if (fullyConfigured()) {
            runButton.setDisable(false);
        } else {
            runButton.setDisable(true);
        }
    }

    private boolean fullyConfigured() {
        BatchProcessConfig config = APP_CONTEXT.getBatchProcessConfig().get();
        return config.getEmitter().isPresent() &&
                config.getPipesIterator().isPresent() &&
                config.getEmitter().isPresent();
    }
    @FXML
    public void runTika(ActionEvent actionEvent) throws Exception {
        Optional<BatchProcess> oldProcess = APP_CONTEXT.getBatchProcess();
        if (! oldProcess.isEmpty()) {
            if (oldProcess.get().getMutableStatus().get() == BatchProcess.STATUS.RUNNING) {
                alert("Tika App", "Still running?!", "Older process is still running");
                actionEvent.consume();
                return;
            }
        }
        if (APP_CONTEXT.getBatchProcessConfig().isEmpty()) {
            LOGGER.warn("batch processConfig must not be empty!");
        }
        //TODO -- all sorts of checks
        //Is there already a batch process.
        //Do we have a fetcher and an emitter already set, etc.
        MutableStatus mutableStatus = new MutableStatus(BatchProcess.STATUS.READY);
        BatchProcess batchProcess = new BatchProcess(mutableStatus);
        APP_CONTEXT.setBatchProcess(batchProcess);
        StatusUpdater statusUpdater = new StatusUpdater(mutableStatus, this);
        batchProcess.start(APP_CONTEXT.getBatchProcessConfig().get(), statusUpdater);
        long maxWait = 30000;
        long start = System.currentTimeMillis();
        while (batchProcess.getMutableStatus().get() != BatchProcess.STATUS.RUNNING) {
            if (batchProcess.getMutableStatus().get() == BatchProcess.STATUS.FAILED_START) {
                if (batchProcess.getJvmStartException().isPresent()) {
                    alertStackTrace("Problem starting tika", "Can't start Tika",
                            "If you see 'No such file or directory' for 'java'," +
                                    "check that java is there and has the right permissions",
                            batchProcess.getJvmStartException().get());
                } else {
                    alert("Can't start Tika", "Can't start Tika", "Don't know why?!");
                }
                break;
            }
            Thread.sleep(100);
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed > maxWait) {
                LOGGER.warn("waited {}, and process still has not started?!");
                break;
            }
        }
        APP_CONTEXT.saveState();
        updateButtons(batchProcess.getMutableStatus().get());
    }

    public void showFetcher(MouseEvent mouseEvent) {
        Optional<ConfigItem> configItem = APP_CONTEXT.getBatchProcessConfig().get().getFetcher();
        if (configItem.isPresent()) {
            String path = configItem.get().getAttributes().get("basePath");
            if (!StringUtils.isBlank(path)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Fetcher");
                alert.setHeaderText(configItem.get().getLabel());
                alert.setResizable(true);
                alert.setContentText("Path: " + path);
                alert.getDialogPane().setMinWidth(500);
                alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                alert.showAndWait();
            }
        }
        mouseEvent.consume();
    }

    public void showEmitter(MouseEvent mouseEvent) {
        Optional<ConfigItem> configItem = APP_CONTEXT.getBatchProcessConfig().get().getEmitter();
        if (configItem.isPresent()) {
            String path = configItem.get().getAttributes().get("basePath");
            if (!StringUtils.isBlank(path)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Fetcher");
                alert.setHeaderText(configItem.get().getLabel());
                alert.setResizable(true);
                alert.setContentText("Path: " + path);
                alert.getDialogPane().setMinWidth(500);
                alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                alert.showAndWait();
            }
        }
        mouseEvent.consume();
    }

    public void openDetailedStatus(MouseEvent mouseEvent) {

    }

    public void cancelBatch(ActionEvent actionEvent) {
        if (APP_CONTEXT.getBatchProcess().isPresent()) {
            APP_CONTEXT.getBatchProcess().get().cancel();
        }
        updateButtons(BatchProcess.STATUS.CANCELED);
    }

    public void checkStatus(ActionEvent actionEvent) throws IOException {
        FXMLLoader fxmlLoader =
                new FXMLLoader(TikaApplication.class.getResource("batch-status-view.fxml"));
        VBox dragTarget = new VBox();
        StackPane root = new StackPane();
        root.getChildren().add(dragTarget);

        Scene scene = new Scene(fxmlLoader.load());

        final Stage stage = new Stage();
        stage.setTitle("Batch Status");
        stage.setScene(scene);
        final BatchStatusController batchStatusController = fxmlLoader.getController();
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent we) {
                batchStatusController.stop();
            }
        });
        stage.show();
    }
}
