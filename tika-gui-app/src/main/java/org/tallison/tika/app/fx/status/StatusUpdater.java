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
package org.tallison.tika.app.fx.status;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.beans.property.SimpleFloatProperty;
import javafx.scene.control.ProgressIndicator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tallison.tika.app.fx.TikaController;
import org.tallison.tika.app.fx.ctx.AppContext;
import org.tallison.tika.app.fx.tools.BatchProcess;

import org.apache.tika.pipes.PipesResult;
import org.apache.tika.pipes.async.AsyncStatus;

public class StatusUpdater implements Callable<Integer> {
    private static Logger LOGGER = LogManager.getLogger(StatusUpdater.class);
    private ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

    private SimpleFloatProperty progressValue = new SimpleFloatProperty(0.0f);
    private final ProgressIndicator progressIndicator;
    private final TikaController tikaController;
    private final MutableStatus mutableStatus;
    public StatusUpdater(MutableStatus mutableStatus, TikaController tikaController) {
        this.mutableStatus = mutableStatus;
        this.tikaController = tikaController;
        this.progressIndicator = tikaController.getBatchProgressIndicator();
        this.progressIndicator.progressProperty().bind(progressValue);

    }


    @Override
    public Integer call() throws Exception {
        progressValue.set(0.0f);

        while (true) {
            if (Files.isRegularFile(AppContext.BATCH_STATUS_PATH)) {
                AsyncStatus asyncStatus = null;
                try {
                    asyncStatus = objectMapper.readValue(AppContext.BATCH_STATUS_PATH.toFile(),
                            AsyncStatus.class);
                } catch (IOException e) {
                    LOGGER.warn("bad json ", e);
                    Thread.sleep(1000);
                    continue;
                }
                if (asyncStatus != null) {
                    long processed = 0;
                    for (Map.Entry<PipesResult.STATUS, Long> e : asyncStatus.getStatusCounts().entrySet()) {
                        processed += e.getValue();
                    }
                    LOGGER.debug("processed {}", asyncStatus);
                    long total = asyncStatus.getTotalCountResult().getTotalCount();
                    if (processed > total) {
                        total = processed;
                    }
                    if (asyncStatus.getAsyncStatus() == AsyncStatus.ASYNC_STATUS.COMPLETED) {
                        progressValue.set(1.0f);
                        mutableStatus.set(BatchProcess.STATUS.COMPLETE);
                        tikaController.updateButtons(BatchProcess.STATUS.COMPLETE);
                        return 1;
                    } else if (total > 0) {
                        float percentage = ((float) processed / (float) total);
                        LOGGER.debug("setting {} :: {} / {}", percentage,
                                processed, total);
                        progressValue.set(percentage);
                    }
                }
            }
            Thread.sleep(1000);
        }
    }
}
