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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tallison.tika.app.fx.Constants;
import org.tallison.tika.app.fx.ctx.AppContext;
import org.tallison.tika.app.fx.tools.BatchProcessConfig;
import org.tallison.tika.app.fx.tools.ConfigItem;

import org.apache.tika.utils.StringUtils;

public class CSVEmitterHelper {

    private static Logger LOGGER = LogManager.getLogger(CSVEmitterHelper.class);

    public static void setUp(AppContext appContext) {

    }

    public static void writeCSV(AppContext appContext) {
        if (appContext == null) {
            return;
        }

        if (appContext.getBatchProcessConfig().isEmpty()) {
            return;
        }
        BatchProcessConfig batchProcessConfig = appContext.getBatchProcessConfig().get();
        Optional<ConfigItem> configItem = batchProcessConfig.getEmitter();
        if (configItem.isEmpty()) {
            return;
        }
        if (! configItem.get().getClazz().equals(Constants.CSV_EMITTER_CLASS)) {
            return;
        }

        //TODO -- replace with column names?
        String select = "select * from " + Constants.CSV_DB_TABLE_NAME;
        Path path = getCsvPath(configItem);
        try (OutputStream os = Files.newOutputStream(path); CSVPrinter printer =
                new CSVPrinter(new OutputStreamWriter(os, UTF_8), CSVFormat.EXCEL)) {
            writeHeaders(printer, configItem);
            Connection connection =
                    DriverManager.getConnection(getConnectionString(configItem.get()));
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
                }
            }
        } catch (SQLException e) {
            LOGGER.warn("Failed to write CSV", e);
        } catch (IOException e) {
            LOGGER.warn("Failed to write CSV", e);
        }


        cleanCSVTempResources();

    }

    private static void cleanCSVTempResources() {
        //TODO
    }

    private static String getConnectionString(ConfigItem item) {
        //TODO
        return null;
    }

    private static void writeHeaders(CSVPrinter printer, Optional<ConfigItem> configItem) {
        //TODO
    }

    private static Path getCsvPath(Optional<ConfigItem> configItem) {
        //TODO
        return null;
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

    public static void cleanTmpResources(AppContext appContext) {

    }
}
