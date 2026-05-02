.PHONY: build up down down-v

# Build the Docker containers
build:
	docker-compose -f docker/docker-compose.yaml --env-file .env build

# Start the Docker containers
up:
	docker-compose -f docker/docker-compose.yaml --env-file .env up -d

# Stop and remove the Docker containers
down:
	docker-compose -f docker/docker-compose.yaml --env-file .env down

# Stop and remove the Docker containers with volumes
down-v:
	docker-compose -f docker/docker-compose.yaml --env-file .env down -v

# Stop, Build and Start the Docker containers
rebuild: down build up

clean:
	mvnw.cmd clean