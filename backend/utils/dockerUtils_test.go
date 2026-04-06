package utils

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/docker/docker/api/types/container"
	"github.com/docker/docker/api/types/image"
	"github.com/docker/docker/client"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

// utilsMock is a minimal fake Docker HTTP server for utils package tests.
type utilsMock struct {
	images     []image.Summary
	containers []container.Summary
	inspects   map[string]interface{}
	failMode   bool
}

func (m *utilsMock) normalizePath(path string) string {
	if len(path) > 4 && path[1] == 'v' {
		if parts := strings.SplitN(path[1:], "/", 2); len(parts) == 2 {
			return "/" + parts[1]
		}
	}
	return path
}

func (m *utilsMock) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	path := m.normalizePath(r.URL.Path)
	switch {
	case path == "/_ping":
		w.Header().Set("Api-Version", "1.47")
		w.WriteHeader(http.StatusOK)
	case path == "/version":
		json.NewEncoder(w).Encode(map[string]interface{}{
			"Version": "28.0.0", "ApiVersion": "1.47", "MinAPIVersion": "1.24",
		})
	case path == "/images/json":
		if m.failMode {
			w.WriteHeader(http.StatusInternalServerError)
			json.NewEncoder(w).Encode(map[string]string{"message": "daemon error"})
			return
		}
		// Apply reference filter if present (Docker sends: ?filters={"reference":{"tag":true}})
		filtered := m.images
		if f := r.URL.Query().Get("filters"); f != "" {
			var fmap map[string]map[string]bool
			if err := json.Unmarshal([]byte(f), &fmap); err == nil {
				if refs, ok := fmap["reference"]; ok && len(refs) > 0 {
					var matched []image.Summary
					for _, img := range m.images {
						for _, tag := range img.RepoTags {
							if refs[tag] {
								matched = append(matched, img)
							}
						}
					}
					filtered = matched
				}
			}
		}
		json.NewEncoder(w).Encode(filtered)
	case path == "/containers/json":
		if m.failMode {
			w.WriteHeader(http.StatusInternalServerError)
			json.NewEncoder(w).Encode(map[string]string{"message": "daemon error"})
			return
		}
		json.NewEncoder(w).Encode(m.containers)
	default:
		if strings.HasPrefix(path, "/containers/") && strings.HasSuffix(path, "/json") {
			id := strings.TrimSuffix(strings.TrimPrefix(path, "/containers/"), "/json")
			if m.inspects != nil {
				if data, ok := m.inspects[id]; ok {
					json.NewEncoder(w).Encode(data)
					return
				}
			}
			w.WriteHeader(http.StatusNotFound)
			json.NewEncoder(w).Encode(map[string]string{"message": "No such container: " + id})
			return
		}
		w.WriteHeader(http.StatusNotFound)
	}
}

func newUtilsDockerClient(t *testing.T, mock *utilsMock) (*client.Client, func()) {
	srv := httptest.NewServer(mock)
	cli, err := client.NewClientWithOpts(
		client.WithHost("tcp://"+strings.TrimPrefix(srv.URL, "http://")),
		client.WithHTTPClient(srv.Client()),
		client.WithAPIVersionNegotiation(),
	)
	require.NoError(t, err)
	return cli, srv.Close
}

// --- ImageExists tests ---

func TestImageExists_ReturnsTrue(t *testing.T) {
	mock := &utilsMock{
		images: []image.Summary{{ID: "sha256:abc", RepoTags: []string{"ubuntu:22.04"}}},
	}
	cli, cleanup := newUtilsDockerClient(t, mock)
	defer cleanup()

	exists, err := ImageExists(context.Background(), cli, "ubuntu:22.04")
	require.NoError(t, err)
	assert.True(t, exists)
}

func TestImageExists_ReturnsFalseWhenEmpty(t *testing.T) {
	cli, cleanup := newUtilsDockerClient(t, &utilsMock{})
	defer cleanup()

	exists, err := ImageExists(context.Background(), cli, "nonexistent:latest")
	require.NoError(t, err)
	assert.False(t, exists)
}

func TestImageExists_ReturnsFalseWrongTag(t *testing.T) {
	mock := &utilsMock{
		images: []image.Summary{{ID: "sha256:abc", RepoTags: []string{"ubuntu:20.04"}}},
	}
	cli, cleanup := newUtilsDockerClient(t, mock)
	defer cleanup()

	exists, err := ImageExists(context.Background(), cli, "ubuntu:22.04")
	require.NoError(t, err)
	assert.False(t, exists)
}

