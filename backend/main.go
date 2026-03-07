package main

import (
	"ITSIContainerBackend/routes"
	"ITSIContainerBackend/services"
	"context"
	"fmt"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/common-nighthawk/go-figure"
	"github.com/docker/docker/client"
	"github.com/gin-contrib/cors"
	"github.com/gin-gonic/gin"
)

func main() {
	banner := figure.NewFigure("ITSI-Backend", "slant", true).String()
	fmt.Println("\033[32m" + banner + "\033[0m")

	cli, err := client.NewClientWithOpts(client.FromEnv, client.WithAPIVersionNegotiation())
	if err != nil {
		log.Fatal("Error starting docker client:\n", err)
	}
	defer func() {
		cli.Close()
		log.Println("Docker client shut down")
	}()
	log.Println("Docker client started")

	liveEnvService := services.NewLiveEnvService(cli)
	cleanupCtx, cleanupCancel := context.WithCancel(context.Background())
	defer cleanupCancel()

	maxIdleTime := 2 * time.Hour
	cleanupInterval := 30 * time.Minute
	liveEnvService.StartCleanupRoutine(cleanupCtx, maxIdleTime, cleanupInterval)

	router := gin.Default()
	router.Use(cors.New(cors.Config{
		AllowOrigins:     []string{"http://localhost:9090"},
		AllowMethods:     []string{"GET", "POST", "DELETE"},
		AllowHeaders:     []string{"*"},
		AllowCredentials: true,
	}))

	router.GET("/health", func(c *gin.Context) {
		c.JSON(200, gin.H{"Status": "OK"})
	})

	routes.RegisterImageRoutes(router, cli)
	routes.RegisterInstanceRoutes(router, cli)
	routes.RegisterLiveEnvironmentRoutes(router, cli)

	srv := &http.Server{
		Addr:    ":3030",
		Handler: router,
	}

	go func() {
		log.Println("Listening on port 3030")
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("Server error: %v", err)
		}
	}()

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	log.Println("Shutting down server...")

	cleanupCancel()

	shutdownCtx, shutdownCancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer shutdownCancel()

	if err := srv.Shutdown(shutdownCtx); err != nil {
		log.Fatal("Server forced to shutdown:", err)
	}

	log.Println("Server exited gracefully")
}
