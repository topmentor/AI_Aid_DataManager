package com.ithows.aidclaude.agentpty;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.nio.ByteBuffer;

/** 로컬 PTY WebSocket 엔드포인트. agent-pty-kit 이식. (프로그래매틱 addEndpoint로 등록) */
@ServerEndpoint("/ws/agent-pty/local/{sessionId}")
public class LocalAgentPtyWebSocketEndpoint {

    @OnOpen
    public void onOpen(Session ws, @PathParam("sessionId") String sessionId) {
        boolean ok = AgentPtyRuntime.get().localRunner().attach(
                sessionId, ws, intParam(ws, "cols", 80), intParam(ws, "rows", 24));
        if (!ok) close(ws, "session not found or already attached");
    }

    @OnMessage
    public void onBinary(ByteBuffer data, @PathParam("sessionId") String sessionId) {
        AgentPtyRuntime.get().localRunner().writeInput(sessionId, data);
    }

    @OnMessage
    public void onText(String text, @PathParam("sessionId") String sessionId) {
        JsonObject json = JsonParser.parseString(text).getAsJsonObject();
        if ("resize".equals(json.get("type").getAsString())) {
            AgentPtyRuntime.get().localRunner().resize(
                    sessionId, json.get("cols").getAsInt(), json.get("rows").getAsInt());
        }
    }

    @OnClose
    public void onClose(@PathParam("sessionId") String sessionId) {
        AgentPtyRuntime.get().localRunner().close(sessionId);
    }

    @OnError
    public void onError(@PathParam("sessionId") String sessionId, Throwable ignored) {
        AgentPtyRuntime.get().localRunner().close(sessionId);
    }

    private static int intParam(Session ws, String key, int fallback) {
        try {
            return Integer.parseInt(ws.getRequestParameterMap().get(key).get(0));
        } catch (Exception e) {
            return fallback;
        }
    }

    private static void close(Session ws, String reason) {
        try {
            ws.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, reason));
        } catch (Exception ignored) { }
    }
}
