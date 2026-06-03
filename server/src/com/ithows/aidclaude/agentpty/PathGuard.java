package com.ithows.aidclaude.agentpty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** 작업 디렉터리 검증. agent-pty-kit 이식(로컬 전용). */
public final class PathGuard {
    private PathGuard() { }

    public static Path requireExistingDir(String path) {
        if (path == null || path.isBlank()) throw new IllegalArgumentException("workingDirectory is required");
        Path normalized = Path.of(path).toAbsolutePath().normalize();
        if (!Files.isDirectory(normalized)) throw new IllegalArgumentException("directory not found: " + normalized);
        return normalized;
    }

    public static void requirePrefixAllowed(Path path, List<Path> allowedPrefixes) {
        if (allowedPrefixes == null || allowedPrefixes.isEmpty()) return;
        Path normalized = path.toAbsolutePath().normalize();
        for (Path prefix : allowedPrefixes) {
            if (normalized.startsWith(prefix.toAbsolutePath().normalize())) return;
        }
        throw new IllegalArgumentException("directory is outside allowed roots: " + normalized);
    }
}
