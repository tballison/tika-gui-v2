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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.value.ObservableValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tallison.tika.app.fx.ctx.AppContext;

import org.apache.tika.exception.TikaException;
import org.apache.tika.pipes.PipesResult;
import org.apache.tika.pipes.async.AsyncStatus;
import org.apache.tika.utils.ProcessUtils;
import org.apache.tika.utils.StreamGobbler;

public class BatchProcess {

    private static Logger LOGGER = LogManager.getLogger(BatchProcess.class);

    private static AppContext APP_CONTEXT = AppContext.getInstance();
    private STATUS status = STATUS.READY;
    private long runningProcessId = -1;
    private Path configFile;
    private BatchRunner batchRunner = null;

    private SimpleFloatProperty progressProperty = new SimpleFloatProperty(0.0f);
    private ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();
    private ExecutorService daemonExecutorService = Executors.newFixedThreadPool(2, r -> {
        Thread t = Executors.defaultThreadFactory().newThread(r);
        t.setDaemon(true);
        return t;
    });
    private ExecutorCompletionService<Integer> executorCompletionService =
            new ExecutorCompletionService<>(daemonExecutorService);
    public BatchProcess() {

    }
    public BatchProcess(STATUS status, long runningProcessId) {


    }

    public synchronized void start(BatchProcessConfig batchProcessConfig)
            throws TikaException, IOException {
        status = STATUS.RUNNING;

        TikaConfigWriter tikaConfigWriter = new TikaConfigWriter();
        try {
            configFile = tikaConfigWriter.writeConfig(batchProcessConfig);
            tikaConfigWriter.writeLog4j2();
        } catch (IOException e) {
            throw new TikaException("parser configuration", e);
        }

        batchRunner = new BatchRunner(configFile, batchProcessConfig);
        StatusChecker statusChecker = new StatusChecker();
        executorCompletionService.submit(statusChecker);
        executorCompletionService.submit(batchRunner);
    }

    public synchronized void cancel() {
        if (batchRunner != null) {
            batchRunner.cancel();
        }
        if (configFile != null && Files.isRegularFile(configFile)) {
            try {
                Files.delete(configFile);
            } catch (IOException e) {
                LOGGER.warn("couldn't delete configfile" + configFile);
            }
        }
        if (ProcessHandle.of(runningProcessId).isPresent()) {
            try {
                ProcessHandle handle = ProcessHandle.of(runningProcessId).get();
                handle.destroyForcibly();
            } catch (NoSuchElementException e) {
                //swallow
            }
        }
    }

    public AsyncStatus checkStatus() {
        try {
            return objectMapper.readValue(AppContext.BATCH_STATUS_PATH.toFile(), AsyncStatus.class);
        } catch (IOException e) {
            LOGGER.warn("couldn't read status file", e);
            return null;
        }
        //TODO -- fix this
        //we need to check that the pid matches and that the status
        //of that process is in alignment with what the AsyncProcessor is reporting
        /*
        if (status == STATUS.COMPLETE) {
            return status;
        }
        if (batchRunner != null && batchRunner.process != null) {
            if (batchRunner.process.isAlive()) {
                status = STATUS.RUNNING;
                return STATUS.RUNNING;
            } else {
                status = STATUS.COMPLETE;
                return STATUS.COMPLETE;
            }
        }
        if (runningProcessId > -1l) {
            ProcessHandle handle = ProcessHandle.of(runningProcessId).get();
            if (handle != null) {
                if (handle.isAlive()) {
                    return STATUS.RUNNING;
                }
            }
        }
        return status;*/
    }

    public void close() {
        LOGGER.info("closing/shutting down now");
        daemonExecutorService.shutdownNow();
        LOGGER.info("after shutdown: " + daemonExecutorService.isShutdown());
    }

    public long getRunningProcessId() {
        return runningProcessId;
    }

    public STATUS getStatus() {
        return status;
    }

    public ObservableValue<? extends Number> progressProperty() {
        return progressProperty;
    }

    public enum STATUS {
        //  this didn't work?!
        READY, RUNNING, COMPLETE
    }

    private enum PROCESS_ID {
        BATCH_PROCESS
    }

    private class BatchRunner implements Callable<Integer> {
        private final Path tikaConfig;
        private final BatchProcessConfig batchProcessConfig;
        private Process process;

        public BatchRunner(Path tikaConfig, BatchProcessConfig batchProcessConfig) {
            this.tikaConfig = tikaConfig;
            this.batchProcessConfig = batchProcessConfig;
        }

        @Override
        public Integer call() throws Exception {
            List<String> commandLine = buildCommandLine();
            //try {

            process = new ProcessBuilder(commandLine).start();
            StreamGobbler inputStreamGobbler = new StreamGobbler(process.getInputStream(), 100000);
            StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), 100000);
            Thread isgThread = new Thread(inputStreamGobbler);
            isgThread.setDaemon(true);
            isgThread.start();

            Thread esgThread = new Thread(errorGobbler);
            esgThread.setDaemon(true);
            esgThread.start();

            runningProcessId = process.pid();
            Thread.sleep(10000);
            System.out.println("INPUT: " + inputStreamGobbler.getLines());
            System.out.println(errorGobbler.getLines());
            return PROCESS_ID.BATCH_PROCESS.ordinal();
        }

        private List<String> buildCommandLine() {
            List<String> commandLine = new ArrayList<>();
            commandLine.add("java");
            commandLine.add("-cp");
            String cp = buildClassPath();
            LOGGER.info("class path: {}", cp);

            commandLine.add(cp);
            commandLine.add("org.apache.tika.pipes.async.AsyncProcessor");
            commandLine.add(ProcessUtils.escapeCommandLine(tikaConfig.toAbsolutePath().toString()));
            LOGGER.info(commandLine);
            return commandLine;
        }

        private String buildClassPath() {
            StringBuilder sb = new StringBuilder();
            sb.append(AppContext.TIKA_CORE_BIN_PATH + "/*");
            //TODO refactor batch process config to generate class path
            //for fetchers/emitters
            sb.append(File.pathSeparator);
            batchProcessConfig.appendPipesClasspath(sb);
            return sb.toString();
        }

        void cancel() {
            process.destroyForcibly();
        }
    }

    private class StatusChecker implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            while (true) {
                if (Files.isRegularFile(AppContext.BATCH_STATUS_PATH)) {
                    AsyncStatus asyncStatus = null;
                    try {
                        asyncStatus = objectMapper.readValue(AppContext.BATCH_STATUS_PATH.toFile(),
                                AsyncStatus.class);
                    } catch (IOException e) {
                        LOGGER.warn("bad json ", e);
                    }
                    if (asyncStatus != null) {
                        long processed = 0;
                        for (Map.Entry<PipesResult.STATUS, Long> e : asyncStatus.getStatusCounts().entrySet()) {
                            processed += e.getValue();
                        }
                        LOGGER.info("processed " + asyncStatus);
                        long total = asyncStatus.getTotalCountResult().getTotalCount();
                        if (processed > total) {
                            total = processed;
                        }
                        if (asyncStatus.getAsyncStatus() == AsyncStatus.ASYNC_STATUS.COMPLETED) {
                            progressProperty.set(100.0f);
                        } else if (total > 0) {
                            float percentage = 100f * ((float) processed / (float) total);
                            LOGGER.info("setting " + percentage);
                            progressProperty.set(percentage);
                        }
                    }
                }
                Thread.sleep(1000);
            }
        }
    }
}
