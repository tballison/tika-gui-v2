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
package org.tallison.tika.app.fx;

import java.math.BigInteger;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tallison.tika.app.fx.batch.BatchProcessConfig;
import org.tallison.tika.app.fx.ctx.AppContext;

import org.apache.tika.utils.StringUtils;

public class AdvancedBatchController extends ControllerBase implements Initializable {
    /**
     * TODO -- we should add immediate responses if someone enters
     * a value that isn't parseable as an int or a long.
     * We're currently silently ignoring problems.  This is bad.
     */

    private static final Logger LOGGER = LogManager.getLogger(BatchInputController.class);
    static AppContext APP_CONTEXT = AppContext.getInstance();
    @FXML
    private ComboBox<String> digestOptions;

    @FXML
    private TextField parseTimeoutSeconds;
    @FXML
    private TextField memoryPerProcess;
    @FXML
    private TextField numProcesses;

    @FXML
    private TextField perFileEmitThresholdMb;

    @FXML
    private TextField totalEmitThresholdMb;

    @FXML
    private TextField emitWithinMs;

    @FXML
    private TextField writeLimit;

    @Override
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {

        if (APP_CONTEXT.getBatchProcessConfig().isEmpty()) {
            LOGGER.warn("batch process config is empty?!");
            return;
        }
        BatchProcessConfig batchProcessConfig = APP_CONTEXT.getBatchProcessConfig().get();
        if (batchProcessConfig.getDigest().isPresent()) {
            digestOptions.getSelectionModel().select(batchProcessConfig.getDigest().get());
        }
        parseTimeoutSeconds.setText(Integer.toString(batchProcessConfig.getParseTimeoutSeconds()));
        memoryPerProcess.setText(Integer.toString(batchProcessConfig.getMaxMemMb()));
        numProcesses.setText(Integer.toString(batchProcessConfig.getNumProcesses()));

        perFileEmitThresholdMb.setText(
                Integer.toString(batchProcessConfig.getPerFileEmitThresholdMb()));

        totalEmitThresholdMb.setText(Integer.toString(batchProcessConfig.getTotalEmitThesholdMb()));
        emitWithinMs.setText(Long.toString(batchProcessConfig.getEmitWithinMs()));
        if (batchProcessConfig.getWriteLimit() > -1) {
            writeLimit.setText(Long.toString(batchProcessConfig.getWriteLimit()));
        }
        writeLimit.focusedProperty().addListener((ov, oldV, newV) -> {
            if (! newV) {
                setWriteLimit();
            }
        });
    }

    public void setWriteLimit() {
        long writeLimitVal = -1l;
        if (! StringUtils.isBlank(writeLimit.getText())) {
            try {
                String txt = writeLimit.getText();
                txt = txt.replaceAll(",", "");
                BigInteger bigInt = new BigInteger(txt);
                writeLimitVal = bigInt.longValueExact();
                writeLimit.setText(txt);
            } catch (NumberFormatException e) {
                alert("Value needs to be a number", "Not a number",
                        ">" + writeLimit.getText() + "< is not parseable as number");
                writeLimit.setText("");
                return;
            } catch (ArithmeticException e) {
                alert("Value out of range", "Value out of range",
                        "\"" + writeLimit.getText() +
                                "\" must be less than 9,223,372,036,854,775,807");
                return;
            }
        }
        writeLimitVal = writeLimitVal < -1 ? -1 : writeLimitVal;
        APP_CONTEXT.getBatchProcessConfig().get().setWriteLimit(writeLimitVal);
        APP_CONTEXT.saveState();
    }

    public void saveAdvanced(ActionEvent actionEvent) {
        Optional<BatchProcessConfig> optionalBpc = APP_CONTEXT.getBatchProcessConfig();
        if (optionalBpc.isEmpty()) {
            actionEvent.consume();
            //log/warn
        }
        saveState();
    }


    public void saveState() {
        Optional<BatchProcessConfig> optionalBpc = APP_CONTEXT.getBatchProcessConfig();
        if (optionalBpc.isEmpty()) {
            LOGGER.warn("batch process config is empty during savestate?!");
            return;
        }
        BatchProcessConfig bpc = optionalBpc.get();

        //TODO -- allow multiple selections
        String selected = digestOptions.getSelectionModel().selectedItemProperty().get();
        bpc.setDigest(selected);

        int val = getInt("parseTimeoutSeconds", parseTimeoutSeconds, 0, 100000,
                bpc.getParseTimeoutSeconds());
        bpc.setParseTimeoutSeconds(val);

        val = getInt("memoryPerProcess", memoryPerProcess, 0, 10000000, bpc.getMaxMemMb());
        bpc.setMaxMemMb(val);

        val = getInt("numProcesses", numProcesses, 1, 100, bpc.getNumProcesses());
        bpc.setNumProcesses(val);

        val = getInt("perFileEmitThresholdMb", perFileEmitThresholdMb, 0, 1000000,
                bpc.getPerFileEmitThresholdMb());
        bpc.setPerFileEmitThresholdMb(val);

        val = getInt("totalEmitThresholdMb", totalEmitThresholdMb, 0, 1000000,
                bpc.getTotalEmitThesholdMb());
        bpc.setTotalEmitThesholdMb(val);

        long emitWithin = getLong("emitWithinMs", emitWithinMs, 0, 1000000000,
                bpc.getEmitWithinMs());
        bpc.setEmitWithinMs(emitWithin);

        APP_CONTEXT.saveState();
    }

    private int getInt(String label, TextField textField, int min, int max, int defaultVal) {

        String txt = textField.getText();
        if (StringUtils.isBlank(txt)) {
            return defaultVal;
        }
        int num = -1;
        try {
            num = Integer.parseInt(txt);
        } catch (NumberFormatException e) {
            //TODO -- alert
            return defaultVal;
        }
        if (num < min) {
            //TODO -- alert
            return defaultVal;
        }
        if (num > 1_000_000_000) {
            //TODO -- alert
            return defaultVal;
        }
        return num;
    }

    private long getLong(String label, TextField textField, long min, long max, long defaultVal) {

        String txt = textField.getText();
        if (StringUtils.isBlank(txt)) {
            return defaultVal;
        }
        long num = -1;
        try {
            num = Long.parseLong(txt);
        } catch (NumberFormatException e) {
            //TODO -- alert
            return defaultVal;
        }
        if (num < min) {
            //TODO -- alert
            return defaultVal;
        }
        if (num > max) {
            //TODO -- alert
            return defaultVal;
        }
        return num;
    }
}
