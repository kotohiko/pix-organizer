package com.jacob.po.service.mover.cmd.impl;

import com.jacob.po.service.mover.cmd.CommandContext;
import com.jacob.po.service.mover.cmd.CommandHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Implementation of the {@link CommandHandler} that checks the status of the delivery directory.
 * Specifically, it counts the number of regular files within the configured delivery car path.
 *
 * @author Kotohiko
 * @since Jan 25, 2026
 */
@Slf4j
public class CheckCommand implements CommandHandler {

    /**
     * Executes the check command.
     * Iterates through the directory provided by the context and logs the total file count.
     *
     * @param input   The raw input string from the user (unused in this implementation).
     * @param context The command context containing environmental configuration like the delivery path.
     */
    @Override
    public void execute(String input, CommandContext context) {
        Path path = context.getDeliveryCarPath();

        if (path == null) {
            log.error("Execution failed: Delivery car path is null.");
            return;
        }

        if (!Files.exists(path) || !Files.isDirectory(path)) {
            log.error("Execution failed: Path does not exist or is not a directory: {}", path);
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            int count = 0;
            for (Path p : stream) {
                if (Files.isRegularFile(p)) {
                    count++;
                }
            }
            log.info("Status check completed. Current file count in delivery car: {}", count);
        } catch (IOException e) {
            log.error("An error occurred while accessing the directory stream: {}", e.getMessage());
        }
    }
}