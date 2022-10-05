package org.tallison.tika.app.fx.tools;

import com.fasterxml.jackson.annotation.JsonSetter;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.tallison.tika.app.fx.Constants;
import org.tallison.tika.app.fx.ctx.AppContext;

import org.apache.tika.utils.ProcessUtils;

public class BatchProcessConfig {

    private ConfigItem pipesIterator;
    private ConfigItem fetcher;
    private ConfigItem emitter;
    private ConfigItem metadataMapper;

    private StringProperty fetcherLabel = new SimpleStringProperty("Unselected");
    private StringProperty emitterLabel = new SimpleStringProperty("Unselected");

    public ConfigItem getPipesIterator() {
        return pipesIterator;
    }

    @JsonSetter
    public void setPipesIterator(ConfigItem pipesIterator) {
        this.pipesIterator = pipesIterator;
    }

    public void setPipesIterator(String ... args) {
        setPipesIterator(ConfigItem.build(args));
    }

    public ConfigItem getFetcher() {
        return fetcher;
    }

    public StringProperty getFetcherLabel() {
        return fetcherLabel;
    }

    public void setFetcherLabel(String label) {
        fetcherLabel.setValue(label);
    }
    @JsonSetter
    public void setFetcher(ConfigItem fetcher) {
        this.fetcher = fetcher;
        this.fetcherLabel.setValue(fetcher.getClazz());
    }

    public void setEmitterLabel(String label) {
        emitterLabel.setValue(label);
    }

    public StringProperty getEmitterLabel() {
        return emitterLabel;
    }
    public void setFetcher(String ... args) {
        setFetcher(ConfigItem.build(args));
    }

    public ConfigItem getEmitter() {
        return emitter;
    }

    @JsonSetter
    public void setEmitter(ConfigItem emitter) {
        this.emitter = emitter;
    }

    public void setEmitter(String ... args) {
        setEmitter(ConfigItem.build(args));
    }

    public ConfigItem getMetadataMapper() {
        return metadataMapper;
    }

    @JsonSetter
    public void setMetadataMapper(ConfigItem metadataMapper) {
        this.metadataMapper = metadataMapper;
    }

    public void appendPipesClasspath(StringBuilder sb) {
        //TODO -- build this out
        if (getEmitter().getClazz().equals(Constants.FS_EMITTER_CLASS)) {
            sb.append(
                    ProcessUtils.escapeCommandLine(
                            AppContext.TIKA_LIB_PATH.resolve("tika-emitter-fs").toAbsolutePath() + "/*"));
        }
    }
}
