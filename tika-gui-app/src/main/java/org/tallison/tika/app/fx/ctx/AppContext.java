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
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tallison.tika.app.fx.tools.BatchProcess;
import org.tallison.tika.app.fx.tools.BatchProcessConfig;


public class AppContext {

    private static Logger LOGGER = LogManager.getLogger(AppContext.class);

    private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        //this is necessary for optionals
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        //this is necessary for timestamps
        OBJECT_MAPPER.registerModule(new Jdk8Module());
    }

    public static Path TIKA_APP_HOME =
            Paths.get(System.getProperty("user.home")).resolve(".tika-app-v2");
    public static Path TIKA_LIB_PATH = TIKA_APP_HOME.resolve("lib");
    public static Path TIKA_CORE_BIN_PATH = TIKA_LIB_PATH.resolve("tika-core");
    public static Path TIKA_APP_BIN_PATH = TIKA_LIB_PATH.resolve("tika-app");
    public static Path TIKA_EXTRAS_BIN_PATH = TIKA_LIB_PATH.resolve("tika-extras");
    public static Path APP_STATE_PATH = TIKA_APP_HOME.resolve("tika-app-v2-config.json");
    private static AppContext APP_CONTEXT = load();
    public static Path CONFIG_PATH = TIKA_APP_HOME.resolve("config");
    public static Path ASYNC_LOG4J2_PATH = CONFIG_PATH.resolve("log4j2-async.xml");
    public static Path LOGS_PATH = TIKA_APP_HOME.resolve("logs");
    public static Path BATCH_STATUS_PATH = LOGS_PATH.resolve("batch_status.json");



    private String tikaVersion = "2.4.1";
    private Optional<BatchProcessConfig> batchProcessConfig = Optional.of(new BatchProcessConfig());
    private Optional<BatchProcess> batchProcess = Optional.empty();
    private volatile boolean closed = false;
    private final boolean allowBatchToRunOnExit = false;

    public static AppContext load() {

        if (Files.isRegularFile(APP_STATE_PATH)) {
            try {
                LOGGER.debug("loading app state from {}", APP_STATE_PATH);
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
            AppContext appContext =  OBJECT_MAPPER.readValue(reader, AppContext.class);
            //for now, set the batch process to null
            appContext.setBatchProcess(null);
            return appContext;
        }
    }

    /*public void setAllowBatchToRunOnExit(boolean allowBatchToRunOnExit) {
        this.allowBatchToRunOnExit = allowBatchToRunOnExit;
    }*/

    public synchronized void close() {
        if (!closed) {
            if (APP_CONTEXT.getBatchProcess().isPresent()) {
                if (!AppContext.getInstance().allowBatchToRunOnExit) {
                    APP_CONTEXT.getBatchProcess().get().cancel();
                }
                APP_CONTEXT.getBatchProcess().get().close();
            }
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
            Files.delete(AppContext.BATCH_STATUS_PATH);
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

    public Optional<BatchProcessConfig> getBatchProcessConfig() {
        return batchProcessConfig;
    }

    @JsonSetter
    public void setBatchProcessConfig(BatchProcessConfig batchProcessConfig) {
        this.batchProcessConfig = Optional.of(batchProcessConfig);
    }

    public Optional<BatchProcess> getBatchProcess() {
        return batchProcess;
    }

    public void setBatchProcess(BatchProcess batchProcess) {
        this.batchProcess = Optional.ofNullable(batchProcess);
    }
}
