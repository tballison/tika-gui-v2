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
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kordamp.ikonli.javafx.FontIcon;
import org.tallison.tika.app.fx.Constants;
import org.tallison.tika.app.fx.metadata.MetadataRow;
import org.tallison.tika.app.fx.metadata.MetadataTuple;
import org.tallison.tika.app.fx.tools.BatchProcessConfig;
import org.tallison.tika.app.fx.tools.ConfigItem;

import org.apache.tika.utils.StringUtils;

public class JDBCEmitterController extends AbstractEmitterController implements Initializable {

    private enum VALIDITY {
        NO_CONNECTION_STRING,
        METADATA_NOT_CONFIGURED,
        METADATA_ANOMALY,
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

    private final static int TAB_INDEX = 3;

    private String insertSql = StringUtils.EMPTY;

    @FXML
    private TextField jdbcConnection;

    @FXML
    private TextField tableName;

    @FXML
    private Button validateJDBC;

    @FXML
    private FontIcon readyIcon;

    @FXML
    private FontIcon notReadyIcon;

    @FXML
    private Accordion jdbcAccordion;



    @Override
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        //Not clear why expanded=true is not working in fxml
        jdbcAccordion.setExpandedPane(jdbcAccordion.getPanes().get(0));

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

        VALIDITY validity = validate();
        if (validity == VALIDITY.VALID) {
            readyIcon.setVisible(true);
            notReadyIcon.setVisible(false);
        } else {
            readyIcon.setVisible(false);
            notReadyIcon.setVisible(true);
        }
    }

    @Override
    public void saveState() {
        String shortLabel = StringUtils.EMPTY;
        String fullLabel = StringUtils.EMPTY;
        String jdbcConnectionString = StringUtils.EMPTY;
        String tableNameString = StringUtils.EMPTY;

        if (! StringUtils.isBlank(jdbcConnection.getText())) {
            jdbcConnectionString = jdbcConnection.getText();
        }

        if (! StringUtils.isBlank(tableName.getText())) {
            tableNameString = tableName.getText();
            shortLabel = "JDBC: " + ellipsize(tableNameString, 30);
            fullLabel = "JDBC: " + tableNameString;
        }

        ConfigItem emitter = ConfigItem.build(shortLabel, fullLabel,
                Constants.JDBC_EMITTER_CLASS,
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


    public void validateJDBC(ActionEvent actionEvent) {
        VALIDITY validity = validate();
        switch (validity) {
            case VALID:
                //TODO -- should we test this insert string against the db?
                //recreate it every time
                insertSql = createInsertString();
                readyIcon.setVisible(true);
                notReadyIcon.setVisible(false);
                LOGGER.debug("insert sql: " + insertSql);
                saveState();
                ((Stage) validateJDBC.getScene().getWindow()).close();
                return;
            case METADATA_ANOMALY:
                jdbcAccordion.setExpandedPane(jdbcAccordion.getPanes().get(1));
                break;
            case METADATA_NOT_CONFIGURED:
                alert(ALERT_TITLE, "Metadata Not Configured", "Need to configure metadata");
                //TODO -- figure out if we can open the metadata accordion
                jdbcAccordion.setExpandedPane(jdbcAccordion.getPanes().get(1));
                break;
            case NO_CONNECTION_STRING:
                alert(ALERT_TITLE, "No connection string?",
                        "Need to specify a jdbc connection string");
                break;
            case COLUMN_MISMATCH:
                columnMismatchDialog();
                break;
            case FAILED_TO_CONNECT:
                alert("JDBC Emitter", "Failed to connect", "Couldn't open jdbc connection");
                break;
            case NEED_TO_CREATE_TABLE:
                boolean success = tryToCreateTable();
                if (success) {
                    validateJDBC(actionEvent);
                    return;
                }
                break;
            case TABLE_EXISTS_WITH_DATA:
                existingDataDialog();
                break;
        }
        notReadyIcon.setVisible(true);
        readyIcon.setVisible(false);
        saveState();
        actionEvent.consume();
    }

    private void columnMismatchDialog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(ALERT_TITLE);
        alert.setContentText("Column mismatch. Drop table and recreate?");
        ButtonType dropButton = new ButtonType("Drop table", ButtonBar.ButtonData.YES);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(dropButton, cancelButton);
        alert.showAndWait().ifPresent(type -> {
            if (type.getText().startsWith("Drop")) {
                dropTable();
            } else if (type.getText().startsWith("Cancel")) {
                return;
            }
        });
    }


    private void existingDataDialog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(ALERT_TITLE);
        alert.setContentText("Data exists. Truncate table, append data or cancel?");
        ButtonType truncateButton = new ButtonType("Truncate Table", ButtonBar.ButtonData.YES);
        ButtonType appendButton = new ButtonType("Append", ButtonBar.ButtonData.NO);

        alert.getButtonTypes().setAll(truncateButton, appendButton);
        alert.showAndWait().ifPresent(type -> {
            if (type.getText().startsWith("Truncate")) {
                truncate();
            } else if (type.getText().startsWith("Append")) {
                return;
            }
        });

    }

