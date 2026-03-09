package routes

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/gin-gonic/gin"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func setupLiveEnvRouter(t *testing.T, mock *routesMock) (*gin.Engine, func()) {
	cli, cleanup := newRoutesMockClient(t, mock)
	router := gin.New()
	RegisterLiveEnvironmentRoutes(router, cli)
	return router, cleanup
}

// addLiveEnvContainer adds a container with the live-environment labels.
func (m *routesMock) addLiveEnvContainer(id, username, state string) {
	labels := map[string]string{"type": "live-environment", "user": username}
	name := "liveenv-" + username
	if state == "running" {
		m.addRunningContainer(id, name, "kali-liveenv:latest", labels)
	} else {
		m.addStoppedContainer(id, name, "kali-liveenv:latest", labels)
	}
}

// --- GET /live/ ---

func TestLiveEnvList_Empty(t *testing.T) {
	mock := newRoutesMock()
	router, cleanup := setupLiveEnvRouter(t, mock)
	defer cleanup()

	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodGet, "/live/", nil)
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)
	var resp map[string]interface{}
	require.NoError(t, json.Unmarshal(w.Body.Bytes(), &resp))
	assert.Equal(t, "Live environments retrieved", resp["message"])
}

func TestLiveEnvList_WithEnvironments(t *testing.T) {
	mock := newRoutesMock()
	mock.addLiveEnvContainer("env-alice-id", "alice", "running")
	mock.addLiveEnvContainer("env-bob-id", "bob", "exited")
	router, cleanup := setupLiveEnvRouter(t, mock)
	defer cleanup()

	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodGet, "/live/", nil)
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)
	var resp map[string]interface{}
	require.NoError(t, json.Unmarshal(w.Body.Bytes(), &resp))
	assert.Equal(t, "Live environments retrieved", resp["message"])
	data, ok := resp["data"].([]interface{})
	require.True(t, ok)
	assert.Len(t, data, 2)
}

// --- GET /live/status/:name ---

func TestLiveEnvStatus_Found(t *testing.T) {
	mock := newRoutesMock()
	mock.addLiveEnvContainer("env-id", "alice", "running")
	router, cleanup := setupLiveEnvRouter(t, mock)
	defer cleanup()

	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodGet, "/live/status/alice", nil)
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)
	var resp map[string]interface{}
	require.NoError(t, json.Unmarshal(w.Body.Bytes(), &resp))
	assert.Equal(t, "Status retrieved", resp["message"])
	data := resp["data"].(map[string]interface{})
	assert.Equal(t, "alice", data["username"])
}

func TestLiveEnvStatus_NotFound(t *testing.T) {
	mock := newRoutesMock()
	router, cleanup := setupLiveEnvRouter(t, mock)
	defer cleanup()

	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodGet, "/live/status/ghost", nil)
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusNotFound, w.Code)
}

// --- POST /live/start ---

func TestLiveEnvStart_AlreadyRunning(t *testing.T) {
	mock := newRoutesMock()
	mock.addImage("kali-id", "kali-liveenv:latest")
	mock.addLiveEnvContainer("env-id", "alice", "running")
	router, cleanup := setupLiveEnvRouter(t, mock)
	defer cleanup()

	body := `{"name":"alice"}`
	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/live/start", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)
	var resp map[string]interface{}
	require.NoError(t, json.Unmarshal(w.Body.Bytes(), &resp))
	assert.Equal(t, "Live environment started", resp["message"])
}

func TestLiveEnvStart_ExistingStopped(t *testing.T) {
	mock := newRoutesMock()
	mock.addImage("kali-id", "kali-liveenv:latest")
	mock.addLiveEnvContainer("env-id", "alice", "exited")
	router, cleanup := setupLiveEnvRouter(t, mock)
	defer cleanup()

	body := `{"name":"alice"}`
	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/live/start", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)
}

func TestLiveEnvStart_ImageMissing(t *testing.T) {
	mock := newRoutesMock() // no kali-liveenv:latest image
	router, cleanup := setupLiveEnvRouter(t, mock)
	defer cleanup()

	body := `{"name":"alice"}`
	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/live/start", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusInternalServerError, w.Code)
}

func TestLiveEnvStart_MissingName(t *testing.T) {
	mock := newRoutesMock()
	router, cleanup := setupLiveEnvRouter(t, mock)
	defer cleanup()

	body := `{}`
	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/live/start", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusBadRequest, w.Code)
}

// --- POST /live/stop ---

func TestLiveEnvStop_Success(t *testing.T) {
	mock := newRoutesMock()
	mock.addLiveEnvContainer("env-id", "alice", "running")
	router, cleanup := setupLiveEnvRouter(t, mock)
	defer cleanup()

	body := `{"name":"alice"}`
	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/live/stop", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)
	var resp map[string]interface{}
	require.NoError(t, json.Unmarshal(w.Body.Bytes(), &resp))
	assert.Equal(t, "Live environment stopped", resp["message"])
}

func TestLiveEnvStop_NotFound(t *testing.T) {
	mock := newRoutesMock()
	router, cleanup := setupLiveEnvRouter(t, mock)
	defer cleanup()

	body := `{"name":"ghost"}`
	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/live/stop", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusInternalServerError, w.Code)
}

func TestLiveEnvStop_AlreadyStopped(t *testing.T) {
	mock := newRoutesMock()
	mock.addLiveEnvContainer("env-id", "alice", "exited")
	router, cleanup := setupLiveEnvRouter(t, mock)
	defer cleanup()

	body := `{"name":"alice"}`
	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/live/stop", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusInternalServerError, w.Code)
}

func TestLiveEnvStop_MissingName(t *testing.T) {
	mock := newRoutesMock()
	router, cleanup := setupLiveEnvRouter(t, mock)
	defer cleanup()

	body := `{}`
	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/live/stop", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusBadRequest, w.Code)
}

// --- POST /live/reset ---

func TestLiveEnvReset_MissingName(t *testing.T) {
	mock := newRoutesMock()
	router, cleanup := setupLiveEnvRouter(t, mock)
	defer cleanup()

	body := `{}`
	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/live/reset", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusBadRequest, w.Code)
}
