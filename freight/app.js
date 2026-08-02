const fs = require('fs');
const path = require('path');
const readline = require('readline');
const { exec } = require('child_process');

// Load configurations
const config = require('./config.json');
const mappings = require('./mappings.json');

// Kawaii Logger Helper Functions ✨
function logInfo(msg) {
    console.log(`\x1b[32m[INFO ${new Date().toLocaleTimeString()}]\x1b[0m ✨ ${msg}`);
}

function logWarn(msg) {
    console.log(`\x1b[33m[WARN ${new Date().toLocaleTimeString()}]\x1b[0m ⚠️ ${msg}`);
}

function logError(msg) {
    console.error(`\x1b[31m[ERROR ${new Date().toLocaleTimeString()}]\x1b[0m 😿 ${msg}`);
}

/**
 * Check if the file is a cute little image! 🖼️
 */
function isImageFile(filename) {
    const ext = path.extname(filename).toLowerCase();
    return ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.bmp', '.svg', '.tiff'].includes(ext);
}

/**
 * Open target directory in Windows Explorer 📂✨
 */
function openFolder(targetDir) {
    if (!fs.existsSync(targetDir)) {
        try {
            fs.mkdirSync(targetDir, { recursive: true });
            logInfo(`Folder didn't exist, so I created a cozy new space for you at: ${targetDir} ✨`);
        } catch (err) {
            logError(`Failed to create folder! Error: ${err.message}`);
            return;
        }
    }

    const cmd = `explorer "${path.normalize(targetDir)}"`;
    exec(cmd, (error) => {
        if (error) {
            logError(`Oopsie! Couldn't open the folder: ${error.message}`);
        } else {
            logInfo(`Swish! Folder opened successfully! Look look! 📂✨`);
        }
    });
}

/**
 * FEATURE 1: Check how many images are currently waiting in the freight folder 📦🔍
 */
async function checkCargoHold() {
    const sourceDir = config.sourceDir;

    logInfo(`Scanning the cargo hold at: ${sourceDir} ... 🔍`);

    if (!fs.existsSync(sourceDir)) {
        logError(`Eeeek! Cargo hold does not exist: ${sourceDir}`);
        return;
    }

    try {
        const files = await fs.promises.readdir(sourceDir);
        const imageFiles = files.filter(file => {
            const fullPath = path.join(sourceDir, file);
            return fs.statSync(fullPath).isFile() && isImageFile(file);
        });

        logInfo(`==================================================`);
        if (imageFiles.length === 0) {
            logWarn(`The cargo hold is currently completely empty! (0 images) 🪹`);
        } else {
            logInfo(`Yay! There are ${imageFiles.length} image(s) waiting to be shipped! 📦💕`);
            imageFiles.forEach((file, idx) => {
                console.log(`   ${idx + 1}. 📄 ${file}`);
            });
        }
        logInfo(`==================================================`);
    } catch (err) {
        logError(`Failed to scan the cargo hold! Error: ${err.message}`);
    }
}

/**
 * FEATURE 2: Real-time File System Watcher 👁️✨
 * Monitors the freight directory and logs when new files arrive!
 */
function startCargoWatcher() {
    const sourceDir = config.sourceDir;

    if (!fs.existsSync(sourceDir)) {
        logWarn(`Watcher couldn't start because source folder doesn't exist yet: ${sourceDir}`);
        return;
    }

    // Prevents duplicate logs triggered by file-system buffering/multi-events
    const debounceMap = new Map();

    try {
        fs.watch(sourceDir, (eventType, filename) => {
            if (!filename || !isImageFile(filename)) return;

            const fullPath = path.join(sourceDir, filename);

            // Debounce mechanism: Ignore duplicate triggers within 500ms
            const now = Date.now();
            if (debounceMap.has(filename) && (now - debounceMap.get(filename) < 500)) {
                return;
            }
            debounceMap.set(filename, now);

            // Verify that the file actually exists (not just deleted)
            if (fs.existsSync(fullPath) && fs.statSync(fullPath).isFile()) {
                logInfo(`[WATCHER ALERT] 🔔 Ooh! New image detected in cargo hold: "${filename}"! Ready for shipping, Master! 📦✨`);
            }
        });
        logInfo(`Folder Sentinel ACTIVATED! Watching for new goodies in: ${sourceDir} 👁️💖`);
    } catch (err) {
        logWarn(`Failed to initialize folder watcher: ${err.message}`);
    }
}

/**
 * Core Dispatch / Shipping logic 🚚💨
 */
