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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.tallison.tika.gui.tools.deprecated.SnapshotParser;
import org.tallison.tika.gui.tools.deprecated.SnapshotResult;

/**
 * this should have been a batch script.
 *
 * At some point, I'll figure out how to assemble this with a
 * maven plugin.  Today is not the day for that.
 */
public class DownloadPluginUpdater {
    private static final String M2_URL_BASE = "https://repo1.maven.org/maven2/";

    private static final String APACHE_SNAPSHOTS_BASE =
            "https://repository.apache.org/content/groups/snapshots/";

    private static final String FOOJAY_ZULU_URL =
            "https://api.foojay.io/disco/v3.0/packages?package_type=jre&latest=available&version" +
                    "=17&javafx_bundled=true&distro=zulu";

    private static final Set<String> OS_ARCH_PKG_ZULU = Set.of(
            "macosx_aarch64.zip",
            "macosx_x64.zip",
            "win_x64.zip",
            "linux_x64.tar.gz"
    );

    private static final String DEPENDENCIES_TEMPLATE_START = """
                      <plugin>
                        <groupId>com.googlecode.maven-download-plugin</groupId>
                        <artifactId>download-maven-plugin</artifactId>
                        <executions>""";
    private static final String DEPENDENCIES_CONFIGURATION =
                        "\n    <execution>\n" +
                        "      <id>{ID}</id>\n" +
                        "      <phase>prepare-package</phase>\n" +
                        "      <goals>\n" +
                        "        <goal>wget</goal>\n" +
                        "      </goals>\n" +
                        "      <configuration>\n" +
                        "        <url>{URL}</url>\n" +
                        "        <unpack>false</unpack>\n" +
                        "        <outputDirectory>${project.build" +
                                ".directory}/lib/{SUB_DIR}</outputDirectory>\n" +
                        "        <md5>{MD5}</md5>\n" +
                        "      </configuration>\n" +
                        "    </execution>";

    private static final String JRE_CONFIGURATION =
            "\n    <execution>\n" +
                    "      <id>{ID}</id>\n" +
                    "      <phase>prepare-package</phase>\n" +
                    "      <goals>\n" +
                    "        <goal>wget</goal>\n" +
                    "      </goals>\n" +
                    "      <configuration>\n" +
                    "        <url>{URL}</url>\n" +
                    "        <unpack>false</unpack>\n" +
                    "        <outputDirectory>${project.build.directory}/jres/{SUB_DIR}</outputDirectory>\n" +
                    "        <sha256>{SHA256}</sha256>\n" +
                    "      </configuration>\n" +
                    "    </execution>";
    private static final String DEPENDENCIES_END = "\n    </executions>\n  </plugin>";


    public static void main(String[] args) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(DEPENDENCIES_TEMPLATE_START);
        getDependencies("/snapshot-dependencies.properties", sb);
        getZulu(sb);
        sb.append(DEPENDENCIES_END);
        System.out.println(sb);
    }

    private static void getDependencies(String dependenciesProps, StringBuilder sb) throws Exception {
        Properties properties = new Properties();
        properties.load(DownloadPluginUpdater.class.getResourceAsStream(
                dependenciesProps));
        sb.append(DEPENDENCIES_TEMPLATE_START);
        for (Object path : properties.keySet()) {
            Object subdir = properties.get(path);
            String p = (String) path;
            String url = M2_URL_BASE + path;
            String md5 = "";
            if (! p.endsWith(".jar")) {
                SnapshotResult result = new SnapshotParser().parse(APACHE_SNAPSHOTS_BASE + path);
                url = result.getUrl();
                md5 = result.getDigests().get("md5");
            } else {
                md5 = getMD5(url);
            }
            String id = FilenameUtils.getName(url).replace(".jar", "");

            String t = DEPENDENCIES_CONFIGURATION;
            t = t.replace("{ID}", id);
            t = t.replace("{MD5}", md5);
            t = t.replace("{URL}", url);
            t = t.replace("{SUB_DIR}", (String)subdir);
            sb.append(t);
        }
    }

    private static String getMD5(String url) throws URISyntaxException, IOException {
        return IOUtils.toString(new URI(url + ".md5"), StandardCharsets.UTF_8);
    }

    private static void getZulu(StringBuilder sb) throws Exception {
        JsonNode root = null;
        try (InputStream is =
                     new URL(FOOJAY_ZULU_URL).openStream()) {
            ObjectMapper mapper = new ObjectMapper();
            root = mapper.readTree(is);
        }

        Matcher pkgMatcher =
                Pattern.compile("\\A.*?-(([^-]+)(?:\\....|\\.tar.gz))\\Z").matcher(
                        "");
        for (JsonNode result : root.get("result")) {
            String filename = result.get("filename").asText();
            String osArchPgk = "";
            String osArch = "";
            if (pkgMatcher.reset(filename).find()) {
                osArchPgk = pkgMatcher.group(1);
                osArch = pkgMatcher.group(2);
            }
            UrlShaPair p = getUrlShaPair(result.get("links").get("pkg_info_uri").asText());
            System.err.println("available jre: " + osArchPgk + " : " + p);
            if (OS_ARCH_PKG_ZULU.contains(osArchPgk)) {
                String t = JRE_CONFIGURATION;
                t = t.replace("{ID}", osArch);
                t = t.replace("{SHA256}", p.sha256);
                t = t.replace("{URL}", p.url);
                t = t.replace("{SUB_DIR}", osArch);
                sb.append(t);
            }
        }
    }

    private static UrlShaPair getUrlShaPair(String pkgUrl) throws Exception {
        JsonNode root = null;
        try (InputStream is =
                     new URL(pkgUrl).openStream()) {
            ObjectMapper mapper = new ObjectMapper();
            root = mapper.readTree(is);
        }
        for (JsonNode result : root.get("result")) {
            String url = result.get("direct_download_uri").asText();
            String sha = result.get("checksum").asText();
            return new UrlShaPair(url, sha);
        }
        return null;
    }

    private static class UrlShaPair {
        private final String url;
        private final String sha256;

        public UrlShaPair(String url, String sha256) {
            this.url = url;
            this.sha256 = sha256;
        }

        @Override
        public String toString() {
            return "UrlShaPair{" + "url='" + url + '\'' + ", sha256='" + sha256 + '\'' + '}';
        }
    }
}
