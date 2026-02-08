package models

type Instance struct {
	Name      string `json:"name" binding:"required"`      // Container-Name wie per Java Middleware definiert (Username + Aufgabenname)
	Reference string `json:"reference" binding:"required"` // Docker-Image referenz (bsp. "chr0/web_injections ")
}
