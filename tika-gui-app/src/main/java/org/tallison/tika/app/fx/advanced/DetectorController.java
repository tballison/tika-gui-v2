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
package org.tallison.tika.app.fx.advanced;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tallison.tika.app.fx.batch.BatchProcessConfig;
import org.tallison.tika.app.fx.ctx.AppContext;

public class DetectorController implements Initializable {

    private static final AppContext APP_CONTEXT = AppContext.getInstance();
    private static final Logger LOGGER = LogManager.getLogger(DetectorController.class);


    @FXML
    TextArea detectorConfigXML;

    @FXML
    TextField detectorConfigPath;


    @Override
    public void initialize(URL location, ResourceBundle resources) {

        if (APP_CONTEXT.getBatchProcessConfig().isEmpty()) {
            return;
        }
        if (APP_CONTEXT.getBatchProcessConfig().get().getDetectorConfig().isEmpty()) {
            return;
        }
        Optional<Path> path =
                APP_CONTEXT.getBatchProcessConfig().get().getDetectorConfig().get().getPath();

        if (path.isEmpty()) {
            return;
        }

        if (!Files.isRegularFile(path.get())) {
            LOGGER.warn("can't find detector config file");
            return;
        }
        detectorConfigPath.setText(path.get().getFileName().toString());
        loadConfig(path.get());
    }

    private void loadConfig(Path path) {
        String xml = null;
        try {
            xml = FileUtils.readFileToString(path.toFile(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            //TODO add dialog
            LOGGER.warn("failed to load " + path);
            return;
        }
        detectorConfigXML.setText(xml);
        detectorConfigPath.setText(path.getFileName().toString());
    }


    public void openDetectorConfig(ActionEvent actionEvent) {
        final Window parent = ((Node) actionEvent.getTarget()).getScene().getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Detector Config");
        BatchProcessConfig batchProcessConfig;
        if (APP_CONTEXT.getBatchProcessConfig().isPresent()) {
            batchProcessConfig = APP_CONTEXT.getBatchProcessConfig().get();
        } else {
            LOGGER.warn("batch process config is null?!");
            actionEvent.consume();
            return;
        }
        Optional<DetectorConfig> detectorConfigOptional = batchProcessConfig.getDetectorConfig();
        if (detectorConfigOptional.isPresent()) {
            Optional<Path> path = detectorConfigOptional.get().getPath();
            if (path.isPresent()) {
                File f = path.get().toFile();
                fileChooser.setInitialDirectory(f.getParentFile());
                fileChooser.setInitialFileName(f.getName());
            }
        }

        File file = fileChooser.showOpenDialog(parent);
        if (file == null) {
            actionEvent.consume();
            return;
        }
        DetectorConfig detectorConfig = detectorConfigOptional.orElse(new DetectorConfig());
        detectorConfig.setPath(file.toPath());
        batchProcessConfig.setDetectorConfig(detectorConfig);
        loadConfig(file.toPath());
        APP_CONTEXT.saveState();
        actionEvent.consume();
    }

    public void saveDetectorConfig(ActionEvent actionEvent) {
        final Window parent = ((Node) actionEvent.getTarget()).getScene().getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Detector Config");
        BatchProcessConfig batchProcessConfig;
        if (APP_CONTEXT.getBatchProcessConfig().isPresent()) {
            batchProcessConfig = APP_CONTEXT.getBatchProcessConfig().get();
        } else {
            LOGGER.warn("batch process config is null?!");
            actionEvent.consume();
            return;
        }
        Optional<DetectorConfig> detectorConfigOptional = batchProcessConfig.getDetectorConfig();
        if (detectorConfigOptional.isPresent()) {
            Optional<Path> path = detectorConfigOptional.get().getPath();
            if (path.isPresent()) {
                File f = path.get().toFile();
                fileChooser.setInitialDirectory(f.getParentFile());
                fileChooser.setInitialFileName(f.getName());
            }
        }

        File file = fileChooser.showSaveDialog(parent);
        if (file == null) {
            actionEvent.consume();
            return;
        }

        try {
            FileUtils.write(file, detectorConfigXML.getText(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("couldn't write file: " + file, e);
            actionEvent.consume();
            return;
        }
        DetectorConfig detectorConfig = detectorConfigOptional.orElse(new DetectorConfig());
        detectorConfig.setPath(file.toPath());
        batchProcessConfig.setDetectorConfig(detectorConfig);
        APP_CONTEXT.saveState();
        actionEvent.consume();
    }

    public void clearDetectorConfig(ActionEvent actionEvent) {
        detectorConfigXML.setText("");
        detectorConfigPath.setText("");
        if (APP_CONTEXT.getBatchProcessConfig().isEmpty()) {
            return;
        }
        if (APP_CONTEXT.getBatchProcessConfig().get().getDetectorConfig().isEmpty()) {
            return;
        }
        //nullify path
        APP_CONTEXT.getBatchProcessConfig().get().getDetectorConfig().get().setPath(null);
    }
}
