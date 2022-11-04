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
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
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

    private enum VALIDITY {
        NO_CONNECTION_STRING,
        FAILED_TO_CONNECT,
        VALID,
        COLUMN_MISMATCH,
        TABLE_EXISTS_WITH_DATA,
        NEED_TO_CREATE_TABLE,
        SQL_EXCEPTION
    }

    private static String ALERT_TITLE = "JDBC Emitter";
    private static Logger LOGGER = LogManager.getLogger(JDBCEmitterController.class);

    private static String PATH_COL_NAME = "path";

    private static String ATTACHMENT_NUM_COL_NAME = "attach_num";

    private final static int TAB_INDEX = 4;

    private String insertSql = StringUtils.EMPTY;

    @FXML
    private TextField jdbcConnection;

    @FXML
    private TextField tableName;

    @FXML
    private Button updateJDBC;

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

        if (emitter.getAttributes().containsKey(Constants.JDBC_CONNECTION_STRING)) {
            String s = emitter.getAttributes().get(Constants.JDBC_CONNECTION_STRING);
            if (! StringUtils.isBlank(s)) {
                jdbcConnection.setText(s);
            }
        }

        if (emitter.getAttributes().containsKey(Constants.JDBC_TABLE_NAME)) {
            String s = emitter.getAttributes().get(Constants.JDBC_TABLE_NAME);
            if (! StringUtils.isBlank(s)) {
                tableName.setText(s);
            }
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
        String tableNameString = StringUtils.EMPTY;

        if (! StringUtils.isBlank(jdbcConnection.getText())) {
            jdbcConnectionString = jdbcConnection.getText();
        }

        if (! StringUtils.isBlank(tableName.getText())) {
            tableNameString = tableName.getText();
            label = "JDBC: " + tableNameString;
        }

        ConfigItem emitter = ConfigItem.build(label, Constants.JDBC_EMITTER_CLASS,
                Constants.JDBC_CONNECTION_STRING, jdbcConnectionString,
                Constants.JDBC_TABLE_NAME, tableNameString,
                Constants.JDBC_INSERT_SQL, insertSql);

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
        VALIDITY validity = validate();
        switch (validity) {
            case VALID:
                ((Stage)updateJDBC.getScene().getWindow()).close();
                break;
            case NO_CONNECTION_STRING:
                alert("JDBC Emitter", "No connection string?",
                        "Need to specify a jdbc connection string");
                break;
            case COLUMN_MISMATCH:
                columnMismatchDialog();
                break;
            case FAILED_TO_CONNECT:
                alert("JDBC Emitter", "Failed to connect", "Couldn't open jdbc connection");
                break;
            case NEED_TO_CREATE_TABLE:
                createTableDialog();
                break;
            case TABLE_EXISTS_WITH_DATA:
                existingDataDialog();
                break;
        }
        if (StringUtils.isBlank(insertSql)) {
            insertSql = createInsertString();
        }
        saveState();
        actionEvent.consume();
    }

    private void columnMismatchDialog() {
        alert(ALERT_TITLE, "Column mismatch", "Couldn't open jdbc connection");
    }

    private void existingDataDialog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(ALERT_TITLE);
        alert.setContentText("Data exists. Truncate table, append data or cancel?");
        ButtonType truncateButton = new ButtonType("Truncate", ButtonBar.ButtonData.YES);
        ButtonType appendButton = new ButtonType("Append", ButtonBar.ButtonData.NO);

        alert.getButtonTypes().setAll(truncateButton, appendButton);
        alert.showAndWait().ifPresent(type -> {
            if (type.getText().startsWith("Truncate Table")) {
                truncate();
            } else if (type.getText().startsWith("Append Data")) {
                return;
            }
        });

    }

    private boolean truncate() {
        String sql = "truncate table " + tableName.getText();
        String msg = StringUtils.EMPTY;
        try (Connection connection = DriverManager.getConnection(jdbcConnection.getText())) {
            try (Statement st = connection.createStatement()) {
                boolean result = st.execute(sql);
                if (result) {
                    return true;
                }
            }
        } catch (SQLException e) {
            LOGGER.warn("failed to truncate", e);
            msg = e.getMessage();
        }
        alert(ALERT_TITLE, "Truncation failed", "Failed to truncate: " + msg);
        return false;
    }

    private VALIDITY validate() {
        String connectionString = jdbcConnection.getText();
        if (StringUtils.isBlank(connectionString)) {
            return VALIDITY.NO_CONNECTION_STRING;
        }

        try (Connection connection = DriverManager.getConnection(connectionString)) {

        } catch (SQLException e) {
            LOGGER.warn("couldn't connect", e);
            return VALIDITY.FAILED_TO_CONNECT;
        }

        try (Connection connection = DriverManager.getConnection(connectionString)) {
            try (Statement st = connection.createStatement()) {
                int rows = 0;
                try (ResultSet rs = st.executeQuery("select * from " + tableName + " limit 10;")) {
                    boolean validColumns = validateColumns(rs.getMetaData());
                    if (! validColumns) {
                        //TODO -- add a drop table or modify option
                        //TODO -- show the specific mismatch
                        return VALIDITY.COLUMN_MISMATCH;
                    }
                    while (rs.next()) {
                        rows++;
                    }
                } catch (SQLException e) {
                    //TODO -- figure out better generic way to test whether a table exists
                    //For now, assume table does not exist
                    return VALIDITY.NEED_TO_CREATE_TABLE;
                }
                if (rows == 0) {
                    return VALIDITY.VALID;
                }
                return VALIDITY.TABLE_EXISTS_WITH_DATA;
            }
        } catch (SQLException e) {
            LOGGER.warn("failed to validate", e);
            return VALIDITY.SQL_EXCEPTION;
        }
    }

    private boolean validateColumns(ResultSetMetaData metaData) {
        //TODO -- looks good for now!
        return true;
    }

    private void createTableDialog() {
        StringBuilder sb = new StringBuilder();
        sb.append("create table ").append(tableName.getText()).append(" (");
        sb.append(PATH_COL_NAME).append(" VARCHAR(1024),\n");
        sb.append(ATTACHMENT_NUM_COL_NAME).append(" INTEGER");
        for (MetadataRow r : getMetadataRows()) {
            sb.append(",\n");
            sb.append(r.getOutput()).append(" ").append(r.getProperty());
        }
        sb.append(")");
        try (Connection connection = DriverManager.getConnection(jdbcConnection.getText())) {
            try (Statement st = connection.createStatement()) {
                st.execute(sb.toString());
            }
        } catch (SQLException e) {
            LOGGER.warn("failed to create table");
            LOGGER.warn(sb.toString());
            LOGGER.warn(e);
        }
    }

    private String createInsertString() {
        StringBuilder sb = new StringBuilder();
        sb.append("insert into ").append(tableName.getText()).append(" (");
        sb.append(PATH_COL_NAME).append(", ").append(ATTACHMENT_NUM_COL_NAME);
        int colCount = 2;
        for (MetadataRow r : getMetadataRows()) {
            sb.append(", ");
            sb.append(r.getOutput());
            colCount++;
        }
        sb.append(") values (?");
        for (int i = 1; i < colCount; i++) {
            sb.append(",?");
        }
        sb.append(")");
        return sb.toString();
    }

}
