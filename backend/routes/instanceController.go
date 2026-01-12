package routes

import (
	"ITSIContainerBackend/models"
	"ITSIContainerBackend/utils"
	"net/http"

	"github.com/docker/docker/api/types/container"
	"github.com/docker/docker/client"
	"github.com/gin-gonic/gin"
)

func RegisterInstanceRoutes(router *gin.Engine, cli *client.Client) {
	instanceRoute := router.Group("/instances")
	instanceRoute.GET("/", func(c *gin.Context) {
		list, err := cli.ContainerList(c.Request.Context(), container.ListOptions{
			All: true,
		})
		if err != nil {
			return
		}
		c.JSON(http.StatusOK, list)
	})

	instanceRoute.POST("/start", func(c *gin.Context) {
		var givenInstance models.Instance
		if err := c.ShouldBindJSON(&givenInstance); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}

		ctx := c.Request.Context()

		exists, err := utils.ImageExists(ctx, cli, givenInstance.ImageRef)
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to check image: " + err.Error()})
			return
		}

		if !exists {
			c.JSON(http.StatusNotFound, gin.H{"error": "Image does not exist on server"})
			return
		}

		existing, err := utils.FindContainerByName(ctx, cli, givenInstance.Name)
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to find container: " + err.Error()})
		}

		if existing != nil {
			if existing.State != container.StateRunning {
				if err := cli.ContainerStart(ctx, givenInstance.Name, container.StartOptions{}); err != nil {
					c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to start container: " + err.Error()})
					return
				}
				c.JSON(http.StatusOK, gin.H{
					"message":       "Container started successfully",
					"containerId":   existing.ID,
					"containerName": givenInstance.Name,
				})
			}
		} else {
			containerInstance, err := cli.ContainerCreate(
				ctx,
				&container.Config{
					Image: givenInstance.ImageRef,
				},
				nil, nil, nil,
				givenInstance.Name,
			)

			if err != nil {
				c.JSON(http.StatusInternalServerError, gin.H{"error": "Container create failed: " + err.Error()})
				return
			}

			if err := cli.ContainerStart(ctx, containerInstance.ID, container.StartOptions{}); err != nil {
				c.JSON(http.StatusInternalServerError, gin.H{"error": "Container start failed: " + err.Error()})
				return
			}

			c.JSON(http.StatusOK, gin.H{
				"message":       "Instance started",
				"containerId":   containerInstance.ID,
				"containerName": givenInstance.Name,
			})
		}
	})

	instanceRoute.POST("/stop", func(c *gin.Context) {
		var givenInstance models.Instance
		if err := c.ShouldBindJSON(&givenInstance); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}

		ctx := c.Request.Context()

		existing, err := utils.FindContainerByName(ctx, cli, givenInstance.Name)
		if err != nil {
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
			})
			return
		}

		if err := cli.ContainerStop(ctx, existing.ID, container.StopOptions{}); err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to stop container: " + err.Error()})
			return
		}

		c.JSON(http.StatusOK, gin.H{
			"message":       "Container stopped",
			"containerId":   existing.ID,
			"containerName": givenInstance.Name,
		})
	})

	instanceRoute.POST("/reset", func(c *gin.Context) {

	})
}
