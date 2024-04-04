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

package org.tallison.tika.gui.tools;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

/**
 * this should have been a batch script.
 *
 * At some point, I'll figure out how to assemble this with a
 * maven plugin.  Today is not the day for that.
 */
public class PackageBinaries {

    //this is not robust and should check both the dlcdn
    //and the apache repo for tika artifacts

    //Further, we should parameterize all dependencies.
    private static final String TIKA_VERSION = "3.0.0-SNAPSHOT";
    private static final Map<String, String> JARS_TO_PATH = new HashMap<>();

    private static final Path LOCAL_M2 = Paths.get(System.getProperty("user.home")).resolve(".m2" +
            "/repository");

    private static final String M2_URL_BASE = "https://repo1.maven.org/maven2/";

    static {

        JARS_TO_PATH.put(
                "org/apache/tika/tika-async-cli/" +
                        TIKA_VERSION + "/tika-async-cli-" + TIKA_VERSION + ".jar",
                "lib/tika-core"
        );

        JARS_TO_PATH.put(
                "org/apache/tika/tika-pipes-reporter-fs-status/" + TIKA_VERSION +
                        "/tika-pipes-reporter-fs-status-" + TIKA_VERSION + ".jar",
                "lib/tika-core"
        );
        JARS_TO_PATH.put(
                "org/apache/tika/tika-pipes-reporter-jdbc/" + TIKA_VERSION +
                        "/tika-pipes-reporter-jdbc-" + TIKA_VERSION + ".jar",
                "lib/tika-core"
        );

        JARS_TO_PATH.put(
                "org/apache/tika/tika-serialization/" + TIKA_VERSION +
                        "/tika-serialization-" + TIKA_VERSION + ".jar",
                "lib/tika-core"
        );
        JARS_TO_PATH.put(
                "org/apache/tika/tika-app/" + TIKA_VERSION +
                        "/tika-app-" + TIKA_VERSION + ".jar", "lib/tika-app");
        JARS_TO_PATH.put(
                "org/apache/tika/tika-parser-sqlite3-package/" + TIKA_VERSION +
                        "/tika-parser-sqlite3-package-" + TIKA_VERSION + ".jar", "lib/tika-app");
        JARS_TO_PATH.put(
                "org/apache/tika/tika-eval-core/" +
                        TIKA_VERSION + "/tika-eval-core-" + TIKA_VERSION + ".jar",
                "lib/tika-extras");

        JARS_TO_PATH.put(
                "org/apache/tika/tika-detector-siegfried/" +
                        TIKA_VERSION + "/tika-detector-siegfried-" + TIKA_VERSION + ".jar",
                "lib/tika-extras");

        JARS_TO_PATH.put(
                "org/apache/tika/tika-emitter-fs/" +
                        TIKA_VERSION + "/tika-emitter-fs-" + TIKA_VERSION + ".jar",
                "lib/tika-emitter-fs");
        JARS_TO_PATH.put(
                "org/apache/tika/tika-emitter-opensearch/" +
                        TIKA_VERSION + "/tika-emitter-opensearch-" + TIKA_VERSION + ".jar",
                "lib/tika-emitter-opensearch");
        JARS_TO_PATH.put(
                "com/h2database/h2/2.2.224/h2-2.2.224.jar",
                "lib/db/h2");
        JARS_TO_PATH.put(
                "org/xerial/sqlite-jdbc/3.45.2.0/sqlite-jdbc-3.45.2.0.jar", "lib/db/sqlite");
        JARS_TO_PATH.put(
                "org/apache/tika/tika-pipes-iterator-s3/" +
                        TIKA_VERSION + "/tika-pipes-iterator-s3-" + TIKA_VERSION + ".jar",
                "lib/tika-pipes-iterator-s3");
        JARS_TO_PATH.put(
                "org/apache/tika/tika-emitter-jdbc/" +
                        TIKA_VERSION + "/tika-emitter-jdbc-" + TIKA_VERSION + ".jar",
                "lib/tika-emitter-jdbc");
        JARS_TO_PATH.put(
                "org/apache/tika/tika-emitter-s3/" +
                        TIKA_VERSION + "/tika-emitter-s3-" + TIKA_VERSION + ".jar",
                "lib/tika-emitter-s3");
        JARS_TO_PATH.put(
                "org/apache/tika/tika-fetcher-s3/" +
                        TIKA_VERSION + "/tika-fetcher-s3-" + TIKA_VERSION + ".jar",
                "lib/tika-fetcher-s3");
        JARS_TO_PATH.put(
                "org/postgresql/postgresql/42.7.3/postgresql-42.7.3.jar",
                "lib/db/postgresql");


    }

    public static void main(String[] args) throws Exception {
        Path target = Paths.get(args[0]);
        FileUtils.deleteDirectory(target.toFile());

        for (Map.Entry<String, String> e : JARS_TO_PATH.entrySet()) {
            try {
                fetchLocalM2(e.getKey(), target, e.getValue());
            } catch (Exception ex) {
                try {
                    fetch(e.getKey(), target, e.getValue());
                } catch (Exception ex2) {
                    System.err.println("was not able to fetch " + e.getKey());
                }
            }
        }

        Path configDir = target.resolve("config");
        if (! Files.isDirectory(configDir)) {
            Files.createDirectories(configDir);
        }
    }

    private static void fetchLocalM2(String path, Path target, String subpath) throws IOException {
        int i = path.lastIndexOf("/");
        String fName = path.substring(i);
        Path jarTarget = target.resolve(subpath + "/" + fName);

        if (!Files.isDirectory(jarTarget.getParent())) {
            Files.createDirectories(jarTarget.getParent());
        }

        Path jarSource = LOCAL_M2.resolve(path);
        System.out.println("about to fetch from local m2: " + path + " to " + jarTarget.toAbsolutePath());
        if (! Files.isRegularFile(jarSource)) {
            System.out.println("couldn't find in local m2: " + jarSource);
            return;
        }
        Files.copy(jarSource, jarTarget, StandardCopyOption.REPLACE_EXISTING);

    }

    private static void fetch(String url, Path target, String subpath) throws Exception {
        int i = url.lastIndexOf("/");
        String fName = url.substring(i);
        Path jarTarget = target.resolve(subpath + "/" + fName);
        System.out.println("about to fetch " + url + " to " + jarTarget.toAbsolutePath());
        if (!Files.isDirectory(jarTarget.getParent())) {
            Files.createDirectories(jarTarget.getParent());
        }

        try (InputStream is = new URL(url).openStream()) {
            Files.copy(is, jarTarget, StandardCopyOption.REPLACE_EXISTING);
        }
    }

}
