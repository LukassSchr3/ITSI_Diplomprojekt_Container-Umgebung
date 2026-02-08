package routes

import (
	"context"
	"fmt"
	"log"
	"net/http"

	"github.com/docker/docker/api/types/container"
	"github.com/docker/docker/client"
	"github.com/gin-gonic/gin"
)

func RegisterLiveEnvironmentRoutes(router *gin.Engine, cli *client.Client) {
	liveenv := router.Group("/live")

	liveenv.POST("/start", func(c *gin.Context) {
		var user struct {
			Name string `json:"name" binding:"required"`
		}
		if err := c.ShouldBindJSON(&user); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}

		ctx := context.Background()
		containerName := fmt.Sprintf("live-env-%s", user.Name)
		
		log.Printf("Starting live environment for user: %s", user.Name)

		// Check if container already exists
		containers, err := cli.ContainerList(ctx, container.ListOptions{All: true})
		if err != nil {
			log.Printf("Failed to list containers: %v", err)
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to list containers"})
			return
		}

		var existingContainer *container.Summary
		for _, cont := range containers {
			for _, name := range cont.Names {
				if name == "/"+containerName {
					existingContainer = &cont
					break
				}
			}
		}

		if existingContainer != nil {
			// Container exists, just start it if not running
			if existingContainer.State == "running" {
				c.JSON(http.StatusOK, gin.H{
					"message":       "Live environment already running",
					"containerName": containerName,
					"containerId":   existingContainer.ID,
				})
				return
			}

			if err := cli.ContainerStart(ctx, existingContainer.ID, container.StartOptions{}); err != nil {
				log.Printf("Failed to start container: %v", err)
				c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to start container"})
				return
			}

			c.JSON(http.StatusOK, gin.H{
				"message":       "Live environment started",
				"containerName": containerName,
				"containerId":   existingContainer.ID,
			})
			return
		}

		// Container doesn't exist - for now just return success
		// In production, you would create the container here
		c.JSON(http.StatusOK, gin.H{
			"message": fmt.Sprintf("Live environment for %s would be created here", user.Name),
			"containerName": containerName,
		})
	})

	liveenv.POST("/stop", func(c *gin.Context) {
		var user struct {
			Name string `json:"name" binding:"required"`
		}
		if err := c.ShouldBindJSON(&user); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}

		ctx := context.Background()
		containerName := fmt.Sprintf("live-env-%s", user.Name)
		
		log.Printf("Stopping live environment for user: %s", user.Name)

		// Find container
		containers, err := cli.ContainerList(ctx, container.ListOptions{All: true})
		if err != nil {
			log.Printf("Failed to list containers: %v", err)
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to list containers"})
			return
		}

		for _, cont := range containers {
			for _, name := range cont.Names {
				if name == "/"+containerName {
					if cont.State != "running" {
						c.JSON(http.StatusOK, gin.H{
							"message":       "Container already stopped",
							"containerName": containerName,
							"containerId":   cont.ID,
						})
						return
					}

					if err := cli.ContainerStop(ctx, cont.ID, container.StopOptions{}); err != nil {
						log.Printf("Failed to stop container: %v", err)
						c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to stop container"})
						return
					}

					c.JSON(http.StatusOK, gin.H{
						"message":       "Live environment stopped",
						"containerName": containerName,
						"containerId":   cont.ID,
					})
					return
				}
			}
		}

		c.JSON(http.StatusNotFound, gin.H{"error": "Container not found"})
	})

	liveenv.POST("/reset", func(c *gin.Context) {
		var user struct {
			Name string `json:"name" binding:"required"`
		}
		if err := c.ShouldBindJSON(&user); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}

		ctx := context.Background()
		containerName := fmt.Sprintf("live-env-%s", user.Name)
		
		log.Printf("Resetting live environment for user: %s", user.Name)

		// Find container
		containers, err := cli.ContainerList(ctx, container.ListOptions{All: true})
		if err != nil {
			log.Printf("Failed to list containers: %v", err)
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to list containers"})
			return
		}

		for _, cont := range containers {
			for _, name := range cont.Names {
				if name == "/"+containerName {
					// Stop if running
					if cont.State == "running" {
						if err := cli.ContainerStop(ctx, cont.ID, container.StopOptions{}); err != nil {
							log.Printf("Failed to stop container: %v", err)
							c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to stop container"})
							return
						}
					}

					// Remove container
					if err := cli.ContainerRemove(ctx, cont.ID, container.RemoveOptions{Force: true}); err != nil {
						log.Printf("Failed to remove container: %v", err)
						c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to remove container"})
						return
					}

					c.JSON(http.StatusOK, gin.H{
						"message":       "Live environment reset (container removed)",
						"containerName": containerName,
					})
					return
				}
			}
		}

		c.JSON(http.StatusNotFound, gin.H{"error": "Container not found"})
	})
}
