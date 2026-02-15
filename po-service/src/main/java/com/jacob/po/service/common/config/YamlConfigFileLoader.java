package com.jacob.po.service.common.config;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

/**
 *
 * @author Kotohiko
 * @since Jan 25, 2026
 */
public class YamlConfigFileLoader {

    private Map<String, Object> config;

    public void load(String configPath) {
        try (InputStream input = new FileInputStream(configPath)) {
            Yaml yaml = new Yaml();
            this.config = yaml.load(input);
            System.out.println("[INFO] YamlConfigFileLoader.load() >>> Config file loaded successfully ");
        } catch (Exception e) {
            System.err.println("[ERROR] YamlConfigFileLoader.load() >>> 加载配置文件失败: " + e.getMessage());
        }
    }

    public String getDeliveryCarPath() {
        return (String) config.get("delivery_car");
    }

    @SuppressWarnings("unchecked")
    public String getDestPath(String command) {
        Map<String, String> mappings = (Map<String, String>) config.get("mappings");
        return mappings.get(command.toLowerCase());
    }
}