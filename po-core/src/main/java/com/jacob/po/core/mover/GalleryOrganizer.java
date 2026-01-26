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
 * {@link GalleryOrganizer} is a lightweight CLI-based asset organizing tool.
 *
 * <p>
 * It continuously monitors a configured "delivery car" directory using
 * {@link java.nio.file.WatchService} and allows users to:
 * </p>
 *
 * <ul>
 *   <li>Batch move newly delivered image files to destination folders via command aliases</li>
 *   <li>Quickly open configured target directories using system file explorer</li>
 *   <li>Reload YAML-based command-to-path mappings at runtime</li>
 * </ul>
 *
 * <p>
 * This class acts as the application entry point and command dispatcher.
 * </p>
 *
 * @author tachi
 * @since 1/25/2026
 */
public class GalleryOrganizer {

    /**
     * Logger for application lifecycle and error reporting
     */
    private static final Logger logger = LoggerFactory.getLogger(GalleryOrganizer.class);

    /**
     * Console prompt prefix
     */
    private static final String PROMPT = ">> ";

    /**
     * Absolute or project-relative path to the YAML configuration file
     * that defines command-to-destination mappings.
     */
    private static final String WH_GUIDE_CONFIG_PATH =
            "po-core/src/main/resources/delivery-guide-config.yaml";

    /**
     * YAML configuration loader responsible for resolving aliases
     * to actual filesystem paths.
     */
    private final YamlConfigFileLoader yamlConfigFileLoader;

    /**
     * Creates a GalleryOrganizer instance and loads the YAML configuration.
     *
     * <p>
     * The configuration file is loaded eagerly during construction.
     * </p>
     *
     * @throws RuntimeException if the configuration file cannot be loaded
     */
    public GalleryOrganizer() {
        this.yamlConfigFileLoader = new YamlConfigFileLoader();
        this.yamlConfigFileLoader.load(WH_GUIDE_CONFIG_PATH);
    }

    /**
     * Application entry point.
     *
     * @param args command-line arguments (currently unused)
     */
    public static void main(String[] args) {
        new GalleryOrganizer().start();
    }

    /**
     * Starts the application.
     *
     * <p>
     * This method initializes the background directory monitor,
     * prints the welcome banner, and enters the interactive
     * command-processing loop.
     * </p>
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
     * Parses and dispatches user input commands.
     *
     * <p>
     * Supported commands include:
     * </p>
     * <ul>
     *   <li>{@code reload} - Reload YAML configuration</li>
     *   <li>{@code open -s [alias]} - Open mapped directory in system explorer</li>
     *   <li>{@code [alias]} - Batch move files using alias mapping</li>
     * </ul>
     *
     * @param input       raw user input string
     * @param deliveryCar source directory containing incoming assets
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
     * Starts a daemon thread that monitors the given directory
     * for newly created files.
     *
     * <p>
     * The monitor uses {@link WatchService} and reports new arrivals
     * asynchronously to the console.
     * </p>
     *
     * @param path directory to monitor
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
     * Processes filesystem watch events and updates the console UI.
     *
     * <p>
     * This method drains all pending {@link WatchKey} events and
     * prints notifications for newly created files.
     * </p>
     *
     * @param key           active WatchKey returned by WatchService
     * @param watchService  watch service instance used to poll additional keys
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
     * Scans the delivery directory for regular files.
     *
     * @param deliveryCar directory to scan
     * @return list of regular files found in the directory
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
     * Moves all scanned files to the destination directory
     * resolved by the given command alias.
     *
     * @param files list of files to move
     * @param cmd   command alias mapped to a destination directory
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
     * Opens the directory associated with the given alias
     * using the system file explorer.
     *
     * @param alias directory alias defined in YAML configuration
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