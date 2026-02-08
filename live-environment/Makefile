IMAGE_NAME = itsi-live-env
VERSION = 1.0
CONTAINER_NAME = test-live-env

.PHONY: help build run stop start remove clean clean-all rebuild test shell logs prune ps images versions

help:
	@echo "Available commands: build run stop start shell remove clean clean-all rebuild test logs prune ps images versions"
	@echo "Current version: $(VERSION)"

build:
	docker build -t $(IMAGE_NAME):$(VERSION) .

run:
	docker run  --name $(CONTAINER_NAME) $(IMAGE_NAME):$(VERSION)

run-interactive:
	docker run -it --name $(CONTAINER_NAME) $(IMAGE_NAME):$(VERSION)


stop:
	docker stop $(CONTAINER_NAME)

start:
	docker start -i $(CONTAINER_NAME)

shell:
	docker exec -it $(CONTAINER_NAME) /bin/bash

remove:
	docker rm -f $(CONTAINER_NAME)

clean:
	-docker rm -f $(CONTAINER_NAME)
	-docker rmi $(IMAGE_NAME):$(VERSION)

clean-all:
	-docker rm -f $(CONTAINER_NAME)
	-docker rmi $$(docker images $(IMAGE_NAME) -q) 2>/dev/null || true

rebuild: clean build

test: build run

logs:
	docker logs $(CONTAINER_NAME)

prune:
	docker image prune -f

ps:
	docker ps -a

images:
	docker images

versions:
	docker images $(IMAGE_NAME)