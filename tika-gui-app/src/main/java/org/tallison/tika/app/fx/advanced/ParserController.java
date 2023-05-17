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

public class ParserController implements Initializable {

    private static final AppContext APP_CONTEXT = AppContext.getInstance();
    private static final Logger LOGGER = LogManager.getLogger(ParserController.class);


    @FXML
    TextArea parserConfigXML;

    @FXML
    TextField parserConfigPath;


    @Override
    public void initialize(URL location, ResourceBundle resources) {

        if (APP_CONTEXT.getBatchProcessConfig().isEmpty()) {
            return;
        }
        if (APP_CONTEXT.getBatchProcessConfig().get().getParserConfig().isEmpty()) {
            return;
        }
        Optional<Path> path =
                APP_CONTEXT.getBatchProcessConfig().get().getParserConfig().get().getPath();

        if (path.isEmpty()) {
            return;
        }

        if (!Files.isRegularFile(path.get())) {
            LOGGER.warn("can't find parser config file");
            return;
        }
        parserConfigPath.setText(path.get().getFileName().toString());
        loadParserConfig(path.get());
    }

    private void loadParserConfig(Path path) {
        String xml = null;
        try {
            xml = FileUtils.readFileToString(path.toFile(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            //TODO add dialog
            LOGGER.warn("failed to load " + path);
            return;
        }
        parserConfigXML.setText(xml);
        parserConfigPath.setText(path.getFileName().toString());
    }


    public void openParseConfig(ActionEvent actionEvent) {
        final Window parent = ((Node) actionEvent.getTarget()).getScene().getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Parser Config");
        BatchProcessConfig batchProcessConfig;
        if (APP_CONTEXT.getBatchProcessConfig().isPresent()) {
            batchProcessConfig = APP_CONTEXT.getBatchProcessConfig().get();
        } else {
            LOGGER.warn("batch process config is null?!");
            actionEvent.consume();
            return;
        }
        Optional<ParserConfig> parserConfigOptional = batchProcessConfig.getParserConfig();
        if (parserConfigOptional.isPresent()) {
            Optional<Path> path = parserConfigOptional.get().getPath();
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
        ParserConfig parserConfig = parserConfigOptional.orElse(new ParserConfig());
        parserConfig.setPath(file.toPath());
        batchProcessConfig.setParserConfig(parserConfig);
        loadParserConfig(file.toPath());
        APP_CONTEXT.saveState();
        actionEvent.consume();
    }

    public void saveParseConfig(ActionEvent actionEvent) {
        final Window parent = ((Node) actionEvent.getTarget()).getScene().getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Parser Config");
        BatchProcessConfig batchProcessConfig;
        if (APP_CONTEXT.getBatchProcessConfig().isPresent()) {
            batchProcessConfig = APP_CONTEXT.getBatchProcessConfig().get();
        } else {
            LOGGER.warn("batch process config is null?!");
            actionEvent.consume();
            return;
        }
        Optional<ParserConfig> parserConfigOptional = batchProcessConfig.getParserConfig();
        if (parserConfigOptional.isPresent()) {
            Optional<Path> path = parserConfigOptional.get().getPath();
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
        //TODO: can we rely on showSaveDialog to warn the user about
        //overwriting an existing file?
        if (!Files.isDirectory(file.getParentFile().toPath())) {
            try {
                Files.createDirectories(file.getParentFile().toPath());
            } catch (IOException e) {
                LOGGER.warn("failed to create parent directory: " + file.getParent());
                actionEvent.consume();
                return;
            }
        }
        try {
            FileUtils.write(file, parserConfigXML.getText(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("couldn't write file: " + file, e);
            actionEvent.consume();
            return;
        }
        ParserConfig parserConfig = parserConfigOptional.orElse(new ParserConfig());
        parserConfig.setPath(file.toPath());
        batchProcessConfig.setParserConfig(parserConfig);
        APP_CONTEXT.saveState();

        actionEvent.consume();
    }

    public void clearParseConfig(ActionEvent actionEvent) {
        parserConfigXML.setText("");
        parserConfigPath.setText("");
        if (APP_CONTEXT.getBatchProcessConfig().isEmpty()) {
            return;
        }
        if (APP_CONTEXT.getBatchProcessConfig().get().getParserConfig().isEmpty()) {
            return;
        }
        //nullify path
        APP_CONTEXT.getBatchProcessConfig().get().getParserConfig().get().setPath(null);
    }

    /*
    boolean overWriteFileDialog(Path path) {
        if (!Files.isRegularFile(path)) {
            return true;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Parser Config");
        alert.setContentText(
                "The file already exists. Do you want to " + "overwrite the file or cancel?");
        ButtonType overwrite = new ButtonType("Overwrite", ButtonBar.ButtonData.YES);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(overwrite, cancelButton);
        AtomicBoolean processAnyways = new AtomicBoolean(false);
        alert.showAndWait().ifPresent(type -> {
            if (type.getText().startsWith("Overwrite")) {
                processAnyways.set(true);
            } else if (type.getText().startsWith("Cancel")) {
                processAnyways.set(false);
            }
        });
        return processAnyways.get();
    }*/

}
