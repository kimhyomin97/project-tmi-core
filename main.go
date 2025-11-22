package main

import (
	"log"
	"project-tmi-core/internal/router"
)

func main() {
	router := router.New()

	if err := router.Run(":8080");  err != nil {
		log.Fatal(err)
	}
}