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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

import javafx.beans.property.SimpleFloatProperty;
import javafx.scene.control.ProgressIndicator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tallison.tika.app.fx.ControllerBase;
import org.tallison.tika.app.fx.TikaController;
import org.tallison.tika.app.fx.tools.BatchProcess;

import org.apache.tika.pipes.PipesResult;
import org.apache.tika.pipes.async.AsyncStatus;

public class StatusUpdater implements Callable<Integer> {
    private static Logger LOGGER = LogManager.getLogger(StatusUpdater.class);
    private SimpleFloatProperty progressValue = new SimpleFloatProperty(0.0f);
    private final ProgressIndicator progressIndicator;
    private final TikaController tikaController;
    private final BatchProcess batchProcess;

    public StatusUpdater(BatchProcess batchProcess, TikaController tikaController) {
        this.batchProcess = batchProcess;
        this.tikaController = tikaController;
        this.progressIndicator = tikaController.getBatchProgressIndicator();
        this.progressIndicator.progressProperty().bind(progressValue);

    }


    @Override
    public Integer call() throws Exception {
        progressValue.set(0.0f);

        while (true) {
            Optional<AsyncStatus> asyncStatusOptional = batchProcess.checkAsyncStatus();
            System.out.println(asyncStatusOptional);
            if (asyncStatusOptional.isPresent()) {
                AsyncStatus asyncStatus = asyncStatusOptional.get();
                long processed = 0;
                for (Map.Entry<PipesResult.STATUS, Long> e : asyncStatus.getStatusCounts()
                        .entrySet()) {
                    processed += e.getValue();
                }
                LOGGER.debug("processed {}", asyncStatus);
                long total = asyncStatus.getTotalCountResult().getTotalCount();
                if (processed > total) {
                    total = processed;
                }
                if (asyncStatus.getAsyncStatus() == AsyncStatus.ASYNC_STATUS.COMPLETED) {
                    progressValue.set(1.0f);
                    tikaController.updateButtons(BatchProcess.STATUS.COMPLETE);
                    return 1;
                } else if (total > 0) {
                    float percentage = ((float) processed / (float) total);
                    LOGGER.debug("setting {} :: {} / {}", percentage, processed, total);
                    progressValue.set(percentage);
                }
            }
            batchProcess.checkBatchRunnerStatus();
            System.out.println("status: " + batchProcess.getMutableStatus());

            BatchProcess.STATUS status = batchProcess.getMutableStatus().get();
            if (status == BatchProcess.STATUS.ERROR) {
                Optional<Exception> exception = batchProcess.getJvmException();
                if (exception.isPresent()) {
                    ControllerBase.alertStackTrace("Batch process failed",
                            "Batch process failed", "Serious problem",
                            exception.get());
                } else {
                    ControllerBase.alert("Batch process failed", "Batch process failed",
                            "Batch process failed with no thrown exception. " +
                                    "Check logs. I'm sorry.");
                }
                return 1;
            }
            if (status != BatchProcess.STATUS.READY && status != BatchProcess.STATUS.RUNNING) {
                LOGGER.debug("status updater sees status {}", status);
                return 1;
            }
            Thread.sleep(1000);
        }
    }
}
