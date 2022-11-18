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

import static org.tallison.tika.app.fx.Constants.BASE_PATH;
import static org.tallison.tika.app.fx.Constants.CSV_EMITTER_CLASS;
import static org.tallison.tika.app.fx.Constants.CSV_JDBC_CONNECTION_STRING;
import static org.tallison.tika.app.fx.Constants.CSV_JDBC_INSERT_SQL;
import static org.tallison.tika.app.fx.Constants.JDBC_CONNECTION_STRING;
import static org.tallison.tika.app.fx.Constants.JDBC_EMITTER_CLASS;
import static org.tallison.tika.app.fx.Constants.JDBC_INSERT_SQL;
import static org.tallison.tika.app.fx.Constants.NO_DIGEST;
import static org.tallison.tika.app.fx.Constants.OPEN_SEARCH_PW;
import static org.tallison.tika.app.fx.Constants.OPEN_SEARCH_UPDATE_STRATEGY;
import static org.tallison.tika.app.fx.Constants.OPEN_SEARCH_URL;
import static org.tallison.tika.app.fx.Constants.OPEN_SEARCH_USER;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tallison.tika.app.fx.Constants;
import org.tallison.tika.app.fx.ctx.AppContext;
import org.tallison.tika.app.fx.metadata.MetadataTuple;

import org.apache.tika.utils.ProcessUtils;
import org.apache.tika.utils.StringUtils;

/**
 * This is an embarrassment of hardcoding.  Need to figure out better
 * solution...
 * <p>
 * This also requires knowledge of all fetchers/emitters in one class. This is, erm,
 * less than entirely ideal.
 * <p>
 * This is also does not escape xml characters.  So, bad, very, very bad.
 */
