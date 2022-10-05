package org.tallison.tika.app.fx.tools;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tallison.tika.app.fx.ctx.AppContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.tika.exception.TikaException;
import org.apache.tika.pipes.FetchEmitTuple;
import org.apache.tika.pipes.pipesiterator.PipesIterator;
import org.apache.tika.utils.ProcessUtils;

public class BatchProcess {

    private static Logger LOGGER = LogManager.getLogger(BatchProcess.class);

    // TODO: it would be great if the app could be closed
    // and let the forked process keep running.  Then, when the app is
    // turned back on, it could either read the status of a finished run
    // or it could pick up the handle of the running process via
    // something like ProcessHandle.of(1000).get().info().
    public enum STATUS {
        READY,
        RUNNING,
        COMPLETE
    }

    private enum PROCESS_ID {
        FILE_COUNTER,
        BATCH_PROCESS
    }
    private STATUS status = STATUS.READY;
    private FileCounter fileCounter = null;

    private BatchRunner batchRunner = null;
    private ExecutorService executorService = Executors.newFixedThreadPool(3);
    private ExecutorCompletionService<Integer> executorCompletionService =
            new ExecutorCompletionService<>(executorService);

    public synchronized void start(BatchProcessConfig batchProcessConfig) throws TikaException, IOException {
        status = STATUS.RUNNING;
        Path tmp = null;
        TikaConfigWriter tikaConfigWriter = new TikaConfigWriter();
        try {
            tmp = tikaConfigWriter.writeConfig(batchProcessConfig);
            tikaConfigWriter.writeLog4j2();
        } catch (IOException e) {
            throw new TikaException("parser configuration", e);
        }

        fileCounter = new FileCounter(tmp);
        batchRunner = new BatchRunner(tmp, batchProcessConfig);
        executorCompletionService.submit(fileCounter);
        executorCompletionService.submit(batchRunner);
    }

    public synchronized void cancel() {
        if (batchRunner != null) {
            batchRunner.cancel();
        }
    }

    private static class FileCounter implements Callable<Integer> {

        private final PipesIterator pipesIterator;

        FileCounter(Path tikaConfig) throws IOException, TikaException {
            pipesIterator = PipesIterator.build(tikaConfig);
        }
        long counter = 0;
        @Override
        public Integer call() throws Exception {
            for (FetchEmitTuple t : pipesIterator) {
                counter++;
            }
            return PROCESS_ID.FILE_COUNTER.ordinal();
        }
    }


    private static class BatchRunner implements Callable<Integer> {
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
            try {

                process = new ProcessBuilder(commandLine).inheritIO().start();
                //TODO -- something better than this.
                process.waitFor();
            } finally {
                Files.delete(tikaConfig);
            }
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

        void cancel()  {
            process.destroyForcibly();
        }
    }
}
