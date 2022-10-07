package org.tallison.tika.app.fx;

import javafx.collections.ObservableArray;
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
