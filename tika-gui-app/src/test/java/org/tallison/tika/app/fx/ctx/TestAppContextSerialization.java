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
package org.tallison.tika.app.fx.ctx;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.tallison.tika.app.fx.batch.BatchProcessConfig;
import org.tallison.tika.app.fx.emitters.EmitterSpec;
import org.tallison.tika.app.fx.emitters.FileSystemEmitterSpec;

import org.apache.tika.pipes.PipesResult;

public class TestAppContextSerialization {

    @BeforeAll
    public static void setUp() throws Exception {
        System.setProperty("TIKA_GUI_JAVA_HOME", System.getProperty("java.home"));
    }

    @Test
    public void testBasic() throws Exception {
        String emitterPath = "somthing123456789";
        AppContext appContext = new AppContext();
        EmitterSpec emitterSpec = new FileSystemEmitterSpec(Collections.EMPTY_LIST);
        ((FileSystemEmitterSpec)emitterSpec).setBasePath(Paths.get(emitterPath));
        emitterSpec.setShortLabel("short label");
        emitterSpec.setFullLabel("full label");
        emitterSpec.initialize();
        BatchProcessConfig batchProcessConfig = new BatchProcessConfig();
        batchProcessConfig.setEmitter(emitterSpec);
        appContext.setBatchProcessConfig(batchProcessConfig);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        //this is necessary for timestamps
        objectMapper.registerModule(new Jdk8Module());

        StringWriter writer = new StringWriter();
        objectMapper.writeValue(writer, appContext);
        AppContext deserialized = objectMapper.readValue(writer.toString(), AppContext.class);

        //cleanup
        Path something = Paths.get(emitterPath);
        if (Files.isDirectory(something)) {
            FileUtils.deleteDirectory(something.toFile());
        }
    }

    @Test
    @Disabled
    public void one() throws Exception {
        //this prints the available status codes for the tika-config.xml
        //pipes reporter template
        for (PipesResult.STATUS status : PipesResult.STATUS.values()) {
            System.out.println("<include>" + status.name() + "</include>");
        }
    }
}
