package id.nawala.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.CloseStatus;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket config for real-time analytics streaming.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(analyticsWebSocketHandler(), "/ws/analytics")
                .setAllowedOrigins("*");
    }

    @Bean
    public AnalyticsWebSocketHandler analyticsWebSocketHandler() {
        return new AnalyticsWebSocketHandler();
    }

    public static class AnalyticsWebSocketHandler extends TextWebSocketHandler {

        private final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            sessions.add(session);
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            sessions.remove(session);
        }

        public void broadcast(String message) {
            sessions.removeIf(s -> !s.isOpen());
            for (WebSocketSession session : sessions) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    sessions.remove(session);
                }
            }
        }
    }
}
