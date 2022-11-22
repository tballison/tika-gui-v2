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
package org.tallison.tika.app.fx.emitters;

import static org.tallison.tika.app.fx.Constants.BASE_PATH;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tallison.tika.app.fx.ctx.AppContext;
import org.tallison.tika.app.fx.metadata.MetadataTuple;
import org.tallison.tika.app.fx.sax.DomWriter;
import org.w3c.dom.Element;

import org.apache.tika.utils.ProcessUtils;

public class FileSystemEmitterSpec extends BaseEmitterSpec {
    private static final String EMITTER_CLASS =
            "org.apache.tika.pipes.emitter.fs.FileSystemEmitter";

    private static final Logger LOGGER = LogManager.getLogger(FileSystemEmitterSpec.class);
    public FileSystemEmitterSpec(@JsonProperty("metadataTuples") List<MetadataTuple> metadataTuples) {
        super(EMITTER_CLASS, metadataTuples);
    }

    private Optional<Path> basePath;

    @Override
    public ValidationResult validate() {
        valid = false;
        if (basePath.isEmpty()) {
            setNotValidMessage("basePath is not specified");
            return new ValidationResult(ValidationResult.VALIDITY.NOT_OK,
                    "File System Emitter", "Base path is not specified",
                    "Base path is not specified");
        }
        valid = true;
        return ValidationResult.OK;
    }

    @Override
    public ValidationResult initialize() {
        ValidationResult result = validate();
        if (result != ValidationResult.OK) {
            return result;
        }
        if (! Files.isDirectory(basePath.get())) {
            try {
                Files.createDirectories(basePath.get());
            } catch (IOException e) {
                setNotValidMessage("couldn't create directory " + basePath.get().toAbsolutePath());
                LOGGER.warn("couldn't create output directory?!", e);
                return new ValidationResult(ValidationResult.VALIDITY.NOT_OK,
                        "Output problem", "Couldn't create output directory",
                        "Couldn't create output directory: " + basePath.get().toAbsolutePath());
            }
        }
        return ValidationResult.OK;
    }

    @Override
    public void write(DomWriter writer, Element properties) {
        Element emitters = writer.createAndGetElement(properties, "emitters");
        Element emitterElement = writer.createAndGetElement(emitters, "emitter", "class",
                "org.apache.tika.pipes.emitter.fs.FileSystemEmitter");
        Element params = writer.createAndGetElement(emitterElement, "params");
        writer.appendTextElement(params, "name", "emitter");
        writer.appendTextElement(params, BASE_PATH, getBasePath().get().toAbsolutePath().toString());
    }

    @Override
    public Set<String> getClassPathDependencies() {
        Set<String> items = new HashSet<>();
        items.add(
                ProcessUtils.escapeCommandLine(
                        AppContext.TIKA_LIB_PATH.resolve("tika-emitter-fs").toAbsolutePath() +
                                "/*")
        );
        return items;
    }

    public void setBasePath(Path basePath) {
        this.basePath = Optional.of(basePath);
    }

    public Optional<Path> getBasePath() {
        return basePath;
    }
}
