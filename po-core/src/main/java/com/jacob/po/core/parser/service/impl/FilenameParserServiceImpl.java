package com.jacob.po.core.parser.service.impl;

import com.jacob.po.core.parser.service.IFilenameParserService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of {@link IFilenameParserService} that utilizes a Strategy Pattern
 * to normalize platform-specific filenames into standard URLs.
 * <p>
 * This design decouples the parsing logic for different platforms (Twitter, Pixiv, Danbooru),
 * making the system easily extendable without modifying the core distribution logic.
 *
 * @author Kotohiko
 * @version 1.1
 * @since Dec 07, 2025
 */
@Service
public class FilenameParserServiceImpl implements IFilenameParserService {

    /**
     * A list of internal strategies used to match and parse specific string formats.
     */
    private final List<ParserStrategy> strategies = List.of(
            new PixivStrategy(),
            new TwitterStrategy(),
            new DanbooruStrategy(),
            new BilibiliOpusStrategy(),
            new BilibiliVideoStrategy()
    );

    /**
     * Entry point for parsing. Iterates through available strategies to find a match.
     *
     * @param filename The raw input string (filename or malformed URL).
     * @return The normalized URL, or an empty string if no strategy matches.
     */
    @Override
    public String parsingDistributor(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }

        return strategies.stream()
                .filter(strategy -> strategy.canHandle(filename))
                .map(strategy -> strategy.parse(filename))
                .findFirst()
                .orElse("");
    }

    // --- Strategy Definitions ---

    /**
     * Internal interface defining the contract for platform-specific parsing.
     */
    private interface ParserStrategy {

        boolean canHandle(String input);

        String parse(String input);
    }

    /**
     * Strategy for parsing Pixiv filenames.
     * <p>Input format: {@code 12345678_p0}</p>
     */
    private static class PixivStrategy implements ParserStrategy {
        private static final Pattern PATTERN = Pattern.compile("(\\d{8,10})_p\\d+");

        @Override
        public boolean canHandle(String input) {
            return PATTERN.matcher(input).find();
        }

        @Override
        public String parse(String input) {
            Matcher matcher = PATTERN.matcher(input);
            return matcher.find() ? "https://www.pixiv.net/artworks/" + matcher.group(1) : "";
        }
    }

    /**
     * Strategy for parsing Twitter/X URLs.
     * <p>
     * Correctly isolates the numeric Status ID from trailing metadata like "{@code photo1}".
     * <li>
     * Input example: "{@code httpsx.comjill_07kmstatus1502553581789978626photo1}"
     * </li>
     * <li>
     * Output example: "{@code https://x.com/jill_07km/status/1502553581789978626}"
     * </li>
     */
    private static class TwitterStrategy implements ParserStrategy {

        /**
         * Regex Breakdown:
         * <ol>
         *     <li>
         *         {@code https(?:x|twitter)\.com}: Matches the domain (x or twitter)
         *     </li>
         *     <li>
         *         {@code (.*?)}: Group 1 (Non-greedy): Captures the username
         *     </li>
         *     <li>
         *         {@code status}: Literal "status" marker
         *     </li>
         *     <li>
         *         {@code (\d+)}: Group 2 (Strict): Captures ONLY the numeric ID
         *     </li>
         *     <li>
         *         {@code (?:photo\d+)?}: Non-capturing group: Matches "photo1" etc., but ignores it
         *     </li>
         * </ol>
         */
        private static final Pattern PATTERN = Pattern.compile("https(?:x|twitter)\\.com(.*?)status(\\d+)" +
                "(?:photo\\d+)?");

        @Override
        public boolean canHandle(String input) {
            return input.contains("httpsx.com") || input.contains("httpstwitter.com");
        }

        @Override
        public String parse(String input) {
            Matcher matcher = PATTERN.matcher(input);
            if (matcher.find()) {
                String username = matcher.group(1);
                String statusId = matcher.group(2);

                // Ensure there is a slash between domain and username if missing
                String cleanUsername = username.startsWith("/") ? username.substring(1) : username;
                if (cleanUsername.endsWith("/")) {
                    cleanUsername = cleanUsername.substring(0, cleanUsername.length() - 1);
                }

                return String.format("https://x.com/%s/status/%s", cleanUsername, statusId);
            }
            return input;
        }
    }

    /**
     * Strategy for parsing Danbooru URLs.
     * <p>Standardizes shorthand Danbooru strings into valid HTTPS links.</p>
     */
    private static class DanbooruStrategy implements ParserStrategy {

        private static final Pattern PATTERN = Pattern.compile("^httpsdanbooru\\.donmai\\.usposts(\\d+)$");

        @Override
        public boolean canHandle(String input) {
            return PATTERN.matcher(input).find();
        }

        @Override
        public String parse(String input) {
            Matcher matcher = PATTERN.matcher(input);
            return matcher.find() ? "https://danbooru.donmai.us/posts/" + matcher.group(1) : "";
        }
    }

    /**
     * Strategy for parsing Bilibili Opus (Dynamic) identifiers.
     * <p>
     * Handles inputs like {@code httpswww.bilibili.comopus12345#1}
     * and normalizes them to a standard browser-executable URL.
     * </p>
     */
    private static class BilibiliOpusStrategy implements ParserStrategy {

        /**
         * Regex Breakdown:
         * <ol>
         *     <li>
         *         {@code httpswww.bilibili.comopus} : Matches the malformed prefix.
         *     </li>
         *     <li>
         *         {@code (\d+)} : Group 1: Captures the unique Opus ID.
         *     </li>
         *     <li>
         *         {@code (?:#\d+)?} : Non-capturing group: Matches the #1, #2 suffix but excludes it from result.
         *     </li>
         * </ol>
         */
        private static final Pattern PATTERN = Pattern.compile("httpswww\\.bilibili\\.comopus(\\d+)(?:#\\d+)?");

        @Override
        public boolean canHandle(String input) {
            return PATTERN.matcher(input).find();
        }

        @Override
        public String parse(String input) {
            Matcher matcher = PATTERN.matcher(input);
            if (matcher.find()) {
                // Group 1 is specifically the digits, stopping before the '#'
                return "https://www.bilibili.com/opus/" + matcher.group(1);
            }
            return "";
        }
    }

    /**
     * Strategy for parsing Bilibili videos identifiers.
     * <p>
     * Handles inputs like {@code httpswww.bilibili.comvideoBV1xx411c7mC}
     * and normalizes them to a standard browser-executable URL.
     * </p>
     */
    private static class BilibiliVideoStrategy implements ParserStrategy {
        // Captures Alphanumeric BV ID (e.g., BV13QzTBuE74)
        private static final Pattern PATTERN = Pattern.compile("httpswww\\.bilibili\\.comvideo([a-zA-Z0-9]+)");

        @Override
        public boolean canHandle(String input) {
            return PATTERN.matcher(input).find();
        }

        @Override
        public String parse(String input) {
            Matcher matcher = PATTERN.matcher(input);
            return matcher.find() ? "https://www.bilibili.com/video/" + matcher.group(1) : "";
        }
    }
}