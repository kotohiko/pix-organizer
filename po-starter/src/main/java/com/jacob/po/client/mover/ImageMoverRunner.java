package com.jacob.po.client.mover;

import com.jacob.po.service.mover.ImageMoverApplication;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 *
 * @author Kotohiko
 * @since Feb 13, 2026
 */
@Component
public class ImageMoverRunner implements ApplicationRunner {

    @Autowired
    private ImageMoverApplication app;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        app.start();
    }
}