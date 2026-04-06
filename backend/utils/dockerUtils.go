package utils

import (
	"context"

	"github.com/docker/docker/api/types/container"
	"github.com/docker/docker/api/types/filters"
	"github.com/docker/docker/api/types/image"
	"github.com/docker/docker/client"
)

func GetContainerIP(ctx context.Context, cli *client.Client, containerID string) string {
	info, err := cli.ContainerInspect(ctx, containerID)
	if err != nil {
		return ""
	}
	ip := info.NetworkSettings.IPAddress
	if ip == "" {
		for _, network := range info.NetworkSettings.Networks {
			if network.IPAddress != "" {
				return network.IPAddress
			}
		}
	}
	return ip
}

func ImageExists(ctx context.Context, cli *client.Client, ref string) (bool, error) {
	args := filters.NewArgs()
	args.Add("reference", ref)

	imgs, err := cli.ImageList(ctx, image.ListOptions{
		Filters: args,
	})
	if err != nil {
		return false, err
	}

	return len(imgs) > 0, nil
}

func FindContainerByName(ctx context.Context, cli *client.Client, name string) (*container.Summary, error) {
	args := filters.NewArgs()
	args.Add("name", name)

	containers, err := cli.ContainerList(ctx, container.ListOptions{
		All:     true,
		Filters: args,
	})
	if err != nil {
		return nil, err
	}

	if len(containers) == 0 {
		return nil, nil
	}

	return &containers[0], nil
}
