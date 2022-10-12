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
import org.tallison.tika.app.fx.ctx.AppContext;

import org.apache.tika.utils.StringUtils;

public class AdvancedBatchController implements Initializable {

    static AppContext APP_CONTEXT = AppContext.getInstance();

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

        digestOptions.getSelectionModel().select(
                APP_CONTEXT.getBatchProcessConfig().getDigest()
        );
        parseTimeoutSeconds.setText(
                Integer.toString(APP_CONTEXT.getBatchProcessConfig().getParseTimeoutSeconds()));
        memoryPerProcess.setText(
                Integer.toString(APP_CONTEXT.getBatchProcessConfig().getMaxMemMb())
        );
        numProcesses.setText(Integer.toString(APP_CONTEXT.getBatchProcessConfig().getNumProcesses()));
    }


    public void showConfig(MouseEvent mouseEvent) {

    }

    public void digestOptions(ActionEvent actionEvent) {
        //TODO -- allow multiple selections
        String selected = digestOptions.getSelectionModel().selectedItemProperty().get();
        APP_CONTEXT.getBatchProcessConfig().setDigest(selected);
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

        APP_CONTEXT.getBatchProcessConfig().setParseTimeoutSeconds(num);
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
        APP_CONTEXT.getBatchProcessConfig().setMaxMemMb(num);
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
        APP_CONTEXT.getBatchProcessConfig().setNumProcesses(num);
        APP_CONTEXT.saveState();
    }
}
