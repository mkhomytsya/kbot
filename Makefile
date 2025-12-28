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
docker-build:
	docker build -t $(APP_NAME):latest .

# Push Docker image (requires login)
docker-push:
	docker push $(APP_NAME):latest

# Full build and test
all: clean test build