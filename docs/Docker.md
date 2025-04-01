# Running the Application with Docker

This guide will walk you through setting up, configuring, and running the application using Docker. You will also learn how to modify the server properties and apply changes without rebuilding the container.

## Prerequisites

- Install [Docker](https://docs.docker.com/get-docker/)
- Install [Docker Compose](https://docs.docker.com/compose/install/) (optional but recommended)

---

## 1. Build the Docker Image

First, navigate to the directory containing `Dockerfile` and run:

```sh
docker build -t myapp .
```

This creates a Docker image named `myapp`.

---

## 2. Running the Container with Default Configuration

By default, the server uses the ports specified in `config.properties`. To run the container with these default values:

```sh
docker run -d --name myapp-container -p 1234:1234 -p 5678:5678/udp myapp
```

Here, the ports 1234 (TCP) and 5678 (UDP) are exposed as per `config.properties`.

---

## 3. Modifying the Server Properties

If you need to change the TCP and UDP ports, you have two options:

### **Option 1: Modify `config.properties` inside the Container**

1. Start a temporary shell inside the running container:

   ```sh
   docker exec -it myapp-container sh
   ```

2. Edit `config.properties` with a text editor (e.g., `vi` or `nano`):

   ```sh
   vi /app/config.properties
   ```

3. Save changes and restart the container:

   ```sh
   docker restart myapp-container
   ```

### **Option 2: Use Environment Variables (Recommended)**

Instead of modifying `config.properties`, you can pass new values as environment variables when starting the container. For example:

TODO 

```sh
docker run -d --name myapp-container \
  -e TCP_PORT=25565 -e UDP_PORT=19132 \
  -p 25565:25565 -p 19132:19132/udp \
  myapp
```

This automatically updates `config.properties` with the new values on startup.

---

## 4. Running with Docker Compose (Optional)

To simplify running the container, create a `docker-compose.yml` file:

```yaml
version: '3.8'
services:
  myapp:
    build: .
    environment:
      - TCP_PORT=25565
      - UDP_PORT=19132
    ports:
      - "25565:25565"
      - "19132:19132/udp"
```

Start the server using:

```sh
docker compose up -d
```

To stop the container:

```sh
docker compose down
```

---

## 5. Checking Logs & Troubleshooting

If you need to check the server logs, use:

```sh
docker logs -f myapp-container
```

To stop and remove the container:

```sh
docker stop myapp-container && docker rm myapp-container
```

---

## Summary

- **Build** the image with `docker build -t myapp .`
- **Run** with `docker run -d --name myapp-container -p 1234:1234 -p 5678:5678/udp myapp`
- **Modify config** via environment variables (`-e TCP_PORT=... -e UDP_PORT=...`) or inside the container
- **Use Docker Compose** for easier management (`docker compose up -d`)
- **Check logs** with `docker logs -f myapp-container`

Now you're all set to run and manage your server with Docker! 🚀
