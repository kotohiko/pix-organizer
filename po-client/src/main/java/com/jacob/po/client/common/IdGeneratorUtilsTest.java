package com.jacob.po.client.common;

import com.jacob.po.utils.IdGeneratorUtils;

/**
 *
 * @author Kotohiko
 * @version 1.0
 * @apiNote PixOrganizer ID generator test class
 * @since 11/21/2025 12:51 AM
 **/
public class IdGeneratorUtilsTest {
    static void main() {
        System.out.println("Random IP tag ID: " + IdGeneratorUtils.getIpTagId());
        System.out.println("Random character tag ID: " + IdGeneratorUtils.getCharTagId());
        System.out.println("Random general tag ID: " + IdGeneratorUtils.getGeneralTagId());
    }
}
