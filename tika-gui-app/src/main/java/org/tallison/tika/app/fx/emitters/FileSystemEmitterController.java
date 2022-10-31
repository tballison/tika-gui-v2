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
import org.tallison.tika.app.fx.Constants;
import org.tallison.tika.app.fx.ctx.AppContext;
import org.tallison.tika.app.fx.tools.BatchProcessConfig;
import org.tallison.tika.app.fx.tools.ConfigItem;

import org.apache.tika.utils.StringUtils;

public class FileSystemEmitterController {
    private static AppContext APP_CONTEXT = AppContext.getInstance();
    private static Logger LOGGER = LogManager.getLogger(FileSystemEmitterController.class);

    @FXML
    private Button fsOutputButton;

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
                String path = emitter.get().getAttributes().get("basePath");
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
        String label = "FileSystem: " + directory.getName();
        batchProcessConfig.setEmitter(label, Constants.FS_EMITTER_CLASS, "basePath",
                directory.toPath().toAbsolutePath().toString());

        //TODO -- do better than hard coding indices
        batchProcessConfig.setOutputSelectedTab(0);
        APP_CONTEXT.saveState();

        ((Stage) fsOutputButton.getScene().getWindow()).close();
    }

}
