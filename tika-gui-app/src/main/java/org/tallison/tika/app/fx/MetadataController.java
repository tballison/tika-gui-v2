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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tallison.tika.app.fx.ctx.AppContext;
import org.tallison.tika.app.fx.metadata.MetadataRow;
import org.tallison.tika.app.fx.tools.BatchProcessConfig;

import org.apache.tika.utils.StringUtils;

public class MetadataController {

    private static Logger LOGGER = LogManager.getLogger(TikaController.class);
    @FXML
    private final ObservableList<MetadataRow> rows = FXCollections.observableArrayList();
    AppContext APP_CONTEXT = AppContext.getInstance();
    @FXML
    private TextField tikaField;
    @FXML
    private TextField outputField;

    public ObservableList<MetadataRow> getRows() {
        return rows;
    }

    @FXML
    protected void addMetadataRow(ActionEvent event) {
        //TODO -- check that key doesn't already exist
        if (!StringUtils.isBlank(tikaField.getText()) &&
                !StringUtils.isBlank(outputField.getText())) {
            rows.add(new MetadataRow(tikaField.getText(), outputField.getText()));
            tikaField.setText("");
            outputField.setText("");
        }
        saveMetadataToContext();
    }

    @FXML
    public void loadMetadataCSV(ActionEvent actionEvent) throws IOException {
        final Window parent = ((Node) actionEvent.getTarget()).getScene().getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Metadata Mapping CSV");
        BatchProcessConfig batchProcessConfig = APP_CONTEXT.getBatchProcessConfig();

        File csvFile = fileChooser.showOpenDialog(parent);
        if (csvFile == null) {
            return;
        }
        //TODO -- warn about deleting existing data
        loadMetadataCSV(csvFile);
    }

    private void loadMetadataCSV(File csvFile) throws IOException {
        char delimiter = csvFile.getName().endsWith(".txt") ? '\t' : ',';
        //TODO add a reader that removes the BOM
        CSVFormat format = CSVFormat.Builder.create(CSVFormat.EXCEL).setDelimiter(delimiter)
                .setHeader() // no clue why this is needed,but it is
                .setSkipHeaderRecord(true).build();
        rows.clear();
        for (CSVRecord record : CSVParser.parse(csvFile, StandardCharsets.UTF_8, format)) {
            if (record.size() > 1) {
                rows.add(new MetadataRow(record.get(0), record.get(1)));
            } else if (record.size() == 1) {
                rows.add(new MetadataRow(record.get(0), record.get(0)));
            }
        }

        saveMetadataToContext();
    }

    @FXML
    public void clearMetadata(ActionEvent actionEvent) {
        rows.clear();
        saveMetadataToContext();
    }

    @FXML
    public void saveMetadataToContext(ActionEvent actionEvent) {
        saveMetadataToContext();
    }

    public void saveMetadataToContext() {
        APP_CONTEXT.getBatchProcessConfig().getMetadataMapper().getAttributes().clear();
        for (MetadataRow metadataRow : rows) {
            APP_CONTEXT.getBatchProcessConfig().getMetadataMapper().getAttributes()
                    .put(metadataRow.getTika(), metadataRow.getOutput());
        }
        APP_CONTEXT.saveState();
    }
}