async function dispatch(command) {
    const rawCmd = command.trim();
    const lowerCmd = rawCmd.toLowerCase();

    // ----------------------------------------------------
    // Special Command: "check" (Check current cargo hold)
    // ----------------------------------------------------
    if (lowerCmd === 'check') {
        await checkCargoHold();
        return;
    }

    // ----------------------------------------------------
    // Special Command: "open <key>" (Open destination folder)
    // ----------------------------------------------------
    if (lowerCmd.startsWith('open ')) {
        const targetKey = lowerCmd.replace(/^open\s+/, '').trim();
        const targetDir = mappings[targetKey];

        if (!targetDir) {
            logError(`Hmph! Secret key "${targetKey}" not found in mappings.json! Cannot open folder! >_<`);
            return;
        }

        logInfo(`Opening folder for key: [ ${targetKey} ] ... Hold on, Master! 📂💫`);
        openFolder(targetDir);
        return;
    }

    // ----------------------------------------------------
    // Standard Dispatch / Shipping logic
    // ----------------------------------------------------
    const targetDir = mappings[lowerCmd];

    if (!targetDir) {
        logError(`Hmph! Command "${rawCmd}" not found in mappings.json! Did you spell it right, Senpai? >_<`);
        return;
    }

    const sourceDir = config.sourceDir;

    logInfo(`Shipment mode ACTIVATED! Let's go! 🚀`);
    logInfo(`Secret Code: [ ${lowerCmd} ]`);
    logInfo(`From (Cargo Hold): ${sourceDir}`);
    logInfo(`To (Destination): ${targetDir}`);

    if (!fs.existsSync(sourceDir)) {
        logError(`Eeeek! Cargo hold does not exist: ${sourceDir}`);
        return;
    }

    let files;
    try {
        files = await fs.promises.readdir(sourceDir);
    } catch (err) {
        logError(`Failed to open cargo hold! Error: ${err.message}`);
        return;
    }

    const imageFiles = files.filter(file => {
        const fullPath = path.join(sourceDir, file);
        return fs.statSync(fullPath).isFile() && isImageFile(file);
    });

    if (imageFiles.length === 0) {
        logWarn(`The cargo hold is completely empty! No image goodies found to ship... (pout)`);
        return;
    }

    logInfo(`Found ${imageFiles.length} shiny picture(s) ready to be delivered! 💕`);

    try {
        if (!fs.existsSync(targetDir)) {
            await fs.promises.mkdir(targetDir, { recursive: true });
            logInfo(`Creating a brand new cozy home at: ${targetDir} ✨`);
        }
    } catch (err) {
        logError(`Failed to build destination path! Error: ${err.message}`);
        return;
    }

    let movedCount = 0;
    let errorCount = 0;

    for (const file of imageFiles) {
        const srcPath = path.join(sourceDir, file);
        let destPath = path.join(targetDir, file);

        if (fs.existsSync(destPath)) {
            const ext = path.extname(file);
            const baseName = path.basename(file, ext);
            const newName = `${baseName}_${Date.now()}${ext}`;
            destPath = path.join(targetDir, newName);
            logWarn(`File "${file}" already exists! Renaming to "${newName}" to keep it safe!`);
        }

        try {
            try {
                await fs.promises.rename(srcPath, destPath);
            } catch (e) {
                if (e.code === 'EXDEV') {
                    await fs.promises.copyFile(srcPath, destPath);
                    await fs.promises.unlink(srcPath);
                } else {
                    throw e;
                }
            }
            movedCount++;
            logInfo(` [✓] Delivered successfully! ${file} -> ${destPath}`);
        } catch (err) {
            errorCount++;
            logError(` [✕] Oh no! Failed to move "${file}" | Reason: ${err.message}`);
        }
    }

    logInfo(`==================================================`);
    logInfo(`Delivery Complete! Successfully shipped: ${movedCount} 💕 | Oopsies: ${errorCount} 😿`);
    logInfo(`==================================================`);
}

// ===== Interactive CLI Terminal Logic =====
const argCmd = process.argv.slice(2).join(' ');

if (argCmd) {
    // Quick execution mode
    dispatch(argCmd);
} else {
    // Interactive mode with real-time folder monitoring
    console.log(`\n==================================================`);
    console.log(`  🌸 Anime File Delivery Express (Node.js) 🌸`);
    console.log(`==================================================`);
    console.log(`Commands:`);
    console.log(` • [ command ]      -> Ship images (e.g., kisaki)`);
    console.log(` • check            -> View files currently in cargo hold`);
    console.log(` • open [ command ] -> Open target folder (e.g., open kisaki)`);
    console.log(` • exit             -> Say goodbye 👋\n`);

    // Start watching freight directory in the background ✨
    startCargoWatcher();

    const rl = readline.createInterface({
        input: process.stdin,
        output: process.stdout
    });

    const promptUser = () => {
        rl.question('\n\x1b[36mWaiting for your order, Sensei! > \x1b[0m', async (input) => {
            const cmd = input.trim();
            if (cmd.toLowerCase() === 'exit') {
                logInfo('See you later, Sensei! Don\'t forget about me, okay? Bye bye! 👋💖');
                rl.close();
                process.exit(0);
            }

            if (cmd) {
                await dispatch(cmd);
            }
            promptUser();
        });
    };

    promptUser();
}