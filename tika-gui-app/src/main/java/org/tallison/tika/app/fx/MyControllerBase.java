package org.tallison.tika.app.fx;

import javafx.scene.control.Alert;
import javafx.scene.layout.Region;

public class MyControllerBase {

    public void alert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setResizable(true);
        alert.setContentText(content);
        alert.getDialogPane().setMinWidth(500);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }
}