func TestImageExists_MultipleImages_FindsCorrect(t *testing.T) {
	mock := &utilsMock{
		images: []image.Summary{
			{ID: "sha256:aaa", RepoTags: []string{"nginx:latest"}},
			{ID: "sha256:bbb", RepoTags: []string{"alpine:3.18"}},
		},
	}
	cli, cleanup := newUtilsDockerClient(t, mock)
	defer cleanup()

	exists, err := ImageExists(context.Background(), cli, "alpine:3.18")
	require.NoError(t, err)
	assert.True(t, exists)
}

func TestImageExists_ReturnsErrorOnDaemonFail(t *testing.T) {
	cli, cleanup := newUtilsDockerClient(t, &utilsMock{failMode: true})
	defer cleanup()

	_, err := ImageExists(context.Background(), cli, "ubuntu:22.04")
	assert.Error(t, err)
}

// --- FindContainerByName tests ---

func TestFindContainerByName_Found(t *testing.T) {
	mock := &utilsMock{
		containers: []container.Summary{
			{ID: "abc123", Names: []string{"/my-container"}, State: "running"},
		},
	}
	cli, cleanup := newUtilsDockerClient(t, mock)
	defer cleanup()

	result, err := FindContainerByName(context.Background(), cli, "my-container")
	require.NoError(t, err)
	require.NotNil(t, result)
	assert.Equal(t, "abc123", result.ID)
}

func TestFindContainerByName_NotFound(t *testing.T) {
	cli, cleanup := newUtilsDockerClient(t, &utilsMock{})
	defer cleanup()

	result, err := FindContainerByName(context.Background(), cli, "nonexistent")
	require.NoError(t, err)
	assert.Nil(t, result)
}

func TestFindContainerByName_ReturnsFirst(t *testing.T) {
	mock := &utilsMock{
		containers: []container.Summary{
			{ID: "first-id", Names: []string{"/target"}, State: "running"},
		},
	}
	cli, cleanup := newUtilsDockerClient(t, mock)
	defer cleanup()

	result, err := FindContainerByName(context.Background(), cli, "target")
	require.NoError(t, err)
	require.NotNil(t, result)
	assert.Equal(t, "first-id", result.ID)
}

func TestFindContainerByName_PreservesState(t *testing.T) {
	mock := &utilsMock{
		containers: []container.Summary{
			{ID: "stopped-id", Names: []string{"/offline"}, State: "exited"},
		},
	}
	cli, cleanup := newUtilsDockerClient(t, mock)
	defer cleanup()

	result, err := FindContainerByName(context.Background(), cli, "offline")
	require.NoError(t, err)
	require.NotNil(t, result)
	assert.Equal(t, "exited", result.State)
}

func TestFindContainerByName_ReturnsErrorOnDaemonFail(t *testing.T) {
	cli, cleanup := newUtilsDockerClient(t, &utilsMock{failMode: true})
	defer cleanup()

	_, err := FindContainerByName(context.Background(), cli, "any")
	assert.Error(t, err)
}

// --- GetContainerIP tests ---

func TestGetContainerIP_ReturnsIPFromDefaultNetwork(t *testing.T) {
	mock := &utilsMock{
		inspects: map[string]interface{}{
			"ctr-id": map[string]interface{}{
				"Id": "ctr-id",
				"NetworkSettings": map[string]interface{}{
					"IPAddress": "172.17.0.5",
					"Networks":  map[string]interface{}{},
				},
				"State": map[string]interface{}{"Running": true},
			},
		},
	}
	cli, cleanup := newUtilsDockerClient(t, mock)
	defer cleanup()

	ip := GetContainerIP(context.Background(), cli, "ctr-id")
	assert.Equal(t, "172.17.0.5", ip)
}

func TestGetContainerIP_FallsBackToCustomNetwork(t *testing.T) {
	mock := &utilsMock{
		inspects: map[string]interface{}{
			"ctr-id": map[string]interface{}{
				"Id": "ctr-id",
				"NetworkSettings": map[string]interface{}{
					"IPAddress": "",
					"Networks": map[string]interface{}{
						"mynet": map[string]interface{}{"IPAddress": "10.0.0.3"},
					},
				},
				"State": map[string]interface{}{"Running": true},
			},
		},
	}
	cli, cleanup := newUtilsDockerClient(t, mock)
	defer cleanup()

	ip := GetContainerIP(context.Background(), cli, "ctr-id")
	assert.Equal(t, "10.0.0.3", ip)
}

func TestGetContainerIP_ReturnsEmptyOnNotFound(t *testing.T) {
	mock := &utilsMock{inspects: map[string]interface{}{}}
	cli, cleanup := newUtilsDockerClient(t, mock)
	defer cleanup()

	ip := GetContainerIP(context.Background(), cli, "nonexistent")
	assert.Equal(t, "", ip)
}
