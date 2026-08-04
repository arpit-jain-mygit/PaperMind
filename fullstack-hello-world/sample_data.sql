-- Sample Data for First Run
-- 7 students to test the learning system

INSERT INTO student (name, email, phone_number, gpa) VALUES
('Alice Johnson', 'alice@example.com', '5551234567', 3.9),
('Bob Smith', 'bob.test.com', '5559876543', 3.1),
('Carol Davis', 'carol@gmail.com', '5553334445', 3.8),
('Diana Prince', 'diana.test.com', '5559999999', 3.2),
('Eve Wilson', 'eve@domain.co.uk', '5556667778', 3.7),
('Frank Brown', 'frank.invalid', '5554445556', 2.9),
('Grace Lee', 'grace@yahoo.com', '5552223334', 3.95);

-- Verification query
SELECT id, name, email, gpa FROM student ORDER BY gpa DESC;
