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
package org.tallison.tika.app.fx.status;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

public class StatusCount implements Comparable<StatusCount> {
    private final SimpleStringProperty statusName = new SimpleStringProperty("");
    private final SimpleDoubleProperty count = new SimpleDoubleProperty();

    public StatusCount(String statusName, double count) {
        this.statusName.set(statusName);
        this.count.set(count);
    }

    public String getStatusName() {
        return statusName.get();
    }

    public SimpleStringProperty statusNameProperty() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName.set(statusName);
    }

    public double getCount() {
        return count.get();
    }

    public SimpleDoubleProperty countProperty() {
        return count;
    }

    public void setCount(double count) {
        this.count.set(count);
    }

    @Override
    public int compareTo(StatusCount o) {
        return Double.compare(o.count.get(), this.count.get());
    }
}
