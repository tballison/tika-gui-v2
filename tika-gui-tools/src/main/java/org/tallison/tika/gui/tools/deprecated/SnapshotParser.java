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

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

//this is a horror show.  Don't do this.
public class SnapshotParser {

    public SnapshotResult parse(String moduleUrl) throws IOException {
        if (moduleUrl.endsWith("/")) {
            moduleUrl = moduleUrl.substring(0, moduleUrl.length() - 1);
        }
        //https://repository.apache.org/content/groups/snapshots/org/apache/tika/tika-core
        String moduleMetadataUrl = moduleUrl + "/maven-metadata.xml";
        String moduleXML = IOUtils.toString(new URL(moduleMetadataUrl), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("<latest>(.*?)</latest>").matcher(moduleXML);
        String version = "";
        if (m.find()) {
            version = m.group(1);
        }
        String moduleVersionUrl = moduleUrl + "/" + version + "/" + "maven-metadata.xml";
        String moduleVersionXML = IOUtils.toString(new URL(moduleVersionUrl),
                StandardCharsets.UTF_8);
        m = Pattern.compile("(?si)<(artifactId)>(.*?)</\\1>")
                        .matcher(moduleVersionXML);

        String artifactId = "";

        while (m.find()) {
            String entity = m.group(1);
            String val = m.group(2);
            if (entity.equals("artifactId")) {
                artifactId = val;
            }
        }
        String nonSnapshotVersion = version.replace("-SNAPSHOT", "");

        String indexHtml = IOUtils.toString(new URL(moduleUrl + "/" + version + "/"),
                StandardCharsets.UTF_8);
        String jarUrl = "";
        //moduleUrl + "/" + version + "/" + artifactId + "-" + nonSnapshotVersion + "-" +
        // timestampAndBuildNumber + ".jar";
        m = Pattern.compile("href=\"([^\"]+)").matcher(indexHtml);
        //find the latest
        while (m.find()) {
            String u = m.group(1);
            if (u.endsWith(".jar")) {
                jarUrl = u;
            }
        }

        m = Pattern.compile("href=\"([^\"]+)").matcher(indexHtml);
        Map<String, String> digests = new HashMap<>();
        while (m.find()) {
            String url = m.group(1);
            if (url.startsWith(jarUrl)) {
                String digest = url.substring(jarUrl.length());
                if (digest.startsWith(".")) {
                    digest = digest.substring(1);
                    String digestString = IOUtils.toString(new URL(url), StandardCharsets.UTF_8);
                    digests.put(digest, digestString);
                }
            }
        }
        return new SnapshotResult(version, jarUrl, digests);
    }
}
