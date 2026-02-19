package com.jacob.po.service.mover;

import com.jacob.po.service.mover.cmd.CommandContext;
import com.jacob.po.service.mover.cmd.CommandHandler;
import com.jacob.po.service.mover.cmd.CommandRegistry;
import com.jacob.po.service.mover.config.YamlConfigFileLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link ImageMoverApplication} is a lightweight CLI-based asset organizing tool.
 *
 * <p>
 * It continuously monitors a configured "delivery car" directory using
 * {@link java.nio.file.WatchService} and allows users to:
 * </p>
 *
 * <ul>
 * <li>Batch move newly delivered image files to destination folders via command aliases</li>
 * <li>Quickly open configured target directories using system file explorer</li>
 * <li>Reload YAML-based command-to-path mappings at runtime</li>
 * </ul>
 *
 * @author Kotohiko
 * @since Jan 25, 2026
 */
@Slf4j
@Component
public class ImageMoverApplication {

    /**
     * The prompt string displayed to the user in the CLI.
     */
    private static final String PROMPT = ">> ";

    @Autowired
    private YamlConfigFileLoader yamlConfigFileLoader;

    @Autowired
    private CommandRegistry commandRegistry;

    @Value("${app.config.path}")
    private String whGuideConfigPath;

    /**
     * Starts the application.
     * <p>
     * Performs initial configuration loading, starts the background file system monitor,
     * and enters the main interactive command loop.
     * </p>
     */
    public void start() {
        yamlConfigFileLoader.load(whGuideConfigPath);
        String carPathStr = yamlConfigFileLoader.getDeliveryCarPath();

        if (carPathStr == null || carPathStr.isBlank()) {
            log.error("Startup failed: Configuration is missing or 'delivery_car' path is not defined.");
            return;
        }

        Path deliveryCarPath = Path.of(carPathStr);
        CommandContext context = new CommandContext(yamlConfigFileLoader);

        this.startBackgroundMonitor(deliveryCarPath);
        this.printWelcomeMessage();
        this.reportDeliveryCarCount();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            boolean running = true;
            while (running) {
                System.out.print(PROMPT);
                String input = reader.readLine();

                if (input == null || "exit".equalsIgnoreCase(input.trim()) || "quit".equalsIgnoreCase(input.trim())) {
                    running = false;
                    log.info("Terminating application...");
                } else {
                    this.processInput(input.trim(), context, deliveryCarPath);
                }
            }
        } catch (IOException e) {
            log.error("System I/O error occurred during the main execution loop: {}", e.getMessage());
        }
    }

    /**
     * Routes user input to the appropriate handler or internal command logic.
     *
     * @param command         The raw command string entered by the user.
     * @param context         The command context for execution.
     * @param deliveryCarPath The current delivery car path.
     */
    private void processInput(String command, CommandContext context, Path deliveryCarPath) {
        CommandHandler handler = commandRegistry.get(command);
        if (handler != null) {
            handler.execute(command, context);
        } else {
            this.processUserCommand(command, deliveryCarPath);
        }
    }

    /**
     * Handles built-in commands like 'reload' and 'open' or delegates to the move logic.
     *
     * @param input       The command string.
     * @param deliveryCar The path to the delivery directory.
     */
    private void processUserCommand(String input, Path deliveryCar) {
        if (input.isEmpty()) return;

        if ("reload".equalsIgnoreCase(input)) {
            log.info("Reloading configuration from source...");
            yamlConfigFileLoader.load(whGuideConfigPath);
            return;
        }

        if (input.toLowerCase().startsWith("open -s")) {
            this.handleOpenFolder(input.substring(7).trim());
        } else {
            this.handleMoveSequence(input, deliveryCar);
        }
    }

    /**
     * Scans the delivery directory and initiates a batch move if files are present.
     *
     * @param cmd         The command alias representing the destination.
     * @param deliveryCar The source directory to scan.
     */
    private void handleMoveSequence(String cmd, Path deliveryCar) {
        List<Path> files = scanDeliveryCar(deliveryCar);
        if (files.isEmpty()) {
            log.info("No files found in delivery car. Movement sequence aborted.");
            return;
        }
        this.handleBatchMove(files, cmd);
    }

    /**
     * Initializes and starts a daemon thread to watch for file creation events.
     *
     * @param path The directory path to monitor for changes.
     */
    private void startBackgroundMonitor(Path path) {
        Thread monitorThread = new Thread(() -> {
            try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
                path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

                while (!Thread.currentThread().isInterrupted()) {
                    WatchKey key = watchService.take();
                    this.processWatchEvents(key, watchService);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Background monitor service has been interrupted.");
            } catch (Exception e) {
                log.error("Background monitor encountered an unexpected failure: {}", e.getMessage());
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    /**
     * Processes events polled from the WatchService.
     *
     * @param key          The current active WatchKey.
     * @param watchService The WatchService instance for additional polling.
     */
    private void processWatchEvents(WatchKey key, WatchService watchService) {
        System.out.print("\r");
        boolean hasNewFile = false;

        WatchKey currentKey = key;
        while (currentKey != null) {
            for (WatchEvent<?> event : currentKey.pollEvents()) {
                if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                    System.out.println("[Notification] New asset detected: " + event.context());
                    hasNewFile = true;
                }
            }
            if (!currentKey.reset()) break;
            currentKey = watchService.poll();
        }

        if (hasNewFile) {
            this.reportDeliveryCarCount();
        }

        System.out.print(PROMPT);
        System.out.flush();
    }

    /**
     * Scans the directory for regular files (excluding subdirectories).
     *
     * @param deliveryCar The directory to scan.
     * @return A list of paths for the files found.
     */
    private List<Path> scanDeliveryCar(Path deliveryCar) {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(deliveryCar)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    files.add(entry);
                }
            }
        } catch (IOException e) {
            log.error("Could not scan delivery car: {}", e.getMessage());
        }
        return files;
    }

    /**
     * Moves a list of files to a destination mapped by the given command.
     *
     * @param files The list of files to be moved.
     * @param cmd   The alias mapped to the destination path.
     */
    private void handleBatchMove(List<Path> files, String cmd) {
        String targetDirStr = yamlConfigFileLoader.getDestPath(cmd);
        if (targetDirStr == null) {
            log.warn("Invalid alias [{}]: No mapping exists in configuration.", cmd);
            return;
        }

        Path targetDir = Path.of(targetDirStr);
        try {
            ensureDirectoryExists(targetDir);
            int count = 0;

            for (Path source : files) {
                Files.move(source, targetDir.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                count++;
            }
            log.info("Success: {} files moved to destination [{}]", count, targetDir.getFileName());
        } catch (IOException e) {
            log.error("File transfer failed: {}", e.getMessage());
        }
    }

    /**
     * Opens the destination folder in the OS file explorer.
     *
     * @param alias The mapping key for the folder path.
     */
    private void handleOpenFolder(String alias) {
        String pathStr = yamlConfigFileLoader.getDestPath(alias);
        if (pathStr == null) {
            log.warn("Cannot open folder: Alias [{}] not recognized.", alias);
            return;
        }

        try {
            File folder = new File(pathStr);
            ensureDirectoryExists(folder.toPath());
            Desktop.getDesktop().open(folder);
            log.info("System explorer triggered for path: {}", pathStr);
        } catch (Exception e) {
            log.error("Failed to launch system explorer: {} ({})", e.getMessage(), e.getClass().getSimpleName());
        }
    }

    /**
     * Logs and prints the current file count within the delivery car directory.
     */
    private void reportDeliveryCarCount() {
        String carPath = yamlConfigFileLoader.getDeliveryCarPath();
        if (carPath == null) return;

        List<Path> files = scanDeliveryCar(Path.of(carPath));
        System.out.print("[Status] Inventory Update - Current file count: " + files.size() + "\n");
    }

    /**
     * Ensures that the specified directory exists, creating it if necessary.
     *
     * @param path The directory path to check/create.
     * @throws IOException If directory creation fails.
     */
    private void ensureDirectoryExists(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    /**
     * Prints the application's visual welcome banner and command help to the console.
     */
    private void printWelcomeMessage() {
        String line = "=============================================================";
        System.out.println(line);
        System.out.println("              Gallery Organizer Engine Active");
        System.out.println(line);
        System.out.println("User Guide:");
        System.out.println("  [alias]           -> Batch move files to destination");
        System.out.println("  open -s [alias]   -> Open folder in file explorer");
        System.out.println("  reload            -> Refresh configuration mappings");
        System.out.println("  exit | quit       -> Terminate the application");
        System.out.println();
    }
}