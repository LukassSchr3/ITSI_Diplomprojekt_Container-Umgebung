package routes

import (
	"ITSIContainerBackend/models"
	"ITSIContainerBackend/utils"
	"log"
	"net/http"

	"github.com/docker/docker/api/types/container"
	"github.com/docker/docker/client"
	"github.com/gin-gonic/gin"
)

func RegisterInstanceRoutes(router *gin.Engine, cli *client.Client) {
	instanceRoute := router.Group("/instances")

	// GET /instances - Zählt alle Container Instanzen auf
	instanceRoute.GET("/", func(c *gin.Context) {
		list, err := cli.ContainerList(c.Request.Context(), container.ListOptions{
			All: true,
		})
		if err != nil {
			log.Printf("Failed to list containers: %v", err)
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to list containers: " + err.Error()})
			return
		}
		c.JSON(http.StatusOK, list)
	})

	// POST /instances/start - Starte einen Container
	instanceRoute.POST("/start", func(c *gin.Context) {
		var givenInstance models.Instance
		if err := c.ShouldBindJSON(&givenInstance); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}

		//
		if givenInstance.Name == "" {
			c.JSON(http.StatusBadRequest, gin.H{"error": "Container name is required"})
			return
		}
		if givenInstance.Reference == "" {
			c.JSON(http.StatusBadRequest, gin.H{"error": "imageRef is required"})
			return
		}

		ctx := c.Request.Context()

		//
		exists, err := utils.ImageExists(ctx, cli, givenInstance.Reference)
		if err != nil {
			log.Printf("Failed to check if image %s exists: %v", givenInstance.Reference, err)
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to check image: " + err.Error()})
			return
		}

		if !exists {
			c.JSON(http.StatusNotFound, gin.H{"error": "Image does not exist on server"})
			return
		}

		//
		existing, err := utils.FindContainerByName(ctx, cli, givenInstance.Name)
		if err != nil {
			log.Printf("Failed to find container %s: %v", givenInstance.Name, err)
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to find container: " + err.Error()})
			return
		}

		if existing != nil {
			//
			if existing.State == container.StateRunning {
				c.JSON(http.StatusOK, gin.H{
					"message":       "Container is already running",
					"containerId":   existing.ID,
					"containerName": givenInstance.Name,
				})
				return
			}

			//
			log.Printf("Starting existing container: %s", givenInstance.Name)
			if err := cli.ContainerStart(ctx, existing.ID, container.StartOptions{}); err != nil {
				log.Printf("Failed to start container %s: %v", givenInstance.Name, err)
				c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to start container: " + err.Error()})
				return
			}

			c.JSON(http.StatusOK, gin.H{
				"message":       "Container started successfully",
				"containerId":   existing.ID,
				"containerName": givenInstance.Name,
			})
			return
		}

		//
		log.Printf("Creating new container: %s with image: %s", givenInstance.Name, givenInstance.Reference)
		containerInstance, err := cli.ContainerCreate(
			ctx,
			&container.Config{
				Image: givenInstance.Reference,
			},
			nil, nil, nil,
			givenInstance.Name,
		)

		if err != nil {
			log.Printf("Failed to create container %s: %v", givenInstance.Name, err)
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Container create failed: " + err.Error()})
			return
		}

		if err := cli.ContainerStart(ctx, containerInstance.ID, container.StartOptions{}); err != nil {
			log.Printf("Failed to start container %s: %v", givenInstance.Name, err)
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Container start failed: " + err.Error()})
			return
		}

		log.Printf("Successfully created and started container: %s (ID: %s)", givenInstance.Name, containerInstance.ID)
		c.JSON(http.StatusCreated, gin.H{
			"message":       "Instance created and started",
			"containerId":   containerInstance.ID,
			"containerName": givenInstance.Name,
		})
	})

	// POST /instances/stop - Stoppt einen Container
	instanceRoute.POST("/stop", func(c *gin.Context) {
		var givenInstance models.Instance
		if err := c.ShouldBindJSON(&givenInstance); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}

		if givenInstance.Name == "" {
			c.JSON(http.StatusBadRequest, gin.H{"error": "Container name is required"})
			return
		}

		ctx := c.Request.Context()

		existing, err := utils.FindContainerByName(ctx, cli, givenInstance.Name)
		if err != nil {
			log.Printf("Failed to find container %s: %v", givenInstance.Name, err)
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to check container: " + err.Error()})
			return
		}

		if existing == nil {
			c.JSON(http.StatusNotFound, gin.H{"error": "Container does not exist"})
			return
		}

		if existing.State != container.StateRunning {
			c.JSON(http.StatusConflict, gin.H{
				"error":         "Container is not running",
				"containerId":   existing.ID,
				"containerName": givenInstance.Name,
				"state":         existing.State,
			})
			return
		}

		log.Printf("Stopping container: %s", givenInstance.Name)
		if err := cli.ContainerStop(ctx, existing.ID, container.StopOptions{}); err != nil {
			log.Printf("Failed to stop container %s: %v", givenInstance.Name, err)
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to stop container: " + err.Error()})
			return
		}

		log.Printf("Successfully stopped container: %s", givenInstance.Name)
		c.JSON(http.StatusOK, gin.H{
			"message":       "Container stopped",
			"containerId":   existing.ID,
			"containerName": givenInstance.Name,
		})
	})

	// POST /instances/reset - Setzt eine Instanz zurück
	instanceRoute.POST("/reset", func(c *gin.Context) {
		var givenInstance models.Instance
		if err := c.ShouldBindJSON(&givenInstance); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}

		if givenInstance.Name == "" {
			c.JSON(http.StatusBadRequest, gin.H{"error": "Container name is required"})
			return
		}

		ctx := c.Request.Context()

		// Find the existing container
		existing, err := utils.FindContainerByName(ctx, cli, givenInstance.Name)
		if err != nil {
			log.Printf("Failed to find container %s: %v", givenInstance.Name, err)
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to check container: " + err.Error()})
			return
		}

		if existing == nil {
			c.JSON(http.StatusNotFound, gin.H{"error": "Container does not exist"})
			return
		}

		//
		containerInfo, err := cli.ContainerInspect(ctx, existing.ID)
		if err != nil {
			log.Printf("Failed to inspect container %s: %v", givenInstance.Name, err)
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to inspect container: " + err.Error()})
			return
		}

		imageRef := containerInfo.Config.Image
		log.Printf("Resetting container %s with image: %s", givenInstance.Name, imageRef)

		//
		if existing.State == container.StateRunning {
			log.Printf("Stopping container %s before reset", givenInstance.Name)
			if err := cli.ContainerStop(ctx, existing.ID, container.StopOptions{}); err != nil {
				log.Printf("Failed to stop container %s: %v", givenInstance.Name, err)
				c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to stop container: " + err.Error()})
				return
			}
		}

		//
		log.Printf("Removing container %s", givenInstance.Name)
		if err := cli.ContainerRemove(ctx, existing.ID, container.RemoveOptions{
			RemoveVolumes: true, //
			Force:         true,
		}); err != nil {
			log.Printf("Failed to remove container %s: %v", givenInstance.Name, err)
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to remove container: " + err.Error()})
			return
		}

		//
		log.Printf("Recreating container %s with image: %s", givenInstance.Name, imageRef)
		newContainer, err := cli.ContainerCreate(
			ctx,
			&container.Config{
				Image: imageRef,
			},
			nil, nil, nil,
			givenInstance.Name,
		)

		if err != nil {
			log.Printf("Failed to recreate container %s: %v", givenInstance.Name, err)
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to recreate container: " + err.Error()})
			return
		}

		//
		log.Printf("Starting reset container %s", givenInstance.Name)
		if err := cli.ContainerStart(ctx, newContainer.ID, container.StartOptions{}); err != nil {
			log.Printf("Failed to start reset container %s: %v", givenInstance.Name, err)
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to start reset container: " + err.Error()})
			return
		}

		log.Printf("Successfully reset container: %s (New ID: %s)", givenInstance.Name, newContainer.ID)
		c.JSON(http.StatusOK, gin.H{
			"message":       "Container reset successfully",
			"containerId":   newContainer.ID,
			"containerName": givenInstance.Name,
			"imageRef":      imageRef,
		})
	})
}
