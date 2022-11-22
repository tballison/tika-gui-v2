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


import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tallison.tika.app.fx.ctx.AppContext;
import org.tallison.tika.app.fx.metadata.MetadataRow;
import org.tallison.tika.app.fx.metadata.MetadataTuple;
import org.tallison.tika.app.fx.sax.DomWriter;
import org.w3c.dom.Element;

public class JDBCEmitterSpec extends BaseEmitterSpec {
    private static final String EMITTER_CLASS =
            "org.apache.tika.pipes.emitter.jdbc.JDBCEmitter";

    private static final Logger LOGGER = LogManager.getLogger(JDBCEmitterSpec.class);

    static String PATH_COL_NAME = "path";

    static String ATTACHMENT_NUM_COL_NAME = "attach_num";

    private Optional<String> connectionString;

    private Optional<String> insertString;

    private Optional<String> tableName;

    JDBCEmitterSpec(String emitterClass, List<MetadataTuple> metadataTuples) {
        super(emitterClass, metadataTuples);
    }

    public JDBCEmitterSpec(@JsonProperty("metadataTuples") List<MetadataTuple> metadataTuples) {
        this(EMITTER_CLASS, metadataTuples);
    }

    @Override
    public ValidationResult validate() {
        return null;
    }

    @Override
    public ValidationResult initialize() throws IOException {
        createAndSetInsertString(getTableName().get());
        //TODO -- fix this
        return ValidationResult.OK;
    }

    Optional<String> getTableName() {
        return tableName;
    }

    public void set(String tableName) {
        this.tableName = Optional.of(tableName);
    }

    @Override
    public void write(DomWriter writer, Element properties) {
        Element emitters = writer.createAndGetElement(properties, "emitters");
        Element emitterElement = writer.createAndGetElement(emitters, "emitter", "class",
                "org.apache.tika.pipes.emitter.jdbc.JDBCEmitter");
        Element params = writer.createAndGetElement(emitterElement, "params");
        writer.appendTextElement(params, "name", "emitter");
        writer.appendTextElement(params, "connection", connectionString.get());
        writer.appendTextElement(params, "insert", insertString.get());
        writer.appendTextElement(params, "attachmentStrategy", "all");

        if (getMetadataTuples().size() == 0) {
            LOGGER.warn("metadata tuples list is empty?!");
            return;
        }
        Map<String, String> map = new LinkedHashMap<>();
        getMetadataTuples().stream()
                .forEach(e -> map.put(e.getOutput(), e.getProperty()));

        writer.appendMap(params, "keys", "key", map);
    }

    public Optional<String> getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = Optional.of(connectionString);
    }

    void createAndSetInsertString(String tableName) {
        StringBuilder sb = new StringBuilder();
        sb.append("insert into ").append(tableName).append(" (");
        sb.append(PATH_COL_NAME).append(", ").append(ATTACHMENT_NUM_COL_NAME);
        int colCount = 2;
        for (MetadataTuple t : getMetadataTuples()) {
            sb.append(", ");
            sb.append(t.getOutput());
            colCount++;
        }
        sb.append(") values (?");
        for (int i = 1; i < colCount; i++) {
            sb.append(",?");
        }
        sb.append(")");
        insertString = Optional.of(sb.toString());
    }


    @Override
    public Set<String> getClassPathDependencies() {
        Set<String> items = new HashSet<>();
        items.add(
                AppContext.TIKA_LIB_PATH.resolve("tika-emitter-jdbc")
                        .toAbsolutePath() + "/*");
        if (getConnectionString().isEmpty()) {
            LOGGER.warn("connection string is empty?!");
            return Collections.EMPTY_SET;
        }
        String connectionString = getConnectionString().get();
        if (connectionString.startsWith("jdbc:sqlite")) {
            items.add(
                    AppContext.TIKA_LIB_PATH.resolve("db/sqlite").toAbsolutePath() + "/*");
        } else if (connectionString.startsWith("jdbc:h2")) {
            items.add(AppContext.TIKA_LIB_PATH.resolve("db/h2").toAbsolutePath() + "/*");
        } else if (connectionString.startsWith("jdbc:postgres")) {
            items.add(AppContext.TIKA_LIB_PATH.resolve("db/postgresql").toAbsolutePath() +
                    "/*");
        }
        return items;
    }


    public void setTableName(String tableNameString) {
        this.tableName = Optional.of(tableNameString);
    }
}
