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
package org.tallison.tika.app.fx.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.tallison.tika.app.fx.Constants;
import org.tallison.tika.app.fx.ctx.AppContext;

import org.apache.tika.utils.ProcessUtils;

public class BatchProcessConfig {

    private Optional<ConfigItem> pipesIterator = Optional.empty();
    private Optional<ConfigItem> fetcher = Optional.empty();
    private Optional<ConfigItem> emitter = Optional.empty();


    @JsonIgnore
    private StringProperty fetcherLabel = new SimpleStringProperty("Unselected");

    @JsonIgnore
    private StringProperty emitterLabel = new SimpleStringProperty("Unselected");

    private int outputSelectedTab = 0;

    private int inputSelectedTab = 0;

    private Optional<String> digest = Optional.of("No Digest");

    private int numProcesses = 5;

    private int maxMemMb = 1024;

    private int parseTimeoutSeconds = 120;

    private int perFileEmitThresholdMb = 100;

    private int totalEmitThesholdMb = 1000;

    private long emitWithinMs = 10000;

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
            this.fetcherLabel.setValue(this.fetcher.get().getLabel());
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

    public Optional<ConfigItem> getEmitter() {
        return emitter;
    }

    @JsonSetter
    public void setEmitter(ConfigItem emitter) {
        this.emitter = Optional.ofNullable(emitter);
        if (this.emitter.isPresent()) {
            setEmitterLabel(this.emitter.get().getLabel());
        }
    }

    public void setEmitter(String... args) {
        setEmitter(ConfigItem.build(args));
    }


    public void appendPipesClasspath(StringBuilder sb) {
        //TODO -- build this out for fetchers, emitters and pipes iterators.
        if (! getEmitter().isEmpty()) {
            ConfigItem emitter = getEmitter().get();
            if (emitter.getClazz().equals(Constants.FS_EMITTER_CLASS)) {
                sb.append(ProcessUtils.escapeCommandLine(
                        AppContext.TIKA_LIB_PATH.resolve("tika-emitter-fs").toAbsolutePath() + "/*"));
            } else if (emitter.getClazz().equals(Constants.OPEN_SEARCH_EMITTER_CLASS)) {
                sb.append(ProcessUtils.escapeCommandLine(
                        AppContext.TIKA_LIB_PATH.resolve("tika-emitter-opensearch").toAbsolutePath() + "/*"));
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

    public void setOutputSelectedTab(int outputSelectedTab) {
        this.outputSelectedTab = outputSelectedTab;
    }

    public void setInputSelectedTab(int inputSelectedTab) {
        this.inputSelectedTab = inputSelectedTab;
    }

    public int getOutputSelectedTab() {
        return outputSelectedTab;
    }

    public int getInputSelectedTab() {
        return inputSelectedTab;
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
}
