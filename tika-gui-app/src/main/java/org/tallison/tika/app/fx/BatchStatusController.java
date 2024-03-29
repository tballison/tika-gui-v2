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

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
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
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.tallison.tika.app.fx.batch.BatchProcess;
import org.tallison.tika.app.fx.ctx.AppContext;
import org.tallison.tika.app.fx.status.StatusCount;

import org.apache.tika.pipes.PipesResult;
import org.apache.tika.pipes.async.AsyncStatus;
import org.apache.tika.pipes.pipesiterator.TotalCountResult;

public class BatchStatusController implements Initializable {

    private static final Map<String, PipesResult.STATUS> PIPES_STATUS_LOOKUP = new HashMap<>();
    private static final String UNPROCESSED_COLOR = "0066cc";
    private static final Map<PipesResult.STATUS, String> COLORS =
            Map.of(PipesResult.STATUS.PARSE_SUCCESS, "009900",
                    PipesResult.STATUS.PARSE_SUCCESS_WITH_EXCEPTION, "ffff00",
                    PipesResult.STATUS.EMIT_SUCCESS, "009900", PipesResult.STATUS.TIMEOUT, "ff9900",
                    PipesResult.STATUS.UNSPECIFIED_CRASH, "ff0000", PipesResult.STATUS.OOM,
                    "ff8000", PipesResult.STATUS.CLIENT_UNAVAILABLE_WITHIN_MS, "",
                    PipesResult.STATUS.INTERRUPTED_EXCEPTION, "", PipesResult.STATUS.EMPTY_OUTPUT,
                    "ffe6cc", PipesResult.STATUS.PARSE_EXCEPTION_EMIT, ""
                    //TODO -- fill out rest?
            );

    static {
        Arrays.stream(PipesResult.STATUS.values())
                .forEach(s -> PIPES_STATUS_LOOKUP.put(s.name(), s));
    }

    @FXML
    private final ObservableList<StatusCount> statusCounts = FXCollections.observableArrayList();

    private final Label pieSliceCaption = new Label("");

    @FXML
    Pane statusPane;
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
    ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

    private final Timeline timeline;

    public BatchStatusController() {
        timeline = new Timeline(new KeyFrame(Duration.millis(100), new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                updateWindow();
            }

        }));
        timeline.setCycleCount(Animation.INDEFINITE);
    }

    public ObservableList<StatusCount> getStatusCounts() {
        return statusCounts;
    }

    @Override
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        //TODO: some of this can't be in the constructor because
        //the objects are created at fxml load time
        pieSliceCaption.setTextFill(Color.DARKORANGE);
        pieSliceCaption.setStyle("-fx-font: 16 arial;");

        statusTable.getSortOrder().add(countColumn);
        countColumn.setSortType(TableColumn.SortType.DESCENDING);
        countColumn.setCellFactory(
                TextFieldTableCell.forTableColumn(new MyDoubleStringConverter()));

        statusPieChart.setLegendVisible(false);
        statusPieChart.setData(pieChartData);
        statusPieChart.setLabelsVisible(true);
        statusPieChart.setAnimated(true);
        overallStatus.setText("Running");
        statusPane.getChildren().add(pieSliceCaption);
        timeline.play();
    }


    public void stop() {
        updateStatusTable();
        timeline.stop();
    }

    public void updateWindow() {
        AppContext appContext = AppContext.getInstance();
        if (appContext == null) {
            return;
        }

        Optional<BatchProcess> batchProcess = appContext.getBatchProcess();

        if (batchProcess.isPresent()) {
            final Optional<AsyncStatus> status = batchProcess.get().checkAsyncStatus();

            if (!status.isEmpty()) {
                updatePieChart(status.get());
                updateTotalToProcess(status.get());
                updateStatusTable();
            }
            BatchProcess.STATUS batchProcessStatus = batchProcess.get().getMutableStatus().get();
            overallStatus.setText(batchProcessStatus.name());
            if (batchProcessStatus != BatchProcess.STATUS.RUNNING) {
                stop();
            }
        }
    }

    private void updateStatusTable() {
        //remove 0 entries
        statusCounts.removeIf(e -> e.getCount() < 0.1);
        statusTable.sort();
        statusTable.refresh();
    }


    private void updateTotalToProcess(AsyncStatus status) {
        TotalCountResult r = status.getTotalCountResult();
        long total = r.getTotalCount();
        long processed = countProcessed(status);
        //if someone is adding files to a directory after the total count
        //has been calculated
        if (total < processed) {
            total = processed;
        }
        String cntString = Long.toString(total);
        if (r.getStatus() == TotalCountResult.STATUS.NOT_COMPLETED) {
            cntString += " (so far)";
        }
        totalToProcess.setText(cntString);
    }

    private void updatePieChart(AsyncStatus status) {

        long totalCount = status.getTotalCountResult().getTotalCount();
        long processed = countProcessed(status);
        //if someone is adding files to a directory after the total count
        //has been calculated
        if (totalCount < processed) {
            totalCount = processed;
        }
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
            if (!seen.contains(e.getKey())) {
                addData(e.getKey().name(), e.getValue());
            }
        }
        totalProcessed.setText(Long.toString(processed));
    }

    private void addData(String name, double value) {
        PieChart.Data data = new PieChart.Data(name, value);
        pieChartData.add(data);
        if (name.equals("UNPROCESSED")) {
            data.getNode().setStyle("-fx-pie-color: #" + UNPROCESSED_COLOR + "; -fx-pie-label-visible: true;");
        } else {
            PipesResult.STATUS status = lookup(name);
            String color = COLORS.getOrDefault(status, "");
            if (color.length() > 0) {
                data.getNode().setStyle("-fx-pie-color: #" + color + "; -fx-pie-label-visible: true;");
            }
        }

        data.getNode().addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                double sum = 0.0;
                for (PieChart.Data d : pieChartData) {
                    sum += d.pieValueProperty().getValue();
                }
                if (sum > 0) {
                    pieSliceCaption.setTranslateX(e.getSceneX());
                    pieSliceCaption.setTranslateY(e.getSceneY());
                    pieSliceCaption.setText(String.format(Locale.US,
                            data.getName() + ": " +
                            "%.0f%%", 100 * data.getPieValue() / sum));
                }
            }
        });

        StatusCount statusCount = new StatusCount(name, value);
        statusCount.countProperty().bind(data.pieValueProperty());
        statusCounts.add(statusCount);
    }

    private PipesResult.STATUS lookup(String name) {

        if (!PIPES_STATUS_LOOKUP.containsKey(name)) {
            return null;
        }
        return PIPES_STATUS_LOOKUP.get(name);
    }

    private long countProcessed(AsyncStatus status) {
        return status.getStatusCounts().values().stream().reduce(0L, Long::sum);
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
