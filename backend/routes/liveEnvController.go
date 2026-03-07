package routes

import (
	"ITSIContainerBackend/services"
	"ITSIContainerBackend/utils"
	"log"
	"net/http"

	"github.com/docker/docker/client"
	"github.com/gin-gonic/gin"
)

func RegisterLiveEnvironmentRoutes(router *gin.Engine, cli *client.Client) {
	liveEnvService := services.NewLiveEnvService(cli)
	liveenv := router.Group("/live")

	liveenv.GET("/", func(c *gin.Context) {
		envs, err := liveEnvService.ListAllContainers(c.Request.Context())
		if err != nil {
			log.Printf("Failed to list live environments: %v", err)
			utils.RespondError(c, http.StatusInternalServerError, "LIST_FAILED", err.Error())
			return
		}
		utils.RespondSuccess(c, http.StatusOK, "Live environments retrieved", envs)
	})

	liveenv.GET("/status/:name", func(c *gin.Context) {
		username := c.Param("name")

		info, err := liveEnvService.GetContainerInfo(c.Request.Context(), username)
		if err != nil {
			log.Printf("Failed to get live environment status for %s: %v", username, err)
			utils.RespondError(c, http.StatusNotFound, "NOT_FOUND", err.Error())
			return
		}

		utils.RespondSuccess(c, http.StatusOK, "Status retrieved", info)
	})

	liveenv.POST("/start", func(c *gin.Context) {
		var user struct {
			Name string `json:"name" binding:"required"`
		}
		if err := c.ShouldBindJSON(&user); err != nil {
			utils.RespondError(c, http.StatusBadRequest, "INVALID_REQUEST", err.Error())
			return
		}

		log.Printf("Starting live environment for user: %s", user.Name)

		info, err := liveEnvService.StartContainer(c.Request.Context(), user.Name)
		if err != nil {
			log.Printf("Failed to start live environment for %s: %v", user.Name, err)
			utils.RespondError(c, http.StatusInternalServerError, "START_FAILED", err.Error())
			return
		}

		log.Printf("Successfully started live environment for user: %s", user.Name)
		utils.RespondSuccess(c, http.StatusOK, "Live environment started", info)
	})

	liveenv.POST("/stop", func(c *gin.Context) {
		var user struct {
			Name string `json:"name" binding:"required"`
		}
		if err := c.ShouldBindJSON(&user); err != nil {
			utils.RespondError(c, http.StatusBadRequest, "INVALID_REQUEST", err.Error())
			return
		}

		log.Printf("Stopping live environment for user: %s", user.Name)

		if err := liveEnvService.StopContainer(c.Request.Context(), user.Name); err != nil {
			log.Printf("Failed to stop live environment for %s: %v", user.Name, err)
			utils.RespondError(c, http.StatusInternalServerError, "STOP_FAILED", err.Error())
			return
		}

		log.Printf("Successfully stopped live environment for user: %s", user.Name)
		utils.RespondSuccess(c, http.StatusOK, "Live environment stopped", nil)
	})

	liveenv.POST("/reset", func(c *gin.Context) {
		var user struct {
			Name string `json:"name" binding:"required"`
		}
		if err := c.ShouldBindJSON(&user); err != nil {
			utils.RespondError(c, http.StatusBadRequest, "INVALID_REQUEST", err.Error())
			return
		}

		log.Printf("Resetting live environment for user: %s", user.Name)

		info, err := liveEnvService.ResetContainer(c.Request.Context(), user.Name)
		if err != nil {
			log.Printf("Failed to reset live environment for %s: %v", user.Name, err)
			utils.RespondError(c, http.StatusInternalServerError, "RESET_FAILED", err.Error())
			return
		}

		log.Printf("Successfully reset live environment for user: %s", user.Name)
		utils.RespondSuccess(c, http.StatusOK, "Live environment reset", info)
	})
}

