const readline = require("readline");
const openBrowser = require("./browser");
const parsers = require("./parsers");

// Create Readline Interface
const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout,
    prompt: "📄 Enter filename > "
});

// Print welcome message and usage guide
console.log("=========================================");
console.log("  File Parsing & Browser Launcher Tool  ");
console.log("  Enter a filename to parse.");
console.log("  Type 'exit' or 'quit' to close.");
console.log("=========================================\n");

rl.prompt();

rl.on("line", async (line) => {
    const input = line.trim();

    // 1. Handle empty input
    if (!input) {
        rl.prompt();
        return;
    }

    // 2. Handle exit commands
    if (["exit", "quit", "q"].includes(input.toLowerCase())) {
        console.log("👋 Thank you for using the tool. Goodbye!");
        rl.close();
        process.exit(0);
    }

    // Pause readline during asynchronous processing
    rl.pause();

    console.log(`\n🔍 Parsing filename: "${input}"...`);

    let result = null;
    let matchedParserName = null;

    // 3. Iterate over parsers and identify which one succeeds
    for (const parser of parsers) {
        try {
            // Support both sync and async parsers
            result = await parser.parse(input);
            if (result) {
                matchedParserName = parser.name || parser.constructor.name || "Unnamed Parser";
                break;
            }
        } catch (err) {
            console.warn(`⚠️ Parser [${parser.name || "Unknown"}] threw an error:`, err.message);
        }
    }

    // 4. Output detailed response logs based on the outcome
    if (result) {
        console.log(`✅ Parse successful! (Matched Parser: ${matchedParserName})`);
        console.log("-----------------------------------------");
        console.log("Result:", JSON.stringify(result, null, 2));
        console.log("-----------------------------------------");

        console.log("🚀 Attempting to open browser...");
        try {
            await openBrowser(result);
            console.log("🎉 Browser opened successfully!\n");
        } catch (e) {
            console.error(`❌ Failed to open browser: ${e.message}`);
            if (process.env.DEBUG) {
                console.error(e.stack);
            }
            console.log(""); // Empty line for spacing
        }
    } else {
        console.error(`❌ Parse failed: No matching parsing rule found for "${input}".\n`);
    }

    // Resume input and display prompt again
    rl.resume();
    rl.prompt();
});

// Handle interruption signals (e.g., Ctrl+C)
rl.on("close", () => {
    console.log("\nProcess terminated.");
    process.exit(0);
});