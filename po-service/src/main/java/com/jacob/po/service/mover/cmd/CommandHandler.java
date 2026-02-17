package com.jacob.po.service.mover.cmd;

@FunctionalInterface
public interface CommandHandler {
    void execute(String input, CommandContext context);
}