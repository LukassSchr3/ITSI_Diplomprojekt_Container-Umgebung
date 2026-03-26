package services

import (
	"context"
	"fmt"
	"log"
	"time"

	"github.com/docker/docker/api/types/container"
	"github.com/docker/docker/api/types/image"
	"github.com/docker/docker/client"
	"github.com/docker/go-connections/nat"
)

const (
	LiveEnvImageName = "kali-liveenv:latest"
	LiveEnvPrefix    = "liveenv-"
)

type LiveEnvService struct {
	cli *client.Client
}

type LiveEnvInfo struct {
	ContainerID string `json:"container_id"`
	VNCUrl      string `json:"vnc_url"`
	VNCPort     string `json:"vnc_port"`
	DirectPort  string `json:"direct_vnc_port"`
	Status      string `json:"status"`
	Username    string `json:"username"`
}

func NewLiveEnvService(cli *client.Client) *LiveEnvService {
	return &LiveEnvService{cli: cli}
}

func (s *LiveEnvService) EnsureImageExists(ctx context.Context) error {
	images, err := s.cli.ImageList(ctx, image.ListOptions{})
	if err != nil {
		return fmt.Errorf("failed to list images: %w", err)
	}

	for _, img := range images {
		for _, tag := range img.RepoTags {
			if tag == LiveEnvImageName {
				return nil
			}
		}
	}

	return fmt.Errorf("live environment image '%s' not found. Please build it first using: docker build -t %s ./live-environment", LiveEnvImageName, LiveEnvImageName)
}

func (s *LiveEnvService) StartContainer(ctx context.Context, username string) (*LiveEnvInfo, error) {
	containerName := LiveEnvPrefix + username

	if err := s.EnsureImageExists(ctx); err != nil {
		return nil, err
	}

	existing, err := s.findContainerByName(ctx, containerName)
	if err != nil {
		return nil, fmt.Errorf("failed to check for existing container: %w", err)
	}

	if existing != nil {
		if existing.State == "running" {
			log.Printf("Live environment for user %s is already running", username)
			return s.GetContainerInfo(ctx, username)
		}

		log.Printf("Starting existing live environment for user: %s", username)
		if err := s.cli.ContainerStart(ctx, existing.ID, container.StartOptions{}); err != nil {
			return nil, fmt.Errorf("failed to start existing container: %w", err)
		}

		return s.GetContainerInfo(ctx, username)
	}

	log.Printf("Creating new live environment for user: %s", username)

	config := &container.Config{
		Image: LiveEnvImageName,
		Env: []string{
			"DISPLAY=:99",
			"VNC_RESOLUTION=1280x720",
			fmt.Sprintf("USER=%s", username),
		},
		ExposedPorts: nat.PortSet{
			"6080/tcp": struct{}{}, // noVNC websocket
			"5900/tcp": struct{}{}, // Direct VNC
		},
		Labels: map[string]string{
			"type": "live-environment",
			"user": username,
		},
		Hostname: fmt.Sprintf("kali-%s", username),
	}

	hostConfig := &container.HostConfig{
		PortBindings: nat.PortMap{
			"6080/tcp": []nat.PortBinding{{HostIP: "0.0.0.0", HostPort: "0"}}, // Random port
			"5900/tcp": []nat.PortBinding{{HostIP: "0.0.0.0", HostPort: "0"}}, // Random port
		},
		Resources: container.Resources{
			Memory:   2 * 1024 * 1024 * 1024, // 2GB RAM limit
			NanoCPUs: 2000000000,             // 2 CPU cores
		},
		AutoRemove: false, // Keep container for restart capability
	}

	resp, err := s.cli.ContainerCreate(ctx, config, hostConfig, nil, nil, containerName)
	if err != nil {
		return nil, fmt.Errorf("failed to create container: %w", err)
	}

	if err := s.cli.ContainerStart(ctx, resp.ID, container.StartOptions{}); err != nil {
		return nil, fmt.Errorf("failed to start container: %w", err)
	}

	log.Printf("Successfully created and started live environment for user: %s (ID: %s)", username, resp.ID)

	time.Sleep(3 * time.Second)

	return s.GetContainerInfo(ctx, username)
}

func (s *LiveEnvService) StopContainer(ctx context.Context, username string) error {
	containerName := LiveEnvPrefix + username

	existing, err := s.findContainerByName(ctx, containerName)
	if err != nil {
		return fmt.Errorf("failed to find container: %w", err)
	}

	if existing == nil {
		return fmt.Errorf("live environment for user '%s' does not exist", username)
	}

	if existing.State != "running" {
		return fmt.Errorf("live environment is not running (current state: %s)", existing.State)
	}

	log.Printf("Stopping live environment for user: %s", username)

	timeout := 30
	stopOptions := container.StopOptions{
		Timeout: &timeout,
	}

	if err := s.cli.ContainerStop(ctx, existing.ID, stopOptions); err != nil {
		return fmt.Errorf("failed to stop container: %w", err)
	}

	log.Printf("Successfully stopped live environment for user: %s", username)
	return nil
}

