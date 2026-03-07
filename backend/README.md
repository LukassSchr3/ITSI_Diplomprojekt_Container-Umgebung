# ITSI Container Backend - API Documentation

This backend provides Docker container management for a cybersecurity education platform. It manages challenge containers, live Kali Linux environments, and Docker images.

## Base URL
```
http://localhost:3030
```

## Authentication

Currently handled by the Java middleware. All requests should come from the authorized middleware.

---

## Endpoints Overview

### Health Check
- `GET /health` - Check backend status

### Image Management
- `GET /images` - List all images
- `POST /images/inspect` - Get image details
- `POST /images/add` - Pull an image
- `POST /images/update` - Update an image
- `DELETE /images/remove` - Remove an image

### Instance Management (Challenge Containers)
- `GET /instances` - List all instances
- `POST /instances/start` - Start a container
- `POST /instances/stop` - Stop a container
- `POST /instances/reset` - Reset a container

### Live Environment Management
- `GET /live` - List all live environments
- `GET /live/status/:name` - Get user's environment status
- `POST /live/start` - Start live environment
- `POST /live/stop` - Stop live environment
- `POST /live/reset` - Reset live environment

---

## Health Check

### `GET /health`

Check if the backend is running and Docker is accessible.

**Response:**
```json
{
  "Status": "OK"
}
```

---

## Image Management

### `GET /images`

List all Docker images available on the server.

**Response:**
```json
[
  {
    "Id": "sha256:abc123...",
    "RepoTags": ["kali-liveenv:latest"],
    "Size": 1234567890,
    "Created": 1234567890
  }
]
```

---

### `POST /images/inspect`

Get detailed information about a specific Docker image.

**Request Body:**
```json
{
  "reference": "nginx:latest"
}
```

**Response:**
```json
{
  "id": "sha256:abc123...",
  "tags": ["nginx:latest"],
  "size": 142000000,
  "created": "2024-01-01T00:00:00Z",
  "architecture": "amd64",
  "os": "linux"
}
```

---

### `POST /images/add`

Download a Docker image from a registry (Docker Hub, etc.).

**Request Body:**
```json
{
  "reference": "nginx:latest"
}
```

**Response (New Pull):**
```json
{
  "message": "Image pulled and stored successfully",
  "image": {
    "reference": "nginx:latest"
  }
}
```

**Response (Already Exists):**
```json
{
  "message": "Image already exists",
  "image": {
    "reference": "nginx:latest"
  },
  "skipped": true
}
```

---

### `POST /images/update`

Remove the old version of an image and pull the latest version.

**Request Body:**
```json
{
  "reference": "nginx:latest"
}
```

**Response:**
```json
{
  "message": "Image updated successfully",
  "image": {
    "reference": "nginx:latest"
  }
}
```

---

### `DELETE /images/remove`

Delete a Docker image from the server.

**Request Body:**
```json
{
  "reference": "nginx:latest"
}
```

**Response:**
```json
{
  "message": "Image deleted successfully",
  "removed": [
    {
      "Untagged": "nginx:latest",
      "Deleted": "sha256:abc123..."
    }
  ]
}
```

---

## Instance Management

Manage CTF challenge containers for students.

### `GET /instances`

List all container instances (running and stopped).

**Response:**
```json
[
  {
    "Id": "abc123...",
    "Names": ["/student1-web-challenge"],
    "Image": "vulnerable-webapp:latest",
    "State": "running",
    "Status": "Up 5 minutes"
  }
]
```

---

### `POST /instances/start`

Start a challenge container. Creates a new container if it doesn't exist, or restarts an existing stopped container.

**Request Body:**
```json
{
  "name": "student1-web-challenge",
  "reference": "vulnerable-webapp:latest"
}
```

**Response (Created):**
```json
{
  "message": "Instance created and started",
  "containerId": "abc123...",
  "containerName": "student1-web-challenge"
}
```

**Response (Already Running):**
```json
{
  "message": "Container is already running",
  "containerId": "abc123...",
  "containerName": "student1-web-challenge"
}
```

**Response (Restarted):**
```json
{
  "message": "Container started successfully",
  "containerId": "abc123...",
  "containerName": "student1-web-challenge"
}
```

---

### `POST /instances/stop`

Stop a running container (does not delete it).

**Request Body:**
```json
{
  "name": "student1-web-challenge"
}
```

**Response:**
```json
{
  "message": "Container stopped",
  "containerId": "abc123...",
  "containerName": "student1-web-challenge"
}
```

**Error Response (Not Running):**
```json
{
  "error": "Container is not running",
  "containerId": "abc123...",
  "containerName": "student1-web-challenge",
  "state": "exited"
}
```

---

### `POST /instances/reset`

Delete and recreate a container with fresh state. Preserves the original configuration (ports, environment variables, etc.).

**Request Body:**
```json
{
  "name": "student1-web-challenge"
}
```

**Response:**
```json
{
  "message": "Container reset successfully",
  "containerId": "def456...",
  "containerName": "student1-web-challenge",
  "imageRef": "vulnerable-webapp:latest"
}
```

---

## Live Environment Management

Manage Kali Linux live environments with VNC access for students.

### `GET /live`

List all live environment containers for all users.

**Response:**
```json
{
  "message": "Live environments retrieved",
  "data": [
    {
      "container_id": "abc123...",
      "vnc_url": "ws://localhost:32768",
      "vnc_port": "32768",
      "direct_vnc_port": "32769",
      "status": "running",
      "username": "student1"
    }
  ]
}
```

---

### `GET /live/status/:name`

Get the status and connection details for a specific user's live environment.

**URL Parameters:**
- `name` - Username (e.g., `student1`)

