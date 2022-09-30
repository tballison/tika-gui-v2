package org.tallison.tika.app.fx.tools;

import java.util.HashMap;
import java.util.Map;

public class ConfigItem {

    public static ConfigItem build(String... args) {
        Map<String, String> params = new HashMap<>();
        for (int i = 1; i < args.length; i++) {
            params.put(args[i], args[++i]);
        }
        return new ConfigItem(args[0], params);
    }

    private String clazz;
    private Map<String, String> attributes;

    public ConfigItem(String clazz, Map<String, String> attributes) {
        this.clazz = clazz;
        this.attributes = attributes;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public void setAttributes(Map<String, String> attrs) {
        this.attributes = attrs;
    }

    public String getClazz() {
        return clazz;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        return "ConfigItem{" + "clazz='" + clazz + '\'' + ", attributes=" + attributes + '}';
    }

}
