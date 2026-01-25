package com.jacob.po.core.common.config;

/**
 * <p>
 *
 * </p>
 *
 * @author tachi
 * @since 1/25/2026
 */

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

public class YamlConfigFileLoader {

    private Map<String, Object> config;

    public void load(String configPath) {
        try (InputStream input = new FileInputStream(configPath)) {
            Yaml yaml = new Yaml();
            this.config = yaml.load(input);
            System.out.println(">>> 配置文件加载成功");
        } catch (Exception e) {
            System.err.println("加载配置文件失败: " + e.getMessage());
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