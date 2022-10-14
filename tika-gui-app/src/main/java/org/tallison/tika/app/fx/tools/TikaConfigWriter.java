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
package org.tallison.tika.app.fx.tools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.tallison.tika.app.fx.Constants;
import org.tallison.tika.app.fx.ctx.AppContext;

import org.apache.tika.utils.ProcessUtils;
import org.apache.tika.utils.StringUtils;

/**
 * This is an embarrassment of hardcoding.  Need to figure out better
 * solution...
 *
 * This is also does not escape xml characters.  So, bad, very, very bad.
 */
public class TikaConfigWriter {

    public void writeLog4j2() throws IOException {
        String template = getTemplateLog4j2("log4j2-async.xml");
        template =
                template.replace("{LOGS_PATH}", AppContext.LOGS_PATH.toAbsolutePath().toString());

        if (!Files.isDirectory(AppContext.ASYNC_LOG4J2_PATH.getParent())) {
            Files.createDirectories(AppContext.ASYNC_LOG4J2_PATH.getParent());
        }
        Files.write(AppContext.ASYNC_LOG4J2_PATH, template.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE);
    }

    public Path writeConfig(BatchProcessConfig batchProcessConfig) throws IOException {
        if (!Files.isDirectory(AppContext.CONFIG_PATH)) {
            Files.createDirectories(AppContext.CONFIG_PATH);
        }
        Path tmp = Files.createTempFile(AppContext.CONFIG_PATH, "tika-config-", ".xml");
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n");
        sb.append("<properties>\n");
        sb.append(getTemplate("parsers.xml")).append("\n");
        appendMetadataFilter(batchProcessConfig, sb);
        appendPipesIterator(batchProcessConfig, sb);
        appendFetcher(batchProcessConfig, sb);
        appendEmitter(batchProcessConfig, sb);
        appendAsync(batchProcessConfig, sb);
        sb.append("</properties>");
        Files.write(tmp, sb.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);
        return tmp;
    }

    private void appendAsync(BatchProcessConfig batchProcessConfig, StringBuilder sb)
            throws IOException {
        String async = getTemplate("async.xml");
        //TODO: fix this
        async = async.replace("{NUM_CLIENTS}", "2");
        async = async.replace("{XMX}", "-Xmx1g");
        async = async.replace("{ASYNC_LOG}",
                AppContext.ASYNC_LOG4J2_PATH.toAbsolutePath().toString());
        async = async.replace("{TIMEOUT_MS}", "60000");
        async = async.replace("{STATUS_FILE}",
                AppContext.BATCH_STATUS_PATH.toAbsolutePath().toString());
        async = async.replace("{CLASS_PATH}", buildClassPath(batchProcessConfig));
        sb.append(async).append("\n");
    }

    private String buildClassPath(BatchProcessConfig batchProcessConfig) {
        StringBuilder sb = new StringBuilder();
        //load these mappings from a properties file or something
        sb.append(ProcessUtils.escapeCommandLine(
                AppContext.TIKA_APP_BIN_PATH.toAbsolutePath() + "/*"));
        sb.append(File.pathSeparator);
        batchProcessConfig.appendPipesClasspath(sb);
        //TODO add s3 and jdbc
        return sb.toString();
    }

    private void appendEmitter(BatchProcessConfig batchProcessConfig, StringBuilder sb)
            throws IOException {
        switch (batchProcessConfig.getEmitter().getClazz()) {
            case Constants.FS_EMITTER_CLASS:
                appendFSEmitter(batchProcessConfig.getEmitter(), sb);
                break;
            case Constants.OPEN_SEARCH_EMITTER_CLASS:
                appendOpenSearchEmitter(batchProcessConfig.getEmitter(), sb);
                break;
            default:
                throw new RuntimeException("I regret I don't yet support " +
                        batchProcessConfig.getEmitter().getClazz());
        }
    }

