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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tallison.tika.app.fx.csv.CSVEmitterHelper;
import org.tallison.tika.app.fx.ctx.AppContext;
import org.tallison.tika.app.fx.status.MutableStatus;
import org.tallison.tika.app.fx.status.StatusUpdater;

import org.apache.tika.exception.TikaException;
import org.apache.tika.pipes.async.AsyncStatus;
import org.apache.tika.utils.ProcessUtils;
import org.apache.tika.utils.StreamGobbler;
import org.apache.tika.utils.StringUtils;

public class BatchProcess {

    public enum STATUS {
        READY, ERROR, RUNNING, COMPLETE, CANCELED;
    }

    private enum PROCESS_ID {
        BATCH_PROCESS
    }

    private static Logger LOGGER = LogManager.getLogger(BatchProcess.class);

    private final MutableStatus mutableStatus = new MutableStatus(STATUS.READY);
    private long runningProcessId = -1;
    private Path configFile;
    private BatchRunner batchRunner = null;

    private Optional<Exception> jvmException = Optional.empty();
    private Optional<String> jvmErrorMsg = Optional.empty();
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

    public synchronized void start(BatchProcessConfig batchProcessConfig, StatusUpdater statusUpdater)
            throws TikaException, IOException {
        deletePreviousRuns();
        TikaConfigWriter tikaConfigWriter = new TikaConfigWriter();

        CSVEmitterHelper.setUp(AppContext.getInstance());

        try {
            configFile = tikaConfigWriter.writeConfig(batchProcessConfig);
            tikaConfigWriter.writeLog4j2();
        } catch (IOException e) {
            throw new TikaException("parser configuration", e);
        }

        batchRunner = new BatchRunner(configFile, batchProcessConfig);

        executorCompletionService.submit(statusUpdater);
        executorCompletionService.submit(batchRunner);
    }

    private void deletePreviousRuns() {
        try {
            if (Files.isRegularFile(AppContext.getInstance().BATCH_STATUS_PATH)) {
                Files.delete(AppContext.getInstance().BATCH_STATUS_PATH);
            }
        } catch (IOException e) {
            LOGGER.warn("couldn't delete batch status file");
        }
/*
        //TODO -- or should we just configure the logger to over write?!
        for (File f : AppContext.LOGS_PATH.toFile().listFiles()) {
            if (f.getName().endsWith(".log")) {
                try {
                    Files.delete(f.toPath());
                } catch (IOException e) {
                    LOGGER.warn("couldn't delete " + f, e);
                }
            }
        }
 */
    }

    public synchronized void cancel() {
        mutableStatus.set(STATUS.CANCELED);
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
        daemonExecutorService.shutdownNow();
        //CSVEmitterHelper.cleanTmpResources(APP_CONTEXT);
    }

    public Optional<AsyncStatus> checkAsyncStatus() {
        if (! Files.isRegularFile(AppContext.BATCH_STATUS_PATH)) {
            return Optional.empty();
        }
        try {
            AsyncStatus asyncStatus = objectMapper.readValue(AppContext.BATCH_STATUS_PATH.toFile(),
                    AsyncStatus.class);
            if (asyncStatus.getAsyncStatus() == AsyncStatus.ASYNC_STATUS.COMPLETED) {
                mutableStatus.set(BatchProcess.STATUS.COMPLETE);
            }
            return Optional.of(asyncStatus);
        } catch (IOException e) {
            LOGGER.warn("couldn't read status file", e);
            return Optional.empty();
        }
    }

    public void checkBatchRunnerStatus() {
        try {
            Future<Integer> future = executorCompletionService.poll();
            System.out.println("executor service shutdown: " + daemonExecutorService.isShutdown());
            if (future != null) {
                Integer i = future.get();
                System.out.println("completed: " + i);
            }
        } catch (InterruptedException e) {
            LOGGER.warn("interrupted?!");
        } catch (ExecutionException e) {
            mutableStatus.set(STATUS.ERROR);
            jvmException = Optional.of(e);
        }
    }

