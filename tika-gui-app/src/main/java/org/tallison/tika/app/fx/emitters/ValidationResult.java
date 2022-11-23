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

import java.util.Optional;

public class ValidationResult {

    public static ValidationResult OK = new ValidationResult(VALIDITY.OK);
    private final VALIDITY validity;
    private final Optional<String> title;
    private final Optional<String> header;
    private final Optional<String> msg;
    public ValidationResult(VALIDITY validity) {
        this.validity = validity;
        this.msg = Optional.empty();
        this.title = Optional.empty();
        this.header = Optional.empty();
    }

    public ValidationResult(VALIDITY validity, String title, String header, String msg) {
        this.validity = validity;
        this.title = Optional.of(title);
        this.header = Optional.of(header);
        this.msg = Optional.of(msg);
    }

    public VALIDITY getValidity() {
        return validity;
    }

    public Optional<String> getTitle() {
        return title;
    }

    public Optional<String> getHeader() {
        return header;
    }

    public Optional<String> getMsg() {
        return msg;
    }

    @Override
    public String toString() {
        return "ValidationResult{" + "validity=" + validity + ", title=" + title + ", header=" +
                header + ", msg=" + msg + '}';
    }

    public enum VALIDITY {
        OK, NOT_OK
    }
}
