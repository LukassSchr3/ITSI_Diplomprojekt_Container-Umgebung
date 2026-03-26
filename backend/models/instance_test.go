package models

import (
	"encoding/json"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestInstance_Fields(t *testing.T) {
	inst := Instance{
		Name:      "test-container",
		Reference: "nginx:latest",
	}
	assert.Equal(t, "test-container", inst.Name)
	assert.Equal(t, "nginx:latest", inst.Reference)
}

func TestInstance_JSONSerialization(t *testing.T) {
	inst := Instance{
		Name:      "my-container",
		Reference: "ubuntu:22.04",
	}

	data, err := json.Marshal(inst)
	require.NoError(t, err)

	var result map[string]string
	err = json.Unmarshal(data, &result)
	require.NoError(t, err)

	assert.Equal(t, "my-container", result["name"])
	assert.Equal(t, "ubuntu:22.04", result["reference"])
}

func TestInstance_JSONDeserialization(t *testing.T) {
	jsonStr := `{"name":"test","reference":"alpine:latest"}`

	var inst Instance
	err := json.Unmarshal([]byte(jsonStr), &inst)
	require.NoError(t, err)
	assert.Equal(t, "test", inst.Name)
	assert.Equal(t, "alpine:latest", inst.Reference)
}

func TestInstance_JSONDeserialization_EmptyFields(t *testing.T) {
	jsonStr := `{}`

	var inst Instance
	err := json.Unmarshal([]byte(jsonStr), &inst)
	require.NoError(t, err)
	assert.Empty(t, inst.Name)
	assert.Empty(t, inst.Reference)
}
