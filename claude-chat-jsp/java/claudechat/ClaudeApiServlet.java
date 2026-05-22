package claudechat;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
 * Single servlet handling all Claude Chat API routes:
 *
 *   GET  /api/claude-chat/probe                    — probe Claude CLI
 *   GET  /api/claude-chat/sessions                 — list sessions
 *   POST /api/claude-chat/sessions                 — create session
 *   POST /api/claude-chat/sessions/{id}/send       — send message (SSE response stream)
 *   POST /api/claude-chat/sessions/{id}/abort      — abort current message
 *
 * Configuration (web.xml init-param):
 *   claudeBin  — path to the claude binary (default: $CLAUDE_BIN env var or "claude")
 *   sessionCwd — default working-directory root for new sessions (optional)
 */
@WebServlet(urlPatterns = "/api/claude-chat/*")
public class ClaudeApiServlet extends HttpServlet {

    private String claudeBin;
    private String sessionCwd;

    @Override
    public void init() {
        claudeBin  = getServletConfig().getInitParameter("claudeBin");
        sessionCwd = getServletConfig().getInitParameter("sessionCwd");
    }

    // ── Routing ───────────────────────────────────────────────────────────────

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding("UTF-8");
        String path = req.getPathInfo();
        if (path == null) path = "/";

        if (path.equals("/probe")) {
            handleProbe(resp);
        } else if (path.equals("/sessions")) {
            handleListSessions(resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        String path = req.getPathInfo();
        if (path == null) path = "/";

        if (path.equals("/sessions")) {
            handleCreateSession(req, resp);
        } else if (path.matches("/sessions/[^/]+/send")) {
            handleSend(req, resp, extractSegment(path, 2));
        } else if (path.matches("/sessions/[^/]+/abort")) {
            handleAbort(resp, extractSegment(path, 2));
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private void handleProbe(HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");
        resp.getWriter().write(ClaudeSessionManager.getInstance().probeJson(claudeBin));
    }

    private void handleListSessions(HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");
        Collection<ClaudeSession> list = ClaudeSessionManager.getInstance().listSessions();
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (ClaudeSession s : list) {
            if (!first) sb.append(',');
            sb.append(s.toJson());
            first = false;
        }
        sb.append(']');
        resp.getWriter().write(sb.toString());
    }

    private void handleCreateSession(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String body = readBody(req);
        String cwd   = extractJsonField(body, "cwd");
        String label = extractJsonField(body, "label");

        // Fall back to configured root
        if ((cwd == null || cwd.isEmpty()) && sessionCwd != null && !sessionCwd.isEmpty()) {
            cwd = sessionCwd;
        }

        try {
            ClaudeSession session = ClaudeSessionManager.getInstance().createSession(cwd, label);
            resp.setContentType("application/json; charset=UTF-8");
            resp.getWriter().write(session.toJson());
        } catch (IOException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("application/json; charset=UTF-8");
            resp.getWriter().write("{\"error\":\"" + ClaudeSession.esc(e.getMessage()) + "\"}");
        }
    }

    /**
     * Starts the Claude subprocess and streams NDJSON lines back as SSE events.
     * The response body IS the SSE stream — the client reads it via fetch + ReadableStream.
     */
    private void handleSend(HttpServletRequest req, HttpServletResponse resp, String sessionId)
            throws IOException {

        String body    = readBody(req);
        String message = extractJsonField(body, "message");
        if (message == null || message.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "message required");
            return;
        }

        // SSE headers
        resp.setContentType("text/event-stream; charset=UTF-8");
        resp.setHeader("Cache-Control", "no-cache, no-store");
        resp.setHeader("X-Accel-Buffering", "no"); // disable nginx buffering
        resp.flushBuffer();

        PrintWriter writer = resp.getWriter();
        ClaudeSessionManager mgr = ClaudeSessionManager.getInstance();

        try {
            Process proc = mgr.startClaudeProcess(sessionId, message, claudeBin);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    writer.write("data: " + line + "\n\n");
                    writer.flush();
                    if (writer.checkError()) {
                        // Client disconnected — kill subprocess
                        proc.destroyForcibly();
                        break;
                    }
                }
            }

            proc.waitFor();
            mgr.removeProcess(sessionId);

            if (!writer.checkError()) {
                writer.write("data: {\"type\":\"done\"}\n\n");
                writer.flush();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            mgr.removeProcess(sessionId);
        } catch (IOException e) {
            mgr.removeProcess(sessionId);
            if (!writer.checkError()) {
                writer.write("data: {\"type\":\"error\",\"message\":\""
                        + ClaudeSession.esc(e.getMessage()) + "\"}\n\n");
                writer.flush();
            }
        }
    }

    private void handleAbort(HttpServletResponse resp, String sessionId) throws IOException {
        ClaudeSessionManager.getInstance().abort(sessionId);
        resp.setContentType("application/json; charset=UTF-8");
        resp.getWriter().write("{\"ok\":true}");
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private String readBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }

    /** Extracts a segment from a path like /sessions/abc123/send — segment(2) → "abc123". */
    private static String extractSegment(String path, int index) {
        String[] parts = path.split("/");
        return (index < parts.length) ? parts[index] : "";
    }

    /**
     * Very small JSON string-field extractor.
     * Handles only string-valued fields. No external JSON library required.
     */
    static String extractJsonField(String json, String field) {
        if (json == null || json.isEmpty()) return null;
        String key = "\"" + field + "\"";
        int ki = json.indexOf(key);
        if (ki < 0) return null;
        int colon = json.indexOf(':', ki + key.length());
        if (colon < 0) return null;
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (start >= json.length()) return null;

        if (json.charAt(start) != '"') {
            // null / number / bool — read until delimiter
            if (json.startsWith("null", start)) return null;
            int end = start;
            while (end < json.length() && ",}]\n\r\t ".indexOf(json.charAt(end)) < 0) end++;
            return json.substring(start, end);
        }

        // String value with escape handling
        StringBuilder sb = new StringBuilder();
        int i = start + 1;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case '"':  sb.append('"');  break;
                    case '\\': sb.append('\\'); break;
                    case 'n':  sb.append('\n'); break;
                    case 'r':  sb.append('\r'); break;
                    case 't':  sb.append('\t'); break;
                    default:   sb.append(next);
                }
                i += 2;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }
}
