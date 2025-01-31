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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tallison.tika.app.fx.batch.BatchProcess;
import org.tallison.tika.app.fx.batch.BatchProcessConfig;

import org.apache.tika.utils.StringUtils;


public class AppContext {

    private static final Logger LOGGER = LogManager.getLogger(AppContext.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static Path TIKA_GUI_JAVA_HOME;
    public static Path TIKA_APP_HOME = Paths.get("");
    public static Path TIKA_LIB_PATH = TIKA_APP_HOME.resolve("lib");
    public static Path TIKA_CORE_BIN_PATH = TIKA_LIB_PATH.resolve("tika-core");
    public static Path TIKA_APP_BIN_PATH = TIKA_LIB_PATH.resolve("tika-app");
    public static Path TIKA_EXTRAS_BIN_PATH = TIKA_LIB_PATH.resolve("tika-extras");
    public static Path APP_STATE_PATH = TIKA_APP_HOME.resolve("config/tika-app-v2-config.json");
    public static Path CONFIG_PATH = TIKA_APP_HOME.resolve("config");
    public static Path ASYNC_LOG4J2_PATH = CONFIG_PATH.resolve("log4j2-async.xml");
    public static Path LOGS_PATH = TIKA_APP_HOME.resolve("logs");
    public static Path BATCH_STATUS_PATH = LOGS_PATH.resolve("batch_status.json");
    private static volatile AppContext APP_CONTEXT;

    static {
        //this is necessary for optionals
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        //this is necessary for timestamps
        OBJECT_MAPPER.registerModule(new Jdk8Module());
    }

    static {
        System.out.println(System.getProperties());
        if (!StringUtils.isBlank(System.getProperty("TIKA_GUI_JAVA_HOME"))) {
            LOGGER.debug("setting TIKA_GUI_JAVA_HOME {}", System.getProperty("TIKA_GUI_JAVA_HOME"));
            TIKA_GUI_JAVA_HOME = Paths.get(System.getProperty("TIKA_GUI_JAVA_HOME"));
        } else if (!StringUtils.isBlank(System.getProperty("java.home"))) {
            TIKA_GUI_JAVA_HOME = Paths.get(System.getProperty("java.home"));
            //TODO -- java_home should not include the bin directory.
            //the "if" branch above is normally triggered through the .sh scripts,
            //which incorrectly set java_home to java_home/bin
            //Clean this up.
            if (Files.isDirectory(TIKA_GUI_JAVA_HOME.resolve("bin"))) {
                TIKA_GUI_JAVA_HOME = TIKA_GUI_JAVA_HOME.resolve("bin");
            }
            LOGGER.debug("setting TIKA_GUI_JAVA_HOME {} from java.home",
                    System.getProperty("java.home"));
        }
    }

    private final boolean allowBatchToRunOnExit = false;
    private String tikaVersion = "2.6.0";
    private Optional<BatchProcessConfig> batchProcessConfig = Optional.of(new BatchProcessConfig());
    @JsonIgnore
    private Optional<BatchProcess> batchProcess = Optional.empty();
    private volatile boolean closed = false;

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

    public static synchronized AppContext getInstance() {
        if (APP_CONTEXT == null) {
            APP_CONTEXT = load();
            return APP_CONTEXT;
        } else {
            return APP_CONTEXT;
        }
    }

    private static AppContext load(Path configPath) throws IOException {
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            AppContext appContext = OBJECT_MAPPER.readValue(reader, AppContext.class);
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
            LOGGER.debug("writing state to " + APP_STATE_PATH);
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

    @JsonIgnore
    public Path getJavaHome() {
        return TIKA_GUI_JAVA_HOME;
    }
}
