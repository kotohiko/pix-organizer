package com.jacob.po.parser.service.impl;

import com.jacob.po.parser.service.IFilenameParserService;

/**
 *
 * @author Kotohiko
 * @version 1.0
 * @apiNote
 * @since Dec 07, 2025, 1:18 PM
 **/
public class FilenameParserServiceImpl implements IFilenameParserService {

    @Override
    public String twitterParser(String str) {
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

    private StringBuilder getStringDraft(String string) {
        return new StringBuilder(string);
    }
}
