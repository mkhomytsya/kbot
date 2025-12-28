.PHONY: build test clean docker-build docker-push

# Variables
APP_NAME := kbot
VERSION := $(shell git describe --tags --always --dirty)
COMMIT := $(shell git rev-parse --short HEAD)
BUILD_TIME := $(shell date -u +"%Y-%m-%dT%H:%M:%SZ")
LDFLAGS := -X main.version=$(VERSION) -X main.commit=$(COMMIT) -X main.buildTime=$(BUILD_TIME)

# Build the binary
build:
	go build -ldflags "$(LDFLAGS)" -o $(APP_NAME) .

# Run tests
test:
	go test ./...

# Clean build artifacts
clean:
	rm -f $(APP_NAME)

# Build Docker image
image:
	docker build -t $(APP):$(VERSION)-$(TARGETOS)-$(TARGETARCH) .

# Push Docker image
push:
	docker tag $(APP):$(VERSION)-$(TARGETOS)-$(TARGETARCH) ghcr.io/$(GITHUB_REPOSITORY):$(VERSION)-$(TARGETOS)-$(TARGETARCH)
	docker push ghcr.io/$(GITHUB_REPOSITORY):$(VERSION)-$(TARGETOS)-$(TARGETARCH)

# Full build and test
all: clean test build