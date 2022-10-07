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

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.tallison.tika.app.fx.ctx.AppContext;
import org.tallison.tika.app.fx.metadata.MetadataRow;

import org.apache.tika.utils.StringUtils;

public class MetadataController {
    AppContext APP_CONTEXT = AppContext.getInstance();


    @FXML
    private TableView<MetadataRow> tableView;
    @FXML private TextField tikaField;
    @FXML private TextField outputField;

    @FXML
    public void onTikaCommit(TableColumn.CellEditEvent cellEditEvent) {
        System.out.println("tika");
        APP_CONTEXT.getBatchProcessConfig().addTikaMetadata(
                cellEditEvent.getTablePosition().getRow(), cellEditEvent.getNewValue().toString());
    }

    @FXML
    public void onOutputCommit(TableColumn.CellEditEvent cellEditEvent) {
        System.out.println("output");
        APP_CONTEXT.getBatchProcessConfig().addOutputMetadata(
                cellEditEvent.getTablePosition().getRow(), cellEditEvent.getNewValue().toString());

    }
    @FXML
    protected void addMetadataRow(ActionEvent event) {
        ObservableList<MetadataRow> data = tableView.getItems();

        if (!StringUtils.isBlank(tikaField.getText()) && !StringUtils.isBlank(outputField.getText())) {
            data.add(new MetadataRow(tikaField.getText(), outputField.getText()));
            tikaField.setText("");
            outputField.setText("");
        }
    }

}
