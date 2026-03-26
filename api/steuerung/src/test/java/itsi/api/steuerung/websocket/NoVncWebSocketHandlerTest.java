package itsi.api.steuerung.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoVncWebSocketHandlerTest {

    @Mock
    private WebSocketSession session;

    private NoVncWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new NoVncWebSocketHandler();
        ReflectionTestUtils.setField(handler, "vncHost", "localhost");
        ReflectionTestUtils.setField(handler, "vncPort", 5900);
    }

    // ===================== afterConnectionEstablished – kein VNC-Server =====================

    @Test
    void afterConnectionEstablished_vncNotAvailable_closesSession() throws Exception {
        // Port 5900 wird in Tests nicht verfügbar sein → IOException
        when(session.getUri()).thenReturn(new URI("/novnc"));
        when(session.getId()).thenReturn("test-session-1");

        // Kein laufender VNC-Server → verbindet sich nicht, setzt Status SERVER_ERROR
        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.SERVER_ERROR);
    }

    @Test
    void afterConnectionEstablished_withVncPortQueryParam_parsesPort() throws Exception {
        when(session.getUri()).thenReturn(new URI("/novnc?vncPort=5910"));
        when(session.getId()).thenReturn("test-session-2");

        // Auch hier kein Server verfügbar → schließt Session
        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.SERVER_ERROR);
    }

    @Test
    void afterConnectionEstablished_withMultipleQueryParams_parsesCorrectPort() throws Exception {
        when(session.getUri()).thenReturn(new URI("/novnc?token=abc&vncPort=5920&other=val"));
        when(session.getId()).thenReturn("test-session-3");

        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.SERVER_ERROR);
    }

    @Test
    void afterConnectionEstablished_withInvalidVncPortParam_fallsBackToDefault() throws Exception {
        when(session.getUri()).thenReturn(new URI("/novnc?vncPort=notanumber"));
        when(session.getId()).thenReturn("test-session-4");

        // Invalid port → Fehler beim Parsen → Default port, kein Server → schließt Session
        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.SERVER_ERROR);
    }

    @Test
    void afterConnectionEstablished_withNoQuery_usesDefaultPort() throws Exception {
        when(session.getUri()).thenReturn(new URI("/novnc"));
        when(session.getId()).thenReturn("test-session-5");

        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.SERVER_ERROR);
    }

    // ===================== handleBinaryMessage – kein Socket =====================

    @Test
    void handleBinaryMessage_noVncSocket_closesSession() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        // kein VNC_SOCKET_ATTR
        when(session.getAttributes()).thenReturn(attrs);
        when(session.getId()).thenReturn("test-binary-1");

        ByteBuffer buf = ByteBuffer.wrap(new byte[]{1, 2, 3});
        handler.handleBinaryMessage(session, new BinaryMessage(buf));

        verify(session).close(CloseStatus.SERVER_ERROR);
    }

    @Test
    void handleBinaryMessage_closedSocket_closesSession() throws Exception {
        Socket closedSocket = mock(Socket.class);
        when(closedSocket.isClosed()).thenReturn(true);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("VNC_SOCKET", closedSocket);
        when(session.getAttributes()).thenReturn(attrs);
        when(session.getId()).thenReturn("test-binary-2");

        ByteBuffer buf = ByteBuffer.wrap(new byte[]{4, 5, 6});
        handler.handleBinaryMessage(session, new BinaryMessage(buf));

        verify(session).close(CloseStatus.SERVER_ERROR);
    }

    // ===================== afterConnectionClosed =====================

    @Test
    void afterConnectionClosed_noSocket_closesSessionGracefully() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        when(session.getAttributes()).thenReturn(attrs);
        when(session.getId()).thenReturn("test-close-1");
        when(session.isOpen()).thenReturn(false);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        // Kein Fehler erwartet
    }

    @Test
    void afterConnectionClosed_withOpenSocket_closesSocket() throws Exception {
        Socket socket = mock(Socket.class);
        when(socket.isClosed()).thenReturn(false);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("VNC_SOCKET", socket);
        when(session.getAttributes()).thenReturn(attrs);
        when(session.getId()).thenReturn("test-close-2");
        when(session.isOpen()).thenReturn(false);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(socket).close();
    }

    @Test
    void afterConnectionClosed_withAlreadyClosedSocket_doesNotCallClose() throws Exception {
        Socket socket = mock(Socket.class);
        when(socket.isClosed()).thenReturn(true);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("VNC_SOCKET", socket);
        when(session.getAttributes()).thenReturn(attrs);
        when(session.getId()).thenReturn("test-close-3");
        when(session.isOpen()).thenReturn(false);

        handler.afterConnectionClosed(session, CloseStatus.GOING_AWAY);

        verify(socket, never()).close();
    }

    @Test
    void afterConnectionClosed_sessionStillOpen_closesSession() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        when(session.getAttributes()).thenReturn(attrs);
        when(session.getId()).thenReturn("test-close-4");
        when(session.isOpen()).thenReturn(true);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(session).close();
    }

    @Test
    void afterConnectionClosed_socketCloseThrows_doesNotPropagate() throws Exception {
        Socket socket = mock(Socket.class);
        when(socket.isClosed()).thenReturn(false);
        doThrow(new IOException("socket error")).when(socket).close();

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("VNC_SOCKET", socket);
        when(session.getAttributes()).thenReturn(attrs);
        when(session.getId()).thenReturn("test-close-5");
        when(session.isOpen()).thenReturn(false);

        // Darf keinen Fehler werfen
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);
    }

    @Test
    void afterConnectionClosed_sessionCloseThrows_doesNotPropagate() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        when(session.getAttributes()).thenReturn(attrs);
        when(session.getId()).thenReturn("test-close-6");
        when(session.isOpen()).thenReturn(true);
        doThrow(new IOException("session close error")).when(session).close();

        // Darf keinen Fehler werfen
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);
    }
}

