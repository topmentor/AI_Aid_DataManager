package com.ithows.aidclaude.agentpty;

import java.util.LinkedHashMap;
import java.util.Map;

/** Agent CLI 명령 카탈로그 (claude / codex). agent-pty-kit 이식. */
public final class AgentCommandCatalog {

    public record AgentCli(String agent, String command, String binary) { }

    private final Map<String, AgentCli> agents;

    public AgentCommandCatalog(Map<String, AgentCli> agents) {
        this.agents = new LinkedHashMap<>(agents);
    }

    public static AgentCommandCatalog defaults() {
        Map<String, AgentCli> map = new LinkedHashMap<>();
        map.put("claude", new AgentCli("claude", "claude", "claude"));
        map.put("codex", new AgentCli("codex", "codex", "codex"));
        return new AgentCommandCatalog(map);
    }

    public String commandForAgent(String agent) {
        AgentCli cli = agents.get(normalize(agent));
        return cli != null ? cli.command() : "claude";
    }

    private static String normalize(String agent) {
        return agent == null || agent.isBlank() ? "claude" : agent.trim().toLowerCase();
    }
}
