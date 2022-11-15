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

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tallison.tika.app.fx.Constants;
import org.tallison.tika.app.fx.ControllerBase;
import org.tallison.tika.app.fx.ctx.AppContext;
import org.tallison.tika.app.fx.tools.BatchProcessConfig;
import org.tallison.tika.app.fx.tools.ConfigItem;

import org.apache.tika.utils.StringUtils;

public class OpenSearchEmitterController extends ControllerBase implements Initializable {
    private static AppContext APP_CONTEXT = AppContext.getInstance();
    private static Logger LOGGER = LogManager.getLogger(OpenSearchEmitterController.class);

    //TODO -- this is bad
    private static final Pattern SIMPLE_URL_PATTERN =
            Pattern.compile("(?i)^https?:\\/\\/[-_a-z0-9\\.]+(?::\\d+)?\\/([-_a-z0-9\\.]+)");

    @FXML
    private TextField openSearchUrl;

    @FXML
    private TextField openSearchUserName;
    @FXML
    private PasswordField openSearchPassword;

    @FXML
    private ComboBox<String> openSearchUpdateStrategy;

    @FXML
    private Button updateOpenSearchEmitter;

    @Override
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        if (APP_CONTEXT.getBatchProcessConfig().isEmpty()) {
            LOGGER.warn("batch process config must not be null at this point");
            return;
        }
        Optional<ConfigItem> emitterOptional =
                APP_CONTEXT.getBatchProcessConfig().get().getEmitter();

        if (emitterOptional.isPresent()) {
            ConfigItem emitter = emitterOptional.get();

            if (emitter.getClazz().equals(Constants.OPEN_SEARCH_EMITTER_CLASS)) {
                openSearchUrl.setText(emitter.getAttributes().get(Constants.OPEN_SEARCH_URL));
                openSearchUserName.setText(emitter.getAttributes().get(Constants.OPEN_SEARCH_USER));
                String selected = emitter.getAttributes().get(Constants.OPEN_SEARCH_UPDATE_STRATEGY);
                if (!StringUtils.isBlank(selected)) {
                    openSearchUpdateStrategy.getSelectionModel().select(selected);
                } else {
                    openSearchUpdateStrategy.getSelectionModel().select("Upsert");
                }
            } else {
                openSearchUpdateStrategy.getSelectionModel().select("Upsert");
            }
        }
    }

    public void updateOpenSearchEmitter(ActionEvent actionEvent) {
        Optional<BatchProcessConfig> batchProcessConfig = APP_CONTEXT.getBatchProcessConfig();
        if (batchProcessConfig.isEmpty()) {
            LOGGER.warn("batch process config is empty?!");
            actionEvent.consume();
            return;
        }
        //TODO -- check that all required information is here...at least the url
        //check that the url includes an index and is not the bare url

        String url = openSearchUrl.getText();
        String index = getIndex(url);
        if (StringUtils.isEmpty(url)) {
            alert("Emitter", "Missing URL?", "Must specify a url including the index, " +
                    "e.g. https://localhost:9500/my-index");
            actionEvent.consume();
            return;
        }
        if (StringUtils.isEmpty(index)) {
            alert("Emitter", "Missing index?", "Please specify an index, I only see: " + url);
            actionEvent.consume();
            return;
        }
        String label = "OpenSearch: " + ellipsize(index, 30);
        String userName = openSearchUserName.getText();
        String password = openSearchPassword.getText();
        if (StringUtils.isEmpty(userName) && !StringUtils.isEmpty(password)) {
            alert("Emitter", "Credentials?", "Password with no username?!");
            actionEvent.consume();
            return;
        }

        if (StringUtils.isEmpty(password) && !StringUtils.isEmpty(userName)) {
            alert("Emitter", "Credentials?", "UserName with no password?!");
            actionEvent.consume();
            return;
        }


        //TODO -- check anything else?
        batchProcessConfig.get().setEmitter(label, Constants.OPEN_SEARCH_EMITTER_CLASS,
                Constants.OPEN_SEARCH_URL, url,
                Constants.OPEN_SEARCH_USER, userName,
                Constants.OPEN_SEARCH_PW, password,
                Constants.OPEN_SEARCH_UPDATE_STRATEGY,
                openSearchUpdateStrategy.getSelectionModel().getSelectedItem());

        //TODO -- do better than hard coding indices
        APP_CONTEXT.getBatchProcessConfig().get().setOutputSelectedTab(2);
        APP_CONTEXT.saveState();
        ((Stage) updateOpenSearchEmitter.getScene().getWindow()).close();
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
