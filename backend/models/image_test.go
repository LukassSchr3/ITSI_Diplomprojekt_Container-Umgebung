package models

import (
	"encoding/json"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestImage_Fields(t *testing.T) {
	img := Image{
		Reference: "ubuntu:22.04",
	}
	assert.Equal(t, "ubuntu:22.04", img.Reference)
}

func TestImage_JSONSerialization(t *testing.T) {
	img := Image{
		Reference: "nginx:latest",
	}

	data, err := json.Marshal(img)
	require.NoError(t, err)

	var result map[string]string
	err = json.Unmarshal(data, &result)
	require.NoError(t, err)

	assert.Equal(t, "nginx:latest", result["reference"])
}

func TestImage_JSONDeserialization(t *testing.T) {
	jsonStr := `{"reference":"alpine:3.18"}`

	var img Image
	err := json.Unmarshal([]byte(jsonStr), &img)
	require.NoError(t, err)
	assert.Equal(t, "alpine:3.18", img.Reference)
}

func TestImage_JSONDeserialization_EmptyReference(t *testing.T) {
	jsonStr := `{}`

	var img Image
	err := json.Unmarshal([]byte(jsonStr), &img)
	require.NoError(t, err)
	assert.Empty(t, img.Reference)
}

func TestImage_JSONRoundTrip(t *testing.T) {
	original := Image{Reference: "my-custom-image:v1.0"}

	data, err := json.Marshal(original)
	require.NoError(t, err)

	var restored Image
	err = json.Unmarshal(data, &restored)
	require.NoError(t, err)

	assert.Equal(t, original.Reference, restored.Reference)
}
