package com.ithows.aida.agentpty;

import com.pty4j.PtyProcess;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/** 로컬 PTY 세션. agent-pty-kit 이식. */
public final class LocalAgentPtySession {
    public final String sessionId;
    public final LocalAgentPtyOptions opts;
    public final PtyProcess process;
    public final Instant startedAt = Instant.now();
    public final AtomicBoolean closed = new AtomicBoolean(false);
    public volatile javax.websocket.Session ws;

    public LocalAgentPtySession(String sessionId, LocalAgentPtyOptions opts, PtyProcess process) {
        this.sessionId = sessionId;
        this.opts = opts;
        this.process = process;
    }

    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        try {
            if (process.isAlive()) {
                process.destroy();
                Thread.sleep(500);
                if (process.isAlive()) process.destroyForcibly();
            }
        } catch (Exception ignored) { }
        try {
            javax.websocket.Session socket = ws;
            if (socket != null && socket.isOpen()) socket.close();
        } catch (Exception ignored) { }
    }
}
