package services

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/docker/docker/api/types/container"
	"github.com/docker/docker/api/types/image"
	"github.com/docker/docker/client"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

// svcMock is a minimal fake Docker HTTP server for service-level tests.
type svcMock struct {
	images     []image.Summary
	containers []container.Summary
	inspects   map[string]interface{}
}

func newSvcMock() *svcMock {
	return &svcMock{inspects: make(map[string]interface{})}
}

func (m *svcMock) normalizePath(path string) string {
	if len(path) > 4 && path[1] == 'v' {
		if parts := strings.SplitN(path[1:], "/", 2); len(parts) == 2 {
			return "/" + parts[1]
		}
	}
	return path
}

func (m *svcMock) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	path := m.normalizePath(r.URL.Path)

	switch path {
	case "/_ping":
		w.Header().Set("Api-Version", "1.47")
		w.WriteHeader(http.StatusOK)
		return
	case "/version":
		json.NewEncoder(w).Encode(map[string]interface{}{
			"Version": "28.0.0", "ApiVersion": "1.47", "MinAPIVersion": "1.24",
		})
		return
	case "/images/json":
		json.NewEncoder(w).Encode(m.images)
		return
	case "/containers/json":
		json.NewEncoder(w).Encode(m.containers)
		return
	case "/containers/create":
		var cfg struct {
			Image  string            `json:"Image"`
			Labels map[string]string `json:"Labels"`
		}
		json.NewDecoder(r.Body).Decode(&cfg)
		name := r.URL.Query().Get("name")
		id := fmt.Sprintf("ctr-%s-id", name)
		cj := makeSvcContainerInspect(id, name, cfg.Image, "running", true)
		m.containers = append(m.containers, container.Summary{
			ID: id, Names: []string{"/" + name}, Image: cfg.Image, State: "created", Labels: cfg.Labels,
		})
		m.inspects[name] = cj
		m.inspects[id] = cj
		w.WriteHeader(http.StatusCreated)
		json.NewEncoder(w).Encode(container.CreateResponse{ID: id})
		return
	}

	switch {
	case strings.HasPrefix(path, "/containers/") && strings.HasSuffix(path, "/json"):
		key := strings.TrimSuffix(strings.TrimPrefix(path, "/containers/"), "/json")
		if data, ok := m.inspects[key]; ok {
			json.NewEncoder(w).Encode(data)
			return
		}
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"message": "No such container: " + key})

	case strings.HasPrefix(path, "/containers/") && strings.HasSuffix(path, "/start"):
		id := strings.TrimSuffix(strings.TrimPrefix(path, "/containers/"), "/start")
		m.setState(id, "running", true)
		w.WriteHeader(http.StatusNoContent)

	case strings.HasPrefix(path, "/containers/") && strings.HasSuffix(path, "/stop"):
		id := strings.TrimSuffix(strings.TrimPrefix(path, "/containers/"), "/stop")
		m.setState(id, "exited", false)
		w.WriteHeader(http.StatusNoContent)

	case strings.HasPrefix(path, "/containers/") && r.Method == http.MethodDelete:
		id := strings.TrimPrefix(path, "/containers/")
		m.removeContainer(id)
		w.WriteHeader(http.StatusNoContent)

	default:
		w.WriteHeader(http.StatusNotFound)
	}
}

func (m *svcMock) setState(idOrName, state string, running bool) {
	for i, c := range m.containers {
		if c.ID == idOrName {
			m.containers[i].State = state
		}
	}
	if data, ok := m.inspects[idOrName]; ok {
		if cmap, ok := data.(map[string]interface{}); ok {
			if st, ok := cmap["State"].(map[string]interface{}); ok {
				st["Running"] = running
				st["Status"] = state
			}
		}
	}
}

func (m *svcMock) removeContainer(id string) {
	filtered := m.containers[:0]
	for _, c := range m.containers {
		if c.ID != id {
			filtered = append(filtered, c)
		}
	}
	m.containers = filtered
	for k, v := range m.inspects {
		if cmap, ok := v.(map[string]interface{}); ok {
			if cmap["Id"] == id || k == id {
				delete(m.inspects, k)
			}
		}
	}
}

