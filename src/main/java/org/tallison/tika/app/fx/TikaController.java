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

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.apache.tika.pipes.fetcher.fs.FileSystemFetcher;
import org.apache.tika.pipes.pipesiterator.fs.FileSystemPipesIterator;

public class TikaController {

    static AppState APP_STATE = AppState.load();
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to Apache Tika App v2!");
    }

    public void handleAboutAction(ActionEvent actionEvent) {
    }

    public void handleKeyInput(KeyEvent keyEvent) {

    }

    public void exitApp(ActionEvent actionEvent) {
        APP_STATE.close();
        System.exit(0);
    }

    public void configureTikaApp(ActionEvent actionEvent) {
        alertNoTikaApp();
    }

    public void alertNoTikaApp() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Download Tika App");
        alert.setHeaderText("Download Tika App");
        alert.setContentText("Are you ok with this?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK){
            try {
                ResourceDownloader.downloadTikaAppJar(APP_STATE.getTikaVersion(), APP_STATE.getTikaAppBinPath());
            } catch (IOException e) {
                //TODO handle failure
            }
            Alert alert2 = new Alert(Alert.AlertType.INFORMATION);
            alert2.setTitle("Download success!");
            alert2.setHeaderText("Download success!");
            alert2.showAndWait();
        } else {
            Alert alert2 = new Alert(Alert.AlertType.INFORMATION);
            alert2.setTitle("Required Download");
            alert2.setHeaderText("Required Download");
            alert2.setContentText("You'll need to download the tika-app jar from xyz and place it" +
                    " here: " + APP_STATE.getTikaAppBinPath());

            alert2.showAndWait();
        }
    }

    public void resetState(ActionEvent actionEvent) {
        APP_STATE.reset();
    }

    public void saveState(ActionEvent actionEvent) {
        try {
            APP_STATE.saveState();
        } catch (IOException e) {
            //TODO something
        }
    }

    public void configureFileSystemFetcher(ActionEvent actionEvent) {
        final Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        //dialog.initOwner(primaryStage);
        VBox dialogVbox = new VBox(20);
        dialogVbox.getChildren().add(new Text("This is a Dialog"));
        Scene dialogScene = new Scene(dialogVbox, 300, 200);
        dialog.setScene(dialogScene);
        dialog.show();
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Open Resource File");
        File directory = directoryChooser.showDialog(dialog);
        APP_STATE.setFetcher(FileSystemFetcher.class.getName(), "basePath",
                directory.toPath().toAbsolutePath().toString());
        APP_STATE.setPipesIterator(
                FileSystemPipesIterator.class.getName(),
                "basePath",
                directory.toPath().toAbsolutePath().toString());
    }

    public void configureFileSystemEmitter(ActionEvent actionEvent) {
        final Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        //dialog.initOwner(primaryStage);
        VBox dialogVbox = new VBox(20);
        dialogVbox.getChildren().add(new Text("This is a Dialog"));
        Scene dialogScene = new Scene(dialogVbox, 300, 200);
        dialog.setScene(dialogScene);
        dialog.show();
        DirectoryChooser directoryChooser = new DirectoryChooser();

        directoryChooser.setTitle("Open Resource File");
        File directory = directoryChooser.showDialog(dialog);
        //TODO: pop up if no filesystem emitter jar?
        APP_STATE.setEmitter("org.apache.tika.pipes.emitter.fs.FileSystemEmitter", "basePath",
                directory.toPath().toAbsolutePath().toString());
    }

    public void runTika(ActionEvent actionEvent) {
        //serialize the config to a tika-config.xml
        //kick off a tika task

    }
}