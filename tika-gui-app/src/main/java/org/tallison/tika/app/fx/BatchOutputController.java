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
import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.tallison.tika.app.fx.ctx.AppContext;
import org.tallison.tika.app.fx.tools.BatchProcessConfig;
import org.tallison.tika.app.fx.tools.ConfigItem;

import org.apache.tika.utils.StringUtils;

public class BatchOutputController implements Initializable {

    private static AppContext APP_CONTEXT = AppContext.getInstance();

    //TODO -- this is bad
    private static final Pattern SIMPLE_URL_PATTERN =
            Pattern.compile("(?i)^https?:\\/\\/[-_a-z0-9\\.]+(?::\\d+)?\\/([-_a-z0-9\\.]+)");

    @FXML
    private Button fsOutputButton;

    @FXML
    private TextField openSearchUrl;

    @FXML
    private TextField openSearchUserName;
    @FXML
    private PasswordField openSearchPassword;

    @FXML
    private ComboBox<String> openSearchUpdateStrategy;

    @Override
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        ConfigItem configItem = APP_CONTEXT.getBatchProcessConfig().getEmitter();
        if (configItem.getClazz().equals(Constants.OPEN_SEARCH_EMITTER_CLASS)) {
            //TODO -- get the last selected version out of the app context
        }
        openSearchUpdateStrategy.getSelectionModel().select("Upsert");
    }

    public void fileSystemOutputDirectorySelect(ActionEvent actionEvent) {
        final Window parent = ((Node) actionEvent.getTarget()).getScene().getWindow();
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Open Target Directory");
        BatchProcessConfig batchProcessConfig = APP_CONTEXT.getBatchProcessConfig();

        if (batchProcessConfig.getEmitter() != null &&
                batchProcessConfig.getEmitter().getClazz() != null &&
                batchProcessConfig.getEmitter().getClazz().equals(Constants.FS_EMITTER_CLASS)) {
            String path = batchProcessConfig.getEmitter().getAttributes().get("basePath");
            if (!StringUtils.isBlank(path)) {
                directoryChooser.setInitialDirectory(new File(path));
            }
        }
        File directory = directoryChooser.showDialog(parent);
        if (directory == null) {
            return;
        }
        String label = "FileSystem: " + directory.getName();
        batchProcessConfig.setEmitter(label, Constants.FS_EMITTER_CLASS, "basePath",
                directory.toPath().toAbsolutePath().toString());

        APP_CONTEXT.saveState();
        ((Stage) fsOutputButton.getScene().getWindow()).close();
    }

    public void updateOpenSearchEmitter(ActionEvent actionEvent) {
        BatchProcessConfig batchProcessConfig = APP_CONTEXT.getBatchProcessConfig();
        //TODO -- check that all required information is here...at least the url
        //check that the url includes an index and is not the bare url

        String url = openSearchUrl.getText();
        String index = getIndex(url);
        if (StringUtils.isEmpty(url)) {
            alert("Missing URL?",
                    "Must specify a url including the index, " +
                            "e.g. https://localhost:9500/my-index");
            actionEvent.consume();
            return;

        }
        if (StringUtils.isEmpty(index)) {
            alert("Missing index?", "Please specify an index, I only see: " + url);
            actionEvent.consume();
            return;
        }
        String label = "OpenSearch: " + index;
        String userName = openSearchUserName.getText();
        String password = openSearchPassword.getText();
        if (StringUtils.isEmpty(userName) && ! StringUtils.isEmpty(password)) {
            alert("Credentials?", "Password with no username?!");
            actionEvent.consume();
            return;
        }

        if (StringUtils.isEmpty(password) && ! StringUtils.isEmpty(userName)) {
            alert("Credentials?", "UserName with no password?!");
            actionEvent.consume();
            return;
        }


        //TODO -- check anything else?
        batchProcessConfig.setEmitter(label, Constants.OPEN_SEARCH_EMITTER_CLASS,
                "openSearchUrl", url, "userName", userName,
                "password", password, "updateStrategy",
                openSearchUpdateStrategy.getSelectionModel().getSelectedItem());
        ((Stage) fsOutputButton.getScene().getWindow()).close();
    }

    private void alert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Fetcher");
        alert.setHeaderText(header);
        alert.setResizable(true);
        alert.setContentText(content);
        alert.getDialogPane().setMinWidth(500);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }

    private String getIndex(String url) {
        if (url == null) {
            return null;
        }
        if (url.length() < 2) {
            return null;
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        Matcher m = SIMPLE_URL_PATTERN.matcher(url);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
}
