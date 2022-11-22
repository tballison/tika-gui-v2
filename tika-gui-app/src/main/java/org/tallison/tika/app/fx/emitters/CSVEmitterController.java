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

import static org.tallison.tika.app.fx.emitters.CSVEmitterSpec.CSV_DB_TABLE_NAME;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kordamp.ikonli.javafx.FontIcon;
import org.tallison.tika.app.fx.Constants;
import org.tallison.tika.app.fx.batch.BatchProcessConfig;
import org.tallison.tika.app.fx.config.ConfigItem;
import org.tallison.tika.app.fx.metadata.MetadataRow;
import org.tallison.tika.app.fx.metadata.MetadataTuple;
import org.tallison.tika.app.fx.utils.OptionalUtil;

import org.apache.tika.pipes.emitter.AbstractEmitter;
import org.apache.tika.utils.StringUtils;

public class CSVEmitterController extends AbstractEmitterController implements Initializable {

    private final static int TAB_INDEX = 1;

    private static final String ALERT_TITLE = "JDBC Emitter";

    private static final Logger LOGGER = LogManager.getLogger(CSVEmitterController.class);

    @FXML
    private TextField csvFileName;

    @FXML
    private Button updateCSV;

    @FXML
    private Accordion csvAccordion;

    @FXML
    private FontIcon readyIcon;

    @FXML
    private FontIcon notReadyIcon;

    @FXML
    private TextField csvDirectory;


    private Optional<File> csvWorkingDirectory = Optional.empty();

    @Override
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        //Not clear why expanded=true is not working in fxml
        csvAccordion.setExpandedPane(csvAccordion.getPanes().get(0));

        if (APP_CONTEXT.getBatchProcessConfig().isEmpty()) {
            LOGGER.warn("batch process config is empty?!");
            return;
        }
        BatchProcessConfig batchProcessConfig = APP_CONTEXT.getBatchProcessConfig().get();
        Optional<EmitterSpec> optionalEmitterSpec = batchProcessConfig.getEmitter();
        if (optionalEmitterSpec.isEmpty()) {
            return;
        }
        if (!(optionalEmitterSpec.get() instanceof CSVEmitterSpec emitter)) {
            return;
        }