func makeSvcContainerInspect(id, name, imgRef, state string, running bool) map[string]interface{} {
	finishedAt := "0001-01-01T00:00:00Z"
	if !running {
		finishedAt = time.Now().Add(-1 * time.Hour).Format(time.RFC3339Nano)
	}
	return map[string]interface{}{
		"Id":   id,
		"Name": "/" + name,
		"State": map[string]interface{}{
			"Status": state, "Running": running, "FinishedAt": finishedAt,
		},
		"Config":     map[string]interface{}{"Image": imgRef},
		"HostConfig": map[string]interface{}{},
		"NetworkSettings": map[string]interface{}{
			"Ports": map[string]interface{}{
				"6080/tcp": []interface{}{
					map[string]interface{}{"HostIp": "0.0.0.0", "HostPort": "32768"},
				},
				"5900/tcp": []interface{}{
					map[string]interface{}{"HostIp": "0.0.0.0", "HostPort": "32769"},
				},
			},
		},
		"Mounts": []interface{}{},
	}
}

func (m *svcMock) addImage(id string, tags ...string) {
	m.images = append(m.images, image.Summary{ID: id, RepoTags: tags})
}

func (m *svcMock) addRunningContainer(id, name, imgRef string, labels map[string]string) {
	m.containers = append(m.containers, container.Summary{
		ID: id, Names: []string{"/" + name}, Image: imgRef, State: "running", Labels: labels,
	})
	cj := makeSvcContainerInspect(id, name, imgRef, "running", true)
	m.inspects[name] = cj
	m.inspects[id] = cj
}

func (m *svcMock) addStoppedContainer(id, name, imgRef string, labels map[string]string) {
	m.containers = append(m.containers, container.Summary{
		ID: id, Names: []string{"/" + name}, Image: imgRef, State: "exited", Labels: labels,
	})
	cj := makeSvcContainerInspect(id, name, imgRef, "exited", false)
	m.inspects[name] = cj
	m.inspects[id] = cj
}

func newSvcDockerClient(t *testing.T, mock *svcMock) (*client.Client, func()) {
	srv := httptest.NewServer(mock)
	cli, err := client.NewClientWithOpts(
		client.WithHost("tcp://"+strings.TrimPrefix(srv.URL, "http://")),
		client.WithHTTPClient(srv.Client()),
		client.WithAPIVersionNegotiation(),
	)
	require.NoError(t, err)
	return cli, srv.Close
}

// --- EnsureImageExists ---

func TestEnsureImageExists_Found(t *testing.T) {
	mock := newSvcMock()
	mock.addImage("kali-id", "kali-liveenv:latest")
	cli, cleanup := newSvcDockerClient(t, mock)
	defer cleanup()

	svc := NewLiveEnvService(cli)
	err := svc.EnsureImageExists(context.Background())
	assert.NoError(t, err)
}

func TestEnsureImageExists_NotFound(t *testing.T) {
	mock := newSvcMock()
	cli, cleanup := newSvcDockerClient(t, mock)
	defer cleanup()

	svc := NewLiveEnvService(cli)
	err := svc.EnsureImageExists(context.Background())
	require.Error(t, err)
	assert.Contains(t, err.Error(), "not found")
}

func TestEnsureImageExists_WrongImage(t *testing.T) {
	mock := newSvcMock()
	mock.addImage("other-id", "ubuntu:22.04")
	cli, cleanup := newSvcDockerClient(t, mock)
	defer cleanup()

	svc := NewLiveEnvService(cli)
	err := svc.EnsureImageExists(context.Background())
	require.Error(t, err)
}

// --- GetContainerInfo ---

func TestGetContainerInfo_Running(t *testing.T) {
	mock := newSvcMock()
	mock.addRunningContainer("env-id", "liveenv-alice", "kali-liveenv:latest", nil)
	cli, cleanup := newSvcDockerClient(t, mock)
	defer cleanup()

	svc := NewLiveEnvService(cli)
	info, err := svc.GetContainerInfo(context.Background(), "alice")
	require.NoError(t, err)
	assert.Equal(t, "alice", info.Username)
	assert.Equal(t, "running", info.Status)
	assert.Equal(t, "32768", info.VNCPort)
	assert.Equal(t, "32769", info.DirectPort)
}

func TestGetContainerInfo_Stopped(t *testing.T) {
	mock := newSvcMock()
	mock.addStoppedContainer("env-id", "liveenv-bob", "kali-liveenv:latest", nil)
	cli, cleanup := newSvcDockerClient(t, mock)
	defer cleanup()

	svc := NewLiveEnvService(cli)
	info, err := svc.GetContainerInfo(context.Background(), "bob")
	require.NoError(t, err)
	assert.Equal(t, "stopped", info.Status)
}

