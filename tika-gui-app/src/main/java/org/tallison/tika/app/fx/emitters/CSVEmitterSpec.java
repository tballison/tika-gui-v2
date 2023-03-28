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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tallison.tika.app.fx.metadata.MetadataTuple;

import org.apache.tika.utils.StringUtils;

public class CSVEmitterSpec extends JDBCEmitterSpec {
    static final String CSV_DB_TABLE_NAME = "tika_extracts";

    private static final Logger LOGGER = LogManager.getLogger(CSVEmitterSpec.class);

    private Optional<Path> tmpDbDirectory = Optional.empty();
    private Optional<Path> csvDirectory = Optional.empty();
    private Optional<String> csvFileName = Optional.empty();
    private volatile boolean closed = false;

    private Connection connection = null;

    public CSVEmitterSpec(@JsonProperty("metadataTuples") List<MetadataTuple> metadataTuples) {
        super(metadataTuples);
        setTableName(CSV_DB_TABLE_NAME);
    }

    @Override
    public ValidationResult initialize() throws IOException {
        if (closed) {
            throw new IOException("This csv emitter has been closed");
        }
        tmpDbDirectory = Optional.of(Files.createTempDirectory("tika-app-csv-tmp"));
        LOGGER.debug("tmp db directory: {}", tmpDbDirectory.get().toAbsolutePath());
        setConnectionString("jdbc:h2:" + tmpDbDirectory.get().toAbsolutePath() +
                "/tika-gui-v2-tmp-csv-db;AUTO_SERVER=TRUE");
        try {
            createTable();
        } catch (SQLException e) {
            LOGGER.warn("can't create tmp table for csv", e);
            setNotValidMessage("can't create tmp table for csv");
            return new ValidationResult(ValidationResult.VALIDITY.NOT_OK, "Output Failure",
                    "Couldn't create temp table for csv", "Couldn't create temp table for csv");
        }
        createAndSetInsertString(getTableName().get());

        return ValidationResult.OK;
    }

    public Optional<Path> getCsvDirectory() {
        return csvDirectory;
    }

    public void setCsvDirectory(Path csvDirectory) {
        this.csvDirectory = Optional.ofNullable(csvDirectory);
    }

    private void createTable() throws SQLException {
        String tableName = CSV_DB_TABLE_NAME;
        String dropTable = "drop table if exists " + tableName;
        StringBuilder createTable = new StringBuilder();
        createTable.append("create table ").append(tableName);
        createTable.append("( ").append(PATH_COL_NAME).append(" varchar(1024), ");
        createTable.append(ATTACHMENT_NUM_COL_NAME).append(" int");
        for (MetadataTuple t : getMetadataTuples()) {
            createTable.append(", ").append(t.getOutput()).append(" ").append(t.getProperty());
        }
        createTable.append(")");
        LOGGER.debug("create table: " + createTable);
        if (getConnectionString().isEmpty()) {
            //TODO throw exception
            LOGGER.warn("connection string is empty?!");
            return;
        }
        connection = DriverManager.getConnection(getConnectionString().get());
            try (Statement st = connection.createStatement()) {
                st.execute(dropTable);
                st.execute(createTable.toString());
            }
    }

    @Override
    public void close() throws IOException {
        //avoid double closures -- TODO figure out a more elegant way of handling this
        if (closed) {
            return;
        }
        try {
            writeCSV();
        } catch (IOException e) {
            LOGGER.warn("problem writing csv", e);
        } finally {
            cleanCSVTempResources();
            closed = true;
        }
    }

