package routes

import (
	"ITSIContainerBackend/models"
	"io"
	"log"
	"net/http"
	"strings"

	"github.com/docker/docker/api/types/image"
	"github.com/docker/docker/client"
	"github.com/gin-gonic/gin"
)

func RegisterImageRoutes(router *gin.Engine, cli *client.Client) {
	imageRoute := router.Group("/images")

	imageRoute.GET("/", func(c *gin.Context) {
		list, err := cli.ImageList(c.Request.Context(), image.ListOptions{})
		if err != nil {
			return
		}
		c.JSON(http.StatusOK, list)
	})

	imageRoute.POST("/add", func(c *gin.Context) {
		var givenImage models.Image
		if err := c.ShouldBindJSON(&givenImage); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}

		ctx := c.Request.Context()
		out, err := cli.ImagePull(ctx, givenImage.ImageRef, image.PullOptions{})
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": "Failed to pull image: " + err.Error()})
			return
		}
		defer out.Close()

		buf := new(strings.Builder)
		if _, err := io.Copy(buf, out); err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to read pull output: " + err.Error()})
			return
		}

		if strings.Contains(buf.String(), `"error"`) {
			c.JSON(http.StatusBadRequest, gin.H{"error": "Docker pull failed: " + buf.String()})
			return
		}

		c.JSON(http.StatusCreated, gin.H{
			"message": "Image pulled and stored successfully",
			"image":   givenImage,
		})
	})

	imageRoute.DELETE("/remove", func(c *gin.Context) {
		var givenImage models.Image
		if err := c.ShouldBindJSON(&givenImage); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}

		ctx := c.Request.Context()
		removed, err := cli.ImageRemove(ctx, givenImage.ImageRef, image.RemoveOptions{
			Force:         true,
			PruneChildren: true,
		})
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to remove Docker image: " + err.Error()})
			return
		}

		log.Printf("Removed image: %+v", removed)

		c.JSON(http.StatusOK, gin.H{
			"message": "Image deleted successfully",
		})
	})
}