public class TikaConfigWriter {
    private static Logger LOGGER = LogManager.getLogger(TikaConfigWriter.class);

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
        appendAutoDetectParserConfig(batchProcessConfig, sb);
        sb.append("</properties>");
        Files.write(tmp, sb.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);
        return tmp;
    }

    private void appendAutoDetectParserConfig(BatchProcessConfig batchProcessConfig,
                                              StringBuilder sb) throws IOException {
        //good enough for now.  we'll have to figure out
        //a better option if we add more params
        if (batchProcessConfig.getDigest().isEmpty()) {
            return;
        }
        String digestString = batchProcessConfig.getDigest().get();
        if (digestString.equals(NO_DIGEST)) {
            return;
        }
        String async = getTemplate("autoDetectParserConfig.xml");

        async = async.replace("{DIGEST_STRING}", digestString);
        sb.append(async).append("\n");
    }

    private void appendAsync(BatchProcessConfig bpc, StringBuilder sb) throws IOException {
        String async = getTemplate("async.xml");
        async = async.replace("{JAVA_PATH}",
                AppContext.getInstance().getJavaHome().resolve("java").toString());
        async = async.replace("{NUM_CLIENTS}", Integer.toString(bpc.getNumProcesses()));
        async = async.replace("{XMX}", "-Xmx" + bpc.getMaxMemMb() + "m");
        async = async.replace("{ASYNC_LOG}",
                AppContext.ASYNC_LOG4J2_PATH.toAbsolutePath().toString());
        async = async.replace("{TIMEOUT_MS}", Long.toString(bpc.getParseTimeoutSeconds() * 1000));
        async = async.replace("{STATUS_FILE}",
                AppContext.BATCH_STATUS_PATH.toAbsolutePath().toString());
        async = async.replace("{EMIT_WITHIN_MS}", Long.toString(bpc.getEmitWithinMs()));
        async = async.replace("{TOTAL_EMIT_THRESHOLD}",
                Long.toString((long) bpc.getTotalEmitThesholdMb()));
        async = async.replace("{PER_FILE_EMIT_THRESHOLD}",
                Long.toString((long) bpc.getPerFileEmitThresholdMb()));

        async = async.replace("{CLASS_PATH}", buildClassPath(bpc));
        async = addReporters(bpc, async);
        sb.append(async).append("\n");
    }

    private String addReporters(BatchProcessConfig bpc, String async) throws IOException {
        //TODO -- add opensearch
        if (bpc.getEmitter().isEmpty() ||
                (!bpc.getEmitter().get().getClazz().equals(JDBC_EMITTER_CLASS) &&
                        !bpc.getEmitter().get().getClazz().equals(CSV_EMITTER_CLASS))) {
            return async.replace("{JDBC_PIPES_REPORTER}", "");
        }
        String jdbcPipesReporter = getTemplate("jdbc-pipes-reporter.xml");
        ConfigItem emitter = bpc.getEmitter().get();
        String connectionString = "";
        if (emitter.getClazz().equals(JDBC_EMITTER_CLASS)) {
            connectionString = emitter.getAttributes().get(JDBC_CONNECTION_STRING);
        } else if (emitter.getClazz().equals(CSV_EMITTER_CLASS)) {
            connectionString = emitter.getAttributes().get(CSV_JDBC_CONNECTION_STRING);
        }
        jdbcPipesReporter = jdbcPipesReporter.replace("{CONNECTION_STRING}", connectionString);
        return async.replace("{JDBC_PIPES_REPORTER}", jdbcPipesReporter);
    }

    private String buildClassPath(BatchProcessConfig batchProcessConfig) {
        StringBuilder sb = new StringBuilder();
        //load these mappings from a properties file or something
        sb.append(ProcessUtils.escapeCommandLine(
                AppContext.TIKA_APP_BIN_PATH.toAbsolutePath() + "/*"));
        sb.append(File.pathSeparator);
        sb.append(ProcessUtils.escapeCommandLine(
                AppContext.TIKA_EXTRAS_BIN_PATH.toAbsolutePath() + "/*"));
        sb.append(File.pathSeparator);
        batchProcessConfig.appendPipesClasspath(sb);
        return sb.toString();
    }

    private void appendEmitter(BatchProcessConfig batchProcessConfig, StringBuilder sb)
            throws IOException {
        Optional<ConfigItem> optionalEmitter = batchProcessConfig.getEmitter();
        if (optionalEmitter.isEmpty()) {
            LOGGER.warn("emitter is empty?!");
            return;
        }
        ConfigItem emitter = optionalEmitter.get();
        switch (emitter.getClazz()) {
            case Constants.FS_EMITTER_CLASS:
                appendFSEmitter(emitter, sb);
                break;
            case Constants.OPEN_SEARCH_EMITTER_CLASS:
                appendOpenSearchEmitter(emitter, sb);
                break;
            case Constants.CSV_EMITTER_CLASS:
                appendJDBCEmitter(emitter,
                        emitter.getAttributes().get(CSV_JDBC_CONNECTION_STRING),
                        emitter.getAttributes().get(CSV_JDBC_INSERT_SQL),
                        sb);
                break;
            case Constants.JDBC_EMITTER_CLASS:
                appendJDBCEmitter(emitter,
                        emitter.getAttributes().get(JDBC_CONNECTION_STRING),
                        emitter.getAttributes().get(JDBC_INSERT_SQL),
                        sb);
                break;
            default:
                throw new RuntimeException("I regret I don't yet support " +
                        batchProcessConfig.getEmitter().get().getClazz());
        }
    }


    private void appendJDBCEmitter(ConfigItem emitter, String connectionString,
                                   String insertString, StringBuilder sb) throws IOException {
        String template = getTemplate("jdbc-pipes-emitter.xml");
        //TODO -- a lot better than this.  LOL...
        connectionString = connectionString.replaceAll("&", "&amp;");
        template = template.replace("{CONNECTION_STRING}", connectionString);
        //for now we assume the table was created via the dialog
        template = template.replace("{CREATE_TABLE_SQL}", StringUtils.EMPTY);

        template = template.replace("{INSERT_SQL}", insertString);

        StringBuilder columns = new StringBuilder();
        //assume these exist
        for (MetadataTuple t : emitter.getMetadataTuples().get()) {
            columns.append("<key k=\"").append(t.getOutput()).append("\"");
            columns.append(" v=\"").append(t.getProperty()).append("\"/>");
        }
        template = template.replace("{COLUMNS_AND_TYPES}", columns.toString());

        sb.append(template);
    }

    private void appendOpenSearchEmitter(ConfigItem emitter, StringBuilder sb) throws IOException {
        String template = getTemplate("opensearch-pipes-emitter.xml");

        String userName = emitter.getAttributes().get(OPEN_SEARCH_USER);
        String password = emitter.getAttributes().get(OPEN_SEARCH_PW);
        if (StringUtils.isBlank(userName) && StringUtils.isBlank(password)) {
            template = template.replace("{USER_NAME}", "");
            template = template.replace("{PASSWORD}", "");
        } else {
            template = template.replace("{USER_NAME}", "<userName>" + userName + "</userName>");
            template = template.replace("{PASSWORD}", "<password>" + password + "</password>");
        }

        template =
                template.replace("{OPENSEARCH_URL}", emitter.getAttributes().get(OPEN_SEARCH_URL));
        template = template.replace("{UPDATE_STRATEGY}",
                emitter.getAttributes().get(OPEN_SEARCH_UPDATE_STRATEGY));
        sb.append(template);
    }

    private void appendFSEmitter(ConfigItem fetcher, StringBuilder sb) throws IOException {
        String template = getTemplate("fs-pipes-emitter.xml");
        template = template.replace("{BASE_PATH}", fetcher.getAttributes().get(BASE_PATH));
        sb.append(template).append("\n");
    }


    private void appendFetcher(BatchProcessConfig batchProcessConfig, StringBuilder sb)
            throws IOException {
        Optional<ConfigItem> optionalFetcher = batchProcessConfig.getFetcher();
        if (optionalFetcher.isEmpty()) {
            LOGGER.warn("fetcher is empty?!");
            return;
        }
        ConfigItem fetcher = optionalFetcher.get();
        switch (fetcher.getClazz()) {
            case "org.apache.tika.pipes.fetcher.fs.FileSystemFetcher":
                appendFSFetcher(fetcher, sb);
                break;
            default:
                throw new RuntimeException("I regret I don't yet support " + fetcher.getClazz());
        }
    }

    private void appendFSFetcher(ConfigItem fetcher, StringBuilder sb) throws IOException {
        String template = getTemplate("fs-pipes-fetcher.xml");
        template = template.replace("{BASE_PATH}", fetcher.getAttributes().get(BASE_PATH));
        sb.append(template).append("\n");
    }

    private void appendPipesIterator(BatchProcessConfig batchProcessConfig, StringBuilder sb)
            throws IOException {
        Optional<ConfigItem> optionalPipesIterator = batchProcessConfig.getPipesIterator();
        if (optionalPipesIterator.isEmpty()) {
            LOGGER.warn("pipesIterator is empty?!");
            return;
        }
        ConfigItem pipesIterator = optionalPipesIterator.get();
        switch (pipesIterator.getClazz()) {
            case "org.apache.tika.pipes.pipesiterator.fs.FileSystemPipesIterator":
                appendFSPipesIterator(pipesIterator, sb);
                break;
            default:
                throw new RuntimeException(
                        "I regret I don't yet support " + pipesIterator.getClazz());
        }
    }

    private void appendFSPipesIterator(ConfigItem pipesIterator, StringBuilder sb)
            throws IOException {
        String template = getTemplate("fs-pipes-iterator.xml");
        template = template.replace("{BASE_PATH}", pipesIterator.getAttributes().get(BASE_PATH));
        sb.append(template).append("\n");
    }

    private void appendMetadataFilter(BatchProcessConfig batchProcessConfig,
                                      StringBuilder tikaConfigBuilder) throws IOException {

        StringBuilder sb = new StringBuilder();
        String template = getTemplate("metadata-filters.xml");
        Optional<ConfigItem> configItem = batchProcessConfig.getEmitter();
        if (configItem.isEmpty()) {
            LOGGER.warn("emitter is empty?!");
            return;
        }
        ConfigItem emitter = configItem.get();
        Optional<List<MetadataTuple>> metadataTuples = emitter.getMetadataTuples();
        if (metadataTuples.isEmpty() || metadataTuples.get().size() == 0) {
            //add templated metadata filters
            template = template.replace("{MAPPING_FILTER}", "");
            tikaConfigBuilder.append(template).append("\n");
            return;
        }

        sb.append("<metadataFilter " +
                "class=\"org.apache.tika.metadata.filter.FieldNameMappingFilter\">");
        sb.append("  <params>\n");
        sb.append("    <excludeUnmapped>true</excludeUnmapped>\n");
        sb.append("    <mappings>\n");

        metadataTuples.get().stream().forEach(e -> sb.append(
                "      <mapping from=\"" + e.getTika() + "\" to=\"" + e.getOutput() + "\"/>\n"));

        sb.append("    </mappings>");
        sb.append("  </params>");
        sb.append("</metadataFilter>\n");

        template = template.replace("{MAPPING_FILTER}", sb.toString());
        tikaConfigBuilder.append(template).append("\n");
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
