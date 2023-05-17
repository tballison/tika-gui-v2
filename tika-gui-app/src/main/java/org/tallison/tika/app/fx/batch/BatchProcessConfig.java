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
package org.tallison.tika.app.fx.batch;


import java.io.File;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.tallison.tika.app.fx.advanced.DetectorConfig;
import org.tallison.tika.app.fx.advanced.ParserConfig;
import org.tallison.tika.app.fx.config.ConfigItem;
import org.tallison.tika.app.fx.emitters.EmitterSpec;

public class BatchProcessConfig {

    @JsonIgnore
    private final StringProperty fetcherLabel = new SimpleStringProperty("Unselected");
    @JsonIgnore
    private final StringProperty emitterLabel = new SimpleStringProperty("Unselected");
    private Optional<ConfigItem> pipesIterator = Optional.empty();
    private Optional<ConfigItem> fetcher = Optional.empty();
    private Optional<EmitterSpec> emitter = Optional.empty();

    private ParserConfig parserConfig = null;
    private DetectorConfig detectorConfig = null;
    private int outputSelectedTab = 0;

    private int inputSelectedTab = 0;

    private Optional<String> digest = Optional.of("sha256");

    private int numProcesses = 5;

    private int maxMemMb = 1024;

    private int parseTimeoutSeconds = 120;

    private int perFileEmitThresholdMb = 1;

    private int totalEmitThesholdMb = 100;

    private long emitWithinMs = 10000;

    private long writeLimit = -1;

    public Optional<ConfigItem> getPipesIterator() {
        return pipesIterator;
    }

    @JsonSetter
    public void setPipesIterator(ConfigItem pipesIterator) {
        this.pipesIterator = Optional.ofNullable(pipesIterator);
    }

    public void setPipesIterator(String... args) {
        setPipesIterator(ConfigItem.build(args));
    }

    public Optional<ConfigItem> getFetcher() {
        return fetcher;
    }

    @JsonSetter
    public void setFetcher(ConfigItem fetcher) {
        this.fetcher = Optional.ofNullable(fetcher);
        if (this.fetcher.isPresent()) {
            this.fetcherLabel.setValue(this.fetcher.get().getShortLabel());
        }
    }

    public void setFetcher(String... args) {
        setFetcher(ConfigItem.build(args));
    }

    public StringProperty getFetcherLabel() {
        return fetcherLabel;
    }

    private void setFetcherLabel(String label) {
        fetcherLabel.setValue(label);
    }

    public StringProperty getEmitterLabel() {
        return emitterLabel;
    }

    private void setEmitterLabel(String label) {
        emitterLabel.setValue(label);
    }

    public Optional<EmitterSpec> getEmitter() {
        return emitter;
    }

    @JsonSetter
    public void setEmitter(EmitterSpec emitter) {
        this.emitter = Optional.ofNullable(emitter);
        if (this.emitter.isPresent() && this.emitter.get().getShortLabel().isPresent()) {
            setEmitterLabel(this.emitter.get().getShortLabel().get());
        }
    }


    //This add a path separator before it appends the class path
    public void appendPipesClasspath(StringBuilder sb) {
        //TODO -- build this out for fetchers and pipes iterators.
        if (!getEmitter().isEmpty()) {
            EmitterSpec emitter = getEmitter().get();
            for (String resource : emitter.getClassPathDependencies()) {
                sb.append(File.pathSeparator);
                sb.append(resource);
            }
        }
    }

    public Optional<String> getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = Optional.of(digest);
    }

    public int getNumProcesses() {
        return numProcesses;
    }

    public void setNumProcesses(int numProcesses) {
        this.numProcesses = numProcesses;
    }

    public int getParseTimeoutSeconds() {
        return parseTimeoutSeconds;
    }

    public void setParseTimeoutSeconds(int parseTimeoutSeconds) {
        this.parseTimeoutSeconds = parseTimeoutSeconds;
    }

    public int getMaxMemMb() {
        return maxMemMb;
    }

    public void setMaxMemMb(int maxMemMb) {
        this.maxMemMb = maxMemMb;
    }

    public int getOutputSelectedTab() {
        return outputSelectedTab;
    }

    public void setOutputSelectedTab(int outputSelectedTab) {
        this.outputSelectedTab = outputSelectedTab;
    }

    public int getInputSelectedTab() {
        return inputSelectedTab;
    }

    public void setInputSelectedTab(int inputSelectedTab) {
        this.inputSelectedTab = inputSelectedTab;
    }

    public int getPerFileEmitThresholdMb() {
        return perFileEmitThresholdMb;
    }

    public void setPerFileEmitThresholdMb(int perFileEmitThresholdMb) {
        this.perFileEmitThresholdMb = perFileEmitThresholdMb;
    }

    public int getTotalEmitThesholdMb() {
        return totalEmitThesholdMb;
    }

    public void setTotalEmitThesholdMb(int totalEmitThesholdMb) {
        this.totalEmitThesholdMb = totalEmitThesholdMb;
    }

    public long getEmitWithinMs() {
        return emitWithinMs;
    }

    public void setEmitWithinMs(long emitWithinMs) {
        this.emitWithinMs = emitWithinMs;
    }

    public long getWriteLimit() {
        return writeLimit;
    }

    public void setWriteLimit(long writeLimit) {
        this.writeLimit = writeLimit;
    }

    public void setParserConfig(ParserConfig parserConfig) {
        this.parserConfig = parserConfig;
    }

    public Optional<ParserConfig> getParserConfig() {
        if (parserConfig == null) {
            return Optional.empty();
        }
        return Optional.of(parserConfig);
    }

    public void setDetectorConfig(DetectorConfig detectorConfig) {
        this.detectorConfig = detectorConfig;
    }

    public Optional<DetectorConfig> getDetectorConfig() {
        if (detectorConfig == null) {
            return Optional.empty();
        }
        return Optional.of(detectorConfig);
    }
}
