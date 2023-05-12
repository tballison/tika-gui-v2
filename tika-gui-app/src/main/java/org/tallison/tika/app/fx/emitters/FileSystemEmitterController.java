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
package org.tallison.tika.app.fx.emitters;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tallison.tika.app.fx.batch.BatchProcessConfig;
import org.tallison.tika.app.fx.ctx.AppContext;

public class FileSystemEmitterController extends AbstractEmitterController
        implements Initializable {
    private static final AppContext APP_CONTEXT = AppContext.getInstance();
    private static final Logger LOGGER = LogManager.getLogger(FileSystemEmitterController.class);

    @FXML
    private Button fsOutputButton;

    @FXML
    private Accordion fsAccordion;

    @FXML
    CheckBox prettyPrintJson;

    private Optional<Path> directory = Optional.empty();

    private boolean overWriteNonEmptyDirectory = false;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //Not clear why expanded=true is not working in fxml
        fsAccordion.setExpandedPane(fsAccordion.getPanes().get(0));

        if (APP_CONTEXT.getBatchProcessConfig().isEmpty()) {
            return;
        }
        if (APP_CONTEXT.getBatchProcessConfig().get().getEmitter().isEmpty()) {
            return;
        }

        EmitterSpec emitter = APP_CONTEXT.getBatchProcessConfig().get().getEmitter().get();
        if (!(emitter instanceof FileSystemEmitterSpec)) {
            return;
        }
        if (((FileSystemEmitterSpec) emitter).getBasePath().isPresent()) {
            directory = Optional.of(((FileSystemEmitterSpec) emitter).getBasePath().get());
        }

        prettyPrintJson.setSelected(((FileSystemEmitterSpec)emitter).isPrettyPrint());
        updateMetadataRows(((FileSystemEmitterSpec) emitter).getMetadataTuples());

    }

    public void fileSystemOutputDirectorySelect(ActionEvent actionEvent) {
        //every time someone selects a new directory, set this to false!
        overWriteNonEmptyDirectory = false;
        final Window parent = ((Node) actionEvent.getTarget()).getScene().getWindow();

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Open Target Directory");
        BatchProcessConfig batchProcessConfig;
        if (APP_CONTEXT.getBatchProcessConfig().isPresent()) {
            batchProcessConfig = APP_CONTEXT.getBatchProcessConfig().get();
        } else {
            LOGGER.warn("batch process config is null?!");
            actionEvent.consume();
            return;
        }
        Optional<EmitterSpec> emitter = batchProcessConfig.getEmitter();
        if (emitter.isPresent()) {
            if (emitter.get() instanceof FileSystemEmitterSpec) {
                Optional<Path> path = ((FileSystemEmitterSpec) emitter.get()).getBasePath();
                if (path.isPresent()) {
                    File f = path.get().toFile();
                    if (f.isDirectory()) {
                        directoryChooser.setInitialDirectory(f);
                    }
                }
            }
        }
        File directory = directoryChooser.showDialog(parent);
        if (directory == null) {
            actionEvent.consume();
            return;
        }
        this.directory = Optional.of(directory.toPath());
        boolean success = checkForEmptyDirectory(this.directory.get());
        saveState(success);
        if (success) {
            ((Stage) fsOutputButton.getScene().getWindow()).close();
        } else {
            actionEvent.consume();
        }
    }

    private boolean checkForEmptyDirectory(Path path) {
        if (Files.isRegularFile(path)) {
            alert("File System Emitter", "Output directory is a file?!",
                    "Output directory is a file?! How in the world did you pull this off?!");
            return false;
        }
        if (Files.isDirectory(path)) {
            LOGGER.debug("fs emitter output directory exists {}", path);
            File[] files = path.toFile().listFiles();
            if (files.length > 0) {
                overWriteNonEmptyDirectory = overWriteNonEmptyDirectoryDialog(path);
                return overWriteNonEmptyDirectory;
            }
        }
        return true;
    }

    @Override
    protected void saveState(boolean validated) {
        if (APP_CONTEXT.getBatchProcessConfig().isEmpty()) {
            return;
        }
        //TODO -- add validate on metadata data
        BatchProcessConfig bpc = APP_CONTEXT.getBatchProcessConfig().get();
        if (directory.isPresent()) {
            Path p = directory.get();
            String shortLabel = "FileSystem: " + ellipsize(p.getFileName().toString(), 30);
            String fullLabel = "FileSystem: " + p.toAbsolutePath();
            EmitterSpec emitter = new FileSystemEmitterSpec(getMetadataTuples());
            ((FileSystemEmitterSpec) emitter).setBasePath(p);
            emitter.setShortLabel(shortLabel);
            emitter.setFullLabel(fullLabel);
            emitter.setValid(validated);
            ((FileSystemEmitterSpec) emitter).setOverWriteNonEmptyDirectory(
                    overWriteNonEmptyDirectory);
            ((FileSystemEmitterSpec) emitter).setPrettyPrint(prettyPrintJson.isSelected());
            bpc.setEmitter(emitter);
        }

        //TODO -- do better than hard coding indices
        bpc.setOutputSelectedTab(0);
        APP_CONTEXT.saveState();
    }

    private boolean overWriteNonEmptyDirectoryDialog(Path path) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("File System Emitter");
        alert.setContentText("The output directory has contents. Do you want to " +
                "potentially overwrite data in" + " that directory or cancel?");
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
    }
}
