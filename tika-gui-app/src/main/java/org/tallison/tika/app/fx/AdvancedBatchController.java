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

import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tallison.tika.app.fx.ctx.AppContext;
import org.tallison.tika.app.fx.tools.BatchProcessConfig;

import org.apache.tika.utils.StringUtils;

public class AdvancedBatchController implements Initializable {

    static AppContext APP_CONTEXT = AppContext.getInstance();
    private static Logger LOGGER = LogManager.getLogger(BatchInputController.class);

    @FXML
    private ComboBox<String> digestOptions;

    @FXML
    private TextField parseTimeoutSeconds;
    @FXML
    private TextField memoryPerProcess;
    @FXML
    private TextField numProcesses;

    @Override
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {

        if (APP_CONTEXT.getBatchProcessConfig().isEmpty()) {
            LOGGER.warn("batch process config is empty?!");
            return;
        }
        BatchProcessConfig batchProcessConfig = APP_CONTEXT.getBatchProcessConfig().get();
        if (batchProcessConfig.getDigest().isPresent()) {
            digestOptions.getSelectionModel().select(batchProcessConfig.getDigest().get());
        }
        parseTimeoutSeconds.setText(
                Integer.toString(batchProcessConfig.getParseTimeoutSeconds()));
        memoryPerProcess.setText(
                Integer.toString(batchProcessConfig.getMaxMemMb())
        );
        numProcesses.setText(Integer.toString(batchProcessConfig.getNumProcesses()));
    }


    public void showConfig(MouseEvent mouseEvent) {

    }

    public void digestOptions(ActionEvent actionEvent) {
        //TODO -- allow multiple selections
        String selected = digestOptions.getSelectionModel().selectedItemProperty().get();
        if (APP_CONTEXT.getBatchProcessConfig().isEmpty()) {
            LOGGER.warn("batch process config should not be empty");
            actionEvent.consume();
            return;
        }
        APP_CONTEXT.getBatchProcessConfig().get().setDigest(selected);
        APP_CONTEXT.saveState();
    }

    public void setParseTimeoutSeconds(ActionEvent actionEvent) {
        String parseTimeoutString = parseTimeoutSeconds.getText();
        if (StringUtils.isBlank(parseTimeoutString)) {
            return;
        }
        int num = -1;
        try {
            num = Integer.parseInt(parseTimeoutString);
        } catch (NumberFormatException e) {
            //TODO -- alert
            return;
        }
        if (num < 0) {
            //TODO -- alert
            return;
        }

        APP_CONTEXT.getBatchProcessConfig().get().setParseTimeoutSeconds(num);
        APP_CONTEXT.saveState();

    }

    public void setMemoryPerProcessGb(ActionEvent actionEvent) {
        String memoryPerProcessText = memoryPerProcess.getText();
        if (StringUtils.isBlank(memoryPerProcessText)) {
            return;
        }
        int num = -1;
        try {
            num = Integer.parseInt(memoryPerProcessText);
        } catch (NumberFormatException e) {
            //TODO -- alert
            return;
        }
        if (num < 0) {
            //TODO -- alert
            return;
        }
        if (num > 100000) {
            //TODO -- alert
            return;
        }
        APP_CONTEXT.getBatchProcessConfig().get().setMaxMemMb(num);
        APP_CONTEXT.saveState();
    }

    public void setNumProcesses(ActionEvent actionEvent) {
        String numProcessesString = numProcesses.getText();
        if (StringUtils.isBlank(numProcessesString)) {
            return;
        }
        int num = -1;
        try {
            num = Integer.parseInt(numProcessesString);
        } catch (NumberFormatException e) {
            //TODO -- alert
            return;
        }
        if (num < 0) {
            //TODO -- alert
            return;
        }
        if (num > 1000) {
            //TODO -- alert
            return;
        }
        APP_CONTEXT.getBatchProcessConfig().get().setNumProcesses(num);
        APP_CONTEXT.saveState();
    }
}