**Example:**
```
GET /live/status/student1
```

**Response:**
```json
{
  "message": "Status retrieved",
  "data": {
    "container_id": "abc123...",
    "vnc_url": "ws://localhost:32768",
    "vnc_port": "32768",
    "direct_vnc_port": "32769",
    "status": "running",
    "username": "student1"
  }
}
```

**Error Response (Not Found):**
```json
{
  "error": "NOT_FOUND",
  "code": "NOT_FOUND",
  "message": "live environment for user 'student1' not found"
}
```

---

### `POST /live/start`

Start a Kali Linux live environment for a user. Creates a new container if it doesn't exist, or restarts an existing stopped container.

**Request Body:**
```json
{
  "name": "student1"
}
```

**Response:**
```json
{
  "message": "Live environment started",
  "data": {
    "container_id": "abc123...",
    "vnc_url": "ws://localhost:32768",
    "vnc_port": "32768",
    "direct_vnc_port": "32769",
    "status": "running",
    "username": "student1"
  }
}
```

**Container Specifications:**
- Memory: 2GB RAM
- CPU: 2 cores
- Resolution: 1280x720
- Ports: Randomly assigned to avoid conflicts

**Usage Notes:**
- `vnc_url` - Used by frontend to connect via noVNC (WebSocket)
- `direct_vnc_port` - Can be used with traditional VNC clients

---

### `POST /live/stop`

Stop a user's live environment (does not delete it).

**Request Body:**
```json
{
  "name": "student1"
}
```

**Response:**
```json
{
  "message": "Live environment stopped",
  "data": null
}
```

**Error Response (Not Running):**
```json
{
  "error": "STOP_FAILED",
  "code": "STOP_FAILED",
  "message": "live environment is not running (current state: exited)"
}
```

---

### `POST /live/reset`

Delete and recreate a user's live environment with fresh state.

**Request Body:**
```json
{
  "name": "student1"
}
```

**Response:**
```json
{
  "message": "Live environment reset",
  "data": {
    "container_id": "def456...",
    "vnc_url": "ws://localhost:32770",
    "vnc_port": "32770",
    "direct_vnc_port": "32771",
    "status": "running",
    "username": "student1"
  }
}
```

**Use Cases:**
- Student broke their environment
- Malware infection during security exercises
- Want to start completely fresh

---

## Error Responses

All endpoints return standard error formats.

### Standard Error Response
```json
{
  "error": "ERROR_CODE",
  "code": "ERROR_CODE",
  "message": "Detailed error description"
}
```

### Common Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `INVALID_REQUEST` | 400 | Missing or invalid request body |
| `NOT_FOUND` | 404 | Container, image, or resource not found |
| `START_FAILED` | 500 | Failed to start container |
| `STOP_FAILED` | 500 | Failed to stop container |
| `RESET_FAILED` | 500 | Failed to reset container |
| `LIST_FAILED` | 500 | Failed to list containers |

### Example Error Response
```json
{
  "error": "START_FAILED",
  "code": "START_FAILED",
  "message": "failed to start container: Container abc123 is not running"
}
```

---

## Container Naming Conventions

### Challenge Containers
Format: `{username}-{challenge-name}`

**Examples:**
- `student1-web-challenge`
- `alice-sql-injection`
- `bob-buffer-overflow`

### Live Environments
Format: `liveenv-{username}`

**Examples:**
- `liveenv-student1`
- `liveenv-alice`
- `liveenv-bob`

---

## Resource Limits

### Live Environment Containers
- **Memory**: 2GB per container
- **CPU**: 2 cores per container
- **Disk**: Shared from host
- **Network**: Isolated per user (if enabled)

### Cleanup Policy
- Stopped containers idle for **2+ hours** are automatically removed
- Cleanup runs every **30 minutes**
- Running containers are never auto-removed

---

## Example Workflows

### Workflow 1: Start a Challenge for a Student
```bash
# 1. Check if image exists
curl -X POST http://localhost:3030/images/inspect \
  -H "Content-Type: application/json" \
  -d '{"reference": "vulnerable-webapp:latest"}'

# 2. Start the challenge container
curl -X POST http://localhost:3030/instances/start \
  -H "Content-Type: application/json" \
  -d '{
    "name": "student1-web-challenge",
    "reference": "vulnerable-webapp:latest"
  }'
```

### Workflow 2: Start a Live Environment
```bash
# Start Kali environment for student1
curl -X POST http://localhost:3030/live/start \
  -H "Content-Type: application/json" \
  -d '{"name": "student1"}'

# Frontend connects to the returned vnc_url via noVNC
```

### Workflow 3: Reset a Broken Environment
```bash
# Student broke their Kali environment, reset it
curl -X POST http://localhost:3030/live/reset \
  -H "Content-Type: application/json" \
  -d '{"name": "student1"}'
```

### Workflow 4: Clean Up After Class
```bash
# Stop all instances for a student
curl -X POST http://localhost:3030/instances/stop \
  -H "Content-Type: application/json" \
  -d '{"name": "student1-web-challenge"}'

curl -X POST http://localhost:3030/live/stop \
  -H "Content-Type: application/json" \
  -d '{"name": "student1"}'

# Automatic cleanup will remove them after 2 hours of inactivity
```

---

## Notes

- All timestamps are in UTC
- Container IDs are truncated in responses (first 12 characters)
- Port assignments are random to support multiple concurrent users
- Network isolation ensures users cannot access each other's containers
- The live environment image must be built before starting live environments

---

## CORS Configuration

The backend accepts requests from:
- `http://localhost:9090` (Default frontend)

To modify allowed origins, update the `main.go` configuration.