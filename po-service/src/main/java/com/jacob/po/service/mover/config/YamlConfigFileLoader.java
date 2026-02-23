package com.jacob.po.service.mover.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

/**
 * Utility class for loading and parsing YAML configuration files.
 * Provides specific methods to retrieve configuration properties like delivery car paths
 * and command-to-destination mappings.
 *
 * @author Kotohiko
 * @since Jan 25, 2026
 */
@Component
@Slf4j
public class YamlConfigFileLoader {

    private Map<Object, Object> config = Collections.emptyMap();

    /**
     * Loads or reloads YAML configuration.
     */
    public synchronized void load(String configPath) {
        File file = new File(configPath);
        if (!file.exists()) {
            log.error("Configuration file not found: {}", file.getAbsolutePath());
            return;
        }

        try (InputStream input = new FileInputStream(file)) {
            Yaml yaml = new Yaml();
            Map<String, Object> loaded = yaml.load(input);

            if (loaded == null) {
                log.warn("YAML file is empty.");
                loaded = Collections.emptyMap();
            }

            this.config = Collections.unmodifiableMap(loaded);

            log.info("Configuration loaded successfully.");
            log.info("Config identity hash: {}", System.identityHashCode(config));

        } catch (Exception e) {
            log.error("Failed to parse YAML file: {}", e.getMessage());
        }
    }

    /**
     * Get delivery_car path dynamically.
     */
    public String getDeliveryCarPath() {
        Object value = config.get("delivery_car");
        return value != null ? value.toString() : null;
    }

    /**
     * Get destination path by alias dynamically.
     */
    @SuppressWarnings("unchecked")
    public String getDestPath(String alias) {

        Object mappingsObj = config.get("mappings");
        if (!(mappingsObj instanceof Map)) {
            return null;
        }

        Map<String, Object> mappings = (Map<String, Object>) mappingsObj;

        Object value = mappings.get(alias);
        return value != null ? value.toString() : null;
    }
}
