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
import java.nio.file.Path;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tallison.tika.app.fx.Constants;
import org.tallison.tika.app.fx.metadata.MetadataRow;
import org.tallison.tika.app.fx.metadata.MetadataTuple;
import org.tallison.tika.app.fx.tools.BatchProcessConfig;
import org.tallison.tika.app.fx.tools.ConfigItem;

import org.apache.tika.utils.StringUtils;

public class CSVEmitterController extends AbstractEmitterController implements Initializable {

    private final static int TAB_INDEX = 1;
    private static Logger LOGGER = LogManager.getLogger(OpenSearchEmitterController.class);

    @FXML
    private TextField csvFileName;

    @FXML
    private Button updateCSV;



    private Optional<File> csvWorkingDirectory = Optional.empty();

    @Override
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {

        if (APP_CONTEXT.getBatchProcessConfig().isEmpty()) {
            LOGGER.warn("batch process config is empty?!");
            return;
        }
        BatchProcessConfig batchProcessConfig = APP_CONTEXT.getBatchProcessConfig().get();
        Optional<ConfigItem> configItem = batchProcessConfig.getEmitter();
        if (configItem.isEmpty()) {
            return;
        }
        ConfigItem emitter = configItem.get();
        if (! emitter.getClazz().equals(Constants.CSV_EMITTER_CLASS)) {
            return;
        }
        if (emitter.getMetadataTuples().isPresent() && emitter.getMetadataTuples().get().size() > 0) {
            getMetadataRows().clear();
            for (MetadataTuple t : emitter.getMetadataTuples().get()) {
                getMetadataRows().add(new MetadataRow(t.getTika(), t.getOutput(), t.getProperty()));
            }
        }
        if (emitter.getAttributes().containsKey(Constants.BASE_PATH)) {
            File directory = new File(emitter.getAttributes().get(Constants.BASE_PATH));
            if (directory.isDirectory()) {
                this.csvWorkingDirectory = Optional.of(directory);
            }
        }
        if (emitter.getAttributes().containsKey(Constants.CSV_FILE_NAME)) {
            csvFileName.setText(emitter.getAttributes().get(Constants.CSV_FILE_NAME));
        }

        safelySetCsvMetadataPath(emitter.getAttributes().get(Constants.CSV_METADATA_PATH));

    }

    public void selectCSVOutputDirectory(ActionEvent actionEvent) {
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
        Optional<ConfigItem> emitter = batchProcessConfig.getEmitter();
        if (emitter.isPresent()) {
            if (emitter.get().getClazz() != null &&
                    emitter.get().getClazz().equals(Constants.CSV_EMITTER_CLASS)) {
                String path = emitter.get().getAttributes().get("directory");
                if (!StringUtils.isBlank(path)) {
                    File f = new File(path);
                    if (f.isDirectory()) {
                        directoryChooser.setInitialDirectory(f);
                    }
                }
            }
        }
        File directory = directoryChooser.showDialog(parent);
        if (directory == null) {
            return;
        }
        this.csvWorkingDirectory = Optional.of(directory);
    }


    @Override
    public void saveState() {
        String label = StringUtils.EMPTY;
        String csvOutputFileString = StringUtils.EMPTY;
        String directoryString = StringUtils.EMPTY;

        if (csvWorkingDirectory.isPresent()) {
            directoryString = csvWorkingDirectory.get().getAbsolutePath();
        }
        if (csvFileName != null) {
            String fString = csvFileName.getText();
            if (!StringUtils.isBlank(fString)) {
                label = "CSV file: " + fString;
                csvOutputFileString = fString;
            }
        }

        Optional<Path> csvMetadataPath = getCsvMetadataPath();
        String csvMetadataPathString = csvMetadataPath.isPresent() ?
                csvMetadataPath.get().toAbsolutePath().toString() : StringUtils.EMPTY;

        ConfigItem emitter = ConfigItem.build(label, Constants.CSV_EMITTER_CLASS,
                Constants.BASE_PATH, directoryString,
                Constants.CSV_FILE_NAME, csvOutputFileString,
                Constants.CSV_METADATA_PATH, csvMetadataPathString);

        saveMetadataToEmitter(emitter);
        if (APP_CONTEXT.getBatchProcessConfig().isEmpty()) {
            LOGGER.warn("no app context?!");
            return;
        }
        BatchProcessConfig batchProcessConfig = APP_CONTEXT.getBatchProcessConfig().get();
        batchProcessConfig.setEmitter(emitter);
        //TODO -- do better than hard coding indices
        batchProcessConfig.setOutputSelectedTab(TAB_INDEX);
        APP_CONTEXT.saveState();
    }

    public void updateCSV(ActionEvent actionEvent) {
        saveState();
        ((Stage)updateCSV.getScene().getWindow()).close();
    }
}
