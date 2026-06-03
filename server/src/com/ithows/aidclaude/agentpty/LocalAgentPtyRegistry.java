package com.ithows.aidclaude.agentpty;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/** 로컬 PTY 세션 레지스트리. agent-pty-kit 이식. */
public final class LocalAgentPtyRegistry {
    private final ConcurrentHashMap<String, LocalAgentPtySession> sessions = new ConcurrentHashMap<>();

    public void register(LocalAgentPtySession session) { sessions.put(session.sessionId, session); }
    public LocalAgentPtySession get(String sessionId) { return sessions.get(sessionId); }
    public LocalAgentPtySession remove(String sessionId) { return sessions.remove(sessionId); }
    public Collection<LocalAgentPtySession> all() { return sessions.values(); }
}
