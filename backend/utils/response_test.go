package utils

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/gin-gonic/gin"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func init() {
	gin.SetMode(gin.TestMode)
}

func newTestContext(method, path string) (*gin.Context, *httptest.ResponseRecorder) {
	w := httptest.NewRecorder()
	c, _ := gin.CreateTestContext(w)
	req, _ := http.NewRequest(method, path, nil)
	c.Request = req
	return c, w
}

func TestRespondError_StatusCode(t *testing.T) {
	c, w := newTestContext(http.MethodGet, "/test")
	RespondError(c, http.StatusBadRequest, "INVALID_INPUT", "Invalid input provided")
	assert.Equal(t, http.StatusBadRequest, w.Code)
}

func TestRespondError_Body(t *testing.T) {
	c, w := newTestContext(http.MethodGet, "/test")
	RespondError(c, http.StatusBadRequest, "INVALID_INPUT", "Invalid input provided")

	var resp ErrorResponse
	err := json.NewDecoder(w.Body).Decode(&resp)
	require.NoError(t, err)
	assert.Equal(t, "INVALID_INPUT", resp.Error)
	assert.Equal(t, "INVALID_INPUT", resp.Code)
	assert.Equal(t, "Invalid input provided", resp.Message)
}

func TestRespondError_InternalServerError(t *testing.T) {
	c, w := newTestContext(http.MethodGet, "/test")
	RespondError(c, http.StatusInternalServerError, "SERVER_ERROR", "Something went wrong")
	assert.Equal(t, http.StatusInternalServerError, w.Code)

	var resp ErrorResponse
	err := json.NewDecoder(w.Body).Decode(&resp)
	require.NoError(t, err)
	assert.Equal(t, "SERVER_ERROR", resp.Code)
	assert.Equal(t, "Something went wrong", resp.Message)
}

func TestRespondSuccess_StatusCode(t *testing.T) {
	c, w := newTestContext(http.MethodGet, "/test")
	RespondSuccess(c, http.StatusOK, "Operation successful", nil)
	assert.Equal(t, http.StatusOK, w.Code)
}

func TestRespondSuccess_WithData(t *testing.T) {
	c, w := newTestContext(http.MethodGet, "/test")
	data := map[string]string{"key": "value"}
	RespondSuccess(c, http.StatusOK, "Operation successful", data)

	var resp SuccessResponse
	err := json.NewDecoder(w.Body).Decode(&resp)
	require.NoError(t, err)
	assert.Equal(t, "Operation successful", resp.Message)
	assert.NotNil(t, resp.Data)
}

func TestRespondSuccess_WithNilData(t *testing.T) {
	c, w := newTestContext(http.MethodGet, "/test")
	RespondSuccess(c, http.StatusOK, "No data", nil)

	var resp SuccessResponse
	err := json.NewDecoder(w.Body).Decode(&resp)
	require.NoError(t, err)
	assert.Equal(t, "No data", resp.Message)
	assert.Nil(t, resp.Data)
}

func TestRespondSuccess_Created(t *testing.T) {
	c, w := newTestContext(http.MethodPost, "/test")
	RespondSuccess(c, http.StatusCreated, "Resource created", map[string]int{"id": 42})
	assert.Equal(t, http.StatusCreated, w.Code)

	var resp SuccessResponse
	err := json.NewDecoder(w.Body).Decode(&resp)
	require.NoError(t, err)
	assert.Equal(t, "Resource created", resp.Message)
}