    public void close() {
        LOGGER.info("closing/shutting down now");
        daemonExecutorService.shutdownNow();
        LOGGER.info("after shutdown: " + daemonExecutorService.isShutdown());
        try {
            if (Files.isRegularFile(AppContext.BATCH_STATUS_PATH)) {
                Files.delete(AppContext.BATCH_STATUS_PATH);
            }
        } catch (IOException e) {
            LOGGER.warn("couldn't delete batch status: " + AppContext.BATCH_STATUS_PATH, e);
        }
    }

    //If the emitter is a csv file,

    public long getRunningProcessId() {
        return runningProcessId;
    }

    public MutableStatus getMutableStatus() {
        return mutableStatus;
    }

    public Optional<Exception> getJvmException() {
        return jvmException;
    }

    public Optional<String> getJvmErrorMsg() {
        return jvmErrorMsg;
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
            process = new ProcessBuilder(commandLine)
                        .inheritIO() //TODO -- for dev purposes only
                        .start();
            mutableStatus.set(STATUS.RUNNING);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("process {}", process.isAlive());
                LOGGER.trace("pid {}", process.pid());
                LOGGER.trace("info {}", process.info());
            }
            StreamGobbler inputStreamGobbler = new StreamGobbler(process.getInputStream(), 100000);
            StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), 100000);
            Thread isgThread = new Thread(inputStreamGobbler);
            isgThread.setDaemon(true);
            isgThread.start();

            Thread esgThread = new Thread(errorGobbler);
            esgThread.setDaemon(true);
            esgThread.start();

            runningProcessId = process.pid();

            int i = 0;
            while (true) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("process {} {}", ++i, process.isAlive());
                    LOGGER.trace("pid {}", process.pid());
                    LOGGER.trace("info {}", process.info());
                }
                if (!process.isAlive()) {
                    if (process.exitValue() != 0) {
                        mutableStatus.set(STATUS.ERROR);
                        LOGGER.error("exit value {}", process.exitValue());
                        esgThread.join(10000);
                        isgThread.join(10000);
                        String error = StringUtils.joinWith("\n", errorGobbler.getLines());
                        String out = StringUtils.joinWith("\n", inputStreamGobbler.getLines());
                        String msg = "process ended with a surprising exit value (" +
                                process.exitValue() + ")\nstdout: " + out + "\nstderr: " + error;
                        LOGGER.warn("process ended with a surprising exit value" +
                                "({})\nstdout: {}\nstderr: {}", process.exitValue(), out, error);
                        jvmErrorMsg = Optional.of(msg);
                        mutableStatus.set(STATUS.ERROR);
                    } else {
                        mutableStatus.set(STATUS.COMPLETE);
                    }
                    CSVEmitterHelper.writeCSV(AppContext.getInstance());
                    return PROCESS_ID.BATCH_PROCESS.ordinal();
                } else {
                    Thread.sleep(500);
                }
            }
        }

        private List<String> buildCommandLine() {
            List<String> commandLine = new ArrayList<>();

            commandLine.add(
                    ProcessUtils.escapeCommandLine(AppContext.getInstance().getJavaHome().resolve("java").toString()));
            commandLine.add("-Dlog4j.configurationFile=config/log4j2-async-cli.xml");
            commandLine.add("-cp");
            String cp = buildClassPath();
            LOGGER.info("class path: {}", cp);

            commandLine.add(cp);
            commandLine.add("org.apache.tika.async.cli.TikaAsyncCLI");
            commandLine.add(ProcessUtils.escapeCommandLine(tikaConfig.toAbsolutePath().toString()));
            LOGGER.info("built commandline: " + commandLine);
            return commandLine;
        }

        private String buildClassPath() {
            StringBuilder sb = new StringBuilder();
            sb.append(ProcessUtils.escapeCommandLine(
                    AppContext.TIKA_CORE_BIN_PATH.toAbsolutePath() + "/*"));
            sb.append(File.pathSeparator);
            sb.append(ProcessUtils.escapeCommandLine(
                    AppContext.TIKA_EXTRAS_BIN_PATH.toAbsolutePath() + "/*"));
            sb.append(File.pathSeparator);
            //TODO refactor batch process config to generate class path
            //for fetchers/emitters
            batchProcessConfig.appendPipesClasspath(sb);
            return sb.toString();
        }

        void cancel() {
            if (process != null) {
                process.destroyForcibly();
            }
        }
    }
}
