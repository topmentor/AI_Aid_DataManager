package com.ithows.aidclaude.agentpty;

import com.ithows.aidclaude.AcContext;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.websocket.server.ServerContainer;
import java.util.List;

/**
 * Agent PTY 런타임 초기화 + WebSocket 엔드포인트 프로그래매틱 등록.
 *
 * <p>fat-jar 실행 시 @ServerEndpoint 자동 스캔이 동작하지 않으므로,
 * WsSci가 만든 ServerContainer에 직접 addEndpoint 한다.</p>
 */
public class AgentPtyInitListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            AgentCommandCatalog catalog = AgentCommandCatalog.defaults();
            LocalAgentPtyRegistry registry = new LocalAgentPtyRegistry();
            // 작업 디렉터리는 데이터 홈 하위만 허용
            LocalAgentPtyRunner runner = new LocalAgentPtyRunner(registry, List.of(AcContext.home().toPath()));
            AgentPtyRuntime.init(new AgentPtyRuntime(runner, catalog));

            ServerContainer sc = (ServerContainer)
                    sce.getServletContext().getAttribute(ServerContainer.class.getName());
            if (sc != null) {
                sc.addEndpoint(LocalAgentPtyWebSocketEndpoint.class);
                System.out.println("[AidClaude] agent-pty WebSocket endpoint registered");
            } else {
                System.err.println("[AidClaude] ServerContainer 없음 — WebSocket 미등록(websocket 의존성 확인)");
            }
        } catch (Exception e) {
            System.err.println("[AidClaude] agent-pty 초기화 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try { AgentPtyRuntime.get().localRunner().shutdown(); } catch (Exception ignored) { }
    }
}
