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
package org.tallison.tika.app.fx.csv;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tallison.tika.app.fx.Constants;
import org.tallison.tika.app.fx.batch.BatchProcessConfig;
import org.tallison.tika.app.fx.config.ConfigItem;
import org.tallison.tika.app.fx.ctx.AppContext;
import org.tallison.tika.app.fx.metadata.MetadataTuple;

import org.apache.tika.utils.StringUtils;

public class CSVEmitterHelper {

    private static Logger LOGGER = LogManager.getLogger(CSVEmitterHelper.class);

    public static void setUp(ConfigItem emitter) throws IOException {
        if (!emitter.getClazz().equals(Constants.CSV_EMITTER_CLASS)) {
            return;
        }
        Path dbDir = Files.createTempDirectory("tika-app-csv-tmp");
        emitter.getAttributes().put(Constants.CSV_DB_DIRECTORY, dbDir.toAbsolutePath().toString());
        String connectionString =
                "jdbc:sqlite:" + dbDir.toAbsolutePath() + "/tika-gui-v2-tmp-csv-db.db";
        emitter.getAttributes().put(Constants.CSV_JDBC_CONNECTION_STRING, connectionString);
    }

    public static void createTable(ConfigItem emitter) throws SQLException {
        String tableName = Constants.CSV_DB_TABLE_NAME;
        String sql = "drop table if exists " + tableName;
        StringBuilder createTable = new StringBuilder();
        createTable.append("create table ").append(tableName);
        createTable.append("( path varchar(1024), attachment_num int");
        for (MetadataTuple t : emitter.getMetadataTuples().get()) {
            createTable.append(", ").append(t.getOutput()).append(" ").append(t.getProperty());
        }
        createTable.append(")");
        LOGGER.debug("create table: " + createTable);
        try (Connection connection = DriverManager.getConnection(
                emitter.getAttributes().get(Constants.CSV_JDBC_CONNECTION_STRING))) {
            try (Statement st = connection.createStatement()) {
                st.execute(sql);
                st.execute(createTable.toString());
            }
        }
    }

    public static void writeCSV(AppContext appContext) {
        if (appContext == null) {
            return;
        }

        if (appContext.getBatchProcessConfig().isEmpty()) {
            return;
        }
        BatchProcessConfig batchProcessConfig = appContext.getBatchProcessConfig().get();
        Optional<ConfigItem> optionalConfigItem = batchProcessConfig.getEmitter();
        if (optionalConfigItem.isEmpty()) {
            return;
        }
        ConfigItem emitter = optionalConfigItem.get();
        if (!emitter.getClazz().equals(Constants.CSV_EMITTER_CLASS)) {
            return;
        }
        LOGGER.debug("about to write csv");
        String select = getSelect(emitter);
        LOGGER.debug("select: {}", select);

        Path csvPath = getCsvPath(emitter);
        LOGGER.debug("about to write " + csvPath.toAbsolutePath());
        int rows = 0;
        try (OutputStream os = Files.newOutputStream(csvPath);
                CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(os, UTF_8),
                         CSVFormat.EXCEL)) {
            writeHeaders(printer, emitter);

            Connection connection = DriverManager.getConnection(getConnectionString(emitter));
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
                    }
                    rows++;
                }
            }
        } catch (SQLException e) {
            LOGGER.warn("Failed to write CSV", e);
        } catch (IOException e) {
            LOGGER.warn("Failed to write CSV", e);
        }
        LOGGER.info("successfully wrote {} rows to {}", rows, csvPath.toAbsolutePath());
        try {
            cleanCSVTempResources(emitter.getAttributes().get(Constants.CSV_DB_DIRECTORY));
        } catch (IOException e) {
            LOGGER.warn("failed to delete tmp db directory for csv");
        }
    }

    private static String getSelect(ConfigItem emitter) {
        String tikaTable = Constants.CSV_DB_TABLE_NAME;
        StringBuilder sb = new StringBuilder();
        sb.append("select ");
        sb.append("t.path as Path, s.status as Status, attachment_num as Attachment_Num");
        for (MetadataTuple t : emitter.getMetadataTuples().get()) {
            sb.append(", ");
            sb.append(t.getOutput());
        }

        sb.append(" from ").append(tikaTable)
                .append(" t left join tika_status s on t.path = s.path")
                .append(" order by t.path asc, attachment_num asc");
        return sb.toString();
    }

    public static void cleanCSVTempResources(ConfigItem emitter) throws IOException {
        if (!emitter.getClazz().equals(Constants.CSV_EMITTER_CLASS)) {
            return;
        }
        cleanCSVTempResources(emitter.getAttributes().get(Constants.CSV_DB_DIRECTORY));
    }

    private static void cleanCSVTempResources(String path) throws IOException {
        Path tmpDbDir = Paths.get(path);
        if (!Files.isDirectory(tmpDbDir)) {
            LOGGER.warn("Not a directory?! {}", path);
            return;
        }
        FileUtils.deleteDirectory(Paths.get(path).toFile());
    }

    private static String getConnectionString(ConfigItem item) {
        return item.getAttributes().get(Constants.CSV_JDBC_CONNECTION_STRING);
    }

    private static void writeHeaders(CSVPrinter printer, ConfigItem configItem) throws IOException {
        List<String> headers = new ArrayList<>();
        headers.add("path");
        headers.add("status");
        headers.add("attachment_num");
        if (configItem.getMetadataTuples().isEmpty()) {
            LOGGER.warn("no metadata items for csv?!");
            return;
        }
        for (MetadataTuple metadataTuple : configItem.getMetadataTuples().get()) {
            headers.add(metadataTuple.getOutput());
        }
        printer.printRecord(headers);
    }

    private static Path getCsvPath(ConfigItem configItem) {
        Path dir = Paths.get(configItem.getAttributes().get(Constants.BASE_PATH));
        return dir.resolve(configItem.getAttributes().get(Constants.CSV_FILE_NAME));
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
