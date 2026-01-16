package com.jacob.po.parser.service.impl;

import com.jacob.po.parser.service.IFilenameParserService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Kotohiko
 * @version 1.0
 * @apiNote
 * @since Dec 07, 2025, 1:18 PM
 **/
public class FilenameParserServiceImpl implements IFilenameParserService {

    private static final Pattern PIXIV_FILENAME_FORMAT = Pattern.compile("\\d{8,9}_p\\d+");

    private StringBuilder getStringDraft(String string) {
        return new StringBuilder(string);
    }

    @Override
    public String parsingDistributor(String filename) {
        Matcher pixivMatcher = PIXIV_FILENAME_FORMAT.matcher(filename);
        if (filename.contains("httpsx.com")) {
            return this.twitterParser(filename);
        } else if (pixivMatcher.find()) {
            return this.pixivParser(filename);
        } else if (filename.contains("httpsdanbooru")) {
            return this.danbooruParser(filename);
        }
        return "";
    }

    private String twitterParser(String str) {
        var sb = this.getStringDraft(str);
        if (str.contains("httpstwitter.com")) {
            sb.replace(0, 16, "https://x.com/");
        } else if (str.contains("httpsx.com")) {
            sb.replace(0, 10, "https://x.com/");
        }
        sb.insert(sb.indexOf("status"), "/");
        sb.insert(sb.indexOf("status") + 6, "/");
        if (str.endsWith("photo1")) {
            return sb.substring(0, sb.indexOf("photo1"));
        } else if (str.endsWith("photo2")) {
            return sb.substring(0, sb.indexOf("photo2"));
        } else if (str.endsWith("photo3")) {
            return sb.substring(0, sb.indexOf("photo3"));
        } else if (str.endsWith("photo4")) {
            return sb.substring(0, sb.indexOf("photo4"));
        } else {
            return sb.toString();
        }
    }

    private String pixivParser(String str) {
        var sb = this.getStringDraft(str);
        return "https://www.pixiv.net/artworks/" + sb.substring(0, sb.indexOf("_p"));
    }

    private String danbooruParser(String str) {
        return str.replaceAll("^(https)(danbooru\\.donmai\\.us)(posts)(\\d+)$", "$1://$2/$3/$4");
    }
}
