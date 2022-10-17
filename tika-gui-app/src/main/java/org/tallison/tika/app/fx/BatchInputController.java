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

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.tallison.tika.app.fx.ctx.AppContext;
import org.tallison.tika.app.fx.tools.BatchProcessConfig;

import org.apache.tika.pipes.pipesiterator.fs.FileSystemPipesIterator;
import org.apache.tika.utils.StringUtils;

public class BatchInputController {

    private static AppContext APP_CONTEXT = AppContext.getInstance();

    @FXML
    private Button fsInputButton;

    public void fileSystemInputDirectorySelect(ActionEvent actionEvent) {
        final Window parent = ((Node) actionEvent.getTarget()).getScene().getWindow();
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Open Resource File");
        BatchProcessConfig batchProcessConfig = APP_CONTEXT.getBatchProcessConfig();
        if (batchProcessConfig.getFetcher() != null &&
                batchProcessConfig.getFetcher().getClazz() != null &&
                batchProcessConfig.getFetcher().getClazz().equals(Constants.FS_FETCHER_CLASS)) {
            String path = batchProcessConfig.getFetcher().getAttributes().get("basePath");
            if (!StringUtils.isBlank(path)) {
                directoryChooser.setInitialDirectory(new File(path));
            }
        }

        File directory = directoryChooser.showDialog(parent);
        if (directory == null) {
            return;
        }
        String label = "FileSystem: " + directory.getName();
        batchProcessConfig.setFetcher(label, Constants.FS_FETCHER_CLASS, "basePath",
                directory.toPath().toAbsolutePath().toString());
        batchProcessConfig.setPipesIterator(label, FileSystemPipesIterator.class.getName(),
                "basePath", directory.toPath().toAbsolutePath().toString());
        APP_CONTEXT.getBatchProcessConfig().setInputSelectedTab(0);
        APP_CONTEXT.saveState();
        ((Stage) fsInputButton.getScene().getWindow()).close();
    }

}
