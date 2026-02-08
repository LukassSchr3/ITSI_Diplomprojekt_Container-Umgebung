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

	// GET /images - Zählt alle Docker Images auf
	imageRoute.GET("/", func(c *gin.Context) {
		list, err := cli.ImageList(c.Request.Context(), image.ListOptions{})
		if err != nil {
			log.Printf("Failed to list images: %v", err)
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to list images: " + err.Error()})
			return
		}
		c.JSON(http.StatusOK, list)
	})

	// POST /images/add - Zieht ein Docker Image herunter
	imageRoute.POST("/add", func(c *gin.Context) {
		var givenImage models.Image
		if err := c.ShouldBindJSON(&givenImage); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}

		// Überprüft ob Reference vorhanden ist
		if givenImage.Reference == "" {
			c.JSON(http.StatusBadRequest, gin.H{"error": "imageRef is required"})
			return
		}

		ctx := c.Request.Context()
		log.Printf("Pulling image: %s", givenImage.Reference)

		out, err := cli.ImagePull(ctx, givenImage.Reference, image.PullOptions{})
		if err != nil {
			log.Printf("Failed to pull image %s: %v", givenImage.Reference, err)
			c.JSON(http.StatusBadRequest, gin.H{"error": "Failed to pull image: " + err.Error()})
			return
		}
		defer out.Close()

		//
		buf := new(strings.Builder)
		if _, err := io.Copy(buf, out); err != nil {
			log.Printf("Failed to read pull output for %s: %v", givenImage.Reference, err)
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to read pull output: " + err.Error()})
			return
		}

		//
		if strings.Contains(buf.String(), `"error"`) {
			log.Printf("Docker pull failed for %s: %s", givenImage.Reference, buf.String())
			c.JSON(http.StatusBadRequest, gin.H{"error": "Docker pull failed: " + buf.String()})
			return
		}

		log.Printf("Successfully pulled image: %s", givenImage.Reference)
		c.JSON(http.StatusCreated, gin.H{
			"message": "Image pulled and stored successfully",
			"image":   givenImage,
		})
	})

	// POST /images/update - Aktualisiert ein Docker Image
	imageRoute.POST("/update", func(c *gin.Context) {
		var givenImage models.Image
		if err := c.ShouldBindJSON(&givenImage); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}

		// Überprüft ob Reference vorhanden ist
		if givenImage.Reference == "" {
			c.JSON(http.StatusBadRequest, gin.H{"error": "reference is required"})
			return
		}

		ctx := c.Request.Context()
		log.Printf("Updating image: %s", givenImage.Reference)

		// Verwirft alte Version
		_, err := cli.ImageRemove(ctx, givenImage.Reference, image.RemoveOptions{
			Force:         true,
			PruneChildren: true,
		})
		if err != nil {
			log.Printf("Note: Could not remove old image %s (may not exist): %v", givenImage.Reference, err)
		} else {
			log.Printf("Removed old version of image: %s", givenImage.Reference)
		}

		// Zieht neue Version herunter
		log.Printf("Pulling fresh version of image: %s", givenImage.Reference)
		out, err := cli.ImagePull(ctx, givenImage.Reference, image.PullOptions{})
		if err != nil {
			log.Printf("Failed to pull updated image %s: %v", givenImage.Reference, err)
			c.JSON(http.StatusBadRequest, gin.H{"error": "Failed to pull updated image: " + err.Error()})
			return
		}
		defer out.Close()

		//
		buf := new(strings.Builder)
		if _, err := io.Copy(buf, out); err != nil {
			log.Printf("Failed to read pull output for %s: %v", givenImage.Reference, err)
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to read pull output: " + err.Error()})
			return
		}

		//
		if strings.Contains(buf.String(), `"error"`) {
			log.Printf("Docker pull failed for %s: %s", givenImage.Reference, buf.String())
			c.JSON(http.StatusBadRequest, gin.H{"error": "Docker pull failed: " + buf.String()})
			return
		}

		log.Printf("Successfully updated image: %s", givenImage.Reference)
		c.JSON(http.StatusOK, gin.H{
			"message": "Image updated successfully",
			"image":   givenImage,
		})
	})

	// DELETE /images/remove - Entfernt ein Docker Image
	imageRoute.DELETE("/remove", func(c *gin.Context) {
		var givenImage models.Image
		if err := c.ShouldBindJSON(&givenImage); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}

		// Überprüft ob Reference vorhanden ist
		if givenImage.Reference == "" {
			c.JSON(http.StatusBadRequest, gin.H{"error": "imageRef is required"})
			return
		}

		ctx := c.Request.Context()
		log.Printf("Removing image: %s", givenImage.Reference)

		removed, err := cli.ImageRemove(ctx, givenImage.Reference, image.RemoveOptions{
			Force:         true,
			PruneChildren: true,
		})
		if err != nil {
			log.Printf("Failed to remove image %s: %v", givenImage.Reference, err)
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to remove Docker image: " + err.Error()})
			return
		}

		log.Printf("Successfully removed image %s: %+v", givenImage.Reference, removed)

		c.JSON(http.StatusOK, gin.H{
			"message": "Image deleted successfully",
			"removed": removed,
		})
	})
}
