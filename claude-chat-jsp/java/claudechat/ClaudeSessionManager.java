package claudechat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton managing Claude chat sessions and subprocesses.
 * Thread-safe: all maps are ConcurrentHashMap, and processes are replaced atomically.
 */
public class ClaudeSessionManager {

    private static final ClaudeSessionManager INSTANCE = new ClaudeSessionManager();

    private final ConcurrentHashMap<String, ClaudeSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Process>       processes = new ConcurrentHashMap<>();

    private ClaudeSessionManager() {}

    public static ClaudeSessionManager getInstance() { return INSTANCE; }

    // ── Session CRUD ──────────────────────────────────────────────────────────

    public ClaudeSession createSession(String cwd, String label) throws IOException {
        String id = UUID.randomUUID().toString().replace("-", "");
        if (cwd == null || cwd.trim().isEmpty()) {
            cwd = System.getProperty("java.io.tmpdir")
                    + File.separator + "claude-sessions"
                    + File.separator + id;
        }
        Files.createDirectories(Paths.get(cwd));
        if (label == null || label.trim().isEmpty()) {
            label = "세션 " + id.substring(0, 8);
        }
        ClaudeSession session = new ClaudeSession(id, cwd, label, Instant.now().toString());
        sessions.put(id, session);
        return session;
    }

    public Collection<ClaudeSession> listSessions() {
        return sessions.values();
    }

    public ClaudeSession getSession(String id) {
        return sessions.get(id);
    }

    // ── Process management ─────────────────────────────────────────────────────

    /**
     * Writes request.md to the session's working directory, then starts the Claude
     * CLI subprocess. The caller is responsible for reading stdout and calling
     * {@link #removeProcess} when done.
     *
     * @return the started Process (stdout is the NDJSON stream)
     */
    public Process startClaudeProcess(String sessionId, String message, String claudeBin)
            throws IOException {

        ClaudeSession session = sessions.get(sessionId);
        if (session == null) throw new IOException("Session not found: " + sessionId);

        // Kill any already-running process for this session
        Process existing = processes.get(sessionId);
        if (existing != null && existing.isAlive()) {
            existing.destroyForcibly();
        }

        // Write message to file — avoids Windows cmd.exe encoding issues with Korean text
        Files.write(Paths.get(session.cwd, "request.md"), message.getBytes(StandardCharsets.UTF_8));

        String bin = resolveClaudeBin(claudeBin);
        boolean isWin = System.getProperty("os.name", "").toLowerCase().contains("win");

        List<String> cmd = new ArrayList<>();
        if (isWin) {
            cmd.add("cmd.exe");
            cmd.add("/c");
        }
        cmd.addAll(Arrays.asList(
            bin, "-p",
            "--output-format", "stream-json",
            "--verbose",
            "--permission-mode", "acceptEdits",
            "--allowed-tools", "Read,Edit,Write",
            "--", "Read request.md and respond to the user request."
        ));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File(session.cwd));
        pb.redirectErrorStream(false); // stderr ignored intentionally

        Process proc = pb.start();
        processes.put(sessionId, proc);
        return proc;
    }

    public void abort(String sessionId) {
        Process proc = processes.get(sessionId);
        if (proc != null && proc.isAlive()) proc.destroy();
    }

    public void removeProcess(String sessionId) {
        processes.remove(sessionId);
    }

    // ── Claude probe ───────────────────────────────────────────────────────────

    /** Returns a JSON string describing Claude CLI availability. No external JSON lib needed. */
    public String probeJson(String claudeBin) {
        String bin = resolveClaudeBin(claudeBin);
        try {
            boolean isWin = System.getProperty("os.name", "").toLowerCase().contains("win");
            List<String> cmd = new ArrayList<>();
            if (isWin) { cmd.add("cmd.exe"); cmd.add("/c"); }
            cmd.addAll(Arrays.asList(bin, "--version"));

            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process p = pb.start();
            String version;
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                version = br.readLine();
            }
            int code = p.waitFor();

            if (code == 0 && version != null && !version.isEmpty()) {
                return "{\"binaryPath\":\"" + ClaudeSession.esc(bin)
                        + "\",\"version\":\"" + ClaudeSession.esc(version.trim())
                        + "\",\"authenticated\":true,\"roundTripMs\":null,\"error\":null}";
            }
        } catch (Exception e) {
            return "{\"binaryPath\":null,\"version\":null,\"authenticated\":false"
                    + ",\"roundTripMs\":null,\"error\":\""
                    + ClaudeSession.esc(e.getMessage()) + "\"}";
        }
        return "{\"binaryPath\":null,\"version\":null,\"authenticated\":false"
                + ",\"roundTripMs\":null,\"error\":"
                + "\"Claude CLI를 찾을 수 없습니다. 설치 후 claude login을 완료해주세요.\"}";
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String resolveClaudeBin(String bin) {
        if (bin != null && !bin.trim().isEmpty()) return bin.trim();
        String env = System.getenv("CLAUDE_BIN");
        return (env != null && !env.isEmpty()) ? env : "claude";
    }
}
