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
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
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
import org.tallison.tika.app.fx.batch.BatchProcess;
import org.tallison.tika.app.fx.batch.BatchProcessConfig;
import org.tallison.tika.app.fx.config.ConfigItem;
import org.tallison.tika.app.fx.ctx.AppContext;
import org.tallison.tika.app.fx.emitters.EmitterSpec;
import org.tallison.tika.app.fx.emitters.ValidationResult;
import org.tallison.tika.app.fx.status.StatusUpdater;
import org.tallison.tika.app.fx.utils.OptionalUtil;

public class TikaController extends ControllerBase {

    private static final Logger LOGGER = LogManager.getLogger(TikaController.class);
    static AppContext APP_CONTEXT = AppContext.getInstance();
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
        outputLabel.textProperty()
                .bind(APP_CONTEXT.getBatchProcessConfig().get().getEmitterLabel());
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
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent we) {
                onEmitterClose(we, stage);
            }
        });

        stage.showAndWait();
        updateButtons(BatchProcess.STATUS.READY);

    }

    private void onEmitterClose(WindowEvent windowEvent, Stage stage) {

        if (APP_CONTEXT.getBatchProcessConfig().isEmpty()) {
            windowEvent.consume();
            return;
        }
        if (APP_CONTEXT.getBatchProcessConfig().get().getEmitter().isEmpty()) {
            windowEvent.consume();
            return;
        }
        EmitterSpec emitterSpec = APP_CONTEXT.getBatchProcessConfig().get().getEmitter().get();
        if (emitterSpec.isValid()) {
            windowEvent.consume();
            updateButtons(BatchProcess.STATUS.READY);
            return;
        }
        AtomicBoolean close = new AtomicBoolean(true);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Output Not Fully Configured");
        if (!OptionalUtil.isEmpty(emitterSpec.getNotValidMessage())) {
            alert.setContentText(emitterSpec.getNotValidMessage().get());
        } else {
            alert.setContentText("");
        }
        ButtonType goBack = new ButtonType("Go Back to Configuration", ButtonBar.ButtonData.YES);
        ButtonType ignore = new ButtonType("Ignore", ButtonBar.ButtonData.NO);
        //ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(ignore, goBack);
        alert.showAndWait().ifPresent(type -> {
            if (type.getText().startsWith("Go Back")) {
                close.set(false);
            } else if (type.getText().startsWith("Ignore")) {
                close.set(true);
            }
        });
        //go back to where we were
        windowEvent.consume();
        if (close.get()) {
            stage.close();
        }

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

        runButton.setDisable(!fullyConfigured());
    }

    private boolean fullyConfigured() {
        BatchProcessConfig config = APP_CONTEXT.getBatchProcessConfig().get();
        return config.getFetcher().isPresent() && config.getFetcher().get().isValid() &&
                config.getPipesIterator().isPresent() &&
                config.getPipesIterator().get().isValid() && config.getEmitter().isPresent() &&
                config.getEmitter().get().isValid();
    }

    @FXML
    public void runTika(ActionEvent actionEvent) throws Exception {
        Optional<BatchProcess> oldProcess = APP_CONTEXT.getBatchProcess();
        if (!oldProcess.isEmpty()) {
            if (oldProcess.get().getMutableStatus().get() == BatchProcess.STATUS.RUNNING) {
                alert("Tika App", "Still running?!", "Older process is still running");
                actionEvent.consume();
                return;
            }
        }
        if (APP_CONTEXT.getBatchProcessConfig().isEmpty()) {
            LOGGER.warn("batch processConfig must not be empty!");
            return;
        }

        BatchProcessConfig bpc = APP_CONTEXT.getBatchProcessConfig().get();
        //TODO -- validate as much as possible
        EmitterSpec emitterSpec = bpc.getEmitter().get();
        ValidationResult result = emitterSpec.initialize();
        //invalidate emitter so that run button is turned off --
        //this forces the user to have to reconfigure the emitters
        emitterSpec.setValid(false);
        if (result != ValidationResult.OK) {
            alert(result.getTitle().get(), result.getHeader().get(), result.getMsg().get());
            return;
        }
        //TODO -- all sorts of checks
        //Is there already a batch process.
        //Do we have a fetcher and an emitter already set, etc.
        BatchProcess batchProcess = new BatchProcess();
        APP_CONTEXT.setBatchProcess(batchProcess);
        StatusUpdater statusUpdater = new StatusUpdater(batchProcess, this);
        batchProcess.start(APP_CONTEXT.getBatchProcessConfig().get(), statusUpdater);
        long maxWait = 30000;
        long start = System.currentTimeMillis();
        while (batchProcess.getMutableStatus().get() != BatchProcess.STATUS.RUNNING) {
            if (batchProcess.getMutableStatus().get() == BatchProcess.STATUS.ERROR) {
                if (batchProcess.getJvmException().isPresent()) {
                    alertStackTrace("Problem starting tika", "Can't start Tika",
                            "If you see 'No such file or directory' for 'java'," +
                                    "check that java is there and has the right permissions",
                            batchProcess.getJvmException().get());
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
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Fetcher");
            alert.setHeaderText(configItem.get().getShortLabel());
            alert.setResizable(true);
            alert.setContentText(configItem.get().getFullLabel());
            alert.getDialogPane().setMinWidth(500);
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            alert.showAndWait();
        }
        mouseEvent.consume();
    }

    public void showEmitter(MouseEvent mouseEvent) {
        Optional<EmitterSpec> emitterSpec = APP_CONTEXT.getBatchProcessConfig().get().getEmitter();
        if (emitterSpec.isPresent()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Emitter");
            alert.setHeaderText(emitterSpec.get().getShortLabel().get());
            alert.setResizable(true);
            alert.setContentText(emitterSpec.get().getFullLabel().get());
            alert.getDialogPane().setMinWidth(500);
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            alert.showAndWait();
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
