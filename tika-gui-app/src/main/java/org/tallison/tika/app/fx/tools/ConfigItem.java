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

import java.util.HashMap;
import java.util.Map;

public class ConfigItem {

    public static ConfigItem build(String... args) {
        Map<String, String> params = new HashMap<>();
        for (int i = 2; i < args.length; i++) {
            params.put(args[i], args[++i]);
        }
        return new ConfigItem(args[0], args[1], params);
    }

    private String label;
    private String clazz;
    private Map<String, String> attributes;

    public ConfigItem() {
        //needed for serialization for now...should figure out cleaner solution

    }
    public ConfigItem(String label, String clazz, Map<String, String> attributes) {
        this.label = label;
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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return "ConfigItem{" + "label='" + label + '\'' + ", clazz='" + clazz + '\'' +
                ", attributes=" + attributes + '}';
    }
}
