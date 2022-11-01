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

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tallison.tika.app.fx.Constants;
import org.tallison.tika.app.fx.metadata.MetadataRow;
import org.tallison.tika.app.fx.metadata.MetadataTuple;
import org.tallison.tika.app.fx.tools.BatchProcessConfig;
import org.tallison.tika.app.fx.tools.ConfigItem;

import org.apache.tika.utils.StringUtils;

public class JDBCEmitterController extends AbstractEmitterController implements Initializable {
    private static Logger LOGGER = LogManager.getLogger(JDBCEmitterController.class);

    private final static int TAB_INDEX = 4;

    @FXML
    private TextField jdbcConnection;

    @FXML
    private Button updateJDBC;

    private boolean isTableCreated = false;

    @Override
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {

        if (APP_CONTEXT.getBatchProcessConfig().isEmpty()) {
            LOGGER.warn("batch process config is empty?!");
            return;
        }
        BatchProcessConfig batchProcessConfig = APP_CONTEXT.getBatchProcessConfig().get();
        Optional<ConfigItem> configItem = batchProcessConfig.getEmitter();
        if (configItem.isEmpty()) {
            return;
        }
        ConfigItem emitter = configItem.get();
        if (! emitter.getClazz().equals(Constants.JDBC_EMITTER_CLASS)) {
            return;
        }
        if (emitter.getMetadataTuples().isPresent() && emitter.getMetadataTuples().get().size() > 0) {
            getMetadataRows().clear();
            for (MetadataTuple t : emitter.getMetadataTuples().get()) {
                getMetadataRows().add(new MetadataRow(t.getTika(), t.getOutput(), t.getProperty()));
            }
        }

    }

    @Override
    public void saveState() {
        String label = StringUtils.EMPTY;
        String jdbcConnectionString = StringUtils.EMPTY;



        ConfigItem emitter = ConfigItem.build(label, Constants.JDBC_EMITTER_CLASS,
                Constants.JDBC_CONNECTION_STRING, jdbcConnectionString,
                Constants.JDBC_TABLE_CREATED, Boolean.toString(isTableCreated));
        saveMetadataToEmitter(emitter);
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


    public void updateJDBC(ActionEvent actionEvent) {
        saveState();
        ((Stage)updateJDBC.getScene().getWindow()).close();
    }
}
