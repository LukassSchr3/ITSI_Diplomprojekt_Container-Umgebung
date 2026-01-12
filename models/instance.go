package models

type Instance struct {
	ID   uint   `json:"id"`
	Name string `json:"name"`

	ImageID  uint   `json:"imageId;not null"`
	ImageRef string `json:"imageRef"`
	UserID   uint   `json:"userId;not null"`
}
