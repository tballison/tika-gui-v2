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
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

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
    private static Set<String> OS_ARCHITECTURE = Set.of(
            "linux x64",
            "mac aarch64",
            "mac x64",
            "windows x64"
            //"windows x32"
    );

    private static Map<String, String> ADOPTIUM_TO_MAVEN = Map.of(
            "linux x64", "unix amd64",
            "windows x64", "windows amd64",
            "mac x64", "mac x64");

    private static String JRE_TEMPLATE = """
                <profile>
                  <id>{ID}</id>
                  <activation>
                    <os>
                      <family>{FAMILY}</family>
                      <arch>{ARCHITECTURE}</arch>
                    </os>
                  </activation>
                  <build>
                    <plugins>
                      <plugin>
                        <groupId>com.googlecode.maven-download-plugin</groupId>
                        <artifactId>download-maven-plugin</artifactId>
                        <executions>
                          <execution>
                            <id>install-{FAMILY}-{ARCHITECTURE}</id>
                            <phase>prepare-package</phase>
                            <goals>
                              <goal>wget</goal>
                            </goals>
                            <configuration>
                              <url>{URL}</url>
                              <unpack>true</unpack>
                              <outputDirectory>${project.build.directory}/jre</outputDirectory>
                              <sha256>{SHA_256}</sha256>
                            </configuration>
                          </execution>
                        </executions>
                      </plugin>
                    </plugins>
                  </build>
                </profile>""";

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
    private static final String DEPENDENCIES_END = "\n    </executions>\n  </plugin>";


    public static void main(String[] args) throws Exception {
        getJRES();
        getDependencies("/snapshot-dependencies.properties");
    }

    private static void getDependencies(String dependenciesProps) throws Exception {
        Properties properties = new Properties();
        properties.load(DownloadPluginUpdater.class.getResourceAsStream(
                dependenciesProps));
        StringBuilder sb = new StringBuilder();
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

            System.out.println(md5 + " " + subdir + " " + url);
            String t = DEPENDENCIES_CONFIGURATION;
            t = t.replace("{ID}", id);
            t = t.replace("{MD5}", md5);
            t = t.replace("{URL}", url);
            t = t.replace("{SUB_DIR}", (String)subdir);
            sb.append(t);
        }
        sb.append(DEPENDENCIES_END);
        System.out.println(sb.toString());

    }

    private static String getMD5(String url) throws URISyntaxException, IOException {
        return IOUtils.toString(new URI(url + ".md5"), StandardCharsets.UTF_8);
    }

    private static void getJRES() throws Exception {
        JsonNode root = null;
        try (InputStream is =
                     new URL("https://api.adoptium.net/v3/assets/latest/17/hotspot").openStream()) {
            ObjectMapper mapper = new ObjectMapper();
            root = mapper.readTree(is);
        }
        for (JsonNode item : root) {
            JsonNode binary = item.get("binary");
            if (! binary.get("image_type").asText().equals("jre")) {
                continue;
            }
            if (! binary.has("package")) {
                continue;
            }
            String os = binary.get("os").asText();
            String architecture = binary.get("architecture").asText();
            if (! OS_ARCHITECTURE.contains(os + " " + architecture)) {
                continue;
            }
            JsonNode pkg = binary.get("package");
            String link = pkg.get("link").asText();
            String checksum = pkg.get("checksum").asText();
            Long sz = pkg.get("size").asLong();
            write(os, architecture, link, checksum);
        }
    }

    private static void write(String os, String architecture, String link, String checksum) {
        String mapped = ADOPTIUM_TO_MAVEN.getOrDefault(os + " " + architecture, os + " " + architecture);
        String[] bits = mapped.split(" ");
        String mappedOs = bits[0];
        String mappedArchitecture = bits[1];

        String id = mappedOs + "-" + mappedArchitecture;
        String template = JRE_TEMPLATE;
        template = template.replace("{ID}", id);
        template = template.replace("{FAMILY}", mappedOs);
        template = template.replace("{ARCHITECTURE}", mappedArchitecture);
        template = template.replace("{URL}", link);
        template = template.replace("{SHA_256}", checksum);
        System.out.println(template);
    }

}
