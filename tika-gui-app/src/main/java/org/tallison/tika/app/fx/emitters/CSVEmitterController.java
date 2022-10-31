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
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tallison.tika.app.fx.Constants;
import org.tallison.tika.app.fx.ctx.AppContext;
import org.tallison.tika.app.fx.metadata.MetadataRow;
import org.tallison.tika.app.fx.metadata.MetadataTuple;
import org.tallison.tika.app.fx.tools.BatchProcessConfig;
import org.tallison.tika.app.fx.tools.ConfigItem;

import org.apache.tika.utils.StringUtils;

public class CSVEmitterController implements Initializable {

    @FXML
    private final ObservableList<MetadataRow> metadataRows = FXCollections.observableArrayList();
    private final static int TAB_INDEX = 1;
    private static AppContext APP_CONTEXT = AppContext.getInstance();
    private static Logger LOGGER = LogManager.getLogger(OpenSearchEmitterController.class);




    public ObservableList<MetadataRow> getMetadataRows() {
        return metadataRows;
    }

    @FXML
    private TextField csvFileName;

    @FXML
    private Button updateCSV;
    @FXML
    private TextField tikaField;
    @FXML
    private TextField outputField;
    @FXML
    private TextField propertyField;

    private Optional<File> directory = Optional.empty();

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
            metadataRows.clear();
            for (MetadataTuple t : emitter.getMetadataTuples().get()) {
                metadataRows.add(new MetadataRow(t.getTika(), t.getOutput(), t.getProperty()));
            }
        }
        if (emitter.getAttributes().containsKey("basePath")) {
            File directory = new File(emitter.getAttributes().get("basePath"));
            if (directory.isDirectory()) {
                this.directory = Optional.of(directory);
            }
        }
        if (emitter.getAttributes().containsKey("csvFileName")) {
            csvFileName.setText(emitter.getAttributes().get("csvFileName"));
        }

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
        this.directory = Optional.of(directory);
    }

    @FXML
    public void loadMetadataCSV(ActionEvent actionEvent) throws IOException {
        final Window parent = ((Node) actionEvent.getTarget()).getScene().getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Metadata Mapping CSV");
        Optional<BatchProcessConfig> batchProcessConfig = APP_CONTEXT.getBatchProcessConfig();
        if (batchProcessConfig.isEmpty()) {
            LOGGER.warn("batch process config should not be null!");
            actionEvent.consume();
            return;
        }

        if (directory.isPresent()) {
            if(directory.get().isDirectory()) {
                fileChooser.setInitialDirectory(directory.get());
            }
        }
        File csvFile = fileChooser.showOpenDialog(parent);
        if (csvFile == null) {
            return;
        }
        //TODO -- warn about deleting existing data
        loadMetadataCSV(csvFile);
    }

    private void loadMetadataCSV(File csvFile) throws IOException {
        char delimiter = csvFile.getName().endsWith(".txt") ||
                csvFile.getName().endsWith(".tsv") ?
                '\t' : ',';
        //TODO add a reader that removes the BOM
        CSVFormat format = CSVFormat.Builder.create(CSVFormat.EXCEL).setDelimiter(delimiter)
                .setHeader() // no clue why this is needed,but it is
                .setSkipHeaderRecord(true).build();
        metadataRows.clear();
        for (CSVRecord record : CSVParser.parse(csvFile, StandardCharsets.UTF_8, format)) {
            List<String> data = new ArrayList<>();
            if (record.size() > 2) {
                metadataRows.add(new MetadataRow(record.get(0), record.get(1), record.get(2)));
            } else if (record.size() > 1) {
                metadataRows.add(new MetadataRow(record.get(0), record.get(1), ""));
            } else if (record.size() == 1) {
                metadataRows.add(new MetadataRow(record.get(0), record.get(0), ""));
            }
        }
        saveState();
    }

    @FXML
    public void clearMetadata(ActionEvent actionEvent) {
        metadataRows.clear();
        saveState();
    }

    @FXML
    protected void addMetadataRow(ActionEvent event) {
        //TODO -- check that key doesn't already exist
        if (!StringUtils.isBlank(tikaField.getText()) &&
                !StringUtils.isBlank(outputField.getText())) {
            //check that property can be parsed to int > 0 if exists
            metadataRows.add(new MetadataRow(tikaField.getText(),
                    outputField.getText(), propertyField.getText()));
            tikaField.setText("");
            outputField.setText("");
            propertyField.setText("");
        }
        saveState();
    }

    private void saveState() {
        String label = StringUtils.EMPTY;
        String csvOutputFileString = StringUtils.EMPTY;
        String directoryString = StringUtils.EMPTY;

        if (directory.isPresent()) {
            directoryString = directory.get().getAbsolutePath();
        }
        if (csvFileName != null) {
            String fString = csvFileName.getText();
            if (!StringUtils.isBlank(fString)) {
                label = "CSV file: " + fString;
                csvOutputFileString = fString;
            }
        }


        ConfigItem emitter = ConfigItem.build(label, Constants.CSV_EMITTER_CLASS, "basePath",
                directoryString, "csvFileName", csvOutputFileString);
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

    private void saveMetadataToEmitter(ConfigItem emitter) {
        List<MetadataTuple> metadataTuples = new ArrayList<>();
        for (MetadataRow metadataRow : metadataRows) {
            metadataTuples.add(new MetadataTuple(metadataRow.getTika(),
                    metadataRow.getOutput(), metadataRow.getProperty()));
        }
        emitter.setMetadataTuples(metadataTuples);
    }

    public void updateCSV(ActionEvent actionEvent) {
        saveState();
        ((Stage)updateCSV.getScene().getWindow()).close();
    }
}
