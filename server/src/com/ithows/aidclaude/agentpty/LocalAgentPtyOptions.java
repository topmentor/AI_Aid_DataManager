package com.ithows.aidclaude.agentpty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** 로컬 PTY 시작 옵션. agent-pty-kit 이식. ~ 확장 + 디렉터리 생성. */
public record LocalAgentPtyOptions(String command, String workingDirectory) {
    public LocalAgentPtyOptions {
        if (command == null || command.isBlank()) command = "claude";

        String home = System.getProperty("user.home", ".");
        String defaultCwd = home + "/.agent-pty";

        if (workingDirectory == null || workingDirectory.isBlank()) {
            workingDirectory = defaultCwd;
        } else if (workingDirectory.equals("~")) {
            workingDirectory = home;
        } else if (workingDirectory.startsWith("~/") || workingDirectory.startsWith("~\\")) {
            workingDirectory = home + workingDirectory.substring(1);
        }

        try {
            Files.createDirectories(Path.of(workingDirectory));
        } catch (IOException ignored) {
            // PathGuard reports the final error later.
        }
    }
}