    private void writeCSV() throws IOException {

        LOGGER.debug("about to write csv");
        String select = getSelect();
        LOGGER.debug("select: {}", select);

        Optional<Path> csvPath = getCSVPath();
        if (csvPath.isEmpty()) {
            LOGGER.warn("CSVPath is empty?!");
            return;
        }
        if (getConnectionString().isEmpty()) {
            LOGGER.warn("connection string is empty?!");
            return;
        }
        LOGGER.debug("about to write " + csvPath.get().toAbsolutePath());
        int rows = 0;
        try (OutputStream os = Files.newOutputStream(csvPath.get());
                CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(os, UTF_8),
                        CSVFormat.EXCEL)) {
            writeHeaders(printer);


                try (Statement st = connection.createStatement()) {
                    List<String> cells = new ArrayList<>();
                    Integer columnCount = null;
                    try (ResultSet rs = st.executeQuery(select)) {
                        while (rs.next()) {
                            if (columnCount == null) {
                                columnCount = rs.getMetaData().getColumnCount();
                            }
                            writeRow(rs, printer, cells, columnCount);
                            cells.clear();
                            rows++;
                        }
                    }
                }

        } catch (SQLException e) {
            LOGGER.warn("Failed to write CSV", e);
        } catch (IOException e) {
            LOGGER.warn("Failed to write CSV", e);
        }
        LOGGER.info("successfully wrote {} rows to {}", rows, csvPath.get().toAbsolutePath());
    }

    public Optional<String> getCsvFileName() {
        return csvFileName;
    }

    public void setCsvFileName(String fileName) {
        this.csvFileName = Optional.of(fileName);
    }

    private Optional<Path> getCSVPath() {
        if (csvDirectory.isEmpty() || csvFileName.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(csvDirectory.get().resolve(csvFileName.get()));
    }

    private void writeHeaders(CSVPrinter printer) throws IOException {
        List<String> headers = new ArrayList<>();
        headers.add("path");
        headers.add("status");
        headers.add("attachment_num");
        if (getMetadataTuples().size() == 0) {
            LOGGER.warn("no metadata items for csv?!");
            return;
        }
        for (MetadataTuple metadataTuple : getMetadataTuples()) {
            headers.add(metadataTuple.getOutput());
        }
        printer.printRecord(headers);
    }

    private String getSelect() {
        String tikaTable = CSV_DB_TABLE_NAME;
        StringBuilder sb = new StringBuilder();
        sb.append("select ");
        sb.append("s.").append(PATH_COL_NAME).append(" as Path, s.status as Status, ");
        sb.append("case when ").append(ATTACHMENT_NUM_COL_NAME).append(" is null then 0");
        sb.append(" else ").append(ATTACHMENT_NUM_COL_NAME).append(" end");
        for (MetadataTuple t : getMetadataTuples()) {
            sb.append(", ");
            String out = t.getOutput();
            //if there's a column in tika_extracts
            if (out.equals(PATH_COL_NAME) || out.equals("status")) {
                sb.append("t.");
            }
            sb.append(t.getOutput());
        }

        sb.append(" from tika_status s left join ").append(tikaTable)
                .append(" t on s.path = t.path")
                .append(" order by s.status, t.path asc, t.attachment_num asc");

        return sb.toString();
    }

    private void cleanCSVTempResources() throws IOException {
        try {
            connection.close();
        } catch (SQLException e) {
            LOGGER.warn("problem closing db?!", e);
        }
        if (tmpDbDirectory.isEmpty()) {
            LOGGER.warn("tmpdb has not been set ?!");
            return;
        }
        if (!Files.isDirectory(tmpDbDirectory.get())) {
            LOGGER.warn("Not a directory?! {}", tmpDbDirectory.get());
            return;
        }
        FileUtils.deleteDirectory(tmpDbDirectory.get().toFile());
    }

    private static void writeRow(ResultSet rs, CSVPrinter printer, List<String> cells,
                                 int columnCount) throws SQLException, IOException {
        for (int i = 1; i <= columnCount; i++) {
            String val = rs.getString(i);
            if (rs.wasNull()) {
                val = StringUtils.EMPTY;
            }
            cells.add(val);
        }
        printer.printRecord(cells);
    }
}