func TestGetContainerInfo_NotFound(t *testing.T) {
	mock := newSvcMock()
	cli, cleanup := newSvcDockerClient(t, mock)
	defer cleanup()

	svc := NewLiveEnvService(cli)
	_, err := svc.GetContainerInfo(context.Background(), "ghost")
	require.Error(t, err)
	assert.Contains(t, err.Error(), "not found")
}

// --- StopContainer ---

func TestStopContainer_Success(t *testing.T) {
	mock := newSvcMock()
	mock.addRunningContainer("env-id", "liveenv-alice", "kali-liveenv:latest", nil)
	cli, cleanup := newSvcDockerClient(t, mock)
	defer cleanup()

	svc := NewLiveEnvService(cli)
	err := svc.StopContainer(context.Background(), "alice")
	assert.NoError(t, err)
}

func TestStopContainer_NotFound(t *testing.T) {
	mock := newSvcMock()
	cli, cleanup := newSvcDockerClient(t, mock)
	defer cleanup()

	svc := NewLiveEnvService(cli)
	err := svc.StopContainer(context.Background(), "nobody")
	require.Error(t, err)
	assert.Contains(t, err.Error(), "does not exist")
}

func TestStopContainer_AlreadyStopped(t *testing.T) {
	mock := newSvcMock()
	mock.addStoppedContainer("env-id", "liveenv-alice", "kali-liveenv:latest", nil)
	cli, cleanup := newSvcDockerClient(t, mock)
	defer cleanup()

	svc := NewLiveEnvService(cli)
	err := svc.StopContainer(context.Background(), "alice")
	require.Error(t, err)
	assert.Contains(t, err.Error(), "not running")
}

// --- ListAllContainers ---

func TestListAllContainers_Empty(t *testing.T) {
	mock := newSvcMock()
	cli, cleanup := newSvcDockerClient(t, mock)
	defer cleanup()

	svc := NewLiveEnvService(cli)
	list, err := svc.ListAllContainers(context.Background())
	require.NoError(t, err)
	assert.Empty(t, list)
}

func TestListAllContainers_IgnoresNonLiveEnv(t *testing.T) {
	mock := newSvcMock()
	mock.addRunningContainer("ctr-id", "some-app", "ubuntu:22.04", nil) // no live-env label
	cli, cleanup := newSvcDockerClient(t, mock)
	defer cleanup()

	svc := NewLiveEnvService(cli)
	list, err := svc.ListAllContainers(context.Background())
	require.NoError(t, err)
	assert.Empty(t, list)
}

func TestListAllContainers_WithLiveEnvs(t *testing.T) {
	mock := newSvcMock()
	labels := map[string]string{"type": "live-environment", "user": "alice"}
	mock.addRunningContainer("env-alice", "liveenv-alice", "kali-liveenv:latest", labels)
	cli, cleanup := newSvcDockerClient(t, mock)
	defer cleanup()

	svc := NewLiveEnvService(cli)
	list, err := svc.ListAllContainers(context.Background())
	require.NoError(t, err)
	require.Len(t, list, 1)
	assert.Equal(t, "alice", list[0].Username)
}

// --- StartContainer (paths without time.Sleep) ---

func TestStartContainer_AlreadyRunning(t *testing.T) {
	mock := newSvcMock()
	mock.addImage("kali-id", "kali-liveenv:latest")
	mock.addRunningContainer("env-id", "liveenv-alice", "kali-liveenv:latest", nil)
	cli, cleanup := newSvcDockerClient(t, mock)
	defer cleanup()

	svc := NewLiveEnvService(cli)
	info, err := svc.StartContainer(context.Background(), "alice", "5901")
	require.NoError(t, err)
	assert.Equal(t, "alice", info.Username)
}

func TestStartContainer_ExistingStopped(t *testing.T) {
	mock := newSvcMock()
	mock.addImage("kali-id", "kali-liveenv:latest")
	mock.addStoppedContainer("env-id", "liveenv-alice", "kali-liveenv:latest", nil)
	cli, cleanup := newSvcDockerClient(t, mock)
	defer cleanup()

	svc := NewLiveEnvService(cli)
	info, err := svc.StartContainer(context.Background(), "alice", "5901")
	require.NoError(t, err)
	assert.Equal(t, "alice", info.Username)
}

