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
package org.tallison.tika.gui.tools.deprecated;

import java.util.Map;

public class SnapshotResult {

    private final String version;
    private final String jarUrl;
    private final Map<String, String> digests;

    public SnapshotResult(String version, String jarUrl, Map<String, String> digests) {
        this.version = version;
        this.jarUrl = jarUrl;
        this.digests = digests;
    }

    public String getUrl() {
        return jarUrl;
    }

    public Map<String, String> getDigests() {
        return digests;
    }

    @Override
    public String toString() {
        return "SnapshotResult{" + "version='" + version + '\'' + ", jarUrl='" + jarUrl + '\'' +
                ", digests=" + digests + '}';
    }
}
