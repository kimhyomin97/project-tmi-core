package router

import (
	"github.com/gin-gonic/gin"
	"project-tmi-core/internal/handler"
)

func New() *gin.Engine {
	router := gin.Default()

	router.GET("/health", handler.Health)
	router.GET("/ping", handler.Ping)

	return router
}