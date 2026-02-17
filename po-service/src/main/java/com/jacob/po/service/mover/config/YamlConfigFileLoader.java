package com.jacob.po.service.mover.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
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

    /**
     * Internal map holding the parsed YAML data.
     */
    private Map<String, Object> config;

    /**
     * Loads the YAML configuration from the specified file path.
     *
     * @param configPath The absolute or relative path to the .yaml file.
     */
    public void load(String configPath) {
        // Check if the file exists before attempting to load
        File file = new File(configPath);
        if (!file.exists()) {
            log.error("Configuration file not found: {}", file.getAbsolutePath());
            return;
        }

        try (InputStream input = new FileInputStream(file)) {
            Yaml yaml = new Yaml();
            this.config = yaml.load(input);
            log.info("Configuration file loaded successfully: {}", configPath);
        } catch (Exception e) {
            log.error("Failed to parse YAML file: {}", e.getMessage());
        }
    }

    /**
     * Retrieves the delivery car path from the configuration.
     *
     * @return The path string associated with the 'delivery_car' key, or null if not loaded.
     */
    public String getDeliveryCarPath() {
        // config might be null if load() failed or wasn't called
        return (config != null) ? (String) config.get("delivery_car") : null;
    }

    /**
     * Retrieves the destination path associated with a specific command.
     * The command is case-insensitive as it is converted to lowercase before lookup.
     *
     * @param command The command key to look up in the mappings.
     * @return The corresponding destination path, or null if not found.
     */
    @SuppressWarnings("unchecked")
    public String getDestPath(String command) {
        if (config == null || command == null) {
            return null;
        }
        Map<String, String> mappings = (Map<String, String>) config.get("mappings");
        return mappings != null ? mappings.get(command.toLowerCase()) : null;
    }
}