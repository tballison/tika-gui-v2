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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tallison.tika.app.fx.batch.BatchProcessConfig;
import org.tallison.tika.app.fx.ctx.AppContext;
import org.tallison.tika.app.fx.utils.OptionalUtil;

import org.apache.tika.utils.StringUtils;

public class OpenSearchEmitterController extends AbstractEmitterController
        implements Initializable {

    //TODO -- this is bad
    private static final Pattern SIMPLE_URL_PATTERN =
            Pattern.compile("(?i)^https?:\\/\\/[-_a-z0-9\\.]+(?::\\d+)?\\/([-_a-z0-9\\.]+)");

    private static final AppContext APP_CONTEXT = AppContext.getInstance();
    private static final Logger LOGGER = LogManager.getLogger(OpenSearchEmitterController.class);
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

    @FXML
    private Accordion openSearchAccordion;

    @Override
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        //Not clear why expanded=true is not working in fxml
        openSearchAccordion.setExpandedPane(openSearchAccordion.getPanes().get(0));
        if (APP_CONTEXT.getBatchProcessConfig().isEmpty()) {
            LOGGER.warn("batch process config must not be null at this point");
            return;
        }
        Optional<EmitterSpec> emitterOptional =
                APP_CONTEXT.getBatchProcessConfig().get().getEmitter();
        if (APP_CONTEXT.getBatchProcessConfig().get().getEmitter().isEmpty()) {
            return;
        }
        EmitterSpec emitter = emitterOptional.get();
        if (!(emitter instanceof OpenSearchEmitterSpec openSearchEmitterSpec)) {
            return;
        }
        if (openSearchEmitterSpec.getUrl().isPresent()) {
            openSearchUrl.setText(openSearchEmitterSpec.getUrl().get());
        }
        if (openSearchEmitterSpec.getUserName().isPresent()) {
            openSearchUserName.setText(openSearchEmitterSpec.getUserName().get());
        }

        if (openSearchEmitterSpec.getPassword().isPresent()) {
            openSearchPassword.setText(openSearchEmitterSpec.getPassword().get());
        }

        openSearchUpdateStrategy.getSelectionModel()
                .select(openSearchEmitterSpec.getUpdateStrategy());
        updateMetadataRows(openSearchEmitterSpec.getMetadataTuples());

    }


    public void updateOpenSearchEmitter(ActionEvent actionEvent) {
        Optional<BatchProcessConfig> batchProcessConfig = APP_CONTEXT.getBatchProcessConfig();
        if (batchProcessConfig.isEmpty()) {
            LOGGER.warn("batch process config is empty?!");
            actionEvent.consume();
            return;
        }
        OpenSearchEmitterSpec emitterSpec = buildEmitterSpec();

        ValidationResult validationResult = validate(emitterSpec);

        if (validationResult.getValidity() != ValidationResult.VALIDITY.OK) {
            saveState(false);
            alert(validationResult);
            return;
        }
        if (emitterSpec.getMetadataTuples().size() == 0) {
            boolean goSchemaless = missingMetadataMappingDialog();
            if (!goSchemaless) {
                saveState(false);
                actionEvent.consume();
                return;
            }
        }
        saveState(true);
        ((Stage) updateOpenSearchEmitter.getScene().getWindow()).close();
    }

    @Override
    public void saveState(boolean validated) {
        OpenSearchEmitterSpec emitter = buildEmitterSpec();
        emitter.setValid(validated);
        APP_CONTEXT.getBatchProcessConfig().get().setEmitter(emitter);
        //TODO -- do better than hard coding indices
        APP_CONTEXT.getBatchProcessConfig().get().setOutputSelectedTab(2);
        APP_CONTEXT.saveState();
    }

    private OpenSearchEmitterSpec buildEmitterSpec() {
        OpenSearchEmitterSpec emitter = new OpenSearchEmitterSpec(getMetadataTuples());
        String url = openSearchUrl.getText();
        String index = getIndex(url);
        emitter.setUrl(url);
        emitter.setIndex(index);
        emitter.setShortLabel("OpenSearch: " + ellipsize(index, 30));
        emitter.setFullLabel("OpenSearch: " + url);
        emitter.setUserName(openSearchUserName.getText());
        emitter.setPassword(openSearchPassword.getText());
        emitter.setUpdateStrategy(openSearchUpdateStrategy.getSelectionModel().getSelectedItem());
        return emitter;
    }

    private ValidationResult validate(OpenSearchEmitterSpec emitter) {
        ValidationResult result = validateMetadataRows();
        if (result.getValidity() != ValidationResult.VALIDITY.OK) {
            return result;
        }

        if (OptionalUtil.isEmpty(emitter.getUrl())) {
            return new ValidationResult(ValidationResult.VALIDITY.NOT_OK, "Emitter", "Missing URL?",
                    "Must specify a url including the index, " +
                            "e.g. https://localhost:9200/my-index");
        }

        if (OptionalUtil.isEmpty(emitter.getIndex())) {
            return new ValidationResult(ValidationResult.VALIDITY.NOT_OK, "Emitter",
                    "Missing index?",
                    "Please specify an index, I only see: " + emitter.getUrl().get());

        }

        if (OptionalUtil.isEmpty(emitter.getPassword()) &&
                !OptionalUtil.isEmpty(emitter.getUserName())) {
            return new ValidationResult(ValidationResult.VALIDITY.NOT_OK, "Emitter", "Credentials?",
                    "Username with no password?!");
        }


        if (OptionalUtil.isEmpty(emitter.getUserName()) &&
                !OptionalUtil.isEmpty(emitter.getPassword())) {
            return new ValidationResult(ValidationResult.VALIDITY.NOT_OK, "Emitter", "Credentials?",
                    "Password with no username?!");
        }
        return ValidationResult.OK;
    }

    private String getIndex(String url) {
        if (url == null) {
            return StringUtils.EMPTY;
        }
        if (url.length() < 2) {
            return StringUtils.EMPTY;
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        Matcher m = SIMPLE_URL_PATTERN.matcher(url);
        if (m.find()) {
            return m.group(1);
        }
        return StringUtils.EMPTY;
    }

    private boolean missingMetadataMappingDialog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText("OpenSearch Emitter");
        alert.setTitle("OpenSearch Emitter");
        alert.setContentText("No metadata mappings have been selected. Continue with schemaless?");
        ButtonType continueButton = new ButtonType("Continue", ButtonBar.ButtonData.YES);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(continueButton, cancelButton);
        AtomicBoolean processAnyways = new AtomicBoolean(false);
        alert.showAndWait().ifPresent(type -> {
            if (type.getText().startsWith("Continue")) {
                processAnyways.set(true);
            } else if (type.getText().startsWith("Cancel")) {
                processAnyways.set(false);
            }
        });
        return processAnyways.get();
    }
}
