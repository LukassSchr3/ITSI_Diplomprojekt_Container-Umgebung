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

func setupImageRouter(t *testing.T, mock *routesMock) (*gin.Engine, func()) {
	cli, cleanup := newRoutesMockClient(t, mock)
	router := gin.New()
	RegisterImageRoutes(router, cli)
	return router, cleanup
}

// --- GET /images/ ---

func TestImageList_ReturnsAllImages(t *testing.T) {
	mock := newRoutesMock()
	mock.addImage("sha256:aaa", "ubuntu:22.04")
	mock.addImage("sha256:bbb", "nginx:latest")
	router, cleanup := setupImageRouter(t, mock)
	defer cleanup()

	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodGet, "/images/", nil)
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)
	var list []interface{}
	require.NoError(t, json.Unmarshal(w.Body.Bytes(), &list))
	assert.Len(t, list, 2)
}

func TestImageList_EmptyList(t *testing.T) {
	mock := newRoutesMock()
	router, cleanup := setupImageRouter(t, mock)
	defer cleanup()

	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodGet, "/images/", nil)
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)
}

// --- POST /images/inspect ---

func TestImageInspect_ReturnsDetails(t *testing.T) {
	mock := newRoutesMock()
	mock.addImage("sha256:abc123", "nginx:latest")
	router, cleanup := setupImageRouter(t, mock)
	defer cleanup()

	body := `{"reference":"nginx:latest"}`
	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/images/inspect", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)
	var resp map[string]interface{}
	require.NoError(t, json.Unmarshal(w.Body.Bytes(), &resp))
	assert.Equal(t, "sha256:abc123", resp["id"])
	assert.Equal(t, "amd64", resp["architecture"])
}

func TestImageInspect_NotFound(t *testing.T) {
	mock := newRoutesMock()
	router, cleanup := setupImageRouter(t, mock)
	defer cleanup()

	body := `{"reference":"missing:latest"}`
	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/images/inspect", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusNotFound, w.Code)
}

func TestImageInspect_MissingReferenceField(t *testing.T) {
	mock := newRoutesMock()
	router, cleanup := setupImageRouter(t, mock)
	defer cleanup()

	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/images/inspect", bytes.NewBufferString(`{}`))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusBadRequest, w.Code)
}

// --- POST /images/add ---

func TestImageAdd_PullsNewImage(t *testing.T) {
	mock := newRoutesMock()
	router, cleanup := setupImageRouter(t, mock)
	defer cleanup()

	body := `{"reference":"alpine:3.18"}`
	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/images/add", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusCreated, w.Code)
	var resp map[string]interface{}
	require.NoError(t, json.Unmarshal(w.Body.Bytes(), &resp))
	assert.Contains(t, resp["message"], "pulled")
}

func TestImageAdd_SkipsWhenAlreadyExists(t *testing.T) {
	mock := newRoutesMock()
	mock.addImage("sha256:existing", "ubuntu:22.04")
	router, cleanup := setupImageRouter(t, mock)
	defer cleanup()

	body := `{"reference":"ubuntu:22.04"}`
	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/images/add", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)
	var resp map[string]interface{}
	require.NoError(t, json.Unmarshal(w.Body.Bytes(), &resp))
	assert.Equal(t, true, resp["skipped"])
}

func TestImageAdd_EmptyReference(t *testing.T) {
	mock := newRoutesMock()
	router, cleanup := setupImageRouter(t, mock)
	defer cleanup()

	body := `{"reference":""}`
	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/images/add", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusBadRequest, w.Code)
}

func TestImageAdd_InvalidJSON(t *testing.T) {
	mock := newRoutesMock()
	router, cleanup := setupImageRouter(t, mock)
	defer cleanup()

	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/images/add", bytes.NewBufferString(`not-json`))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusBadRequest, w.Code)
}

// --- POST /images/update ---

func TestImageUpdate_RemovesAndRepulls(t *testing.T) {
	mock := newRoutesMock()
	mock.addImage("sha256:old", "nginx:latest")
	router, cleanup := setupImageRouter(t, mock)
	defer cleanup()

	body := `{"reference":"nginx:latest"}`
	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/images/update", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)
	var resp map[string]interface{}
	require.NoError(t, json.Unmarshal(w.Body.Bytes(), &resp))
	assert.Contains(t, resp["message"], "updated")
}

func TestImageUpdate_EmptyReference(t *testing.T) {
	mock := newRoutesMock()
	router, cleanup := setupImageRouter(t, mock)
	defer cleanup()

	body := `{"reference":""}`
	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/images/update", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusBadRequest, w.Code)
}

func TestImageUpdate_InvalidJSON(t *testing.T) {
	mock := newRoutesMock()
	router, cleanup := setupImageRouter(t, mock)
	defer cleanup()

	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodPost, "/images/update", bytes.NewBufferString(`bad`))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusBadRequest, w.Code)
}

// --- DELETE /images/remove ---

func TestImageRemove_Success(t *testing.T) {
	mock := newRoutesMock()
	mock.addImage("sha256:abc", "ubuntu:22.04")
	router, cleanup := setupImageRouter(t, mock)
	defer cleanup()

	body := `{"reference":"ubuntu:22.04"}`
	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodDelete, "/images/remove", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)
	var resp map[string]interface{}
	require.NoError(t, json.Unmarshal(w.Body.Bytes(), &resp))
	assert.Equal(t, "Image deleted successfully", resp["message"])
}

func TestImageRemove_EmptyReference(t *testing.T) {
	mock := newRoutesMock()
	router, cleanup := setupImageRouter(t, mock)
	defer cleanup()

	body := `{"reference":""}`
	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodDelete, "/images/remove", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusBadRequest, w.Code)
}

func TestImageRemove_InvalidJSON(t *testing.T) {
	mock := newRoutesMock()
	router, cleanup := setupImageRouter(t, mock)
	defer cleanup()

	w := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodDelete, "/images/remove", bytes.NewBufferString(`{bad}`))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusBadRequest, w.Code)
}
