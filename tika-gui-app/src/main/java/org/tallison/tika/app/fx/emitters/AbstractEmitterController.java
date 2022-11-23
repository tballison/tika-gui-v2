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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import org.tallison.tika.app.fx.ControllerBase;
import org.tallison.tika.app.fx.batch.BatchProcessConfig;
import org.tallison.tika.app.fx.ctx.AppContext;
import org.tallison.tika.app.fx.metadata.MetadataRow;
import org.tallison.tika.app.fx.metadata.MetadataTuple;

import org.apache.tika.utils.StringUtils;

public abstract class AbstractEmitterController extends ControllerBase {
    private static final Logger LOGGER = LogManager.getLogger(AbstractEmitterController.class);
    static AppContext APP_CONTEXT = AppContext.getInstance();
    @FXML
    private final ObservableList<MetadataRow> metadataRows = FXCollections.observableArrayList();
    @FXML
    private TextField tikaField;
    @FXML
    private TextField outputField;
    @FXML
    private TextField propertyField;
    private Optional<Path> csvMetadataPath = Optional.empty();

    public ObservableList<MetadataRow> getMetadataRows() {
        return metadataRows;
    }

    abstract protected void saveState(boolean isValid);

    /**
     * This confirms the string is not empty and represents an
     * actual path that exists as a regular file.
     * <p>
     * It will silently do nothing if these conditions are not met.
     *
     * @param csvMetadataFilePath
     */
    protected void safelySetCsvMetadataPath(String csvMetadataFilePath) {
        if (StringUtils.isBlank(csvMetadataFilePath)) {
            return;
        }
        Path p = Paths.get(csvMetadataFilePath);
        if (Files.isRegularFile(p)) {
            csvMetadataPath = Optional.of(p);
        }
    }

    protected Optional<Path> getCsvMetadataPath() {
        return csvMetadataPath;
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

        if (csvMetadataPath.isPresent()) {
            Path parentDir = csvMetadataPath.get().getParent();
            if (Files.isDirectory(parentDir)) {
                fileChooser.setInitialDirectory(parentDir.toFile());
            }
        }
        File csvFile = fileChooser.showOpenDialog(parent);
        if (csvFile == null) {
            return;
        }
        csvMetadataPath = Optional.of(csvFile.toPath());
        //TODO -- warn about deleting existing data
        loadMetadataCSV(csvFile);
    }

    private void loadMetadataCSV(File csvFile) throws IOException {
        char delimiter =
                csvFile.getName().endsWith(".txt") || csvFile.getName().endsWith(".tsv") ? '\t' :
                        ',';
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
        saveState(false);
    }

    @FXML
    public void clearMetadata(ActionEvent actionEvent) {
        metadataRows.clear();
        saveState(false);
    }

    @FXML
    protected void addMetadataRow(ActionEvent event) {
        //TODO -- check that key doesn't already exist
        if (!StringUtils.isBlank(tikaField.getText()) &&
                !StringUtils.isBlank(outputField.getText())) {
            //check that property can be parsed to int > 0 if exists
            metadataRows.add(new MetadataRow(tikaField.getText(), outputField.getText(),
                    propertyField.getText()));
            tikaField.setText("");
            outputField.setText("");
            propertyField.setText("");
        }
        saveState(false);
    }


    protected List<MetadataTuple> getMetadataTuples() {
        List<MetadataTuple> metadataTuples = new ArrayList<>();
        for (MetadataRow metadataRow : getMetadataRows()) {
            metadataTuples.add(new MetadataTuple(metadataRow.getTika(), metadataRow.getOutput(),
                    metadataRow.getProperty()));
        }
        return metadataTuples;
    }

    protected void updateMetadataRows(List<MetadataTuple> metadataTuples) {
        metadataRows.clear();
        for (MetadataTuple t : metadataTuples) {
            metadataRows.add(new MetadataRow(t.getTika(), t.getOutput(), t.getProperty()));
        }
    }

    /**
     * This checks for empty keys and duplicate output keys
     *
     * @return
     */
    protected ValidationResult validateMetadataRows() {

        //Set<String> tika = new HashSet<>();
        //duplicate tika keys are ok?
        Set<String> output = new HashSet<>();
        int i = 0;
        for (MetadataTuple row : getMetadataTuples()) {
            String t = row.getTika();
            if (StringUtils.isBlank(t)) {
                return new ValidationResult(ValidationResult.VALIDITY.NOT_OK, "Blank Tika key",
                        "Blank Tika key",
                        "There's an empty Tika key in row " + i + ". The output value is: " +
                                row.getOutput());
            }
            String o = row.getOutput();
            if (StringUtils.isBlank(o)) {
                return new ValidationResult(ValidationResult.VALIDITY.NOT_OK, "Blank output key",
                        "Blank output key",
                        "There's an empty output key in row " + i + ". The Tika value is: " +
                                row.getTika());
            } else {
                if (output.contains(o)) {
                    return new ValidationResult(ValidationResult.VALIDITY.NOT_OK,
                            "Duplicate output key", "Duplicate output key",
                            "There's a duplicate output key '" + o + "'");
                }
            }
            output.add(o);
            i++;
        }
        return ValidationResult.OK;
    }


}
