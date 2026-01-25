package com.jacob.po.core.mover;

import com.jacob.po.core.common.config.YamlConfigFileLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Core organizer service that handles batch moving of image assets
 * and provides quick directory navigation via command aliases.
 * </p>
 *
 * @author tachi
 * @since 1/25/2026
 */
public class GalleryOrganizer {

    private static final Logger logger = LoggerFactory.getLogger(GalleryOrganizer.class);

    private static final String PROMPT = ">> ";

    private static final String WH_GUIDE_CONFIG_PATH = "po-core/src/main/resources/delivery-guide-config.yaml";

    private final YamlConfigFileLoader yamlConfigFileLoader;

    public GalleryOrganizer() {
        this.yamlConfigFileLoader = new YamlConfigFileLoader();
        this.yamlConfigFileLoader.load(WH_GUIDE_CONFIG_PATH);
    }

    public static void main(String[] args) {
        new GalleryOrganizer().start();
    }

    /**
     * Entry point of the application. Sets up monitor and command loop.
     */
    public void start() {
        Path deliveryCarPath = Paths.get(yamlConfigFileLoader.getDeliveryCarPath());

        this.startBackgroundMonitor(deliveryCarPath);
        this.printWelcomeMessage();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.print("\n" + PROMPT);
                String input = reader.readLine();
                if (input == null || "exit".equalsIgnoreCase(input.trim())) break;

                this.processUserCommand(input.trim(), deliveryCarPath);
            }
        } catch (IOException e) {
            logger.error("System I/O error: {}", e.getMessage());
        }
    }

    /**
     * Decides which action to take based on user input.
     *
     * @param input       The raw user input
     * @param deliveryCar The source deliveryCar path
     */
    private void processUserCommand(String input, Path deliveryCar) {
        if (input.isEmpty()) return;

        if ("reload".equalsIgnoreCase(input)) {
            yamlConfigFileLoader.load(WH_GUIDE_CONFIG_PATH);
            return;
        }

        if (input.toLowerCase().startsWith("open -s")) {
            this.handleOpenFolder(input.substring(7).trim());
        } else {
            this.handleMoveSequence(input, deliveryCar);
        }
    }

    /**
     * Coordinates the file scanning and moving process.
     */
    private void handleMoveSequence(String cmd, Path deliveryCar) {
        List<Path> files = scanDeliveryCar(deliveryCar);
        if (files.isEmpty()) {
            System.out.println("[Info] Delivery car is empty. No files to move.");
            return;
        }
        this.handleBatchMove(files, cmd);
    }

    /**
     * Configures and starts the WatchService daemon thread.
     */
    private void startBackgroundMonitor(Path path) {
        Thread monitorThread = new Thread(() -> {
            // Create the service once and ensure it closes automatically
            try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
                path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

                while (!Thread.currentThread().isInterrupted()) {
                    // Block until at least one event is available
                    WatchKey key = watchService.take();
                    // Pass the existing watchService to the processing method
                    this.processWatchEvents(key, watchService);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupted status
            } catch (Exception e) {
                logger.error("Background monitor failure: {}", e.getMessage());
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    /**
     * Drains events from the WatchKey and updates the console UI.
     * * @param key          The current active WatchKey
     *
     * @param watchService The existing WatchService instance to reuse
     */
    private void processWatchEvents(WatchKey key, WatchService watchService) {
        // 1. Move cursor to start of line to "hide" the prompt
        System.out.print("\r");

        WatchKey currentKey = key;
        while (currentKey != null) {
            for (WatchEvent<?> event : currentKey.pollEvents()) {
                if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                    System.out.println("[Notification] New asset arrived: " + event.context());
                }
            }

            // Reset the key and check if it's still valid
            if (!currentKey.reset()) break;

            // 2. IMPORTANT: Reuse the existing watchService to poll for more pending keys
            currentKey = watchService.poll();
        }

        // 3. Restore the prompt
        System.out.print(PROMPT);
        System.out.flush();
    }

    /**
     * Scans the deliveryCar directory for regular files.
     */
    private List<Path> scanDeliveryCar(Path deliveryCar) {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(deliveryCar)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) files.add(entry);
            }
        } catch (IOException e) {
            logger.error("Failed to scan delivery car: {}", e.getMessage());
        }
        return files;
    }

    /**
     * Executes the file moving logic.
     */
    private void handleBatchMove(List<Path> files, String cmd) {
        String targetDirStr = yamlConfigFileLoader.getDestPath(cmd);
        if (targetDirStr == null) {
            logger.warn("Invalid command [{}]: No mapping found in config.yaml", cmd);
            return;
        }

        Path targetDir = Paths.get(targetDirStr);
        try {
            ensureDirectoryExists(targetDir);
            int success = 0;

            for (Path source : files) {
                Files.move(source, targetDir.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                ++success;
            }
            logger.info("Batch move completed: {} files moved to [{}]", success, targetDir.getFileName());
        } catch (IOException e) {
            logger.error("File move operation failed: {}", e.getMessage());
        }
    }

    /**
     * Handles directory opening logic.
     */
    private void handleOpenFolder(String alias) {
        String pathStr = yamlConfigFileLoader.getDestPath(alias);
        if (pathStr == null) {
            logger.warn("Alias [{}] not found in configuration", alias);
            return;
        }

        try {
            File folder = new File(pathStr);
            ensureDirectoryExists(folder.toPath());
            Desktop.getDesktop().open(folder);
            logger.info("Opened directory: {}", pathStr);
        } catch (Exception e) {
            logger.error("Failed to open folder: {}", e.getMessage());
        }
    }

    private void ensureDirectoryExists(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    private void printWelcomeMessage() {
        System.out.println("=== Gallery Organizer Started (Monitoring via WatchService) ===");
        System.out.println("Commands: [alias] move | open -s [alias] view | reload sync | exit quit");
    }
}