func TestStartContainer_ImageNotFound(t *testing.T) {
	mock := newSvcMock() // no kali-liveenv:latest
	cli, cleanup := newSvcDockerClient(t, mock)
	defer cleanup()

	svc := NewLiveEnvService(cli)
	_, err := svc.StartContainer(context.Background(), "alice", "5901")
	require.Error(t, err)
}

// --- ResetContainer ---

func TestResetContainer_ExistingRunning(t *testing.T) {
	mock := newSvcMock()
	mock.addImage("kali-id", "kali-liveenv:latest")
	mock.addRunningContainer("env-id", "liveenv-alice", "kali-liveenv:latest", nil)
	cli, cleanup := newSvcDockerClient(t, mock)
	defer cleanup()

	svc := NewLiveEnvService(cli)
	info, err := svc.ResetContainer(context.Background(), "alice", "5901")
	require.NoError(t, err)
	assert.Equal(t, "alice", info.Username)
}

func TestResetContainer_ExistingStopped(t *testing.T) {
	mock := newSvcMock()
	mock.addImage("kali-id", "kali-liveenv:latest")
	mock.addStoppedContainer("env-id", "liveenv-alice", "kali-liveenv:latest", nil)
	cli, cleanup := newSvcDockerClient(t, mock)
	defer cleanup()

	svc := NewLiveEnvService(cli)
	info, err := svc.ResetContainer(context.Background(), "alice", "5901")
	require.NoError(t, err)
	assert.Equal(t, "alice", info.Username)
}

func TestResetContainer_NoExisting_CreatesNew(t *testing.T) {
	mock := newSvcMock()
	mock.addImage("kali-id", "kali-liveenv:latest")
	// No existing container
	cli, cleanup := newSvcDockerClient(t, mock)
	defer cleanup()

	svc := NewLiveEnvService(cli)
	info, err := svc.ResetContainer(context.Background(), "bob", "5901")
	require.NoError(t, err)
	assert.Equal(t, "bob", info.Username)
}

// --- cleanupIdleContainers (via exported StartContainer path) ---

func TestCleanupIdleContainers_RemovesOldStopped(t *testing.T) {
	mock := newSvcMock()
	// Add a stopped live-env container with old FinishedAt
	labels := map[string]string{"type": "live-environment", "user": "olduser"}
	mock.addStoppedContainer("old-id", "liveenv-olduser", "kali-liveenv:latest", labels)
	cli, cleanup := newSvcDockerClient(t, mock)
	defer cleanup()

	svc := NewLiveEnvService(cli)
	err := svc.cleanupIdleContainers(context.Background(), 30*time.Minute)
	require.NoError(t, err)
	// Container was stopped >1h ago (set by addStoppedContainer), so it should be removed
	assert.Empty(t, mock.containers)
}

func TestCleanupIdleContainers_KeepsRunning(t *testing.T) {
	mock := newSvcMock()
	labels := map[string]string{"type": "live-environment", "user": "activeuser"}
	mock.addRunningContainer("run-id", "liveenv-activeuser", "kali-liveenv:latest", labels)
	cli, cleanup := newSvcDockerClient(t, mock)
	defer cleanup()

	svc := NewLiveEnvService(cli)
	err := svc.cleanupIdleContainers(context.Background(), 30*time.Minute)
	require.NoError(t, err)
	assert.Len(t, mock.containers, 1)
}

func TestCleanupIdleContainers_IgnoresNonLiveEnv(t *testing.T) {
	mock := newSvcMock()
	mock.addStoppedContainer("other-id", "some-app", "ubuntu:22.04", nil) // no labels
	cli, cleanup := newSvcDockerClient(t, mock)
	defer cleanup()

	svc := NewLiveEnvService(cli)
	err := svc.cleanupIdleContainers(context.Background(), 0)
	require.NoError(t, err)
	assert.Len(t, mock.containers, 1) // untouched
}

// --- StartCleanupRoutine ---

func TestStartCleanupRoutine_StartsAndCancels(t *testing.T) {
	mock := newSvcMock()
	cli, cleanup := newSvcDockerClient(t, mock)
	defer cleanup()

	svc := NewLiveEnvService(cli)
	ctx, cancel := context.WithCancel(context.Background())

	// Start routine with very short interval
	svc.StartCleanupRoutine(ctx, 30*time.Minute, 50*time.Millisecond)

	// Let it tick at least once
	time.Sleep(120 * time.Millisecond)

	// Cancel → goroutine should exit
	cancel()
	time.Sleep(20 * time.Millisecond) // give goroutine time to exit
}
