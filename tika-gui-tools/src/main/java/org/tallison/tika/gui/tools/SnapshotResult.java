package org.tallison.tika.gui.tools;

import java.util.List;
import java.util.Map;

public class SnapshotResult {

    private String version;
    private String jarUrl;
    private Map<String, String> digests;

    public SnapshotResult(String version, String jarUrl, Map<String, String> digests) {
        this.version = version;
        this.jarUrl = jarUrl;
        this.digests = digests;
    }

    public String getUrl() {
        return jarUrl;
    }

    public Map<String, String> getDigests() {
        return digests;
    }

    @Override
    public String toString() {
        return "SnapshotResult{" + "version='" + version + '\'' + ", jarUrl='" + jarUrl + '\'' +
                ", digests=" + digests + '}';
    }
}
