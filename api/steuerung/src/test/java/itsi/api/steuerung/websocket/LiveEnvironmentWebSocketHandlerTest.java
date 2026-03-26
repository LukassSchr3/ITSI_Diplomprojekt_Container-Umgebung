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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    void sendToUserSessionPresentAndOpenSendsMessage() throws Exception {
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
    void sendToUserSessionNotOpenDoesNotSend() throws Exception {
        when(session.isOpen()).thenReturn(false);

        Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
        sessions.put(2L, session);
        ReflectionTestUtils.setField(handler, "sessions", sessions);

        handler.sendToUser(2L, Map.of("status", "stopped"));
        verify(session, never()).sendMessage(any());
    }

    @Test
    void sendToUserNoSessionForUserDoesNothing() throws Exception {
        Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(handler, "sessions", sessions);

        handler.sendToUser(99L, Map.of("status", "running"));
        verify(session, never()).sendMessage(any());
    }

    @Test
    void sendToUserSendThrowsExceptionDoesNotPropagate() throws Exception {
        when(session.isOpen()).thenReturn(true);
        doThrow(new RuntimeException("IO error")).when(session).sendMessage(any());

        Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
        sessions.put(3L, session);
        ReflectionTestUtils.setField(handler, "sessions", sessions);

        // Darf keine Exception werfen
        handler.sendToUser(3L, Map.of("status", "running"));
    }

    @Test
    void sendToUserMessageIsValidJson() throws Exception {
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
    void sendToUserMultipleUsersSendsOnlyToCorrectUser() throws Exception {
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
    void sendToUserEmptyMapStillSendsValidJson() throws Exception {
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
    void afterConnectionClosedRemovesSessionFromMap() throws Exception {
        Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
        sessions.put(7L, session);
        ReflectionTestUtils.setField(handler, "sessions", sessions);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        assertThat(sessions).doesNotContainValue(session);
    }

    @Test
    void afterConnectionClosedUnknownSessionNoError() throws Exception {
        Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(handler, "sessions", sessions);

        handler.afterConnectionClosed(session, CloseStatus.GOING_AWAY);
        // kein Fehler erwartet
    }

    @Test
    void afterConnectionClosedRemovesOnlyTargetSession() throws Exception {
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
    void afterConnectionClosedWithServerErrorStatusStillRemovesSession() throws Exception {
        Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
        sessions.put(8L, session);
        ReflectionTestUtils.setField(handler, "sessions", sessions);

        handler.afterConnectionClosed(session, CloseStatus.SERVER_ERROR);

        assertThat(sessions).doesNotContainValue(session);
    }

    // ===================== afterConnectionEstablished – URL-Parsing =====================

    @Test
    void afterConnectionEstablishedNoLiveEnvironmentInPathClosesSession() throws Exception {
        when(session.getUri()).thenReturn(new URI("/api/something/else"));

        handler.afterConnectionEstablished(session);

        verify(session).close();
    }

    @Test
    void afterConnectionEstablishedRootPathClosesSession() throws Exception {
        when(session.getUri()).thenReturn(new URI("/"));

        handler.afterConnectionEstablished(session);

        verify(session).close();
    }

    @Test
    void afterConnectionEstablishedEmptySegmentsClosesSession() throws Exception {
        when(session.getUri()).thenReturn(new URI("/api/live-environment"));

        handler.afterConnectionEstablished(session);

        verify(session).close();
    }

    // ===================== handleTextMessage =====================

    @Test
    void handleTextMessageDoesNotThrow() throws Exception {
        // handleTextMessage ist leer – darf nicht werfen
        handler.handleTextMessage(session, new TextMessage("test"));
        verify(session, never()).close();
    }
}
