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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringWriter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.jupiter.api.Test;
import org.tallison.tika.app.fx.Constants;
import org.tallison.tika.app.fx.tools.BatchProcess;

public class TestAppContextSerialization {

    @Test
    public void testBasic() throws Exception {
        AppContext appContext = new AppContext();
        appContext.getBatchProcessConfig().get().setEmitter(
                "emitter label",
                Constants.FS_EMITTER_CLASS,
                "basePath", "something");
        BatchProcess batchProcess = new BatchProcess();
        appContext.setBatchProcess(batchProcess);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        //this is necessary for timestamps
        objectMapper.registerModule(new Jdk8Module());

        StringWriter writer = new StringWriter();
        objectMapper.writeValue(writer, appContext);
        AppContext deserialized = objectMapper.readValue(writer.toString(), AppContext.class);
        assertEquals(-1l, deserialized.getBatchProcess().get().getRunningProcessId());
        assertEquals(BatchProcess.STATUS.READY, deserialized.getBatchProcess().get().getStatus());
    }
}
