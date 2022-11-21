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
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
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
import org.tallison.tika.app.fx.Constants;
import org.tallison.tika.app.fx.batch.BatchProcessConfig;
import org.tallison.tika.app.fx.ctx.AppContext;
import org.tallison.tika.app.fx.metadata.MetadataTuple;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
            Element properties = document.createElement("properties");
            document.appendChild(properties);
            appendParsers(batchProcessConfig, document, properties);
            //sb.append(getTemplate("parsers.xml")).append("\n");
            appendMetadataFilter(batchProcessConfig, document, properties);
            appendPipesIterator(batchProcessConfig, document, properties);
            appendFetcher(batchProcessConfig, document, properties);
            appendEmitter(batchProcessConfig, document, properties);
            appendAsync(batchProcessConfig, document, properties);
            appendAutoDetectParserConfig(batchProcessConfig, document, properties);

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            StringWriter debug = new StringWriter();
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                StreamResult result = new StreamResult(writer);
                DOMSource source = new DOMSource(document);
                transformer.transform(source, result);
            }
        } catch (XMLStreamException | TransformerException | ParserConfigurationException e) {
            throw new IOException(e);
        }
        return tmp;
    }


    private void appendParsers(BatchProcessConfig batchProcessConfig, Document document,
                               Element properties) throws XMLStreamException {
        Element parsers = document.createElement("parsers");
        properties.appendChild(parsers);
        Element dflt = document.createElement("parser");
        parsers.appendChild(dflt);
        dflt.setAttribute("class", "org.apache.tika.parser.DefaultParser");
        excludeParsers(document, dflt, "org.apache.tika.parser.ocr.TesseractOCRParser",
                "org.apache.tika.parser.pdf.PDFParser",
                "org.apache.tika.parser.microsoft.ooxml.OOXMLParser",
                "org.apache.tika.parser.microsoft.OfficeParser");
        addLegacyParams(document, parsers, "parser", "org.apache.tika.parser.pdf.PDFParser",
                "extractActions", "bool", "true");

        addLegacyParams(document, parsers, "parser",
                "org.apache.tika.parser.microsoft.ooxml.OOXMLParser", "extractMacros", "bool",
                "true", "includeDeletedContent", "bool", "true", "includeMoveFromContent", "bool",
                "true");

        addLegacyParams(document, parsers, "parser",
                "org.apache.tika.parser.microsoft.OfficeParser", "extractMacros", "bool", "true");


    }

    private void addLegacyParams(Document document, Element parent, String nodeName, String clz,
                                 String... tuples) {
        //total hack
        Element parser = createAndGetElement(document, parent, nodeName, "class", clz);
        if (tuples.length == 0) {
            return;
        }
        Element params = createAndGetElement(document, parser, "params");
        for (int i = 0; i < tuples.length; i += 3) {
            appendTextElement(document, params, "param", tuples[2], "name", tuples[0], "type",
                    tuples[1]);
        }
    }

    private void excludeParsers(Document document, Element parser, String... classes) {
        for (String clz : classes) {
            createAndGetElement(document, parser, "parser-exclude", "class", clz);
        }
    }

    private void appendAutoDetectParserConfig(BatchProcessConfig batchProcessConfig,
                                              Document document, Element properties)
            throws IOException {
        Element adpc = createAndGetElement(document, properties, "autoDetectParserConfig");
        Element params = createAndGetElement(document, adpc, "params");
        appendTextElement(document, params, "spoolToDisk", "0");
        appendTextElement(document, params, "outputThreshold", "10000");
        appendTextElement(document, params, "maximumCompressionRatio", "100");
        appendTextElement(document, params, "maximumDepth", "100");
        appendTextElement(document, params, "maximumPackageEntryDepth", "100");

        //good enough for now.  we'll have to figure out
        //a better option if we add more params
        if (batchProcessConfig.getDigest().isEmpty()) {
            return;
        }
        String digestString = batchProcessConfig.getDigest().get();
        if (digestString.equals(NO_DIGEST)) {
            return;
        }
        Element digester = createAndGetElement(document, adpc, "digesterFactory", "class",
                "org.apache.tika.parser.digestutils.CommonsDigesterFactory");
        Element digesterParams = createAndGetElement(document, digester, "params");
        appendTextElement(document, digesterParams, "markLimit", "1000000");
        appendTextElement(document, digesterParams, "algorithmString", digestString);

    }

    private void appendAsync(BatchProcessConfig bpc, Document document, Element properties)
            throws IOException {
        Element async = createAndGetElement(document, properties, "async");
        Element params = createAndGetElement(document, properties, "params");
        appendTextElement(document, params, "javaPath",
                AppContext.getInstance().getJavaHome().resolve("java").toString());
        appendTextElement(document, params, "numClients", Integer.toString(bpc.getNumProcesses()));
        appendTextElement(document, params, "numEmitters", "1");
        appendListElement(document, params, "forkedJVMArgs", "arg",
                "-Xmx" + bpc.getMaxMemMb() + "m", "-Dlog4j.configurationFile=" +
                        AppContext.ASYNC_LOG4J2_PATH.toAbsolutePath().toString(), "-cp",
                buildClassPath(bpc));
        appendTextElement(document, params, "timeoutMillis",
                Long.toString(bpc.getParseTimeoutSeconds() * 1000));
        appendTextElement(document, params, "emitWithinMillis",
                Long.toString(bpc.getEmitWithinMs()));
        appendTextElement(document, params, "emitMaxEstimatedBytes",
                Long.toString(bpc.getTotalEmitThesholdMb() * 1024 * 1024));
        appendTextElement(document, params, "maxForEmitBatchBytes",
                Long.toString(bpc.getPerFileEmitThresholdMb() * 1024 * 1024));

        appendPipesReporters(document, async, bpc);

    }

    private void appendPipesReporters(Document document, Element async, BatchProcessConfig bpc) {
        Element compositePipesReporter =
                createAndGetElement(document, async, "pipesReporter", "class",
                        "org.apache.tika.pipes.CompositePipesReporter");
        Element params = createAndGetElement(document, compositePipesReporter, "params");
        Element pipesReporters = createAndGetElement(document, params, "pipesReporters", "class",
                "org.apache.tika.pipes.PipesReporter");
        Element fsReporter = createAndGetElement(document, pipesReporters, "pipesReporter", "class",
                "org.apache.tika.pipes.reporters.fs.FileSystemStatusReporter");
        Element fsReporterParams = createAndGetElement(document, fsReporter, "params");
        appendTextElement(document, fsReporterParams, "statusFile",
                AppContext.BATCH_STATUS_PATH.toAbsolutePath().toString());
        appendTextElement(document, fsReporterParams, "reportUpdateMillis", "1000");


        //parameterize
        if (bpc.getEmitter().isEmpty()) {
            return;
        }
        ConfigItem emitter = bpc.getEmitter().get();
        if (!emitter.getClazz().equals(JDBC_EMITTER_CLASS) &&
                !emitter.getClazz().equals(CSV_EMITTER_CLASS)) {
            return;
        }
        Element jdbc = createAndGetElement(document, pipesReporters, "pipesReporter", "class",
                "org.apache.tika.pipes.reporters.jdbc.JDBCPipesReporter");

        Element jdbcParams = createAndGetElement(document, jdbc, "params");
        appendTextElement(document, jdbcParams, "connection",
                emitter.getAttributes().get(JDBC_CONNECTION_STRING));
    }

    private void appendListElement(Document document, Element parent, String itemNames,
                                   String itemName, String... elements) {
        Element items = createAndGetElement(document, parent, itemNames);
        for (String element : elements) {
            Element item = createAndGetElement(document, items, itemName);
            item.setTextContent(element);
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

    private void appendEmitter(BatchProcessConfig batchProcessConfig, Document document,
                               Element properties) throws IOException {
        Optional<ConfigItem> optionalEmitter = batchProcessConfig.getEmitter();
        if (optionalEmitter.isEmpty()) {
            LOGGER.warn("emitter is empty?!");
            return;
        }
        ConfigItem emitter = optionalEmitter.get();
        Element emitters = createAndGetElement(document, properties, "emitters");
        switch (emitter.getClazz()) {
            case Constants.FS_EMITTER_CLASS:
                appendFSEmitter(emitter, document, emitters);
                break;
            case Constants.OPEN_SEARCH_EMITTER_CLASS:
                appendOpenSearchEmitter(emitter, document, emitters);
                break;
            case Constants.CSV_EMITTER_CLASS:
                appendJDBCEmitter(emitter, emitter.getAttributes().get(CSV_JDBC_CONNECTION_STRING),
                        emitter.getAttributes().get(CSV_JDBC_INSERT_SQL), document, emitters);
                break;
            case Constants.JDBC_EMITTER_CLASS:
                appendJDBCEmitter(emitter, emitter.getAttributes().get(JDBC_CONNECTION_STRING),
                        emitter.getAttributes().get(JDBC_INSERT_SQL), document, emitters);
                break;
            default:
                throw new RuntimeException("I regret I don't yet support " +
                        batchProcessConfig.getEmitter().get().getClazz());
        }
    }

    private void appendJDBCEmitter(ConfigItem emitter, String connectionString, String insertString,
                                   Document document, Element emitters) throws IOException {
        Element emitterElement = createAndGetElement(document, emitters, "emitter", "class",
                "org.apache.tika.pipes.emitter.jdbc.JDBCEmitter");
        Element params = createAndGetElement(document, emitterElement, "params");
        appendTextElement(document, params, "name", "emitter");
        appendTextElement(document, params, "connection", connectionString);
        appendTextElement(document, params, "insert", insertString);
        appendTextElement(document, params, "attachmentStrategy", "all");

        if (emitter.getMetadataTuples().isEmpty() ||
                emitter.getMetadataTuples().get().size() == 0) {
            return;
        }
        Map<String, String> map = new HashMap<>();
        emitter.getMetadataTuples().get().stream()
                .forEach(e -> map.put(e.getTika(), e.getOutput()));

        appendMap(document, params, "keys", "key", map);
    }

    private void appendOpenSearchEmitter(ConfigItem emitter, Document document, Element emitters)
            throws IOException {
        Element emitterElement = createAndGetElement(document, emitters, "emitter", "class",
                "org.apache.tika.pipes.emitter.opensearch.OpenSearchEmitter");
        Element params = createAndGetElement(document, emitterElement, "params");
        appendTextElement(document, params, "name", "emitter");
        appendTextElement(document, params, "idField", "_id");

        appendTextElement(document, params, OPEN_SEARCH_URL,
                emitter.getAttributes().get(OPEN_SEARCH_URL));
        appendTextElement(document, params, OPEN_SEARCH_UPDATE_STRATEGY,
                emitter.getAttributes().get(OPEN_SEARCH_UPDATE_STRATEGY));
        appendTextElement(document, params, "connectionTimeout", "60000");
        appendTextElement(document, params, "socketTimeout", "120000");
        ;
        String userName = emitter.getAttributes().get(OPEN_SEARCH_USER);
        String password = emitter.getAttributes().get(OPEN_SEARCH_PW);
        if (!StringUtils.isBlank(userName) && !StringUtils.isBlank(password)) {
            appendTextElement(document, params, "userName",
                    emitter.getAttributes().get(OPEN_SEARCH_USER));
            appendTextElement(document, params, "password",
                    emitter.getAttributes().get(OPEN_SEARCH_PW));
        }
    }

    private void appendFSEmitter(ConfigItem emitter, Document document, Element emitters)
            throws IOException {
        Element emitterElement = createAndGetElement(document, emitters, "emitter", "class",
                "org.apache.tika.pipes.emitter.fs.FileSystemEmitter");
        Element params = createAndGetElement(document, emitterElement, "params");
        appendTextElement(document, params, "name", "emitter");
        appendTextElement(document, params, BASE_PATH, emitter.getAttributes().get(BASE_PATH));
    }


    private void appendFetcher(BatchProcessConfig batchProcessConfig, Document document,
                               Element properties) throws IOException {
        Optional<ConfigItem> optionalFetcher = batchProcessConfig.getFetcher();
        if (optionalFetcher.isEmpty()) {
            LOGGER.warn("fetcher is empty?!");
            return;
        }
        ConfigItem fetcher = optionalFetcher.get();
        switch (fetcher.getClazz()) {
            case "org.apache.tika.pipes.fetcher.fs.FileSystemFetcher":
                appendFSFetcher(fetcher, document, properties);
                break;
            default:
                throw new RuntimeException("I regret I don't yet support " + fetcher.getClazz());
        }
    }

    private void appendFSFetcher(ConfigItem fetcher, Document document, Element properties)
            throws IOException {
        Element fetchers = createAndGetElement(document, properties, "fetchers");
        Element fetcherElement = createAndGetElement(document, fetchers, "fetcher");
        Element params = createAndGetElement(document, fetcherElement, "params");
        appendTextElement(document, params, "name", "fetcher");
        appendTextElement(document, params, BASE_PATH, fetcher.getAttributes().get(BASE_PATH));

    }

    private void appendPipesIterator(BatchProcessConfig batchProcessConfig, Document document,
                                     Element properties) throws IOException {
        Optional<ConfigItem> optionalPipesIterator = batchProcessConfig.getPipesIterator();
        if (optionalPipesIterator.isEmpty()) {
            LOGGER.warn("pipesIterator is empty?!");
            return;
        }
        ConfigItem pipesIterator = optionalPipesIterator.get();
        switch (pipesIterator.getClazz()) {
            case "org.apache.tika.pipes.pipesiterator.fs.FileSystemPipesIterator":
                appendFSPipesIterator(pipesIterator, document, properties);
                break;
            default:
                throw new RuntimeException(
                        "I regret I don't yet support " + pipesIterator.getClazz());
        }
    }

    private void appendFSPipesIterator(ConfigItem pipesIterator, Document document, Element parent)
            throws IOException {
        Element pipesIteratorElement =
                createAndGetElement(document, parent, "pipesIterator", "class");
        Element params = createAndGetElement(document, pipesIteratorElement, "params");
        appendTextElement(document, params, "fetcherName", "fetcher");
        appendTextElement(document, params, "emitterName", "emitter");
        appendTextElement(document, params, "basePath",
                pipesIterator.getAttributes().get(BASE_PATH));
        appendTextElement(document, params, "countTotal", "true");
    }

    private void appendMetadataFilter(BatchProcessConfig batchProcessConfig, Document document,
                                      Element properties) throws IOException {

        Optional<ConfigItem> configItem = batchProcessConfig.getEmitter();
        if (configItem.isEmpty()) {
            LOGGER.warn("emitter is empty?!");
            return;
        }
        Element metadataFilters = document.createElement("metadataFilters");
        properties.appendChild(metadataFilters);
        appendLeafElement(document, metadataFilters, "metadataFilter", "class",
                "org.apache.tika.metadata.filter.GeoPointMetadataFilter");
        appendLeafElement(document, metadataFilters, "metadataFilter", "class",
                "org.apache.tika.metadata.filter.DateNormalizingMetadataFilter");
        appendLeafElement(document, metadataFilters, "metadataFilter", "class",
                "org.apache.tika.eval.core.metadata.TikaEvalMetadataFilter");

        ConfigItem emitter = configItem.get();
        Optional<List<MetadataTuple>> metadataTuples = emitter.getMetadataTuples();
        if (metadataTuples.isEmpty() || metadataTuples.get().size() == 0) {
            return;
        }
        Element filter = createAndGetElement(document, metadataFilters, "metadataFilter", "class",
                "org.apache.tika.metadata.filter.FieldNameMappingFilter");
        Element params = createAndGetElement(document, filter, "params");
        appendTextElement(document, params, "excludeUnmapped", "true");

        Map<String, String> map = new HashMap<>();
        metadataTuples.get().stream().forEach(e -> map.put(e.getTika(), e.getOutput()));

        appendMap(document, params, "mappings", "mapping", map);
    }

    private void appendMap(Document document, Element parent, String mappingsElementName,
                           String mappingElementName, Map<String, String> map, String... attrs) {
        Element mappings = createAndGetElement(document, parent, mappingElementName, attrs);
        for (Map.Entry<String, String> e : map.entrySet()) {
            appendLeafElement(document, mappings, mappingElementName, "from", e.getKey(), "to",
                    e.getValue());
        }
    }

    private void appendTextElement(Document document, Element parent, String itemName, String text,
                                   String... attrs) {
        Element el = createAndGetElement(document, parent, itemName, attrs);
        el.setTextContent(text);
    }

    private Element createAndGetElement(Document document, Element parent, String elementName,
                                        String... attrs) {
        Element el = document.createElement(elementName);
        parent.appendChild(el);
        for (int i = 0; i < attrs.length; i += 2) {
            el.setAttribute(attrs[i], attrs[i + 1]);
        }
        return el;
    }

    private void appendLeafElement(Document document, Element parent, String elementName,
                                   String... attrs) {
        createAndGetElement(document, parent, elementName, attrs);
    }

    private String getTemplateLog4j2(String template) throws IOException {
        try (InputStream is = this.getClass()
                .getResourceAsStream("/templates/log4j2/" + template)) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        }
    }
}
