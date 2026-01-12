package main

import (
	"ITSIContainerBackend/routes"
	"fmt"
	"log"

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

	err = router.Run(":3030")
	if err != nil {
		log.Fatal("Error starting server:\n", err)
	}
	log.Println("Listening on port 3030")
}
