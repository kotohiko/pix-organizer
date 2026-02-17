package com.jacob.po.service.mover;

import com.jacob.po.service.mover.config.YamlConfigFileLoader;
import com.jacob.po.service.mover.cmd.CommandContext;
import com.jacob.po.service.mover.cmd.CommandHandler;
import com.jacob.po.service.mover.cmd.CommandRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 *   <li>Batch move newly delivered image files to destination folders via command aliases</li>
 *   <li>Quickly open configured target directories using system file explorer</li>
 *   <li>Reload YAML-based command-to-path mappings at runtime</li>
 * </ul>
 *
 * <p>
 * This class acts as the application entry point and command dispatcher.
 * </p>
 *
 * @author Kotohiko
 * @since Jan 25, 2026
 */
@Component
public class ImageMoverApplication {

    private static final Logger logger = LoggerFactory.getLogger(ImageMoverApplication.class);
    private static final String PROMPT = ">> ";

    @Autowired
    private YamlConfigFileLoader yamlConfigFileLoader;

    @Autowired
    private CommandRegistry commandRegistry;

    @Value("${app.config.path}")
    private String whGuideConfigPath;

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
        yamlConfigFileLoader.load(whGuideConfigPath);
        String carPathStr = yamlConfigFileLoader.getDeliveryCarPath();
        if (carPathStr == null) {
            logger.error("❌ 启动失败：无法加载配置或配置文件中缺少 'delivery_car' 路径！");
            // 优雅退出，防止后续 Paths.get(null) 导致崩溃
            return;
        }

        Path deliveryCarPath = Paths.get(carPathStr);
        CommandContext context = new CommandContext(yamlConfigFileLoader);

        this.startBackgroundMonitor(deliveryCarPath);
        this.printWelcomeMessage();
        this.reportDeliveryCarCountV2();

        boolean running = true;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (running) {
                System.out.print("\n" + PROMPT);
                String input = reader.readLine();

                if (input == null) {
                    running = false;
                } else {
                    String command = input.trim();

                    if ("exit".equalsIgnoreCase(command)) {
                        running = false;
                    } else {
                        CommandHandler handler = commandRegistry.get(command);
                        if (handler != null) {
                            handler.execute(command, context);
                        } else {
                            this.processUserCommand(command, deliveryCarPath);
                        }
                    }
                }
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
     * @param key          active WatchKey returned by WatchService
     * @param watchService watch service instance used to poll additional keys
     */
    private void processWatchEvents(WatchKey key, WatchService watchService) {
        // 1. Move cursor to start of line to "hide" the prompt
        System.out.print("\r");
        boolean hasNewFile = false;

        WatchKey currentKey = key;
        while (currentKey != null) {
            for (WatchEvent<?> event : currentKey.pollEvents()) {
                if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                    System.out.println("[Notification] New asset arrived: " + event.context());
                    hasNewFile = true;
                }
            }

            // Reset the key and check if it's still valid
            if (!currentKey.reset()) break;

            // 2. IMPORTANT: Reuse the existing watchService to poll for more pending keys
            currentKey = watchService.poll();
        }

        // ⭐ 核心：有新文件 → 重新扫描并报告
        if (hasNewFile) {
            this.reportDeliveryCarCountV2();
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
            logger.error("Failed to open folder: {} {}", e.getMessage(),e.getClass());
        }
    }

    /**
     * Reports the current total number of files in the delivery car.
     */
    private void reportDeliveryCarCountV2() {
        Path deliveryCar = Paths.get(yamlConfigFileLoader.getDeliveryCarPath());
        List<Path> files = scanDeliveryCar(deliveryCar);

        System.out.println("[Status] Delivery car updated");
        System.out.println("[Status] Current file count: " + files.size());
    }


    private void ensureDirectoryExists(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    private void printWelcomeMessage() {
        String line = "=============================================================";
        System.out.println(line);
        System.out.println("              Gallery Organizer Started");
        System.out.println(line);
        System.out.println();

        System.out.println("Available Commands:");
        System.out.println("  alias <name>      - move");
        System.out.println("  open -s <alias>   - view");
        System.out.println("  reload            - sync");
        System.out.println("  exit | quit       - Exit application");
        System.out.println();
    }

}