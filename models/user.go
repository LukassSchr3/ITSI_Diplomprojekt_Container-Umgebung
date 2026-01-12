package models

type User struct {
	ID   uint   `json:"id"`
	Name string `json:"name"`
	Role string `json:"role"`

	Instances []Instance `json:"instances"`
}
