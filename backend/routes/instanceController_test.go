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

func setupInstanceRouter(t *testing.T, mock *routesMock) (*gin.Engine, func()) {
	cli, cleanup := newRoutesMockClient(t, mock)
	router := gin.New()
	RegisterInstanceRoutes(router, cli)
	return router, cleanup
}

// --- GET /instances/ ---

func TestInstanceList_ReturnsAll(t *testing.T) {
	mock := newRoutesMock()
	mock.addRunningContainer("abc", "app1", "ubuntu:22.04", nil)
	mock.addStoppedContainer("def", "app2", "nginx:latest", nil)
	router, cleanup := setupInstanceRouter(t, mock)
	defer cleanup()

	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodGet, "/instances/", nil)
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)
	var list []interface{}
	require.NoError(t, json.Unmarshal(w.Body.Bytes(), &list))
	assert.Len(t, list, 2)
}

func TestInstanceList_Empty(t *testing.T) {
	mock := newRoutesMock()
	router, cleanup := setupInstanceRouter(t, mock)
	defer cleanup()

	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodGet, "/instances/", nil)
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)
}

// --- POST /instances/start ---

func TestInstanceStart_CreatesNewContainer(t *testing.T) {
	mock := newRoutesMock()
	mock.addImage("sha256:img", "ubuntu:22.04")
	router, cleanup := setupInstanceRouter(t, mock)
	defer cleanup()

	body := `{"name":"testcontainer","reference":"ubuntu:22.04"}`
	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/instances/start", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusCreated, w.Code)
	var resp map[string]interface{}
	require.NoError(t, json.Unmarshal(w.Body.Bytes(), &resp))
	assert.Equal(t, "testcontainer", resp["containerName"])
}

func TestInstanceStart_AlreadyRunning(t *testing.T) {
	mock := newRoutesMock()
	mock.addImage("sha256:img", "ubuntu:22.04")
	mock.addRunningContainer("ctr-id", "mycontainer", "ubuntu:22.04", nil)
	router, cleanup := setupInstanceRouter(t, mock)
	defer cleanup()

	body := `{"name":"mycontainer","reference":"ubuntu:22.04"}`
	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/instances/start", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)
	var resp map[string]interface{}
	require.NoError(t, json.Unmarshal(w.Body.Bytes(), &resp))
	assert.Contains(t, resp["message"], "already running")
}

func TestInstanceStart_StartsExistingStopped(t *testing.T) {
	mock := newRoutesMock()
	mock.addImage("sha256:img", "ubuntu:22.04")
	mock.addStoppedContainer("stopped-id", "mycontainer", "ubuntu:22.04", nil)
	router, cleanup := setupInstanceRouter(t, mock)
	defer cleanup()

	body := `{"name":"mycontainer","reference":"ubuntu:22.04"}`
	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/instances/start", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)
	var resp map[string]interface{}
	require.NoError(t, json.Unmarshal(w.Body.Bytes(), &resp))
	assert.Contains(t, resp["message"], "started")
}

func TestInstanceStart_ImageNotFound(t *testing.T) {
	mock := newRoutesMock() // no images
	router, cleanup := setupInstanceRouter(t, mock)
	defer cleanup()

	body := `{"name":"mycontainer","reference":"missing:latest"}`
	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/instances/start", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusNotFound, w.Code)
}

func TestInstanceStart_MissingName(t *testing.T) {
	mock := newRoutesMock()
	router, cleanup := setupInstanceRouter(t, mock)
	defer cleanup()

	body := `{"name":"","reference":"ubuntu:22.04"}`
	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/instances/start", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusBadRequest, w.Code)
}

func TestInstanceStart_MissingReference(t *testing.T) {
	mock := newRoutesMock()
	router, cleanup := setupInstanceRouter(t, mock)
	defer cleanup()

	body := `{"name":"mycontainer","reference":""}`
	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/instances/start", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusBadRequest, w.Code)
}

func TestInstanceStart_InvalidJSON(t *testing.T) {
	mock := newRoutesMock()
	router, cleanup := setupInstanceRouter(t, mock)
	defer cleanup()

	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/instances/start", bytes.NewBufferString(`bad`))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusBadRequest, w.Code)
}

// --- POST /instances/stop ---

func TestInstanceStop_Success(t *testing.T) {
	mock := newRoutesMock()
	mock.addRunningContainer("ctr-id", "mycontainer", "ubuntu:22.04", nil)
	router, cleanup := setupInstanceRouter(t, mock)
	defer cleanup()

	body := `{"name":"mycontainer","reference":"ubuntu:22.04"}`
	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/instances/stop", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)
	var resp map[string]interface{}
	require.NoError(t, json.Unmarshal(w.Body.Bytes(), &resp))
	assert.Equal(t, "Container stopped", resp["message"])
}

func TestInstanceStop_ContainerNotFound(t *testing.T) {
	mock := newRoutesMock()
	router, cleanup := setupInstanceRouter(t, mock)
	defer cleanup()

	body := `{"name":"nonexistent","reference":"ubuntu:22.04"}`
	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/instances/stop", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusNotFound, w.Code)
}

func TestInstanceStop_AlreadyStopped(t *testing.T) {
	mock := newRoutesMock()
	mock.addStoppedContainer("ctr-id", "mycontainer", "ubuntu:22.04", nil)
	router, cleanup := setupInstanceRouter(t, mock)
	defer cleanup()

	body := `{"name":"mycontainer","reference":"ubuntu:22.04"}`
	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/instances/stop", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusConflict, w.Code)
}

func TestInstanceStop_MissingName(t *testing.T) {
	mock := newRoutesMock()
	router, cleanup := setupInstanceRouter(t, mock)
	defer cleanup()

	body := `{"name":""}`
	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/instances/stop", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusBadRequest, w.Code)
}

// --- POST /instances/reset ---

func TestInstanceReset_Success(t *testing.T) {
	mock := newRoutesMock()
	mock.addRunningContainer("old-id", "mycontainer", "ubuntu:22.04", nil)
	router, cleanup := setupInstanceRouter(t, mock)
	defer cleanup()

	body := `{"name":"mycontainer","reference":"ubuntu:22.04"}`
	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/instances/reset", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)
	var resp map[string]interface{}
	require.NoError(t, json.Unmarshal(w.Body.Bytes(), &resp))
	assert.Equal(t, "Container reset successfully", resp["message"])
}

func TestInstanceReset_ContainerNotFound(t *testing.T) {
	mock := newRoutesMock()
	router, cleanup := setupInstanceRouter(t, mock)
	defer cleanup()

	body := `{"name":"ghost","reference":"ubuntu:22.04"}`
	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/instances/reset", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusNotFound, w.Code)
}

func TestInstanceReset_MissingName(t *testing.T) {
	mock := newRoutesMock()
	router, cleanup := setupInstanceRouter(t, mock)
	defer cleanup()

	body := `{"name":"","reference":"ubuntu:22.04"}`
	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/instances/reset", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusBadRequest, w.Code)
}
