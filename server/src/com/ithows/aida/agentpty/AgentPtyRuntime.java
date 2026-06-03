package com.ithows.aida.agentpty;

/** Agent PTY 런타임 싱글턴 (로컬 전용). agent-pty-kit의 슬림 버전. */
public final class AgentPtyRuntime {

    private static volatile AgentPtyRuntime current;

    private final LocalAgentPtyRunner localRunner;
    private final AgentCommandCatalog commandCatalog;

    public AgentPtyRuntime(LocalAgentPtyRunner localRunner, AgentCommandCatalog commandCatalog) {
        this.localRunner = localRunner;
        this.commandCatalog = commandCatalog;
    }

    public static void init(AgentPtyRuntime runtime) { current = runtime; }

    public static AgentPtyRuntime get() {
        AgentPtyRuntime runtime = current;
        if (runtime == null) throw new IllegalStateException("AgentPtyRuntime is not initialized");
        return runtime;
    }

    public LocalAgentPtyRunner localRunner() { return localRunner; }
    public AgentCommandCatalog commandCatalog() { return commandCatalog; }
}
