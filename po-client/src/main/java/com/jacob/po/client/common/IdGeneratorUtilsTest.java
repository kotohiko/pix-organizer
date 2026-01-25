package com.jacob.po.client.common;

import com.jacob.po.core.common.helper.IdGeneratorHelper;

/**
 *
 * @author Kotohiko
 * @version 1.0
 * @apiNote PixOrganizer ID generator test class
 * @since 11/21/2025 12:51 AM
 **/
public class IdGeneratorUtilsTest {
    static void main() {
        System.out.println("Random IP tag ID: " + IdGeneratorHelper.getIpTagId());
        System.out.println("Random character tag ID: " + IdGeneratorHelper.getCharTagId());
        System.out.println("Random general tag ID: " + IdGeneratorHelper.getGeneralTagId());
    }
}
