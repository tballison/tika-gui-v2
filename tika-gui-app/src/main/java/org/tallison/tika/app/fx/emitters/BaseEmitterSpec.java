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
import java.util.List;
import java.util.Optional;

import org.tallison.tika.app.fx.metadata.MetadataTuple;


public abstract class BaseEmitterSpec implements EmitterSpec {


    private final List<MetadataTuple> metadataTuples;
    boolean valid = false;
    private Optional<String> shortLabel = Optional.empty();
    private Optional<String> fullLabel = Optional.empty();
    private Optional<String> notValidMessage = Optional.empty();

    public BaseEmitterSpec(List<MetadataTuple> metadataTuples) {
        this.metadataTuples = metadataTuples;
    }

    public List<MetadataTuple> getMetadataTuples() {
        return metadataTuples;
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
    public void setValid(boolean valid) {
        this.valid = valid;
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
    public void setShortLabel(String shortLabel) {
        this.shortLabel = Optional.of(shortLabel);
    }

    @Override
    public Optional<String> getFullLabel() {
        return fullLabel;
    }

    @Override
    public void setFullLabel(String fullLabel) {
        this.fullLabel = Optional.of(fullLabel);
    }

}