    private boolean truncate() {
        //TODO -- pop up "are you sure?"
        String sql = "truncate table " + tableName.getText();
        //TODO -- fix this horror show
        if (jdbcConnection.getText().startsWith("jdbc:sqlite")) {
            sql = "delete from " + tableName.getText();
        }
        String msg = StringUtils.EMPTY;
        try (Connection connection = DriverManager.getConnection(jdbcConnection.getText())) {
            try (Statement st = connection.createStatement()) {
                st.execute(sql);
                return true;
            }
        } catch (SQLException e) {
            LOGGER.warn("failed to truncate", e);
            msg = e.getMessage();
        }
        alert(ALERT_TITLE, "Truncation failed", "Failed to truncate: " + msg);
        return false;
    }

    private boolean dropTable() {
        //TODO -- pop up "are you sure?"
        String sql = "drop table " + tableName.getText();

        String msg = StringUtils.EMPTY;
        try (Connection connection = DriverManager.getConnection(jdbcConnection.getText())) {
            try (Statement st = connection.createStatement()) {
                st.execute(sql);
                return true;
            }
        } catch (SQLException e) {
            LOGGER.warn("failed to drop table", e);
            msg = e.getMessage();
        }
        alert(ALERT_TITLE, "Dropping table failed", "Failed to drop: " + msg);
        return false;
    }

    private VALIDITY validate() {

        if (getMetadataRows().size() == 0) {
            return VALIDITY.METADATA_NOT_CONFIGURED;
        }
        boolean validMetadata = validateMetadata();
        if (! validMetadata) {
            return VALIDITY.METADATA_ANOMALY;
        }

        String connectionString = jdbcConnection.getText();
        if (StringUtils.isBlank(connectionString)) {
            return VALIDITY.NO_CONNECTION_STRING;
        }
        String cString = connectionString.replace("AUTO_SERVER=TRUE", "");

        try (Connection connection = DriverManager.getConnection(cString)) {

        } catch (SQLException e) {
            LOGGER.warn("couldn't connect", e);
            return VALIDITY.FAILED_TO_CONNECT;
        }

        try (Connection connection = DriverManager.getConnection(cString)) {
            try (Statement st = connection.createStatement()) {
                int rows = 0;
                try (ResultSet rs = st.executeQuery("select * from " + tableName.getText() + " limit 10;")) {
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

    private boolean validateMetadata() {
        return validateMetadataRows();
    }

    private boolean validateColumns(ResultSetMetaData metaData) throws SQLException {
        //TODO -- check column types!
        for (int i = 1;  i <= metaData.getColumnCount(); i++) {
            if (i == 1) {
                if (! PATH_COL_NAME.equalsIgnoreCase(metaData.getColumnName(i))) {
                    alert(ALERT_TITLE, "Unexpected column name", "First column should be: " + PATH_COL_NAME);
                    return false;
                }
            }
            if (i == 2) {
                if (! ATTACHMENT_NUM_COL_NAME.equalsIgnoreCase(metaData.getColumnName(i))) {
                    alert(ALERT_TITLE, "Unexpected column name",
                            "Second column should be: " + ATTACHMENT_NUM_COL_NAME);
                    return false;
                }
            }
            if (i > 2) {
                int tableRow = i - 3;
                if (!metaData.getColumnName(i).equalsIgnoreCase(getMetadataRows().get(tableRow).getOutput())) {
                    alert(ALERT_TITLE, "Unexpected column name",
                            "Column number (" + i + ")  should be: " +
                                    getMetadataRows().get(tableRow).getOutput() + " but is " +
                            metaData.getColumnName(i));
                    return false;
                }
            }
        }
        StringBuilder sb = new StringBuilder();

        for (int i = metaData.getColumnCount() - 2; i < getMetadataRows().size(); i++) {
            String col = getMetadataRows().get(i).getOutput();
            sb.append("'").append(col).append("'").append(" ");
        }
        String warn = sb.toString().trim();
        if (! StringUtils.isBlank(warn)) {
            alert(ALERT_TITLE, "Unexpected column(s)",
                    "Columns defined in metadata but not defined in the table: " + warn);
            return false;
        }
        return true;
    }

    private boolean tryToCreateTable() {
        StringBuilder sb = new StringBuilder();
        sb.append("create table ").append(tableName.getText()).append(" (");
        sb.append(PATH_COL_NAME).append(" VARCHAR(1024),\n");
        sb.append(ATTACHMENT_NUM_COL_NAME).append(" INTEGER");
        for (MetadataRow r : getMetadataRows()) {
            sb.append(",\n");
            sb.append(r.getOutput()).append(" ").append(r.getProperty());
        }
        sb.append(")");
        String cString = jdbcConnection.getText();

        //TODO -- get rid of this hack
        cString = cString.replace("AUTO_SERVER=TRUE", "");
        try (Connection connection = DriverManager.getConnection(cString)) {
            try (Statement st = connection.createStatement()) {
                st.execute(sb.toString());
            }
        } catch (SQLException e) {
            LOGGER.warn("failed to create table");
            LOGGER.warn(sb.toString());
            LOGGER.warn(e);
            alertStackTrace("Failed to create table", "Failed to create table",
                    "SQL:\n" + sb.toString(), e);
            return false;
        }
        return true;
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