        if (emitter.getMetadataTuples().size() > 0) {
            getMetadataRows().clear();
            for (MetadataTuple t : emitter.getMetadataTuples()) {
                getMetadataRows().add(new MetadataRow(t.getTika(), t.getOutput(), t.getProperty()));
            }
        }
        if (emitter.getCsvDirectory().isPresent()) {
            File directory = emitter.getCsvDirectory().get().toFile();
            if (directory.isDirectory()) {
                this.csvWorkingDirectory = Optional.of(directory);
                this.csvDirectory.setText(directory.getName());
            }
        }
        if (!OptionalUtil.isEmpty(emitter.getCsvFileName())) {
            csvFileName.setText(emitter.getCsvFileName().get());
        }
        //safelySetCsvMetadataPath(emitter.getAttributes().get(Constants.CSV_METADATA_PATH));
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
        Optional<EmitterSpec> emitter = batchProcessConfig.getEmitter();
        if (emitter.isPresent() && emitter.get() instanceof CSVEmitterSpec) {
            if (((CSVEmitterSpec) emitter.get()).getCsvDirectory().isPresent()) {
                File f = ((CSVEmitterSpec) emitter.get()).getCsvDirectory().get().toFile();
                if (f.isDirectory()) {
                    directoryChooser.setInitialDirectory(f);
                }
            }
        }
        File directory = directoryChooser.showDialog(parent);
        if (directory == null) {
            return;
        }
        this.csvWorkingDirectory = Optional.of(directory);
        this.csvDirectory.setText(directory.getName());
    }


    @Override
    public void saveState() {
        String shortLabel = StringUtils.EMPTY;
        String fullLabel = StringUtils.EMPTY;
        String csvOutputFileString = StringUtils.EMPTY;
        String directoryString = StringUtils.EMPTY;

        if (csvWorkingDirectory.isPresent()) {
            directoryString = csvWorkingDirectory.get().getAbsolutePath();
        }
        if (csvFileName != null) {
            String fString = csvFileName.getText();
            if (!StringUtils.isBlank(fString)) {
                shortLabel = "CSV file: " + ellipsize(fString, 30);
                fullLabel = "CSV file: " + fString;
                csvOutputFileString = fString;
            }
        }

        Optional<Path> csvMetadataPath = getCsvMetadataPath();
        String csvMetadataPathString =
                csvMetadataPath.isPresent() ? csvMetadataPath.get().toAbsolutePath().toString() :
                        StringUtils.EMPTY;

        CSVEmitterSpec emitter = new CSVEmitterSpec(getMetadataTuples());
        emitter.setCsvDirectory(csvWorkingDirectory.get().toPath());
        emitter.setCsvFileName(csvOutputFileString);


        List<MetadataTuple> metadataTuples = emitter.getMetadataTuples();
        List<MetadataTuple> updatedTuples = new ArrayList<>();
        for (MetadataTuple t : metadataTuples) {
            updatedTuples.add(new MetadataTuple(t.getTika(), t.getOutput(), "VARCHAR(32000)"));
        }

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


    private void addInsertSQL(BaseEmitterSpec emitter) {
        StringBuilder sb = new StringBuilder();
        String tableName = CSV_DB_TABLE_NAME;
        sb.append("insert into ").append(tableName);
        sb.append(" (path, attachment_num");
        int cols = 2;
        for (MetadataTuple t : emitter.getMetadataTuples()) {
            sb.append(", " + t.getOutput());
            cols++;
        }
        sb.append(") values (");
        for (int i = 0; i < cols; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("?");
        }
        sb.append(")");
        LOGGER.trace("insert sql: " + sb);
        ///emitter.getAttributes().put(Constants.CSV_JDBC_INSERT_SQL, sb.toString());
    }

    public void updateCSV(ActionEvent actionEvent) {
        /*
        saveState();

        if (APP_CONTEXT.getBatchProcessConfig().isEmpty()) {
            return;
        }
        if (APP_CONTEXT.getBatchProcessConfig().get().getEmitter().isEmpty()) {
            return;
        }
        ConfigItem emitter = APP_CONTEXT.getBatchProcessConfig().get().getEmitter().get();
        Map<String, String> attributes = emitter.getAttributes();
        try {
            CSVEmitterHelper.setUp(emitter);
        } catch (IOException e) {
            LOGGER.error("can't create tmp directory");
            return;
        }
        String csvDir = attributes.get(Constants.BASE_PATH);
        if (StringUtils.isBlank(csvDir)) {
            return;
        }
        Path basePath = Paths.get(csvDir);
        if (!Files.isDirectory(basePath)) {
            try {
                Files.createDirectories(basePath);
            } catch (IOException e) {
                alertStackTrace("Can't create directory", "Can't create directory for csv",
                        "Can't create " + basePath.toAbsolutePath(), e);
                actionEvent.consume();
                return;
            }
        }
        String csvFileName = attributes.get(Constants.CSV_FILE_NAME);
        if (StringUtils.isBlank(csvFileName)) {
            LOGGER.debug("empty csv file name");
            return;
        }
        Path csvFile = basePath.resolve(csvFileName);
        boolean success = true;
        if (Files.isRegularFile(csvFile)) {
            success = deleteCSVFileDialog(csvFile);
        }
        if (!success) {
            LOGGER.warn("didn't delete csv file");
            actionEvent.consume();
            return;
        }

        try {
            CSVEmitterHelper.createTable(emitter);
        } catch (SQLException e) {
            alertStackTrace("Couldn't create tmp table", "Couldn't create tmp table",
                    "Couldn't create tmp table", e);
            actionEvent.consume();
            return;
        }
        LOGGER.debug("success, all good; close window");
        readyIcon.setVisible(true);
        notReadyIcon.setVisible(false);
*/
        ((Stage) updateCSV.getScene().getWindow()).close();
    }

    private boolean deleteCSVFileDialog(Path csvFile) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(ALERT_TITLE);
        alert.setContentText("CSV file (" + csvFile.getFileName() + ") exists. Delete it?");
        ButtonType dropButton = new ButtonType("Delete CSV file", ButtonBar.ButtonData.YES);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(dropButton, cancelButton);
        final AtomicBoolean success = new AtomicBoolean(false);
        alert.showAndWait().ifPresent(type -> {
            if (type.getText().startsWith("Delete")) {
                try {
                    Files.delete(csvFile);
                    success.set(true);
                } catch (IOException e) {
                    alert(ALERT_TITLE, "Couldn't delete csv file",
                            "Couldn't delete file: " + csvFile.toAbsolutePath());
                }
            } else if (type.getText().startsWith("Cancel")) {
            }
        });
        return success.get();
    }
}
