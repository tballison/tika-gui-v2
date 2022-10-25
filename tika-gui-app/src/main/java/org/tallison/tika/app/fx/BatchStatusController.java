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

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;
import org.tallison.tika.app.fx.ctx.AppContext;
import org.tallison.tika.app.fx.status.StatusCount;
import org.tallison.tika.app.fx.tools.BatchProcess;

import org.apache.tika.pipes.PipesResult;
import org.apache.tika.pipes.async.AsyncStatus;
import org.apache.tika.pipes.pipesiterator.TotalCountResult;

public class BatchStatusController implements Initializable {

    private static Map<String, PipesResult.STATUS> PIPES_STATUS_LOOKUP = new HashMap<>();

    static {
        Arrays.stream(PipesResult.STATUS.values()).forEach(
                s -> PIPES_STATUS_LOOKUP.put(s.name(), s));
    }
    @FXML
    PieChart statusPieChart;

    @FXML
    TextField totalToProcess;

    @FXML
    TextField totalProcessed;

    @FXML
    TextField overallStatus;

    @FXML
    TableColumn countColumn;

    @FXML
    TableView statusTable;

    @FXML
    private final ObservableList<StatusCount> statusCounts = FXCollections.observableArrayList();


    public ObservableList<StatusCount> getStatusCounts() {
        return statusCounts;
    }


    private Thread updaterThread;

    private final Label pieSliceCaption = new Label("");


    ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        pieSliceCaption.setTextFill(Color.DARKORANGE);
        pieSliceCaption.setStyle("-fx-font: 24 arial;");
        statusTable.getSortOrder().add(countColumn);
        countColumn.setSortType(TableColumn.SortType.DESCENDING);
        countColumn.setCellFactory(
                TextFieldTableCell.forTableColumn(new MyDoubleStringConverter()));
        updaterThread = new Thread(new Updater());
        updaterThread.setDaemon(true);
        updaterThread.start();
    }

    public void stop() {
        updaterThread.interrupt();
    }

    private class Updater implements Runnable {
        @Override
        public void run() {
            statusPieChart.setData(pieChartData);
            AppContext appContext = AppContext.getInstance();
            overallStatus.setText("Running");
            while (true) {
                Optional<BatchProcess> batchProcess = appContext.getBatchProcess();

                if (batchProcess.isPresent()) {
                    final Optional<AsyncStatus> status = batchProcess.get().checkStatus();

                    if (! status.isEmpty()) {
                        Platform.runLater(() -> {
                            updatePieChart(status.get());
                            updateTotalToProcess(status.get());
                        });

                        if (status.get().getAsyncStatus()  == AsyncStatus.ASYNC_STATUS.COMPLETED) {
                            overallStatus.setText("COMPLETED");
                            return;
                        }
                    }
                    statusTable.sort();
                    statusTable.refresh();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }
            }

        }

        private void updateTotalToProcess(AsyncStatus status) {
            TotalCountResult r = status.getTotalCountResult();
            String cntString = Long.toString(r.getTotalCount());
            if (r.getStatus() == TotalCountResult.STATUS.NOT_COMPLETED) {
                cntString += " (so far)";
            }
            totalToProcess.setText(cntString);
        }

        private void updatePieChart(AsyncStatus status) {

            long totalCount = status.getTotalCountResult().getTotalCount();
            long processed = countProcessed(status);
            long unprocessed = 0;
            if (totalCount > processed) {
                unprocessed = totalCount - processed;
            }
            if (pieChartData.size() == 0) {
                addData("UNPROCESSED", unprocessed);
            }
            Set<PipesResult.STATUS> seen = new HashSet<>();
            for (PieChart.Data d : pieChartData) {
                String name = d.nameProperty().get();
                if (name.equals("UNPROCESSED")) {
                    d.setPieValue(unprocessed);
                } else {
                    PipesResult.STATUS s = lookup(name);
                    if (s != null) {
                        Long value = status.getStatusCounts().get(s);
                        if (value != null) {
                            d.setPieValue(value);
                        }
                        seen.add(s);
                    }
                }
            }
            for (Map.Entry<PipesResult.STATUS, Long> e : status.getStatusCounts().entrySet()) {
                if (! seen.contains(e.getKey())) {
                    addData(e.getKey().name(), e.getValue());
                }
            }
            totalProcessed.setText(Long.toString(processed));
        }

        private void addData(String name, double value) {
            PieChart.Data data = new PieChart.Data(name, value);
            pieChartData.add(data);

            data.getNode().addEventHandler(MouseEvent.MOUSE_PRESSED,
                    new EventHandler<MouseEvent>() {
                        @Override public void handle(MouseEvent e) {
                            pieSliceCaption.setTranslateX(e.getSceneX());
                            pieSliceCaption.setTranslateY(e.getSceneY());
                            pieSliceCaption.setText(String.valueOf(data.getPieValue()) + "%");
                        }
                    });
            StatusCount statusCount = new StatusCount(name, value);
            statusCount.countProperty().bind(data.pieValueProperty());
            statusCounts.add(statusCount);
        }
        private PipesResult.STATUS lookup(String name) {

            if (! PIPES_STATUS_LOOKUP.containsKey(name)) {
                return null;
            }
            return PIPES_STATUS_LOOKUP.get(name);
        }

        private long countProcessed(AsyncStatus status) {
            return status.getStatusCounts().values().stream().reduce(0l, Long::sum);
        }
    }

    private class MyDoubleStringConverter extends StringConverter<Double> {

        @Override
        public String toString(Double aDouble) {
            return String.format(Locale.US, "%.0f", aDouble);
        }

        @Override
        public Double fromString(String s) {
            return Double.parseDouble(s);
        }
    }
}
