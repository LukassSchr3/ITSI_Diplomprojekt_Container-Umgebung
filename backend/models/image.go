package models

type Image struct {
	ID       uint   `json:"id"`
	Name     string `json:"name"`
	ImageRef string `json:"imageRef"`

	Instances []Instance `json:"instances"`
}
