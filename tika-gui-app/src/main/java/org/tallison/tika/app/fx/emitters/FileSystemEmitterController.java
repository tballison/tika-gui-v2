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
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tallison.tika.app.fx.Constants;
import org.tallison.tika.app.fx.ControllerBase;
import org.tallison.tika.app.fx.ctx.AppContext;
import org.tallison.tika.app.fx.metadata.MetadataRow;
import org.tallison.tika.app.fx.metadata.MetadataTuple;
import org.tallison.tika.app.fx.tools.BatchProcessConfig;
import org.tallison.tika.app.fx.tools.ConfigItem;

import org.apache.tika.utils.StringUtils;

public class FileSystemEmitterController extends AbstractEmitterController implements
        Initializable {
    private static AppContext APP_CONTEXT = AppContext.getInstance();
    private static Logger LOGGER = LogManager.getLogger(FileSystemEmitterController.class);

    @FXML
    private Button fsOutputButton;

    @FXML
    private Accordion fsAccordion;

    private Optional<Path> directory = Optional.empty();


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

        ConfigItem emitter = APP_CONTEXT.getBatchProcessConfig().get().getEmitter().get();
        if (! emitter.getClazz().equals(Constants.FS_EMITTER_CLASS)) {
            return;
        }
        if (emitter.getMetadataTuples().isPresent() && emitter.getMetadataTuples().get().size() > 0) {
            getMetadataRows().clear();
            for (MetadataTuple t : emitter.getMetadataTuples().get()) {
                getMetadataRows().add(new MetadataRow(t.getTika(), t.getOutput(), t.getProperty()));
            }
        }

    }

    public void fileSystemOutputDirectorySelect(ActionEvent actionEvent) {

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
                    emitter.get().getClazz().equals(Constants.FS_EMITTER_CLASS)) {
                String path = emitter.get().getAttributes().get(Constants.BASE_PATH);
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
        this.directory = Optional.of(directory.toPath());
        saveState();
        ((Stage) fsOutputButton.getScene().getWindow()).close();
    }

    @Override
    protected void saveState() {
        if (APP_CONTEXT.getBatchProcessConfig().isEmpty()) {
            return;
        }
        //TODO -- add validate on metadata data
        BatchProcessConfig bpc = APP_CONTEXT.getBatchProcessConfig().get();
        if (directory.isPresent()) {
            Path p = directory.get();
            String shortLabel = "FileSystem: " + ellipsize(p.getFileName().toString(), 30);
            String fullLabel = "FileSystem: " + p.toAbsolutePath();
            ConfigItem emitter = ConfigItem.build(
                    shortLabel, fullLabel, Constants.FS_EMITTER_CLASS,
                    Constants.BASE_PATH, p.toAbsolutePath().toString()
            );
            saveMetadataToEmitter(emitter);
            bpc.setEmitter(emitter);
        }
        //TODO -- do better than hard coding indices
        bpc.setOutputSelectedTab(0);
        APP_CONTEXT.saveState();
    }

}
