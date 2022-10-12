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
package org.tallison.tika.app.fx.ctx;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tallison.tika.app.fx.tools.BatchProcess;
import org.tallison.tika.app.fx.tools.BatchProcessConfig;


public class AppContext {

    private static Logger LOGGER = LogManager.getLogger(AppContext.class);

    private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static Path TIKA_APP_HOME =
            Paths.get(System.getProperty("user.home")).resolve(".tika-app-v2");
    public static Path TIKA_LIB_PATH = TIKA_APP_HOME.resolve("lib");
    public static Path TIKA_CORE_BIN_PATH = TIKA_LIB_PATH.resolve("tika-core");
    public static Path TIKA_APP_BIN_PATH = TIKA_LIB_PATH.resolve("tika-app");
    public static Path APP_STATE_PATH = TIKA_APP_HOME.resolve("tika-app-v2-config.json");
    private static AppContext APP_CONTEXT = load();
    public static Path CONFIG_PATH = TIKA_APP_HOME.resolve("config");
    public static Path ASYNC_LOG4J2_PATH = CONFIG_PATH.resolve("log4j2-async.xml");
    public static Path LOGS_PATH = TIKA_APP_HOME.resolve("logs");
    public static Path BATCH_STATUS_PATH = LOGS_PATH.resolve("batch_status.json");



    private String tikaVersion = "2.4.1";
    private BatchProcessConfig batchProcessConfig = new BatchProcessConfig();
    private BatchProcess batchProcess;
    private volatile boolean closed = false;
    private volatile boolean allowBatchToRunOnExit = false;

    public static AppContext load() {
        if (Files.isRegularFile(APP_STATE_PATH)) {
            try {
                return AppContext.load(APP_STATE_PATH);
            } catch (IOException e) {
                LOGGER.warn("failed to load " + APP_STATE_PATH, e);
                return new AppContext();
            }
        }
        return new AppContext();
    }

    public static AppContext getInstance() {
        return APP_CONTEXT;
    }

    private static AppContext load(Path configPath) throws IOException {
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            return OBJECT_MAPPER.readValue(reader, AppContext.class);
        }
    }

    public void setAllowBatchToRunOnExit(boolean allowBatchToRunOnExit) {
        this.allowBatchToRunOnExit = allowBatchToRunOnExit;
    }

    public void close() {
        if (!closed) {
            if (!AppContext.getInstance().allowBatchToRunOnExit) {
                AppContext.getInstance().getBatchProcess().cancel();
            }
            AppContext.getInstance().getBatchProcess().close();
            saveState();
            closed = true;
        }
    }

    public void saveState() {
        try {
            if (!Files.isDirectory(APP_STATE_PATH.getParent())) {
                Files.createDirectories(APP_STATE_PATH.getParent());
            }
            LOGGER.info("writing state to " + APP_STATE_PATH);
            OBJECT_MAPPER.writeValue(APP_STATE_PATH.toFile(), this);
        } catch (IOException e) {
            LOGGER.warn("can't save state!", e);
        }
    }

    public void reset() {
        try {
            Files.delete(TIKA_APP_HOME.resolve("tika-app-v2-config.json"));
            FileUtils.deleteDirectory(TIKA_APP_HOME.resolve("config").toFile());
            FileUtils.deleteDirectory(TIKA_APP_HOME.resolve("logs").toFile());
        } catch (IOException e) {
            LOGGER.warn("couldn't delete " + TIKA_APP_HOME, e);
        }
    }

    public String getTikaVersion() {
        return tikaVersion;
    }

    public void setTikaVersion(String tikaVersion) {
        //TODO add range checks
        this.tikaVersion = tikaVersion;
    }

    public BatchProcessConfig getBatchProcessConfig() {
        return batchProcessConfig;
    }

    @JsonSetter
    public void setBatchProcessConfig(BatchProcessConfig batchProcessConfig) {
        this.batchProcessConfig = batchProcessConfig;
    }

    public BatchProcess getBatchProcess() {
        return batchProcess;
    }

    public void setBatchProcess(BatchProcess batchProcess) {
        this.batchProcess = batchProcess;
    }
}
