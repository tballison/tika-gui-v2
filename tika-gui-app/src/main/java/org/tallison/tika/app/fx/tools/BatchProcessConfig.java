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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.tallison.tika.app.fx.Constants;
import org.tallison.tika.app.fx.ctx.AppContext;

import org.apache.tika.utils.ProcessUtils;

public class BatchProcessConfig {

    private ConfigItem pipesIterator;
    private ConfigItem fetcher;
    private ConfigItem emitter;
    private ConfigItem metadataMapper;

    private List<String> tikaMetadata = new ArrayList<>();
    private List<String> outputMetadata = new ArrayList<>();

    @JsonIgnore
    private StringProperty fetcherLabel = new SimpleStringProperty("Unselected");

    @JsonIgnore
    private StringProperty emitterLabel = new SimpleStringProperty("Unselected");

    public ConfigItem getPipesIterator() {
        return pipesIterator;
    }

    @JsonSetter
    public void setPipesIterator(ConfigItem pipesIterator) {
        this.pipesIterator = pipesIterator;
    }

    public void setPipesIterator(String ... args) {
        setPipesIterator(ConfigItem.build(args));
    }

    public ConfigItem getFetcher() {
        return fetcher;
    }

    public StringProperty getFetcherLabel() {
        return fetcherLabel;
    }

    public StringProperty getEmitterLabel() {
        return emitterLabel;
    }
    private void setFetcherLabel(String label) {
        fetcherLabel.setValue(label);
    }

    @JsonSetter
    public void setFetcher(ConfigItem fetcher) {
        this.fetcher = fetcher;
        if (fetcher != null) {
            this.fetcherLabel.setValue(fetcher.getLabel());
        }
    }

    private void setEmitterLabel(String label) {
        emitterLabel.setValue(label);
    }

    public void setFetcher(String ... args) {
        setFetcher(ConfigItem.build(args));
        setFetcherLabel(fetcher.getLabel());
    }

    public ConfigItem getEmitter() {
        return emitter;
    }

    @JsonSetter
    public void setEmitter(ConfigItem emitter) {
        this.emitter = emitter;
        setEmitterLabel(emitter.getLabel());
    }

    public void setEmitter(String ... args) {
        setEmitter(ConfigItem.build(args));
    }

    public ConfigItem getMetadataMapper() {
        return metadataMapper;
    }

    @JsonSetter
    public void setMetadataMapper(ConfigItem metadataMapper) {
        this.metadataMapper = metadataMapper;
    }

    public void addTikaMetadata(int row, String data) {
        if (tikaMetadata.size() <= row) {
            for (int i = tikaMetadata.size(); i <= row; i++) {
                tikaMetadata.add("");
            }
        }
        System.out.println("adding to tika " + row + " : " + data);
        tikaMetadata.set(row, data);
    }

    public void addOutputMetadata(int row, String data) {
        if (outputMetadata.size() <= row) {
            for (int i = outputMetadata.size(); i <= row; i++) {
                outputMetadata.add("");
            }
        }
        System.out.println("adding to output " + row + " : " + data);
        outputMetadata.set(row, data);
    }
    public void appendPipesClasspath(StringBuilder sb) {
        //TODO -- build this out
        if (getEmitter().getClazz().equals(Constants.FS_EMITTER_CLASS)) {
            sb.append(
                    ProcessUtils.escapeCommandLine(
                            AppContext.TIKA_LIB_PATH.resolve("tika-emitter-fs").toAbsolutePath() + "/*"));
        }
    }
}
