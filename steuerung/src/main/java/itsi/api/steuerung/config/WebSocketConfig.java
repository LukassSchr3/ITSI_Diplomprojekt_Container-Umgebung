package itsi.api.steuerung.config;

import itsi.api.steuerung.websocket.NoVncWebSocketHandler;
import itsi.api.steuerung.websocket.LiveEnvironmentWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final NoVncWebSocketHandler noVncWebSocketHandler;
    private final LiveEnvironmentWebSocketHandler liveEnvironmentWebSocketHandler;

    @Autowired
    public WebSocketConfig(NoVncWebSocketHandler noVncWebSocketHandler, LiveEnvironmentWebSocketHandler liveEnvironmentWebSocketHandler) {
        this.noVncWebSocketHandler = noVncWebSocketHandler;
        this.liveEnvironmentWebSocketHandler = liveEnvironmentWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // WebSocket endpoint used by noVNC: ws://host:6081/ws/novnc
        registry.addHandler(noVncWebSocketHandler, "/ws/novnc")
                .setAllowedOrigins("*");
        registry.addHandler(liveEnvironmentWebSocketHandler, "/ws/live-environment/{userId}")
                .setAllowedOrigins("*");
    }
}