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

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class MetadataRow {

    private final SimpleStringProperty tika = new SimpleStringProperty("");
    private final SimpleStringProperty output = new SimpleStringProperty("");

    private final SimpleStringProperty property = new SimpleStringProperty("");

    public MetadataRow() {
    }

    public MetadataRow(String tikaVal, String outputVal, String propertyVal) {
        tika.set(tikaVal);
        output.set(outputVal);
        property.set(propertyVal);
    }

    public String getTika() {
        return tika.get();
    }


    public void setTika(String tika) {
        this.tika.set(tika);
    }

    public String getOutput() {
        return output.get();
    }

    public void setOutput(String output) {
        this.output.set(output);
    }

    public StringProperty outputProperty() {
        return output;
    }

    public StringProperty tikaProperty() {
        return tika;
    }

    public String getProperty() {
        return property.get();
    }

    public SimpleStringProperty propertyProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property.set(property);
    }

    @Override
    public String toString() {
        return "MetadataRow{" + "tika=" + tika + ", output=" + output + ", property=" + property +
                '}';
    }
}
