package com.ithows.aida.agentpty;

import com.google.gson.Gson;
import com.ithows.aida.AidaContext;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Agent PTY HTTP 라우팅 (로컬 전용). web.xml에서 /api/agent-pty/* 로 매핑.
 *   POST /api/agent-pty/local                 → 세션 시작
 *   POST /api/agent-pty/local/{sessionId}/kill → 세션 종료
 */
public class AgentPtyServlet extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo() == null ? "" : req.getPathInfo();
        if ("/check".equals(pathInfo)) {
            checkInstalled(req, resp);
        } else {
            json(resp, 404, Map.of("error", "unknown agent-pty path: " + pathInfo));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo() == null ? "" : req.getPathInfo();
        String[] parts = pathInfo.split("/"); // ["", "local"] 또는 ["", "local", "{id}", "kill"]

        if (parts.length == 2 && "local".equals(parts[1])) {
            startLocal(req, resp);
        } else if (parts.length == 2 && "install".equals(parts[1])) {
            startInstall(req, resp);
        } else if (parts.length == 4 && "local".equals(parts[1]) && "kill".equals(parts[3])) {
            killLocal(parts[2], resp);
        } else {
            json(resp, 404, Map.of("error", "unknown agent-pty path: " + pathInfo));
        }
    }

    /** GET /api/agent-pty/check?agent=claude → {agent, binary, installed} */
    private void checkInstalled(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AgentPtyRuntime rt = AgentPtyRuntime.get();
        String agent = req.getParameter("agent");
        String binary = rt.commandCatalog().binaryForAgent(agent);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("agent", agent == null || agent.isBlank() ? "claude" : agent);
        out.put("binary", binary);
        out.put("installed", isInstalled(binary));
        out.put("installCommand", rt.commandCatalog().installForAgent(agent));
        json(resp, 200, out);
    }

    /** POST /api/agent-pty/install (agent, [command], [workingDirectory]) → PTY로 설치 명령 실행 */
    private void startInstall(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AgentPtyRuntime rt = AgentPtyRuntime.get();
        String agent = req.getParameter("agent");
        String command = req.getParameter("command");
        if (command == null || command.isBlank()) command = rt.commandCatalog().installForAgent(agent);

        String cwd = req.getParameter("workingDirectory");
        if (cwd == null || cwd.isBlank()) cwd = AidaContext.home().getAbsolutePath();

        LocalAgentPtyRunner.StartResult result = rt.localRunner().startShell(command, cwd);
        if (result.outcome() == LocalAgentPtyRunner.StartOutcome.OK) {
            LocalAgentPtySession ss = result.session();
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("sessionId", ss.sessionId);
            out.put("agent", agent == null || agent.isBlank() ? "claude" : agent);
            out.put("command", ss.opts.command());
            out.put("workingDirectory", ss.opts.workingDirectory());
            json(resp, 201, out);
        } else {
            int status = result.outcome() == LocalAgentPtyRunner.StartOutcome.BAD_INPUT ? 400 : 500;
            json(resp, status, Map.of("error", result.errorMessage()));
        }
    }

    /** where(Windows)/which(Unix)로 바이너리 존재 확인. */
    private static boolean isInstalled(String binary) {
        if (binary == null || binary.isBlank()) return false;
        String os = System.getProperty("os.name", "").toLowerCase();
        String[] cmd = os.contains("win") ? new String[]{"where", binary} : new String[]{"which", binary};
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            if (!p.waitFor(5, TimeUnit.SECONDS)) { p.destroyForcibly(); return false; }
            return p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void startLocal(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AgentPtyRuntime rt = AgentPtyRuntime.get();
        String agent = req.getParameter("agent");
        String command = req.getParameter("command");
        if (command == null || command.isBlank()) command = rt.commandCatalog().commandForAgent(agent);

        LocalAgentPtyRunner.StartResult result = rt.localRunner().start(
                new LocalAgentPtyOptions(command, req.getParameter("workingDirectory")));

        if (result.outcome() == LocalAgentPtyRunner.StartOutcome.OK) {
            LocalAgentPtySession ss = result.session();
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("sessionId", ss.sessionId);
            out.put("agent", agent == null || agent.isBlank() ? "claude" : agent);
            out.put("command", ss.opts.command());
            out.put("workingDirectory", ss.opts.workingDirectory());
            json(resp, 201, out);
        } else {
            int status = result.outcome() == LocalAgentPtyRunner.StartOutcome.BAD_INPUT ? 400 : 500;
            json(resp, status, Map.of("error", result.errorMessage()));
        }
    }

    private void killLocal(String sessionId, HttpServletResponse resp) throws IOException {
        boolean closed = sessionId != null && AgentPtyRuntime.get().localRunner().close(sessionId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sessionId", sessionId);
        out.put("closed", closed);
        json(resp, 200, out);
    }

    private void json(HttpServletResponse resp, int status, Object body) throws IOException {
        resp.setStatus(status);
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(gson.toJson(body));
    }
}