func (s *LiveEnvService) ResetContainer(ctx context.Context, username string) (*LiveEnvInfo, error) {
	containerName := LiveEnvPrefix + username

	existing, err := s.findContainerByName(ctx, containerName)
	if err != nil {
		return nil, fmt.Errorf("failed to find container: %w", err)
	}

	if existing == nil {
		log.Printf("No existing container found for user %s, creating new one", username)
		return s.StartContainer(ctx, username)
	}

	log.Printf("Resetting live environment for user: %s", username)

	if existing.State == "running" {
		log.Printf("Stopping container before reset: %s", username)
		timeout := 10
		stopOptions := container.StopOptions{
			Timeout: &timeout,
		}
		if err := s.cli.ContainerStop(ctx, existing.ID, stopOptions); err != nil {
			log.Printf("Warning: Failed to stop container: %v", err)
		}
	}

	log.Printf("Removing container for user: %s", username)
	removeOptions := container.RemoveOptions{
		RemoveVolumes: true,
		Force:         true,
	}
	if err := s.cli.ContainerRemove(ctx, existing.ID, removeOptions); err != nil {
		return nil, fmt.Errorf("failed to remove container: %w", err)
	}

	log.Printf("Successfully removed old container, creating fresh one for user: %s", username)

	return s.StartContainer(ctx, username)
}

func (s *LiveEnvService) GetContainerInfo(ctx context.Context, username string) (*LiveEnvInfo, error) {
	containerName := LiveEnvPrefix + username

	containerJSON, err := s.cli.ContainerInspect(ctx, containerName)
	if err != nil {
		if client.IsErrNotFound(err) {
			return nil, fmt.Errorf("live environment for user '%s' not found", username)
		}
		return nil, fmt.Errorf("failed to inspect container: %w", err)
	}

	vncPort := ""
	directPort := ""

	if bindings, ok := containerJSON.NetworkSettings.Ports["6080/tcp"]; ok && len(bindings) > 0 {
		vncPort = bindings[0].HostPort
	}

	if bindings, ok := containerJSON.NetworkSettings.Ports["5900/tcp"]; ok && len(bindings) > 0 {
		directPort = bindings[0].HostPort
	}

	status := "stopped"
	if containerJSON.State.Running {
		status = "running"
	}

	return &LiveEnvInfo{
		ContainerID: containerJSON.ID,
		VNCUrl:      fmt.Sprintf("ws://localhost:%s", vncPort),
		VNCPort:     vncPort,
		DirectPort:  directPort,
		Status:      status,
		Username:    username,
	}, nil
}

func (s *LiveEnvService) ListAllContainers(ctx context.Context) ([]LiveEnvInfo, error) {
	containers, err := s.cli.ContainerList(ctx, container.ListOptions{
		All: true,
	})
	if err != nil {
		return nil, fmt.Errorf("failed to list containers: %w", err)
	}

	var liveEnvs []LiveEnvInfo

	for _, c := range containers {
		if userLabel, ok := c.Labels["user"]; ok && c.Labels["type"] == "live-environment" {
			info, err := s.GetContainerInfo(ctx, userLabel)
			if err != nil {
				log.Printf("Warning: Failed to get info for container %s: %v", c.ID, err)
				continue
			}
			liveEnvs = append(liveEnvs, *info)
		}
	}

	return liveEnvs, nil
}

func (s *LiveEnvService) findContainerByName(ctx context.Context, name string) (*container.Summary, error) {
	containers, err := s.cli.ContainerList(ctx, container.ListOptions{
		All: true,
	})
	if err != nil {
		return nil, err
	}

	for _, c := range containers {
		for _, n := range c.Names {
			if n == "/"+name || n == name {
				return &c, nil
			}
		}
	}

	return nil, nil
}

func (s *LiveEnvService) StartCleanupRoutine(ctx context.Context, maxIdleTime, interval time.Duration) {
	ticker := time.NewTicker(interval)

	go func() {
		defer ticker.Stop()
		log.Printf("Cleanup routine started (interval: %v, max idle: %v)", interval, maxIdleTime)

		for {
			select {
			case <-ticker.C:
				cleanupCtx := context.Background()
				if err := s.cleanupIdleContainers(cleanupCtx, maxIdleTime); err != nil {
					log.Printf("Error during cleanup: %v", err)
				}
			case <-ctx.Done():
				log.Println("Cleanup routine stopped (context cancelled)")
				return
			}
		}
	}()
}

func (s *LiveEnvService) cleanupIdleContainers(ctx context.Context, maxIdleTime time.Duration) error {
	containers, err := s.cli.ContainerList(ctx, container.ListOptions{
		All: true,
	})
	if err != nil {
		return fmt.Errorf("failed to list containers: %w", err)
	}

	cleanedCount := 0

	for _, c := range containers {
		if c.Labels["type"] != "live-environment" {
			continue
		}

		if c.State == "running" {
			continue
		}

		inspect, err := s.cli.ContainerInspect(ctx, c.ID)
		if err != nil {
			log.Printf("Warning: Failed to inspect container %s: %v", c.ID, err)
			continue
		}

		if inspect.State.FinishedAt == "" || inspect.State.FinishedAt == "0001-01-01T00:00:00Z" {
			continue
		}

		finishedAt, err := time.Parse(time.RFC3339Nano, inspect.State.FinishedAt)
		if err != nil {
			log.Printf("Warning: Failed to parse finish time for container %s: %v", c.ID, err)
			continue
		}

		idleDuration := time.Since(finishedAt)
		if idleDuration > maxIdleTime {
			username := c.Labels["user"]
			log.Printf("Cleaning up idle container for user %s (idle for %v)", username, idleDuration)

			removeOptions := container.RemoveOptions{
				RemoveVolumes: true,
				Force:         true,
			}

			if err := s.cli.ContainerRemove(ctx, c.ID, removeOptions); err != nil {
				log.Printf("Warning: Failed to remove container %s: %v", c.ID, err)
			} else {
				cleanedCount++
			}
		}
	}

	if cleanedCount > 0 {
		log.Printf("Cleanup completed: removed %d idle containers", cleanedCount)
	}

	return nil
}
