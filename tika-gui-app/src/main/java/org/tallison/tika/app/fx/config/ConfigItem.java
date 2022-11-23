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
package org.tallison.tika.app.fx.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.tallison.tika.app.fx.metadata.MetadataTuple;

public class ConfigItem {

    private String shortLabel;

    private String fullLabel;
    private String clazz;
    private Map<String, String> attributes;


    //TODO -- this is a hack for now...
    private boolean valid = true;


    private Optional<List<MetadataTuple>> metadataTuples = Optional.empty();

    public ConfigItem() {
        //needed for serialization for now...should figure out cleaner solution

    }

    public ConfigItem(String shortLabel, String fullLabel, String clazz,
                      Map<String, String> attributes) {
        this.shortLabel = shortLabel;
        this.fullLabel = fullLabel;
        this.clazz = clazz;
        this.attributes = attributes;
    }

    public static ConfigItem build(String... args) {
        Map<String, String> params = new HashMap<>();
        for (int i = 3; i < args.length; i++) {
            params.put(args[i], args[++i]);
        }
        return new ConfigItem(args[0], args[1], args[2], params);
    }

    public String getShortLabel() {
        return shortLabel;
    }

    public void setShortLabel(String shortLabel) {
        this.shortLabel = shortLabel;
    }

    public String getFullLabel() {
        return fullLabel;
    }

    public void setFullLabel(String fullLabel) {
        this.fullLabel = fullLabel;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }


    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    @Override
    public String toString() {
        return "ConfigItem{" + "shortLabel='" + shortLabel + '\'' + ", fullLabel='" + fullLabel +
                '\'' + ", clazz='" + clazz + '\'' + ", attributes=" + attributes +
                ", metadataTuples=" + metadataTuples + '}';
    }
}
