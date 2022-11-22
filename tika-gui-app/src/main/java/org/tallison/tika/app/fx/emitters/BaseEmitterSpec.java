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

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.tallison.tika.app.fx.metadata.MetadataRow;
import org.tallison.tika.app.fx.metadata.MetadataTuple;

import org.apache.tika.utils.StringUtils;


public abstract class BaseEmitterSpec implements EmitterSpec {

    private final String emitterClass;
    private final List<MetadataTuple> metadataTuples;

    private Optional<String> shortLabel = Optional.empty();

    private Optional<String> fullLabel = Optional.empty();

    private Optional<String> notValidMessage = Optional.empty();

    boolean valid = false;

    public BaseEmitterSpec(String emitterClass, List<MetadataTuple> metadataTuples) {
        this.emitterClass = emitterClass;
        this.metadataTuples = metadataTuples;
    }

    public List<MetadataTuple> getMetadataTuples() {
        return metadataTuples;
    }

    void validateMetadata() {

    }

    /**
     * This checks for empty keys and duplicate output keys
     *
     * @return
     */
    protected ValidationResult validateMetadataRows() {

        //Set<String> tika = new HashSet<>();
        //duplicate tika keys are ok?
        Set<String> output = new HashSet<>();
        int i = 0;
        for (MetadataTuple row : getMetadataTuples()) {
            String t = row.getTika();
            if (StringUtils.isBlank(t)) {
                return new ValidationResult(ValidationResult.VALIDITY.NOT_OK,
                    "Blank Tika key", "Blank Tika key",
                        "There's an empty Tika key in row " + i + ". The output value is: " +
                                row.getOutput());
            }
            String o = row.getOutput();
            if (StringUtils.isBlank(o)) {
                return new ValidationResult(ValidationResult.VALIDITY.NOT_OK,"Blank output key",
                        "Blank output key",
                        "There's an empty output key in row " + i + ". The Tika value is: " +
                                row.getTika());
            } else {
                if (output.contains(o)) {
                    return new ValidationResult(ValidationResult.VALIDITY.NOT_OK,
                            "Duplicate output key", "Duplicate output key",
                            "There's a duplicate output key '" + o + "'");
                }
            }
            output.add(o);
            i++;
        }
        return ValidationResult.OK;
    }

    @Override
    public void close() throws IOException {
        //no-op
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public Optional<String> getNotValidMessage() {
        return notValidMessage;
    }

    public void setNotValidMessage(String notValidMessage) {
        this.notValidMessage = Optional.of(notValidMessage);
    }

    public Optional<String> getShortLabel() {
        return shortLabel;
    }

    @Override
    public Optional<String> getFullLabel() {
        return fullLabel;
    }

    @Override
    public void setShortLabel(String shortLabel) {
        this.shortLabel = Optional.of(shortLabel);
    }

    @Override
    public void setFullLabel(String fullLabel) {
        this.fullLabel = Optional.of(fullLabel);
    }

}
