package routes

import (
	"github.com/docker/docker/client"
	"github.com/gin-gonic/gin"
)

func RegisterLiveEnvironmentRoutes(router *gin.Engine, cli *client.Client) {
	liveenv := router.Group("/live")

	liveenv.POST("/start", func(c *gin.Context) {

	})

	liveenv.POST("/stop", func(c *gin.Context) {

	})

	liveenv.POST("/reset", func(c *gin.Context) {

	})
}
