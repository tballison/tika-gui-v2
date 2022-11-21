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
module org.tallison.tika.app.fx {
    requires java.sql;
    requires javafx.graphics;
    requires org.apache.tika.core;
    requires org.apache.commons.io;
    requires com.fasterxml.jackson.annotation;
    requires javafx.controls;
    requires javafx.fxml;
    requires commons.csv;
    requires org.apache.logging.log4j;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.datatype.jdk8;
    requires org.kordamp.ikonli.javafx;

    exports org.tallison.tika.app.fx;

    opens org.tallison.tika.app.fx to javafx.fxml, com.fasterxml.jackson.databind;
    opens org.tallison.tika.app.fx.ctx to com.fasterxml.jackson.databind, javafx.fxml;
    opens org.tallison.tika.app.fx.status to javafx.base;
    opens org.tallison.tika.app.fx.metadata to com.fasterxml.jackson.databind, javafx.fxml, javafx.base;
    exports org.tallison.tika.app.fx.emitters;
    opens org.tallison.tika.app.fx.emitters to com.fasterxml.jackson.databind, javafx.fxml;
    opens org.tallison.tika.app.fx.config to com.fasterxml.jackson.databind, javafx.fxml;
    opens org.tallison.tika.app.fx.batch to com.fasterxml.jackson.databind, javafx.fxml;
}
