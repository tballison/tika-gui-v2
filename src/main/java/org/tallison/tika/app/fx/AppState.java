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
package org.tallison.tika.app.fx;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class AppState {

    private static Logger LOGGER = LogManager.getLogger(AppState.class);
    private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static Path TIKA_GUI_APP_PATH = Paths.get(System.getProperty("user.home"))
            .resolve(".tika-app-v2");

    private static Path DEFAULT_TIKA_APP_BIN_PATH = TIKA_GUI_APP_PATH.resolve("bin");
    private static Path DEFAULT_STATE_PATH = TIKA_GUI_APP_PATH.resolve("tika-app-v2-config.json");

    private Path tikaAppBinPath = DEFAULT_TIKA_APP_BIN_PATH;

    private String tikaVersion = "2.4.1";

    private ConfigItem pipesIterator;
    private ConfigItem fetcher;

    private ConfigItem emitter;

    public void setTikaAppBin(Path tikaAppBin) {
        this.tikaAppBinPath = tikaAppBin;
    }

    public Path getTikaAppBinPath() {
        return tikaAppBinPath;
    }
    public static AppState load() {
        if (Files.isRegularFile(DEFAULT_STATE_PATH)) {
            try {
                return AppState.load(DEFAULT_STATE_PATH);
            } catch (IOException e) {
                LOGGER.warn("failed to load " + DEFAULT_STATE_PATH, e);
                return new AppState();
            }
        }
        return new AppState();
    }

    private static AppState load(Path configPath) throws IOException {
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            return OBJECT_MAPPER.readValue(reader, AppState.class);
        }
    }

    public void close() {
        try {
            saveState();
        } catch (IOException e) {
            LOGGER.warn("Failed to save state file " +
                    DEFAULT_STATE_PATH, e);
        }
    }

    public void saveState() throws IOException {
        if (! Files.isDirectory(DEFAULT_STATE_PATH.getParent())) {
            Files.createDirectories(DEFAULT_STATE_PATH.getParent());
        }
        LOGGER.info("writing state to " + DEFAULT_STATE_PATH);
        OBJECT_MAPPER.writeValue(DEFAULT_STATE_PATH.toFile(), this);
    }

    public void reset() {
        tikaAppBinPath = DEFAULT_TIKA_APP_BIN_PATH;
        try {
            FileUtils.deleteDirectory(TIKA_GUI_APP_PATH.toFile());
        } catch (IOException e) {
            LOGGER.warn("couldn't delete " + TIKA_GUI_APP_PATH, e);
        }
    }

    public String getTikaVersion() {
        return tikaVersion;
    }

    public void setTikaVersion(String tikaVersion) {
        //TODO add range checks
        this.tikaVersion = tikaVersion;
    }

    public void setFetcher(String ... args) {
        setFetcher(ConfigItem.build(args));
    }

    @JsonSetter
    public void setFetcher(ConfigItem configItem) {
        this.fetcher = configItem;
        LOGGER.info("set fetcher " + configItem);
    }

    public void setPipesIterator(String ... args) {
        setPipesIterator(ConfigItem.build(args));
    }

    @JsonSetter
    public void setPipesIterator(ConfigItem configItem) {
        this.pipesIterator = configItem;
        LOGGER.info("set pipes iterator " + configItem);
    }

    public void setEmitter(String ... args) {
        setFetcher(ConfigItem.build(args));
    }

    @JsonSetter
    public void setEmitter(ConfigItem configItem) {
        this.fetcher = configItem;
        LOGGER.info("set emitter " + configItem);
    }

    public ConfigItem getPipesIterator() {
        return pipesIterator;
    }

    public ConfigItem getFetcher() {
        return fetcher;
    }

    public ConfigItem getEmitter() {
        return emitter;
    }

    private static class ConfigItem {
        private static ConfigItem build(String ... args) {
            Map<String, String> params = new HashMap<>();
            for (int i = 1; i < args.length; i++) {
                params.put(args[i], args[++i]);
            }
            return new ConfigItem(args[0], params);
        }

        private String clazz;
        private Map<String, String> attributes;

        public ConfigItem(String clazz, Map<String, String> attributes) {
            this.clazz = clazz;
            this.attributes = attributes;
        }

        public void setClazz(String clazz) {
            this.clazz = clazz;
        }

        public void setAttributes(Map<String, String> attrs) {
            this.attributes = attrs;
        }

        public String getClazz() {
            return clazz;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }

        @Override
        public String toString() {
            return "ConfigItem{" + "clazz='" + clazz + '\'' + ", attributes=" + attributes + '}';
        }
    }
}
