package models

type Image struct {
	//	Name     string `json:"name" binding:"required"`     // Aufgaben-Name (bsp. "ITSI 7.2 Injections")
	Reference string `json:"reference" binding:"required"` // Docker image Referenz (bsp. "chr0/web_injections")
}
