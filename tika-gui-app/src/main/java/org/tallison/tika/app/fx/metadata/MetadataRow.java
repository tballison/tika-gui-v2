package org.tallison.tika.app.fx.metadata;

import javafx.beans.property.SimpleStringProperty;

public class MetadataRow {

    private final SimpleStringProperty tika = new SimpleStringProperty("");
    private final SimpleStringProperty output = new SimpleStringProperty("");

    public MetadataRow() {
    }

    public MetadataRow(String tikaVal, String outputVal) {
        tika.set(tikaVal);
        output.set(outputVal);
    }

    public String getTika() {
        return tika.get();
    }


    public void setTika(String tika) {
        this.tika.set(tika);
    }

    public String getOutput() {
        return output.get();
    }

    public void setOutput(String output) {
        this.output.set(output);
    }
}
