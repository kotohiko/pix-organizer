package com.jacob.po.client.mover;

import com.jacob.po.service.mover.ImageMoverApplication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Entry point runner that triggers the ImageMoverApplication logic after Spring context initialization.
 *
 * @author Kotohiko
 * @since Feb 13, 2026
 */
@Slf4j
@Component
public class ImageMoverRunner implements ApplicationRunner {

    @Autowired
    private ImageMoverApplication app;

    /**
     * Executes the application.
     * Uses ApplicationArguments to provide insights into startup parameters,
     * effectively neutralizing IDE "unused parameter" warnings.
     *
     * @param args command line arguments passed to the application.
     */
    @Override
    public void run(ApplicationArguments args) {
        // 1. Use args to eliminate "unused" warnings and log startup info
        if (args.getSourceArgs().length > 0) {
            log.info("Application started with arguments: {}", Arrays.toString(args.getSourceArgs()));
        }

        // 2. Example: You could check for a specific option like --debug
        if (args.containsOption("debug")) {
            log.info("Debug mode detected via command line arguments.");
        }

        app.start();
    }
}