    private void appendOpenSearchEmitter(ConfigItem emitter, StringBuilder sb) throws IOException {
        String template = getTemplate("opensearch-pipes-emitter.xml");

        String userName = emitter.getAttributes().get("userName");
        String password = emitter.getAttributes().get("password");
        if (StringUtils.isBlank(userName) && StringUtils.isBlank(password)) {
            template = template.replace("{USER_NAME}", "");
            template = template.replace("{PASSWORD}", "");
        } else {
            template = template.replace("{USER_NAME}",
                    "<userName>" + userName + "</userName>");
            template = template.replace("{PASSWORD}",
                    "<password>" + password + "</password>");
        }

        template = template.replace("{OPENSEARCH_URL}",
                emitter.getAttributes().get("openSearchUrl"));
        template = template.replace("{UPDATE_STRATEGY}",
                emitter.getAttributes().get("updateStrategy"));
        sb.append(template);
    }

    private void appendFSEmitter(ConfigItem fetcher, StringBuilder sb) throws IOException {
        String template = getTemplate("fs-pipes-emitter.xml");
        template = template.replace("{BASE_PATH}", fetcher.getAttributes().get("basePath"));
        sb.append(template).append("\n");
    }


    private void appendFetcher(BatchProcessConfig batchProcessConfig, StringBuilder sb)
            throws IOException {
        switch (batchProcessConfig.getFetcher().getClazz()) {
            case "org.apache.tika.pipes.fetcher.fs.FileSystemFetcher":
                appendFSFetcher(batchProcessConfig.getFetcher(), sb);
                break;
            default:
                throw new RuntimeException("I regret I don't yet support " +
                        batchProcessConfig.getFetcher().getClazz());
        }
    }

    private void appendFSFetcher(ConfigItem fetcher, StringBuilder sb) throws IOException {
        String template = getTemplate("fs-pipes-fetcher.xml");
        template = template.replace("{BASE_PATH}", fetcher.getAttributes().get("basePath"));
        sb.append(template).append("\n");
    }

    private void appendPipesIterator(BatchProcessConfig batchProcessConfig, StringBuilder sb)
            throws IOException {
        switch (batchProcessConfig.getPipesIterator().getClazz()) {
            case "org.apache.tika.pipes.pipesiterator.fs.FileSystemPipesIterator":
                appendFSPipesIterator(batchProcessConfig.getPipesIterator(), sb);
                break;
            default:
                throw new RuntimeException("I regret I don't yet support " +
                        batchProcessConfig.getPipesIterator().getClazz());
        }
    }

    private void appendFSPipesIterator(ConfigItem pipesIterator, StringBuilder sb)
            throws IOException {
        String template = getTemplate("fs-pipes-iterator.xml");
        template = template.replace("{BASE_PATH}", pipesIterator.getAttributes().get("basePath"));
        sb.append(template).append("\n");
    }

    private void appendMetadataFilter(BatchProcessConfig batchProcessConfig,
                                      StringBuilder tikaConfigBuilder) throws IOException {


        StringBuilder sb = new StringBuilder();
        String template = getTemplate("metadata-filters.xml");
        Map<String, String> mappings = batchProcessConfig.getMetadataMapper().getAttributes();
        if (batchProcessConfig.getMetadataMapper() != null &&
                batchProcessConfig.getMetadataMapper().getAttributes().size() > 0) {
            sb.append("<metadataFilter " +
                    "class=\"org.apache.tika.metadata.filter.FieldNameMappingFilter\">");
            sb.append("  <params>\n");
            sb.append("    <excludeUnmapped>true</excludeUnmapped>\n");
            sb.append("    <mappings>\n");

            mappings.entrySet().stream().forEach(e -> sb.append(
                    "      <mapping from=\"" + e.getKey() + "\" to=\"" + e.getValue() + "\"/>"));

            sb.append("    </mappings>");
            sb.append("  </params>");
            sb.append("</metadataFilter>\n");
        }
        template = template.replace("{MAPPING_FILTER}", sb.toString());
        tikaConfigBuilder.append(template);
    }

    private String getTemplate(String template) throws IOException {
        try (InputStream is = this.getClass()
                .getResourceAsStream("/templates/config/" + template)) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        }
    }

    private String getTemplateLog4j2(String template) throws IOException {
        try (InputStream is = this.getClass()
                .getResourceAsStream("/templates/log4j2/" + template)) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        }
    }
}
