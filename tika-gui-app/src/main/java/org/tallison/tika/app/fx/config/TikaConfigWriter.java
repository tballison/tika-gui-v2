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
package org.tallison.tika.app.fx.config;

import static org.tallison.tika.app.fx.Constants.BASE_PATH;
import static org.tallison.tika.app.fx.Constants.EXTRACT_FILE_SYSTEM_METADATA;
import static org.tallison.tika.app.fx.Constants.FS_FETCHER_CLASS;
import static org.tallison.tika.app.fx.Constants.NO_DIGEST;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tallison.tika.app.fx.batch.BatchProcessConfig;
import org.tallison.tika.app.fx.ctx.AppContext;
import org.tallison.tika.app.fx.emitters.BaseEmitterSpec;
import org.tallison.tika.app.fx.emitters.CSVEmitterSpec;
import org.tallison.tika.app.fx.emitters.EmitterSpec;
import org.tallison.tika.app.fx.emitters.JDBCEmitterSpec;
import org.tallison.tika.app.fx.metadata.MetadataTuple;
import org.tallison.tika.app.fx.sax.DomWriter;
import org.tallison.tika.app.fx.sax.XMLStringToDOM;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import org.apache.tika.exception.TikaException;
import org.apache.tika.utils.ProcessUtils;

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
    private static final Logger LOGGER = LogManager.getLogger(TikaConfigWriter.class);

    public void writeLog4j2() throws IOException {
        String template = getTemplateLog4j2("log4j2-async.xml");
        template =
                template.replace("{LOGS_PATH}", AppContext.LOGS_PATH.toAbsolutePath().toString());

        if (!Files.isDirectory(AppContext.ASYNC_LOG4J2_PATH.getParent())) {
            Files.createDirectories(AppContext.ASYNC_LOG4J2_PATH.getParent());
        }
        Files.writeString(AppContext.ASYNC_LOG4J2_PATH, template, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE);

        //not actually a template
        String xml = getTemplateLog4j2("log4j2-async-cli.xml");
        Files.writeString(AppContext.CONFIG_PATH.resolve("log4j2-async-cli.xml"), xml,
                StandardCharsets.UTF_8, StandardOpenOption.CREATE);

    }

    public Path writeConfig(BatchProcessConfig batchProcessConfig) throws IOException {

        return writeConfig(batchProcessConfig, AppContext.CONFIG_PATH);
    }


    public Path writeConfig(BatchProcessConfig batchProcessConfig, Path workingDir)
            throws IOException {
        if (!Files.isDirectory(workingDir)) {
            Files.createDirectories(workingDir);
        }
        Path tmp = Files.createTempFile(workingDir, "tika-config-", ".xml");
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Document document = dbf.newDocumentBuilder().newDocument();
            DomWriter domWriter = new DomWriter(document);
            Element properties = document.createElement("properties");
            document.appendChild(properties);
            appendDetectors(batchProcessConfig, domWriter, properties);
            appendParsers(batchProcessConfig, domWriter, properties);
            //sb.append(getTemplate("parsers.xml")).append("\n");
            appendMetadataFilter(batchProcessConfig, domWriter, properties);
            appendPipesIterator(batchProcessConfig, domWriter, properties);
            appendFetcher(batchProcessConfig, domWriter, properties);
            appendEmitter(batchProcessConfig, domWriter, properties);
            appendAsync(batchProcessConfig, domWriter, properties);
            appendAutoDetectParserConfig(batchProcessConfig, domWriter, properties);

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                StreamResult result = new StreamResult(writer);
                DOMSource source = new DOMSource(document);
                transformer.transform(source, result);
            }
        } catch (XMLStreamException | TransformerException | ParserConfigurationException e) {
            throw new IOException(e);
        }
        LOGGER.debug("Finished writing tika-config: {}", tmp.toAbsolutePath());
        return tmp;
    }

    private void appendDetectors(BatchProcessConfig batchProcessConfig, DomWriter writer,
                               Element properties) throws XMLStreamException {
        if (batchProcessConfig.getDetectorConfig().isEmpty() ||
                batchProcessConfig.getDetectorConfig().get().getPath().isEmpty()) {
            //don't do anything, right(?)
        } else {
            //this is a total hack.
            try (InputStream is = Files.newInputStream(
                    batchProcessConfig.getDetectorConfig().get().getPath().get())) {
                XMLStringToDOM.write(writer, properties, is);
            } catch (TikaException | IOException | SAXException e) {
                LOGGER.warn("couldn't write dom");
            }
        }
    }

    private void appendParsers(BatchProcessConfig batchProcessConfig, DomWriter writer,
                               Element properties) throws XMLStreamException {
        if (batchProcessConfig.getParserConfig().isEmpty() ||
                batchProcessConfig.getParserConfig().get().getPath().isEmpty()) {
            //this is a total hack.
            try (InputStream is =
                         TikaConfigWriter.class.getResourceAsStream("/default_parsers.xml")) {
                XMLStringToDOM.write(writer, properties, is);
            } catch (TikaException | IOException | SAXException e) {
                LOGGER.warn("couldn't write dom");
            }
        } else {
            //this is a total hack.
            try (InputStream is =
                         Files.newInputStream(
                                 batchProcessConfig.getParserConfig().get().getPath().get())) {

                XMLStringToDOM.write(writer, properties, is);
            } catch (TikaException | IOException | SAXException e) {
                LOGGER.warn("couldn't write dom");
            }
        }
    }

    private void addLegacyParams(DomWriter writer, Element parent, String nodeName, String clz,
                                 String... tuples) {
        //total hack
        Element parser = writer.createAndGetElement(parent, nodeName, "class", clz);
        if (tuples.length == 0) {
            return;
        }
        Element params = writer.createAndGetElement(parser, "params");
        for (int i = 0; i < tuples.length; i += 3) {
            writer.appendTextElement(params, "param", tuples[i + 2], "name", tuples[i], "type",
                    tuples[i + 1]);
        }
    }

    private void excludeParsers(DomWriter writer, Element parser, String... classes) {
        for (String clz : classes) {
            writer.createAndGetElement(parser, "parser-exclude", "class", clz);
        }
    }

    private void appendAutoDetectParserConfig(BatchProcessConfig batchProcessConfig,
                                              DomWriter writer, Element properties)
            throws IOException {
        Element adpc = writer.createAndGetElement(properties, "autoDetectParserConfig");
        Element params = writer.createAndGetElement(adpc, "params");
        writer.appendTextElement(params, "spoolToDisk", "0");
        writer.appendTextElement(params, "outputThreshold", "10000");
        writer.appendTextElement(params, "maximumCompressionRatio", "100");
        writer.appendTextElement(params, "maximumDepth", "100");
        writer.appendTextElement(params, "maximumPackageEntryDepth", "100");

        //good enough for now.  we'll have to figure out
        //a better option if we add more params
        if (batchProcessConfig.getDigest().isEmpty()) {
            return;
        }
        String digestString = batchProcessConfig.getDigest().get();
        if (digestString.equals(NO_DIGEST)) {
            return;
        }
        Element digester = writer.createAndGetElement(adpc, "digesterFactory", "class",
                "org.apache.tika.parser.digestutils.CommonsDigesterFactory");
        Element digesterParams = writer.createAndGetElement(digester, "params");
        writer.appendTextElement(digesterParams, "markLimit", "1000000");
        writer.appendTextElement(digesterParams, "algorithmString", digestString);

    }

    private void appendAsync(BatchProcessConfig bpc, DomWriter writer, Element properties)
            throws IOException {
        Element async = writer.createAndGetElement(properties, "async");
        Element params = writer.createAndGetElement(async, "params");
        writer.appendTextElement(params, "javaPath",
                AppContext.getInstance().getJavaHome().resolve("java").toString());
        writer.appendTextElement(params, "numClients", Integer.toString(bpc.getNumProcesses()));
        writer.appendTextElement(params, "numEmitters", "1");
        writer.appendTextElement(params, "emitIntermediateResults", "true");
        writer.appendListElement(params, "forkedJvmArgs", "arg", "-Xmx" + bpc.getMaxMemMb() + "m",
                "-Dlog4j.configurationFile=" + AppContext.ASYNC_LOG4J2_PATH.toAbsolutePath(), "-cp",
                buildClassPath(bpc));
        writer.appendTextElement(params, "timeoutMillis",
                Long.toString(bpc.getParseTimeoutSeconds() * 1000L));
        writer.appendTextElement(params, "emitWithinMillis", Long.toString(bpc.getEmitWithinMs()));
        writer.appendTextElement(params, "emitMaxEstimatedBytes",
                Long.toString((long) bpc.getTotalEmitThesholdMb() * 1024 * 1024));
        writer.appendTextElement(params, "maxForEmitBatchBytes",
                Long.toString((long) bpc.getPerFileEmitThresholdMb() * 1024 * 1024));

        appendPipesReporters(writer, async, bpc);

    }

    private void appendPipesReporters(DomWriter writer, Element async, BatchProcessConfig bpc) {
        Element compositePipesReporter = writer.createAndGetElement(async, "pipesReporter", "class",
                "org.apache.tika.pipes.CompositePipesReporter");
        //Element params = writer.createAndGetElement(compositePipesReporter, "params");
        Element fsReporter = writer.createAndGetElement(compositePipesReporter, "pipesReporter", "class",
                "org.apache.tika.pipes.reporters.fs.FileSystemStatusReporter");
        Element fsReporterParams = writer.createAndGetElement(fsReporter, "params");
        writer.appendTextElement(fsReporterParams, "statusFile",
                AppContext.BATCH_STATUS_PATH.toAbsolutePath().toString());
        writer.appendTextElement(fsReporterParams, "reportUpdateMillis", "1000");


        //parameterize
        if (bpc.getEmitter().isEmpty()) {
            return;
        }
        EmitterSpec emitter = bpc.getEmitter().get();
        if (!(emitter instanceof JDBCEmitterSpec) && !(emitter instanceof CSVEmitterSpec)) {
            return;
        }
        Optional<String> connectionString = ((JDBCEmitterSpec) emitter).getConnectionString();

        if (connectionString.isPresent()) {
            Element jdbc = writer.createAndGetElement(compositePipesReporter, "pipesReporter", "class",
                    "org.apache.tika.pipes.reporters.jdbc.JDBCPipesReporter");

            Element jdbcParams = writer.createAndGetElement(jdbc, "params");
            writer.appendTextElement(jdbcParams, "connection", connectionString.get());
        }
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

    private void appendEmitter(BatchProcessConfig batchProcessConfig, DomWriter writer,
                               Element properties) throws IOException {
        Optional<EmitterSpec> optionalEmitter = batchProcessConfig.getEmitter();
        if (optionalEmitter.isEmpty()) {
            LOGGER.warn("emitter is empty?!");
            return;
        }
        optionalEmitter.get().write(writer, properties);
    }

    private void appendFetcher(BatchProcessConfig batchProcessConfig, DomWriter writer,
                               Element properties) throws IOException {
        Optional<ConfigItem> optionalFetcher = batchProcessConfig.getFetcher();
        if (optionalFetcher.isEmpty()) {
            LOGGER.warn("fetcher is empty?!");
            return;
        }
        ConfigItem fetcher = optionalFetcher.get();
        if ("org.apache.tika.pipes.fetcher.fs.FileSystemFetcher".equals(fetcher.getClazz())) {
            appendFSFetcher(fetcher, writer, properties);
        } else {
            throw new RuntimeException("I regret I don't yet support " + fetcher.getClazz());
        }
    }

    private void appendFSFetcher(ConfigItem fetcher, DomWriter writer, Element properties)
            throws IOException {
        Element fetchers = writer.createAndGetElement(properties, "fetchers");
        Element fetcherElement =
                writer.createAndGetElement(fetchers, "fetcher", "class", FS_FETCHER_CLASS);
        Element params = writer.createAndGetElement(fetcherElement, "params");
        writer.appendTextElement(params, "name", "fetcher");
        writer.appendTextElement(params, BASE_PATH, fetcher.getAttributes().get(BASE_PATH));
        writer.appendTextElement(params, EXTRACT_FILE_SYSTEM_METADATA,
                fetcher.getAttributes().get("extractFileSystemMetadata"));
    }

    private void appendPipesIterator(BatchProcessConfig batchProcessConfig, DomWriter writer,
                                     Element properties) throws IOException {
        Optional<ConfigItem> optionalPipesIterator = batchProcessConfig.getPipesIterator();
        if (optionalPipesIterator.isEmpty()) {
            LOGGER.warn("pipesIterator is empty?!");
            return;
        }
        ConfigItem pipesIterator = optionalPipesIterator.get();
        if ("org.apache.tika.pipes.pipesiterator.fs.FileSystemPipesIterator".equals(
                pipesIterator.getClazz())) {
            appendFSPipesIterator(batchProcessConfig, pipesIterator, writer, properties);
        } else {
            throw new RuntimeException("I regret I don't yet support " + pipesIterator.getClazz());
        }
    }

    private void appendFSPipesIterator(BatchProcessConfig batchProcessConfig,
                                       ConfigItem pipesIterator, DomWriter writer, Element parent)
            throws IOException {
        Element pipesIteratorElement = writer.createAndGetElement(parent, "pipesIterator", "class",
                "org.apache.tika.pipes.pipesiterator.fs.FileSystemPipesIterator");
        Element params = writer.createAndGetElement(pipesIteratorElement, "params");
        writer.appendTextElement(params, "fetcherName", "fetcher");
        writer.appendTextElement(params, "emitterName", "emitter");
        writer.appendTextElement(params, "basePath", pipesIterator.getAttributes().get(BASE_PATH));
        writer.appendTextElement(params, "countTotal", "true");

        if (batchProcessConfig.getWriteLimit() >= 0) {
            writer.appendTextElement(params, "writeLimit", Long.toString(batchProcessConfig.getWriteLimit()));
            writer.appendTextElement(params, "throwOnWriteLimitReached",
                    Boolean.toString(batchProcessConfig.isThrowOnWriteLimitReached()));
        }
    }

    private void appendMetadataFilter(BatchProcessConfig batchProcessConfig, DomWriter writer,
                                      Element properties) throws IOException {

        Optional<EmitterSpec> emitterSpec = batchProcessConfig.getEmitter();
        if (emitterSpec.isEmpty()) {
            LOGGER.warn("emitter is empty?!");
            return;
        }
        Element metadataFilters = writer.createAndGetElement(properties, "metadataFilters");
        writer.appendLeafElement(metadataFilters, "metadataFilter", "class",
                "org.apache.tika.metadata.filter.GeoPointMetadataFilter");
        writer.appendLeafElement(metadataFilters, "metadataFilter", "class",
                "org.apache.tika.metadata.filter.DateNormalizingMetadataFilter");
        writer.appendLeafElement(metadataFilters, "metadataFilter", "class",
                "org.apache.tika.eval.core.metadata.TikaEvalMetadataFilter");


        Element captureGroupFilter = writer.createAndGetElement(metadataFilters,
                "metadataFilter", "class",
                "org.apache.tika.metadata.filter.CaptureGroupMetadataFilter");
        Element captureGroupParams = writer.createAndGetElement(captureGroupFilter, "params");
        writer.appendTextElement(captureGroupParams, "sourceField", "Content-Type");
        writer.appendTextElement(captureGroupParams, "targetField", "mime-short");
        writer.appendTextElement(captureGroupParams, "regex", "\\A([^;]+)");

        BaseEmitterSpec emitter = (BaseEmitterSpec) emitterSpec.get();

        List<MetadataTuple> metadataTuples = emitter.getMetadataTuples();
        if (metadataTuples.size() == 0) {
            return;
        }
        Element fieldNameMappingFilter = writer.createAndGetElement(metadataFilters, "metadataFilter", "class",
                "org.apache.tika.metadata.filter.FieldNameMappingFilter");
        Element fieldNameMappingParams = writer.createAndGetElement(fieldNameMappingFilter, "params");
        writer.appendTextElement(fieldNameMappingParams, "excludeUnmapped", "true");

        Map<String, String> map = new LinkedHashMap<>();
        metadataTuples.stream().forEach(e -> map.put(e.getTika(), e.getOutput()));

        writer.appendMap(fieldNameMappingParams, "mappings", "mapping", map);
    }


    private String getTemplateLog4j2(String template) throws IOException {
        try (InputStream is = this.getClass()
                .getResourceAsStream("/templates/log4j2/" + template)) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        }
    }
}
