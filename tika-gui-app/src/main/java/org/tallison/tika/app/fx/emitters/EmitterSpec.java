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

import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.tallison.tika.app.fx.sax.DomWriter;
import org.w3c.dom.Element;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public interface EmitterSpec extends Closeable {


    @JsonIgnore
    boolean isValid();

    void setValid(boolean valid);

    /**
     * This is the heavier duty stuff, like creating tables.
     * This is called before the batch process is kicked off and after
     * the user is prevented from making any changes to the configuration.
     */
    ValidationResult initialize() throws IOException;

    void write(DomWriter domWriter, Element properties);

    /**
     * If this is not valid, what is the message that should be shown.
     *
     * @return
     */
    @JsonIgnore
    Optional<String> getNotValidMessage();

    Optional<String> getShortLabel();

    void setShortLabel(String shortLabel);

    Optional<String> getFullLabel();

    void setFullLabel(String fullLabel);

    @JsonIgnore
    Set<String> getClassPathDependencies();

}
