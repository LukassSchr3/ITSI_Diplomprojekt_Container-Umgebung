package routes

import (
	"net/http"

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
	})

	liveenv.POST("/stop", func(c *gin.Context) {
		var user struct {
			Name string `json:"name" binding:"required"`
		}
		if err := c.ShouldBindJSON(&user); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}
	})

	liveenv.POST("/reset", func(c *gin.Context) {
		var user struct {
			Name string `json:"name" binding:"required"`
		}
		if err := c.ShouldBindJSON(&user); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}
	})
}
