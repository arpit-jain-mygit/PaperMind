# Quick Start Guide (No Docker)

## Prerequisites
- PostgreSQL installed and running
- Java 17+ installed
- Node.js 18+ installed
- Maven 3.8+ installed
- Angular CLI installed

## Setup Steps

### Step 1: Set Up PostgreSQL Database

Follow [database/SETUP.md](database/SETUP.md) for your OS

Quick summary:
```bash
# Connect to PostgreSQL
psql -U postgres

# In psql:
CREATE DATABASE hello_world_db;
\c hello_world_db;
CREATE TABLE message (
    id BIGSERIAL PRIMARY KEY,
    text VARCHAR(255) NOT NULL
);
INSERT INTO message (id, text) VALUES (1, 'Hello World from Spring Boot + PostgreSQL!');
SELECT * FROM message;
\q
```

Verify connection:
```bash
psql -h localhost -U postgres -d hello_world_db -c "SELECT * FROM message;"
```

### Step 2: Start Spring Boot Backend

```bash
cd fullstack-hello-world/backend
mvn clean install
mvn spring-boot:run
```

Backend runs on **http://localhost:8080**

### Step 3: Start Angular Frontend

```bash
cd fullstack-hello-world/frontend
npm install
ng serve
```

Frontend runs on **http://localhost:4200**

## Access the App
Open browser: **http://localhost:4200**

Click "Fetch Message from Backend" button

## Expected Data Flow
```
Browser (Angular 4200)
  ↓ Click Button
  ↓ HTTP GET /api/hello
Spring Boot (8080)
  ↓ Query Database
PostgreSQL (5432)
  ↓ Returns Message
Browser
  ↓ Display: "Hello World from Spring Boot + PostgreSQL!"
```

## Troubleshooting

### PostgreSQL won't connect
```bash
# Check if PostgreSQL is running
psql -U postgres -c "SELECT version();"

# On macOS with Homebrew
brew services start postgresql@15
```

### Backend fails to start
- Verify database exists: `psql -l | grep hello_world_db`
- Check credentials in `backend/src/main/resources/application.yml`
- Make sure PostgreSQL is running on port 5432

### Frontend can't reach backend
- Verify backend is running: `curl http://localhost:8080/api/hello`
- Check browser console for CORS errors
- Ensure you waited for backend to fully start

### Angular CLI not found
```bash
npm install -g @angular/cli
```

## Stop Everything
```bash
# Press Ctrl+C in backend and frontend terminals
# PostgreSQL keeps running (stop with: brew services stop postgresql@15 on macOS)
```
