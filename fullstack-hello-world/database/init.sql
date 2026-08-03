-- Create table (if database hello_world_db already exists)
CREATE TABLE IF NOT EXISTS message (
    id BIGSERIAL PRIMARY KEY,
    text VARCHAR(255) NOT NULL
);

-- Insert sample data
DELETE FROM message WHERE id = 1;
INSERT INTO message (id, text) VALUES (1, 'Hello World from Spring Boot + PostgreSQL!');
