# Project Setup and Docker Management

This project uses Docker for containerized development and deployment. The provided `Makefile` simplifies common Docker operations using `docker-compose`.

## Prerequisites

- [Docker](https://www.docker.com/get-started) installed
- [Docker Compose](https://docs.docker.com/compose/install/) installed
- (Optional) [Make](https://www.gnu.org/software/make/) installed for using Makefile commands

## Usage

All commands below should be run from the project root directory.

### Build Docker Containers

```
make build
```
Builds the Docker containers as defined in `docker/docker-compose.yaml` using environment variables from `.env`.

### Start Docker Containers

```
make up
```
Starts the containers in detached mode.

### Stop and Remove Docker Containers

```
make down
```
Stops and removes the containers.

### Rebuild and Restart Containers

```
make rebuild
```
Runs `down`, then `build`, then `up` to fully restart the containers.

## Notes

- Ensure your `.env` file is properly configured before running these commands.
- You can also run the equivalent `docker-compose` commands manually if you prefer.

---

For more details, see the `Makefile` and `docker/docker-compose.yaml` files.

