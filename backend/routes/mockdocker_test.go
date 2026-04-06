// Package routes shared mock Docker HTTP server for route tests.
package routes

import (
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
	"github.com/gin-gonic/gin"
	"github.com/stretchr/testify/require"
)

func init() {
	gin.SetMode(gin.TestMode)
}

// routesMock is a configurable fake Docker HTTP server shared across all route tests.
type routesMock struct {
	images     []image.Summary
	containers []container.Summary
	inspects   map[string]interface{} // container name or ID → ContainerJSON-like map
}

func newRoutesMock() *routesMock {
	return &routesMock{inspects: make(map[string]interface{})}
}

func (m *routesMock) normalizePath(path string) string {
	if len(path) > 4 && path[1] == 'v' {
		if parts := strings.SplitN(path[1:], "/", 2); len(parts) == 2 {
			return "/" + parts[1]
		}
	}
	return path
}

func (m *routesMock) ServeHTTP(w http.ResponseWriter, r *http.Request) {
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
	case "/images/create":
		fmt.Fprint(w, `{"status":"Pull complete"}`+"\n")
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
		// auto-register inspect so subsequent GetContainerInfo succeeds
		m.inspects[name] = makeContainerInspect(id, name, cfg.Image, "running", true)
		m.inspects[id] = m.inspects[name]
		m.containers = append(m.containers, container.Summary{
			ID:     id,
			Names:  []string{"/" + name},
			Image:  cfg.Image,
			State:  "created",
			Labels: cfg.Labels,
		})
		w.WriteHeader(http.StatusCreated)
		json.NewEncoder(w).Encode(container.CreateResponse{ID: id})
		return
	}

	switch {
	case strings.HasPrefix(path, "/images/") && strings.HasSuffix(path, "/json"):
		name := strings.TrimSuffix(strings.TrimPrefix(path, "/images/"), "/json")
		for _, img := range m.images {
			for _, tag := range img.RepoTags {
				if tag == name || img.ID == name {
					json.NewEncoder(w).Encode(map[string]interface{}{
						"Id": img.ID, "RepoTags": img.RepoTags,
						"Size": img.Size, "Created": time.Now().Format(time.RFC3339),
						"Architecture": "amd64", "Os": "linux",
					})
					return
				}
			}
		}
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"message": "No such image: " + name})

	case strings.HasPrefix(path, "/images/") && r.Method == http.MethodDelete:
		name := strings.TrimPrefix(path, "/images/")
		found := false
		filtered := m.images[:0]
		for _, img := range m.images {
			keep := true
			for _, tag := range img.RepoTags {
				if tag == name {
					keep = false
					found = true
				}
			}
			if img.ID == name {
				keep = false
				found = true
			}
			if keep {
				filtered = append(filtered, img)
			}
		}
		m.images = filtered
		if !found {
			w.WriteHeader(http.StatusNotFound)
			json.NewEncoder(w).Encode(map[string]string{"message": "No such image: " + name})
			return
		}
		json.NewEncoder(w).Encode([]map[string]string{{"Deleted": name}})

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
		m.updateContainerState(id, "running", true)
		w.WriteHeader(http.StatusNoContent)

	case strings.HasPrefix(path, "/containers/") && strings.HasSuffix(path, "/stop"):
		id := strings.TrimSuffix(strings.TrimPrefix(path, "/containers/"), "/stop")
		m.updateContainerState(id, "exited", false)
		w.WriteHeader(http.StatusNoContent)

	case strings.HasPrefix(path, "/containers/") && r.Method == http.MethodDelete:
		id := strings.TrimPrefix(path, "/containers/")
		m.removeContainer(id)
		w.WriteHeader(http.StatusNoContent)

	default:
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"message": "not implemented: " + path})
	}
}

func (m *routesMock) updateContainerState(idOrName, state string, running bool) {
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

func (m *routesMock) removeContainer(id string) {
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

func makeContainerInspect(id, name, imgRef, state string, running bool) map[string]interface{} {
	finishedAt := "0001-01-01T00:00:00Z"
	if !running {
		finishedAt = time.Now().Add(-1 * time.Hour).Format(time.RFC3339Nano)
	}
	return map[string]interface{}{
		"Id":   id,
		"Name": "/" + name,
		"State": map[string]interface{}{
			"Status":     state,
			"Running":    running,
			"FinishedAt": finishedAt,
		},
		"Config":     map[string]interface{}{"Image": imgRef},
		"HostConfig": map[string]interface{}{},
		"NetworkSettings": map[string]interface{}{
			"IPAddress": "172.17.0.5",
			"Networks":  map[string]interface{}{},
			"Ports":     map[string]interface{}{},
		},
		"Mounts": []interface{}{},
	}
}

func (m *routesMock) addImage(id string, tags ...string) {
	m.images = append(m.images, image.Summary{ID: id, RepoTags: tags, Size: 50 * 1024 * 1024})
}

func (m *routesMock) addRunningContainer(id, name, imgRef string, labels map[string]string) {
	m.containers = append(m.containers, container.Summary{
		ID: id, Names: []string{"/" + name}, Image: imgRef, State: "running", Labels: labels,
	})
	cj := makeContainerInspect(id, name, imgRef, "running", true)
	m.inspects[name] = cj
	m.inspects[id] = cj
}

func (m *routesMock) addStoppedContainer(id, name, imgRef string, labels map[string]string) {
	m.containers = append(m.containers, container.Summary{
		ID: id, Names: []string{"/" + name}, Image: imgRef, State: "exited", Labels: labels,
	})
	cj := makeContainerInspect(id, name, imgRef, "exited", false)
	m.inspects[name] = cj
	m.inspects[id] = cj
}

func newRoutesMockClient(t *testing.T, mock *routesMock) (*client.Client, func()) {
	srv := httptest.NewServer(mock)
	cli, err := client.NewClientWithOpts(
		client.WithHost("tcp://"+strings.TrimPrefix(srv.URL, "http://")),
		client.WithHTTPClient(srv.Client()),
		client.WithAPIVersionNegotiation(),
	)
	require.NoError(t, err)
	return cli, srv.Close
}
