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
package org.tallison.tika.app.fx.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MetadataTuple {

    private final String tika;
    private final String output;
    private final String property;

    public MetadataTuple(@JsonProperty("tika") String tika, @JsonProperty("output") String output,
                         @JsonProperty("property") String property) {
        this.tika = tika;
        this.output = output;
        this.property = property;
    }

    public String getTika() {
        return tika;
    }

    public String getOutput() {
        return output;
    }

    public String getProperty() {
        return property;
    }

    @Override
    public String toString() {
        return "MetadataTuple{" + "tika='" + tika + '\'' + ", output='" + output + '\'' +
                ", property='" + property + '\'' + '}';
    }
}
