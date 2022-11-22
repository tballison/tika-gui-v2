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
import java.util.Optional;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tallison.tika.app.fx.batch.BatchProcessConfig;
import org.tallison.tika.app.fx.config.ConfigItem;
import org.tallison.tika.app.fx.ctx.AppContext;

import org.apache.tika.pipes.pipesiterator.fs.FileSystemPipesIterator;
import org.apache.tika.utils.StringUtils;

public class BatchInputController extends ControllerBase {

    private static final AppContext APP_CONTEXT = AppContext.getInstance();
    private static final Logger LOGGER = LogManager.getLogger(BatchInputController.class);

    @FXML
    private Button fsInputButton;

    public void fileSystemInputDirectorySelect(ActionEvent actionEvent) {
        final Window parent = ((Node) actionEvent.getTarget()).getScene().getWindow();
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Open Resource File");
        if (!APP_CONTEXT.getBatchProcessConfig().isPresent()) {
            LOGGER.warn("BatchProcessConfig must not be empty");
            actionEvent.consume();
            return;
        }
        BatchProcessConfig batchProcessConfig = APP_CONTEXT.getBatchProcessConfig().get();
        Optional<ConfigItem> fetcher = batchProcessConfig.getFetcher();
        if (fetcher.isPresent() && fetcher.get().getClazz().equals(Constants.FS_FETCHER_CLASS)) {
            String path = fetcher.get().getAttributes().get("basePath");
            if (!StringUtils.isBlank(path)) {
                File f = new File(path);
                if (f.isDirectory()) {
                    directoryChooser.setInitialDirectory(f);
                }
            }
        }

        File directory = directoryChooser.showDialog(parent);
        if (directory == null) {
            return;
        }
        String shortLabel = "FileSystem: " + ellipsize(directory.getName(), 30);
        String fullLabel = "FileSystem: " + directory.getAbsolutePath();

        batchProcessConfig.setFetcher(shortLabel, fullLabel, Constants.FS_FETCHER_CLASS, "basePath",
                directory.toPath().toAbsolutePath().toString());
        batchProcessConfig.setPipesIterator(shortLabel, fullLabel,
                FileSystemPipesIterator.class.getName(), "basePath",
                directory.toPath().toAbsolutePath().toString());
        batchProcessConfig.setInputSelectedTab(0);
        APP_CONTEXT.saveState();
        ((Stage) fsInputButton.getScene().getWindow()).close();
    }


}
