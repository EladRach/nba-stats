# NBA Stats Logging System
A **scalable backend system** for logging and retrieving **NBA player statistics** using **Spring Boot, PostgresSQL, and Redis**.  
It ensures **real-time updates** with **consistency** between the database and cache.
---
## Tech Stack
| **Technology**    | **Usage** |
|-------------------|----------|
| **Java**        | Backend development (Spring Boot) |
| **Spring Boot**   | REST API implementation |
| **PostgreSQL**    | Primary database for storing game stats |
| **Redis**         | Caching and distributed locking |
| **Docker Compose** | Local development setup |
| **JUnit & Mockito** | Unit testing |

---

## How to Run the Project
Follow these steps to set up and run the **NBA Stats Logging System** locally.

---
1. Start PostgresSql & Redis : `docker-compose up -d`
2. Build the project: `nvm clean install`
3. Run the application: `mvn spring-boot:run`
---
### Test the API
- POST player stats (congratulations Deni Avdija for your first triple double 🇮🇱):
    ```curl
    curl -X POST 'http://localhost:8080/stats' \
    -H 'Content-Type: application/json' \
    -d '{
        "playerId": 8,
        "teamId": 21,
        "gameId": 61,
        "points": 30,
        "rebounds": 12,
        "assists": 10,
        "steals": 0,
        "blocks": 0,
        "fouls": 4,
        "turnovers": 5,
        "minutesPlayed": 42.1
    }'
  ```
- GET player stats:
    ```curl
  curl -X GET 'http://localhost:8080/stats/player/8'
   ```

- GET team stats:
    ```curl
  curl -X GET 'http://localhost:8080/stats/team/21'
   ```
  
---

## Thought Process & Design Decisions

- I designed the system to prioritize fast `GET` requests since more users will likely retrieve stats rather than update them. To achieve this, I used Redis caching to store precomputed averages for quick access.
- Initially, I planned to use a background worker with Kafka to process updates asynchronously—allowing `POST` requests to be fast while the worker recalculated averages in the background. However, this introduced data consistency issues, as `GET` requests could return outdated data.
- To guarantee consistency, I removed Kafka & the background worker and ensured every `POST` request directly updates both PostgresSQL and Redis. While this made `POST` requests slightly slower, I prioritized accuracy over speed.
- I encountered race conditions, which I resolved using Redis locks and transactional database operations to prevent concurrent requests from causing inconsistent data.
- In the end I chose consistency over faster writes to ensure that once data is written for a player’s game, it is immediately available for fetching aggregate stats

---
- Note on AWS deployment: I don’t really have hands-on experience deploying production-ready applications in AWS. I understand the general concepts, but I would need to learn more before I can implement it properly