package com.jacob.po.service.mover.cmd;

import com.jacob.po.service.mover.config.YamlConfigFileLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;

/**
 * Provides the execution context for command handlers.
 * This class acts as a bridge between the configuration loader and the commands,
 * offering access to environmental settings like file paths.
 *
 * @author Kotohiko
 * @since Feb 17, 2026
 */
@Slf4j
public class CommandContext {

    /**
     * The loader responsible for reading YAML configuration files.
     */
    @Autowired
    private YamlConfigFileLoader configLoader;

    /**
     * Constructs a new CommandContext with the specified configuration loader.
     *
     * @param configLoader The loader to be used for accessing configuration data.
     */
    public CommandContext(YamlConfigFileLoader configLoader) {
        this.configLoader = configLoader;
    }

    /**
     * Retrieves the delivery car path as a {@link Path} object.
     *
     * @return The {@link Path} representation of the delivery car directory,
     * or null if the configuration is missing or invalid.
     */
    public Path getDeliveryCarPath() {
        String pathStr = configLoader.getDeliveryCarPath();
        if (pathStr == null || pathStr.isBlank()) {
            log.warn("Delivery car path is not defined in the configuration.");
            return null;
        }
        return Path.of(pathStr);
    }

    /**
     * Triggers a reload of the configuration from the specified file path.
     *
     * @param configPath The path to the YAML configuration file to load.
     */
    @SuppressWarnings("unused")
    public void reloadConfig(String configPath) {
        if (configPath == null || configPath.isBlank()) {
            log.error("Cannot reload configuration: Provided path is null or empty.");
            return;
        }
        log.info("Reloading configuration from: {}", configPath);
        configLoader.load(configPath);
    }
}