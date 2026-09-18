# Sitebanner_redis
# Site Banner API with Redis

A Spring Boot backend project for managing site-banner data using Redis as a caching layer and PostgreSQL as the persistent database.

## Architecture

The project follows a cache-aside caching pattern using Redis.

![Site Banner Architecture](https://raw.githubusercontent.com/sumanth2005-hub/Sitebanner_redis/main/architecture.png)

## Request Flow

1. Client sends a request to the API.
2. Spring Boot Controller receives the request.
3. Controller passes the request to the Service Layer.
4. Service Layer uses Jedis to communicate with Redis.
5. If the data exists in Redis, it is returned as a cache hit.
6. If the data is not present, it is a cache miss.
7. On a cache miss, the application fetches the data from PostgreSQL.
8. The fetched data is saved to Redis.
9. The data is returned to the client.

## Technologies Used

- Java
- Spring Boot
- Redis
- Jedis
- PostgreSQL
- Maven
- Docker
- Docker Compose
- Postman

## Caching Flow

```text
Client
   |
   v
Spring Boot Controller
   |
   v
Service Layer
   |
   v
Redis
   |
   +---- Cache Hit ----> Return Data
   |
   +---- Cache Miss ---> PostgreSQL
                            |
                            v
                       Save to Redis
                            |
                            v
                       Return Data
