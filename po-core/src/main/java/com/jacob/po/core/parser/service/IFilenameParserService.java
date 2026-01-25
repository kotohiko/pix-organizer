package com.jacob.po.core.parser.service;

import org.springframework.stereotype.Service;

/**
 *
 * @author Kotohiko
 * @version 1.0
 * @apiNote
 * @since Dec 07, 2025, 1:22 PM
 **/
@Service
public interface IFilenameParserService {
    String parsingDistributor(String filename);
}
