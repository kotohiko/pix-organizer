package com.jacob.po.service.mover.cmd;

import java.nio.file.Path;

@FunctionalInterface
public interface CommandHandler {
    void execute(String input, Path deliveryCarPath);
}

