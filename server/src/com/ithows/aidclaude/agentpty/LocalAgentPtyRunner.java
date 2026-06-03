package com.ithows.aidclaude.agentpty;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;

import javax.websocket.Session;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** 로컬 PTY 실행/입출력 펌프. agent-pty-kit 이식. */
public final class LocalAgentPtyRunner {

    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();

    public enum StartOutcome { OK, BAD_INPUT, SPAWN_FAILED }
    public record StartResult(StartOutcome outcome, LocalAgentPtySession session, String errorMessage) {
        static StartResult ok(LocalAgentPtySession session) { return new StartResult(StartOutcome.OK, session, null); }
        static StartResult badInput(String message) { return new StartResult(StartOutcome.BAD_INPUT, null, message); }
        static StartResult spawnFailed(String message) { return new StartResult(StartOutcome.SPAWN_FAILED, null, message); }
    }

    private final LocalAgentPtyRegistry registry;
    private final List<Path> allowedCwdPrefixes;
    private final ExecutorService executor;

    public LocalAgentPtyRunner(LocalAgentPtyRegistry registry, List<Path> allowedCwdPrefixes) {
        this.registry = registry;
        this.allowedCwdPrefixes = allowedCwdPrefixes == null ? List.of() : List.copyOf(allowedCwdPrefixes);
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "agent-pty-local-" + THREAD_COUNTER.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }

    public StartResult start(LocalAgentPtyOptions opts) {
        Path cwd;
        try {
            cwd = PathGuard.requireExistingDir(opts.workingDirectory());
            PathGuard.requirePrefixAllowed(cwd, allowedCwdPrefixes);
        } catch (IllegalArgumentException e) {
            return StartResult.badInput(e.getMessage());
        }

        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("TERM", "xterm-256color");
        env.putIfAbsent("COLORTERM", "truecolor");

        PtyProcess process;
        try {
            process = new PtyProcessBuilder()
                    .setCommand(new String[]{opts.command()})
                    .setEnvironment(env)
                    .setDirectory(cwd.toString())
                    .setInitialColumns(80)
                    .setInitialRows(24)
                    .start();
        } catch (IOException e) {
            return StartResult.spawnFailed("PTY spawn failed: " + e.getMessage());
        }

        String sessionId = UUID.randomUUID().toString();
        LocalAgentPtySession session = new LocalAgentPtySession(sessionId, opts, process);
        registry.register(session);
        return StartResult.ok(session);
    }

    /** 셸 명령줄(인자 포함)을 OS 셸로 PTY 실행. CLI 설치 스크립트 등에 사용. */
    public StartResult startShell(String commandLine, String workingDirectory) {
        if (commandLine == null || commandLine.isBlank()) {
            return StartResult.badInput("install command is empty");
        }
        Path cwd;
        try {
            cwd = PathGuard.requireExistingDir(workingDirectory);
            PathGuard.requirePrefixAllowed(cwd, allowedCwdPrefixes);
        } catch (IllegalArgumentException e) {
            return StartResult.badInput(e.getMessage());
        }

        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("TERM", "xterm-256color");
        env.putIfAbsent("COLORTERM", "truecolor");

        PtyProcess process;
        try {
            process = new PtyProcessBuilder()
                    .setCommand(shellArgv(commandLine))
                    .setEnvironment(env)
                    .setDirectory(cwd.toString())
                    .setInitialColumns(80)
                    .setInitialRows(24)
                    .start();
        } catch (IOException e) {
            return StartResult.spawnFailed("PTY spawn failed: " + e.getMessage());
        }

        String sessionId = UUID.randomUUID().toString();
        LocalAgentPtyOptions opts = new LocalAgentPtyOptions(commandLine, workingDirectory);
        LocalAgentPtySession session = new LocalAgentPtySession(sessionId, opts, process);
        registry.register(session);
        return StartResult.ok(session);
    }

    private static String[] shellArgv(String commandLine) {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return new String[]{"cmd.exe", "/c", commandLine};
        }
        return new String[]{"/bin/sh", "-lc", commandLine};
    }

    public boolean attach(String sessionId, Session ws, int cols, int rows) {
        LocalAgentPtySession session = registry.get(sessionId);
        if (session == null || session.closed.get() || session.ws != null) return false;
        try {
            session.process.setWinSize(new WinSize(Math.max(cols, 1), Math.max(rows, 1)));
            session.ws = ws;
            executor.submit(() -> pumpOutput(session));
            return true;
        } catch (Exception e) {
            close(sessionId);
            return false;
        }
    }

    public void writeInput(String sessionId, ByteBuffer data) {
        LocalAgentPtySession session = registry.get(sessionId);
        if (session == null || session.closed.get() || !session.process.isAlive()) return;
        try {
            byte[] bytes = new byte[data.remaining()];
            data.get(bytes);
            OutputStream out = session.process.getOutputStream();
            synchronized (session) {
                out.write(bytes);
                out.flush();
            }
        } catch (IOException e) {
            close(sessionId);
        }
    }

    public void resize(String sessionId, int cols, int rows) {
        LocalAgentPtySession session = registry.get(sessionId);
        if (session == null || !session.process.isAlive()) return;
        try {
            session.process.setWinSize(new WinSize(cols, rows));
        } catch (Exception ignored) { }
    }

    public boolean close(String sessionId) {
        LocalAgentPtySession session = registry.remove(sessionId);
        if (session == null) return false;
        session.close();
        return true;
    }

    public void shutdown() {
        for (LocalAgentPtySession session : registry.all()) session.close();
        executor.shutdownNow();
    }

    private void pumpOutput(LocalAgentPtySession session) {
        byte[] buffer = new byte[8192];
        try (InputStream in = session.process.getInputStream()) {
            int n;
            while ((n = in.read(buffer)) > 0) {
                Session ws = session.ws;
                if (ws == null || !ws.isOpen()) break;
                synchronized (ws) {
                    ws.getBasicRemote().sendBinary(ByteBuffer.wrap(buffer, 0, n));
                }
            }
        } catch (IOException ignored) {
        } finally {
            close(session.sessionId);
        }
    }
}
