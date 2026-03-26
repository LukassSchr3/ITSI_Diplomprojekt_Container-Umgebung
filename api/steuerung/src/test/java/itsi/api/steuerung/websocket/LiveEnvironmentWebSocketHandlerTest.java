package itsi.api.steuerung.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveEnvironmentWebSocketHandlerTest {

    @Mock
    private WebSocketSession session;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    private LiveEnvironmentWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        handler = new LiveEnvironmentWebSocketHandler(webClientBuilder);
    }

    // ===================== sendToUser =====================

    @Test
    void sendToUser_sessionPresentAndOpen_sendsMessage() throws Exception {
        when(session.isOpen()).thenReturn(true);

        Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
        sessions.put(1L, session);
        ReflectionTestUtils.setField(handler, "sessions", sessions);

        Map<String, Object> liveEnv = Map.of("id", 1, "status", "running");
        handler.sendToUser(1L, liveEnv);

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());

        String payload = captor.getValue().getPayload();
        assertThat(payload).contains("running");
        assertThat(payload).contains("status");
    }

    @Test
    void sendToUser_sessionNotOpen_doesNotSend() throws Exception {
        when(session.isOpen()).thenReturn(false);

        Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
        sessions.put(2L, session);
        ReflectionTestUtils.setField(handler, "sessions", sessions);

        handler.sendToUser(2L, Map.of("status", "stopped"));
        verify(session, never()).sendMessage(any());
    }

    @Test
    void sendToUser_noSessionForUser_doesNothing() throws Exception {
        Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(handler, "sessions", sessions);

        handler.sendToUser(99L, Map.of("status", "running"));
        verify(session, never()).sendMessage(any());
    }

    @Test
    void sendToUser_sendThrowsException_doesNotPropagate() throws Exception {
        when(session.isOpen()).thenReturn(true);
        doThrow(new RuntimeException("IO error")).when(session).sendMessage(any());

        Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
        sessions.put(3L, session);
        ReflectionTestUtils.setField(handler, "sessions", sessions);

        // Darf keine Exception werfen
        handler.sendToUser(3L, Map.of("status", "running"));
    }

    @Test
    void sendToUser_messageIsValidJson() throws Exception {
        when(session.isOpen()).thenReturn(true);

        Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
        sessions.put(5L, session);
        ReflectionTestUtils.setField(handler, "sessions", sessions);

        Map<String, Object> liveEnv = new HashMap<>();
        liveEnv.put("id", 42);
        liveEnv.put("status", "running");
        liveEnv.put("vncPort", 5905);

        handler.sendToUser(5L, liveEnv);

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());

        ObjectMapper mapper = new ObjectMapper();
        Map<?, ?> parsed = mapper.readValue(captor.getValue().getPayload(), Map.class);
        assertThat(parsed.get("vncPort")).isEqualTo(5905);
        assertThat(parsed.get("status")).isEqualTo("running");
    }

    @Test
    void sendToUser_multipleUsers_sendsOnlyToCorrectUser() throws Exception {
        WebSocketSession session2 = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);

        Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
        sessions.put(1L, session);
        sessions.put(2L, session2);
        ReflectionTestUtils.setField(handler, "sessions", sessions);

        handler.sendToUser(1L, Map.of("status", "running"));

        verify(session, times(1)).sendMessage(any());
        verify(session2, never()).sendMessage(any());
    }

    @Test
    void sendToUser_emptyMapStillSendsValidJson() throws Exception {
        when(session.isOpen()).thenReturn(true);

        Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
        sessions.put(6L, session);
        ReflectionTestUtils.setField(handler, "sessions", sessions);

        handler.sendToUser(6L, new HashMap<>());

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        assertThat(captor.getValue().getPayload()).isEqualTo("{}");
    }

    // ===================== afterConnectionClosed =====================

    @Test
    void afterConnectionClosed_removesSessionFromMap() throws Exception {
        Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
        sessions.put(7L, session);
        ReflectionTestUtils.setField(handler, "sessions", sessions);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        assertThat(sessions).doesNotContainValue(session);
    }

    @Test
    void afterConnectionClosed_unknownSession_noError() throws Exception {
        Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(handler, "sessions", sessions);

        handler.afterConnectionClosed(session, CloseStatus.GOING_AWAY);
        // kein Fehler erwartet
    }

    @Test
    void afterConnectionClosed_removesOnlyTargetSession() throws Exception {
        WebSocketSession otherSession = mock(WebSocketSession.class);

        Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
        sessions.put(10L, session);
        sessions.put(11L, otherSession);
        ReflectionTestUtils.setField(handler, "sessions", sessions);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        assertThat(sessions).doesNotContainValue(session);
        assertThat(sessions).containsValue(otherSession);
    }

    @Test
    void afterConnectionClosed_withServerErrorStatus_stillRemovesSession() throws Exception {
        Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
        sessions.put(8L, session);
        ReflectionTestUtils.setField(handler, "sessions", sessions);

        handler.afterConnectionClosed(session, CloseStatus.SERVER_ERROR);

        assertThat(sessions).doesNotContainValue(session);
    }

    // ===================== afterConnectionEstablished – URL-Parsing =====================

    @Test
    void afterConnectionEstablished_noLiveEnvironmentInPath_closesSession() throws Exception {
        when(session.getUri()).thenReturn(new URI("/api/something/else"));

        handler.afterConnectionEstablished(session);

        verify(session).close();
    }

    @Test
    void afterConnectionEstablished_rootPath_closesSession() throws Exception {
        when(session.getUri()).thenReturn(new URI("/"));

        handler.afterConnectionEstablished(session);

        verify(session).close();
    }

    @Test
    void afterConnectionEstablished_emptySegments_closesSession() throws Exception {
        when(session.getUri()).thenReturn(new URI("/api/live-environment"));

        handler.afterConnectionEstablished(session);

        verify(session).close();
    }

    // ===================== handleTextMessage =====================

    @Test
    void handleTextMessage_doesNotThrow() throws Exception {
        // handleTextMessage ist leer – darf nicht werfen
        handler.handleTextMessage(session, new TextMessage("test"));
        verify(session, never()).close();
    }
}
