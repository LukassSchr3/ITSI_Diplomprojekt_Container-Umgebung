package itsi.api.steuerung.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class NoVncWebSocketHandler extends BinaryWebSocketHandler {

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Value("${novnc.vnc-host:localhost}")
    private String vncHost;

    @Value("${novnc.vnc-port:5900}")
    private int vncPort;

    private static final String VNC_SOCKET_ATTR = "VNC_SOCKET";

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("noVNC WebSocket connection established: {}", session.getId());

        // VNC-Port dynamisch aus Query-Param oder Session-Attribut bestimmen
        int dynamicVncPort = vncPort;
        String query = session.getUri().getQuery();
        if (query != null && query.contains("vncPort=")) {
            try {
                for (String param : query.split("&")) {
                    if (param.startsWith("vncPort=")) {
                        dynamicVncPort = Integer.parseInt(param.split("=")[1]);
                        break;
                    }
                }
            } catch (Exception e) {
                log.warn("Invalid vncPort query param, fallback to default: {}", vncPort);
            }
        }

        try {
            Socket socket = new Socket(vncHost, dynamicVncPort);
            session.getAttributes().put(VNC_SOCKET_ATTR, socket);

            // Start background task to read from VNC TCP socket and forward to WebSocket client
            executorService.submit(() -> {
                try (InputStream in = socket.getInputStream()) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while (session.isOpen() && (read = in.read(buffer)) != -1) {
                        ByteBuffer payload = ByteBuffer.wrap(Arrays.copyOf(buffer, read));
                        try {
                            synchronized (session) {
                                session.sendMessage(new BinaryMessage(payload));
                            }
                        } catch (IOException sendEx) {
                            log.warn("Failed to send binary frame to client {}", session.getId(), sendEx);
                            break;
                        }
                    }
                } catch (IOException ex) {
                    log.warn("Error reading from VNC socket for session {}", session.getId(), ex);
                } finally {
                    closeResources(session);
                }
            });
        } catch (IOException e) {
            log.error("Failed to connect to VNC server {}:{}", vncHost, dynamicVncPort, e);
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        Socket socket = (Socket) session.getAttributes().get(VNC_SOCKET_ATTR);
        if (socket == null || socket.isClosed()) {
            log.warn("Received binary message but VNC socket is not available for session {}", session.getId());
            session.close(CloseStatus.SERVER_ERROR);
            return;
        }

        try {
            OutputStream out = socket.getOutputStream();
            ByteBuffer payload = message.getPayload();
            out.write(payload.array(), payload.position(), message.getPayloadLength());
            out.flush();
        } catch (IOException e) {
            log.warn("Failed to forward binary message to VNC server for session {}", session.getId(), e);
            closeResources(session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("noVNC WebSocket connection closed: {} - status {}", session.getId(), status);
        closeResources(session);
        super.afterConnectionClosed(session, status);
    }

    private void closeResources(WebSocketSession session) {
        Map<String, Object> attrs = session.getAttributes();
        Object socketObj = attrs.remove(VNC_SOCKET_ATTR);
        if (socketObj instanceof Socket) {
            Socket socket = (Socket) socketObj;
            try {
                if (!socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                log.debug("Error closing VNC socket for session {}", session.getId(), e);
            }
        }

        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (IOException e) {
            log.debug("Error closing WebSocket session {}", session.getId(), e);
        }
    }
}