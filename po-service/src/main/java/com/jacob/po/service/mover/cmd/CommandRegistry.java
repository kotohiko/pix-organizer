package com.jacob.po.service.mover.cmd;

import com.jacob.po.service.mover.cmd.impl.CheckCommand;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for command handlers.
 * This component manages the mapping between command strings and their
 * respective {@link CommandHandler} implementations.
 *
 * @author Kotohiko
 * @since Feb 17, 2026
 */
@Slf4j
@Component
public class CommandRegistry {

    /**
     * Internal storage for command mappings.
     * Uses {@link ConcurrentHashMap} for thread safety and to prevent IDEA warnings
     * regarding potential mutation issues in a multithreaded Spring context.
     */
    private final Map<String, CommandHandler> commandMap = new ConcurrentHashMap<>();

    /**
     * Initializes the registry with default command mappings.
     * Uses {@link PostConstruct} to ensure all dependencies are ready before registration.
     */
    @PostConstruct
    public void init() {
        CheckCommand checkCommand = new CheckCommand();

        this.register("check", checkCommand);
        this.register("list", checkCommand);
        this.register("ls", checkCommand);

        log.info("CommandRegistry initialized with {} commands.", commandMap.size());
    }

    /**
     * Registers a new command handler with a specific name.
     * The name is automatically converted to lowercase to ensure case-insensitive lookup.
     *
     * @param name    The trigger string for the command.
     * @param handler The implementation of {@link CommandHandler} to execute.
     */
    private void register(String name, CommandHandler handler) {
        if (name == null || handler == null) {
            log.warn("Attempted to register a null command name or handler.");
            return;
        }
        commandMap.put(name.toLowerCase(), handler);
    }

    /**
     * Retrieves the command handler associated with the given name.
     *
     * @param name The name of the command to look up.
     * @return The corresponding {@link CommandHandler}, or null if no mapping exists.
     */
    public CommandHandler get(String name) {
        if (name == null) {
            return null;
        }
        return commandMap.get(name.toLowerCase());
    }
}