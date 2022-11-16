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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

public class ZuluDownloader {

    private static final String ZULU_URL =
            "https://www.azul.com/downloads/?version=java-17-lts&package=jre-fx";

    private static final Set<String> OS_ARCH_PKG_SET = Set.of(
            "linux_x64.tar.gz",
//            "linux_i686.tar.gz",
            "win_x64.zip",
//            "win_i686.zip",
            "macosx_x64.zip",
            "macosx_aarch64.zip"
            );

    private static final String DEPENDENCIES_TEMPLATE_START = """
                      <plugin>
                        <groupId>com.googlecode.maven-download-plugin</groupId>
                        <artifactId>download-maven-plugin</artifactId>
                        <executions>""";
    private static final String DEPENDENCIES_CONFIGURATION =
            "<execution>\n" +
                    "      <id>{ID}</id>\n" +
                    "      <phase>prepare-package</phase>\n" +
                    "      <goals>\n" +
                    "        <goal>wget</goal>\n" +
                    "      </goals>\n" +
                    "      <configuration>\n" +
                    "        <url>{URL}</url>\n" +
                    "        <unpack>true</unpack>\n" +
                    "        <outputDirectory>${project.build.directory}/jres/{SUB_DIR}</outputDirectory>\n" +
                    "        <sha256>{SHA_256}</sha256>\n" +
                    "      </configuration>\n" +
                    "    </execution>";
    private static final String DEPENDENCIES_END = "\n    </executions>\n  </plugin>";

    /**
     * Until we can figure out the zulu download api, we download the html source from the url
     * above manually, and then run this against the index.html that we download.
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        Path index = Paths.get(args[0]);
        String html = IOUtils.toString(Files.newInputStream(index), StandardCharsets.UTF_8);
        List<DownloadTuple> downloadTuples = parse(html);
        for (DownloadTuple t : downloadTuples) {
            String tmp = DEPENDENCIES_CONFIGURATION;
            tmp = tmp.replace("{URL}", t.url);
            tmp = tmp.replace("{SHA_256}", t.sha256);
            String osArch = t.osArchPkg.replace(".tar.gz", "").replace(".zip", "");
            tmp = tmp.replace("{ID}", osArch);
            tmp = tmp.replace("{SUB_DIR}", osArch);
            System.out.println(tmp);
        }
    }

    protected static List<DownloadTuple> parse(String html) {
        Pattern SHA_CLASS_PATTERN = Pattern.compile("class=\"js-downloads-trigger-clipboard\"");
        Pattern SHA_PATTERN = Pattern.compile("data-name=\"([^\"]+)\"");

        Pattern URL_DOWNLOAD_PATTERN = Pattern.compile("class=\"c-downloads__package-download-button\"");
        Pattern HREF = Pattern.compile("href=\"([^\"]+)");
        Matcher MAIN_MATCHER = Pattern.compile("(?:<span ([^>]+)>)|<a ([^>]+)>").matcher(html);
        Pattern OS_ARCH_PKG = Pattern.compile("-([^-]+)\\Z");
        String sha = "";
        String url = "";
        List<DownloadTuple> downloadTuples = new ArrayList<>();
        while (MAIN_MATCHER.find()) {
            if (MAIN_MATCHER.group(1) != null) {
                String attrs = MAIN_MATCHER.group(1);
                Matcher m = SHA_CLASS_PATTERN.matcher(attrs);
                if (m.find()) {
                    Matcher shaMatcher = SHA_PATTERN.matcher(attrs);
                    if (shaMatcher.find()) {
                        sha = shaMatcher.group(1);
                    }
                }
            } else {
                String aAttrs = MAIN_MATCHER.group(2);
                Matcher m = URL_DOWNLOAD_PATTERN.matcher(aAttrs);
                if (m.find()) {
                    Matcher hrefMatcher = HREF.matcher(aAttrs);
                    if (hrefMatcher.find()) {
                        String u = hrefMatcher.group(1);
                        Matcher os = OS_ARCH_PKG.matcher(u);
                        if (os.find()) {
                            String osArchPkg = os.group(1);
                            System.out.println("\"" + osArchPkg + "\",");// + " :: " + sha + " " +
                            if (OS_ARCH_PKG_SET.contains(osArchPkg)) {
                                downloadTuples.add(new DownloadTuple(osArchPkg, sha, u));
                            }
                            // u);
                        }
                        sha = "";
                    }
                }
            }
        }
        return downloadTuples;
    }

    public static class DownloadTuple {
        String osArchPkg;
        private String sha256;
        private String url;

        public DownloadTuple(String osArchPkg, String sha256, String url) {
            this.osArchPkg = osArchPkg;
            this.sha256 = sha256;
            this.url = url;
        }

        @Override
        public String toString() {
            return "DownloadTuple{" + "osArchPkg='" + osArchPkg + '\'' + ", sha256='" + sha256 +
                    '\'' + ", url='" + url + '\'' + '}';
        }
    }

}
