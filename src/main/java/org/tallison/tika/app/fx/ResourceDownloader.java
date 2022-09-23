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
package org.tallison.tika.app.fx;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ResourceDownloader {

    private static Logger LOGGER = LogManager.getLogger(ResourceDownloader.class);

    private static final String TIKA_DLCDN_BASE = "https://dlcdn.apache.org/tika/";

    private static final String ASF_ARCHIVE_BASE = "https://archive.apache.org/dist/tika/";//;"https://archive.apache.org/dist/tika/2.4.1/"

    public static void downloadTikaAppJar(String tikaVersion, Path tikaBin) throws IOException {

        if (!Files.isDirectory(tikaBin)) {
            try {
                Files.createDirectories(tikaBin);
            } catch (IOException e) {
                LOGGER.warn("couldn't create directory {}" + tikaBin);
                throw e;
            }
        }
        //TODO -- delete the existing tika-app jar
        String tikaAppJar = "tika-app-" + tikaVersion + ".jar";
        String url = TIKA_DLCDN_BASE + tikaVersion + "/" + tikaAppJar;
        Path target = tikaBin.resolve(tikaAppJar);
        LOGGER.info("About to download tika from " + url + " to " + target);
        try (InputStream is =
                     new URL(url).openStream()) {
            Files.copy(is, tikaBin.resolve(tikaAppJar), StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("successfully downloaded tika from " + url + " to " + target);
            return;
        } catch (MalformedURLException e) {
            LOGGER.error("bad url " + url, e);
        } catch (IOException e) {
            LOGGER.warn("Couldn't download the app from the dlcdn, going to try the archives: " +
                    url);
        }

        url = ASF_ARCHIVE_BASE + tikaVersion + "/" + tikaAppJar;
        try (InputStream is =
                     new URL(url).openStream()) {
            Files.copy(is, tikaBin.resolve(tikaAppJar), StandardCopyOption.REPLACE_EXISTING);
        } catch (MalformedURLException e) {
            LOGGER.error("bad url " + url, e);
        } catch (IOException e) {
            LOGGER.warn("Couldn't download the app from the Apache archive " +
                    url);
        }
    }
}
