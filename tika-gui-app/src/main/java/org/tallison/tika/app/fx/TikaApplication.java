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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.tallison.tika.app.fx.ctx.AppContext;
import org.tallison.tika.app.fx.tools.BatchProcess;

public class TikaApplication extends Application {

    private static final AppContext APP_CONTEXT = AppContext.getInstance();

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(TikaApplication.class.getResource("tika-view.fxml"));
        VBox dragTarget = new VBox();
        StackPane root = new StackPane();
        root.getChildren().add(dragTarget);
        Scene scene = new Scene(fxmlLoader.load());

        stage.setTitle("Apache Tika Appv2");
        stage.setScene(scene);
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent windowEvent) {
                onClose(windowEvent);
            }
        });
        stage.show();
    }

    private void onClose(WindowEvent windowEvent) {

        AtomicBoolean close = new AtomicBoolean(true);
        if (APP_CONTEXT.getBatchProcess().isPresent()) {
            BatchProcess batchProcess = APP_CONTEXT.getBatchProcess().get();
            if (batchProcess.getMutableStatus().get() == BatchProcess.STATUS.RUNNING) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("A batch process is still running");
                alert.setContentText("Cancel the process and quit?");
                ButtonType okButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
                ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);
                //ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(okButton, noButton);
                alert.showAndWait().ifPresent(type -> {
                    if (type.getText().startsWith("Yes")) {
                        close.set(true);
                    } else if (type.getText().startsWith("No")) {
                        close.set(false);
                    }
                });
            }
        }
        //}
        if (close.get()) {
            AppContext.getInstance().close();
            System.exit(0);
        } else {
            //go back to where we were
            windowEvent.consume();
        }

    }

    @Override
    public void stop() {
        AppContext.getInstance().close();
    }
}
