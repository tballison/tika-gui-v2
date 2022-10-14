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

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
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
import org.tallison.tika.app.fx.tools.BatchProcess;
import org.tallison.tika.app.fx.tools.ConfigItem;

import org.apache.tika.utils.StringUtils;

public class TikaController {

    static AppContext APP_CONTEXT = AppContext.getInstance();
    private static Logger LOGGER = LogManager.getLogger(TikaController.class);
    @FXML
    private Label welcomeText;

    @FXML
    private Button runButton;

    @FXML
    private Label inputLabel;

    @FXML
    private Label outputLabel;

    @FXML
    private ProgressIndicator batchProgress;

    @FXML
    public void initialize() {
        inputLabel.textProperty().bind(APP_CONTEXT.getBatchProcessConfig().getFetcherLabel());
        outputLabel.textProperty().bind(APP_CONTEXT.getBatchProcessConfig().getEmitterLabel());
        //batchProgress.setVisible(false);
        batchProgress.progressProperty().bind(APP_CONTEXT.getBatchProcess().progressProperty());
    }


    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to Apache Tika Demo App v2!");
    }

    public void handleAboutAction(ActionEvent actionEvent) {
    }

    public void handleKeyInput(KeyEvent keyEvent) {

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
        Scene scene = new Scene(fxmlLoader.load());

        final Stage stage = new Stage();
        stage.setTitle("Select Input");
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    public void configureMetadata(ActionEvent actionEvent) throws IOException {
        FXMLLoader fxmlLoader =
                new FXMLLoader(TikaApplication.class.getResource("batch-metadata-view.fxml"));
        VBox dragTarget = new VBox();
        StackPane root = new StackPane();
        root.getChildren().add(dragTarget);
        Scene scene = new Scene(fxmlLoader.load());

        final Stage stage = new Stage();
        stage.setTitle("Select Metadata");
        stage.setScene(scene);
        final MetadataController metadataController = fxmlLoader.getController();
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent we) {
                metadataController.saveMetadataToContext();
            }
        });
        stage.show();
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
        Scene scene = new Scene(fxmlLoader.load());

        final Stage stage = new Stage();
        stage.setTitle("Select Output");
        stage.setScene(scene);
        stage.show();
        // TODO add check that both input and output are selected
        // TODO add this to configureInput
        runButton.setDisable(false);
    }

    @FXML
    public void runTika(ActionEvent actionEvent) throws Exception {
        //TODO -- all sorts of checks
        //Is there already a batch process.
        //Do we have a fetcher and an emitter already set, etc.
        BatchProcess batchProcess = new BatchProcess();
        batchProgress.progressProperty().bind(batchProcess.progressProperty());
        APP_CONTEXT.setBatchProcess(batchProcess);
        batchProcess.start(APP_CONTEXT.getBatchProcessConfig());
        APP_CONTEXT.saveState();
    }

    public void showFetcher(MouseEvent mouseEvent) {
        ConfigItem configItem = APP_CONTEXT.getBatchProcessConfig().getFetcher();
        if (configItem != null) {
            String path = configItem.getAttributes().get("basePath");
            if (!StringUtils.isBlank(path)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Fetcher");
                alert.setHeaderText(configItem.getLabel());
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
        ConfigItem configItem = APP_CONTEXT.getBatchProcessConfig().getEmitter();
        if (configItem != null) {
            String path = configItem.getAttributes().get("basePath");
            if (!StringUtils.isBlank(path)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Fetcher");
                alert.setHeaderText(configItem.getLabel());
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
}
