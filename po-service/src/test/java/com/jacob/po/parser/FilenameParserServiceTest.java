package com.jacob.po.parser;

import com.jacob.po.parser.service.impl.FilenameParserServiceImpl;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * CLI tool to parse filenames and automatically open links in the browser.
 *
 * @author Kotohiko
 * @version 1.2
 * @since Dec 07, 2025
 **/
public class FilenameParserServiceTest {

    static void main() throws IOException {
        // Initialize resources outside the loop for better performance
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        FilenameParserServiceImpl parserService = new FilenameParserServiceImpl();

        System.out.println("===========================================");
        System.out.println("   Twitter Filename Parser Utility v1.2    ");
        System.out.println("   (Type 'exit' or 'quit' to stop)         ");
        System.out.println("===========================================");

        while (true) {
            System.out.print("\n[Input] Please paste the filename: ");
            String input = reader.readLine();

            // Handle exit commands
            if (input == null || input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                System.out.println("Exiting... Have a great day!");
                break;
            }

            // Skip empty inputs
            if (input.trim().isEmpty()) {
                continue;
            }

            try {
                // Parse the input
                String out = parserService.twitterParser(input);

                System.out.println("✅ Success!");
                System.out.println("🔗 URL: " + out);

                // Trigger system browser
                openWebBrowser(out);

            } catch (Exception e) {
                System.err.println("❌ Error: Failed to parse the filename.");
                System.err.println("Details: " + e.getMessage());
            }
        }
    }

    /**
     * Opens the specified URL in the system's default web browser.
     * @param url The target URL string
     */
    private static void openWebBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                System.out.println("🚀 Launching browser...");
            } else {
                System.err.println("⚠️ Warning: Automatic browsing is not supported on this OS.");
                System.out.println("Please copy the link manually.");
            }
        } catch (IOException | URISyntaxException e) {
            System.err.println("❌ Browser Error: Could not open the link.");
        }
    }
}