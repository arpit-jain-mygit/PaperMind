# PostgreSQL Setup (No Docker)

## Option 1: Install PostgreSQL Locally

### macOS (Homebrew)
```bash
brew install postgresql@15
brew services start postgresql@15
```

### macOS (Using PostgreSQL.app)
Download from https://postgresapp.com and follow installation

### Linux (Ubuntu/Debian)
```bash
sudo apt-get update
sudo apt-get install postgresql postgresql-contrib
sudo service postgresql start
```

### Windows
Download installer from https://www.postgresql.org/download/windows/

## Create Database & Table

### 1. Connect to PostgreSQL
```bash
psql -U postgres
```

### 2. Create Database
```sql
CREATE DATABASE hello_world_db;
```

### 3. Connect to New Database
```bash
\c hello_world_db
```

### 4. Create Table
```sql
CREATE TABLE message (
    id BIGSERIAL PRIMARY KEY,
    text VARCHAR(255) NOT NULL
);
```

### 5. Insert Sample Data
```sql
INSERT INTO message (id, text) VALUES (1, 'Hello World from Spring Boot + PostgreSQL!');
```

### 6. Verify
```sql
SELECT * FROM message;
```

Output should show:
```
 id |              text              
----+--------------------------------
  1 | Hello World from Spring Boot + PostgreSQL!
```

### 7. Exit psql
```bash
\q
```

## Connection Details
- **Host:** localhost
- **Port:** 5432
- **Database:** hello_world_db
- **User:** postgres
- **Password:** postgres (default)

## Verify PostgreSQL is Running
```bash
psql -U postgres -c "SELECT version();"
```

If you changed the default password during installation, update it in `backend/src/main/resources/application.yml`
