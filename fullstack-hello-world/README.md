# Hello World Fullstack App
Angular Frontend → Spring Boot Microservice → PostgreSQL Database

## Architecture
```
Frontend (Angular, port 4200)
         ↓ HTTP
Backend (Spring Boot, port 8080)
         ↓ JDBC
Database (PostgreSQL, port 5432)
```

## Prerequisites
- Java 17+
- Node.js 18+
- Maven 3.8+
- Docker & Docker Compose
- Angular CLI

## Setup Instructions

### 1. Set Up PostgreSQL Database
See [database/SETUP.md](database/SETUP.md) for detailed OS-specific instructions.

Quick setup:
```bash
# Connect to PostgreSQL
psql -U postgres

# In psql terminal:
CREATE DATABASE hello_world_db;
\c hello_world_db;
CREATE TABLE message (id BIGSERIAL PRIMARY KEY, text VARCHAR(255) NOT NULL);
INSERT INTO message (id, text) VALUES (1, 'Hello World from Spring Boot + PostgreSQL!');
\q
```

### 2. Build & Run Spring Boot Backend
```bash
cd backend
mvn clean install
mvn spring-boot:run
```
Backend runs on `http://localhost:8080`

Test the endpoint:
```bash
curl http://localhost:8080/api/hello
```

### 3. Run Angular Frontend
```bash
cd frontend
npm install
ng serve
```
Frontend runs on `http://localhost:4200`

## Project Structure
```
fullstack-hello-world/
├── backend/                    # Spring Boot microservice
│   ├── src/main/java/
│   │   └── com/example/
│   │       ├── HelloWorldApplication.java
│   │       ├── controller/
│   │       │   └── HelloController.java
│   │       ├── entity/
│   │       │   └── Message.java
│   │       └── repository/
│   │           └── MessageRepository.java
│   ├── src/main/resources/
│   │   └── application.yml
│   └── pom.xml
├── frontend/                   # Angular frontend
│   ├── src/
│   │   ├── app/
│   │   │   └── app.component.ts
│   │   ├── index.html
│   │   ├── main.ts
│   │   └── styles.css
│   ├── angular.json
│   ├── package.json
│   └── tsconfig.json
└── database/                   # PostgreSQL setup
    ├── init.sql
    └── docker-compose.yml
```

## How It Works
1. **Frontend** (Angular) displays a button to fetch messages
2. **Button click** → HTTP GET request to `/api/hello`
3. **Backend** (Spring Boot) receives request → queries PostgreSQL
4. **Database** returns message → Backend sends JSON response
5. **Frontend** displays message from database

## Environment Configuration
Backend connects to PostgreSQL using:
- Host: localhost
- Port: 5432
- Database: hello_world_db
- User: postgres
- Password: postgres

## Troubleshooting

### Backend can't connect to database
```bash
# Check PostgreSQL is running
psql -U postgres -c "SELECT version();"

# Check database exists
psql -U postgres -l | grep hello_world_db
```

### Frontend can't reach backend
- Verify backend is running: `curl http://localhost:8080/api/hello`
- Check CORS is enabled (HelloWorldApplication.java has `@CrossOrigin`)
- Check browser console for errors

### Angular CLI not found
```bash
npm install -g @angular/cli
```

## Database Setup Guide
For detailed PostgreSQL setup instructions by OS, see [database/SETUP.md](database/SETUP.md)

## Next Steps
- Add more endpoints to the Spring Boot backend
- Implement authentication/authorization
- Add database migrations with Flyway
- Deploy to cloud (AWS, GCP, Azure)
