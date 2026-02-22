-- Beispieldaten für die Datenbank

-- Users einfügen
INSERT INTO users (email, name, password, class, role, created_at, expired_at)
VALUES ('admin@example.com', 'admin', '$2a$10$abcdefghijklmnopqrstuv', NULL, 'ADMIN', CURRENT_TIMESTAMP, NULL);

INSERT INTO users (email, name, password, class, role, created_at, expired_at)
VALUES ('john.doe@example.com', 'john_doe', '$2a$10$abcdefghijklmnopqrstuv', '5AHIT', 'USER', CURRENT_TIMESTAMP, NULL);

INSERT INTO users (email, name, password, class, role, created_at, expired_at)
VALUES ('jane.smith@example.com', 'jane_smith', '$2a$10$abcdefghijklmnopqrstuv', '5BHIT', 'USER', CURRENT_TIMESTAMP, NULL);

INSERT INTO users (email, name, password, class, role, created_at, expired_at)
VALUES ('dev.user@example.com', 'dev_user', '$2a$10$abcdefghijklmnopqrstuv', '4AHIT', 'DEVELOPER', CURRENT_TIMESTAMP, NULL);

INSERT INTO users (email, name, password, class, role, created_at, expired_at)
VALUES ('test.user@example.com', 'test_user', '$2a$10$abcdefghijklmnopqrstuv', '3AHIT', 'TESTER', CURRENT_TIMESTAMP, NULL);

-- Images einfügen
INSERT INTO images (name, image_ref) VALUES ('ubuntu-latest', 'docker.io/library/ubuntu:latest');
INSERT INTO images (name, image_ref) VALUES ('nginx-alpine', 'docker.io/library/nginx:alpine');
INSERT INTO images (name, image_ref) VALUES ('postgres-14', 'docker.io/library/postgres:14');
INSERT INTO images (name, image_ref) VALUES ('redis-latest', 'docker.io/library/redis:latest');
INSERT INTO images (name, image_ref) VALUES ('node-18', 'docker.io/library/node:18');
INSERT INTO images (name, image_ref) VALUES ('python-3.11', 'docker.io/library/python:3.11');

-- Kurse/Semester einfügen
INSERT INTO courses (name, description) VALUES 
('Web Security Basics', 'Grundlagen der Web-Sicherheit: SQL Injection, XSS, CSRF');

INSERT INTO courses (name, description) VALUES 
('Linux Administration', 'Linux Grundlagen und System Administration');

INSERT INTO courses (name, description) VALUES 
('Network Hacking', 'Netzwerk-Sicherheit und Penetration Testing');

-- Tasks einfügen (OHNE course_id, da Many-to-Many)
INSERT INTO tasks (title, description, points, image_id) VALUES 
('SQL Injection Challenge', 'Finde die Schwachstelle in der Login-Seite', 100, 2);

INSERT INTO tasks (title, description, points, image_id) VALUES 
('XSS Attack', 'Nutze Cross-Site Scripting um die Flag zu finden', 150, 2);

INSERT INTO tasks (title, description, points, image_id) VALUES 
('Linux Basics', 'Grundlegende Linux Befehle und Navigation', 50, 1);

INSERT INTO tasks (title, description, points, image_id) VALUES 
('Database Security', 'Sichere eine PostgreSQL Datenbank ab', 200, 3);

INSERT INTO tasks (title, description, points, image_id) VALUES 
('Port Scanning', 'Scanne das Netzwerk und finde offene Ports', 250, 1);

INSERT INTO tasks (title, description, points, image_id) VALUES 
('CSRF Protection', 'Implementiere CSRF-Schutz in einer Webanwendung', 120, 2);

INSERT INTO tasks (title, description, points, image_id) VALUES 
('File Upload Vulnerability', 'Finde und nutze eine File Upload Schwachstelle', 180, 2);

INSERT INTO tasks (title, description, points, image_id) VALUES 
('Privilege Escalation', 'Erlange Root-Rechte auf einem Linux-System', 300, 1);

INSERT INTO tasks (title, description, points, image_id) VALUES 
('Redis Security', 'Sichere einen Redis-Server ab', 150, 4);

INSERT INTO tasks (title, description, points, image_id) VALUES 
('Python Web Scraping', 'Erstelle einen Web Scraper mit Python', 100, 6);

INSERT INTO tasks (title, description, points, image_id) VALUES 
('API Security Testing', 'Teste eine REST API auf Sicherheitslücken', 200, 5);

INSERT INTO tasks (title, description, points, image_id) VALUES 
('Docker Container Escape', 'Versuche aus einem Docker Container auszubrechen', 400, 1);

-- Aufgaben zu Kursen zuordnen (Many-to-Many mit Reihenfolge)
-- Kurs 1 (Web Security Basics): 6 Aufgaben
INSERT INTO course_tasks (course_id, task_id, order_index) VALUES (1, 1, 1);  -- SQL Injection
INSERT INTO course_tasks (course_id, task_id, order_index) VALUES (1, 2, 2);  -- XSS Attack
INSERT INTO course_tasks (course_id, task_id, order_index) VALUES (1, 6, 3);  -- CSRF Protection
INSERT INTO course_tasks (course_id, task_id, order_index) VALUES (1, 7, 4);  -- File Upload Vulnerability
INSERT INTO course_tasks (course_id, task_id, order_index) VALUES (1, 4, 5);  -- Database Security (wiederverwendet!)
INSERT INTO course_tasks (course_id, task_id, order_index) VALUES (1, 11, 6); -- API Security Testing

-- Kurs 2 (Linux Administration): 5 Aufgaben
INSERT INTO course_tasks (course_id, task_id, order_index) VALUES (2, 3, 1);  -- Linux Basics
INSERT INTO course_tasks (course_id, task_id, order_index) VALUES (2, 4, 2);  -- Database Security
INSERT INTO course_tasks (course_id, task_id, order_index) VALUES (2, 8, 3);  -- Privilege Escalation
INSERT INTO course_tasks (course_id, task_id, order_index) VALUES (2, 9, 4);  -- Redis Security
INSERT INTO course_tasks (course_id, task_id, order_index) VALUES (2, 10, 5); -- Python Web Scraping

-- Kurs 3 (Network Hacking): 6 Aufgaben
INSERT INTO course_tasks (course_id, task_id, order_index) VALUES (3, 3, 1);  -- Linux Basics (wiederverwendet!)
INSERT INTO course_tasks (course_id, task_id, order_index) VALUES (3, 5, 2);  -- Port Scanning
INSERT INTO course_tasks (course_id, task_id, order_index) VALUES (3, 8, 3);  -- Privilege Escalation (wiederverwendet!)
INSERT INTO course_tasks (course_id, task_id, order_index) VALUES (3, 4, 4);  -- Database Security (wiederverwendet!)
INSERT INTO course_tasks (course_id, task_id, order_index) VALUES (3, 11, 5); -- API Security Testing (wiederverwendet!)
INSERT INTO course_tasks (course_id, task_id, order_index) VALUES (3, 12, 6); -- Docker Container Escape

-- Many-to-Many: Schüler zu Kursen zuordnen
INSERT INTO student_courses (user_id, course_id) VALUES (2, 1); -- john_doe -> Web Security Basics
INSERT INTO student_courses (user_id, course_id) VALUES (2, 2); -- john_doe -> Linux Administration
INSERT INTO student_courses (user_id, course_id) VALUES (3, 1); -- jane_smith -> Web Security Basics
INSERT INTO student_courses (user_id, course_id) VALUES (4, 1); -- dev_user -> Web Security Basics
INSERT INTO student_courses (user_id, course_id) VALUES (4, 2); -- dev_user -> Linux Administration
INSERT INTO student_courses (user_id, course_id) VALUES (4, 3); -- dev_user -> Network Hacking

-- Instances einfügen (mit task_id und course_id Verknüpfung)
INSERT INTO instances (container_id, name, image_id, user_id, task_id, course_id, status)
VALUES ('cont_1', 'web-server-1', 2, 2, 1, 1, 'running'); -- john_doe arbeitet an SQL Injection in Web Security Kurs

INSERT INTO instances (container_id, name, image_id, user_id, task_id, course_id, status)
VALUES ('cont_2', 'linux-basics-env', 1, 2, 3, 2, 'running'); -- john_doe arbeitet an Linux Basics in Linux Admin Kurs

INSERT INTO instances (container_id, name, image_id, user_id, task_id, course_id, status)
VALUES ('cont_3', 'xss-challenge', 2, 3, 2, 1, 'running'); -- jane_smith arbeitet an XSS in Web Security Kurs

INSERT INTO instances (container_id, name, image_id, user_id, task_id, course_id, status)
VALUES ('cont_4', 'db-security', 3, 4, 4, 2, 'stopped'); -- dev_user hat DB Security in Linux Admin gestoppt

INSERT INTO instances (container_id, name, image_id, user_id, task_id, course_id, status)
VALUES ('cont_5', 'port-scan-lab', 1, 4, 5, 3, 'created'); -- dev_user hat Port Scanning in Network Hacking erstellt

INSERT INTO live_environments (user_id, vnc_port, vnc_password, status)
VALUES (1, 5901, 'password123',  'stopped');

-- Questions einfügen (jetzt verknüpft mit tasks statt images)
INSERT INTO questions (task_id, frage, antworten, bestehgrenze_prozent, maximalpunkte)
VALUES (
    1, -- SQL Injection Challenge
    'Welches SQL-Statement kann eine Login-Seite umgehen?',
    '[
        {"text": "'' OR ''1''=''1", "richtig": true, "punkte": 10},
        {"text": "admin''; DROP TABLE users;--", "richtig": false, "punkte": 0},
        {"text": "1=1", "richtig": false, "punkte": 0},
        {"text": "'' OR 1=1--", "richtig": true, "punkte": 10}
    ]'::jsonb,
    60.00,
    10
);

INSERT INTO questions (task_id, frage, antworten, bestehgrenze_prozent, maximalpunkte)
VALUES (
    2, -- XSS Attack
    'Welcher Code führt zu Cross-Site Scripting?',
    '[
        {"text": "<script>alert(\"XSS\")</script>", "richtig": true, "punkte": 10},
        {"text": "<img src=x onerror=alert(1)>", "richtig": true, "punkte": 10},
        {"text": "<div>Normal HTML</div>", "richtig": false, "punkte": 0},
        {"text": "SELECT * FROM users", "richtig": false, "punkte": 0}
    ]'::jsonb,
    50.00,
    10
);

INSERT INTO questions (task_id, frage, antworten, bestehgrenze_prozent, maximalpunkte)
VALUES (
    3, -- Linux Basics
    'Welche Befehle werden verwendet, um Pakete in Ubuntu zu installieren?',
    '[
        {"text": "apt-get install", "richtig": true, "punkte": 5},
        {"text": "yum install", "richtig": false, "punkte": 0},
        {"text": "apt install", "richtig": true, "punkte": 5},
        {"text": "pacman -S", "richtig": false, "punkte": 0}
    ]'::jsonb,
    60.00,
    10
);

INSERT INTO questions (task_id, frage, antworten, bestehgrenze_prozent, maximalpunkte)
VALUES (
    4, -- Database Security
    'Welche SQL-Befehle werden für Datenbank-Operationen verwendet?',
    '[
        {"text": "SELECT", "richtig": true, "punkte": 4},
        {"text": "INSERT", "richtig": true, "punkte": 4},
        {"text": "COMPILE", "richtig": false, "punkte": 0},
        {"text": "UPDATE", "richtig": true, "punkte": 4},
        {"text": "RUN", "richtig": false, "punkte": 0}
    ]'::jsonb,
    70.00,
    12
);

-- Question Results Beispieldaten
INSERT INTO question_results (user_id, question_id, erreichte_punkte, bestanden)
VALUES (2, 1, 10, true); -- john_doe hat Frage 1 bestanden mit voller Punktzahl

INSERT INTO question_results (user_id, question_id, erreichte_punkte, bestanden)
VALUES (2, 2, 10, true); -- john_doe hat Frage 2 bestanden

INSERT INTO question_results (user_id, question_id, erreichte_punkte, bestanden)
VALUES (3, 1, 5, false); -- jane_smith hat Frage 1 nicht bestanden

INSERT INTO question_results (user_id, question_id, erreichte_punkte, bestanden)
VALUES (4, 3, 12, true); -- dev_user hat Frage 3 bestanden

-- Task Grades Beispieldaten
INSERT INTO task_grades (user_id, task_id, grade, passed, feedback) VALUES 
(2, 1, '1', true, 'Sehr gut gelöst! Alle Schwachstellen gefunden.');

INSERT INTO task_grades (user_id, task_id, grade, passed, feedback) VALUES 
(2, 3, '2', true, 'Gute Arbeit, alle Befehle korrekt verwendet.');

INSERT INTO task_grades (user_id, task_id, grade, passed, feedback) VALUES 
(3, 2, '4', false, 'Nicht alle XSS-Vektoren identifiziert.');

INSERT INTO task_grades (user_id, task_id, grade, passed, feedback) VALUES 
(4, 4, '1', true, 'Ausgezeichnete Absicherung der Datenbank.');

INSERT INTO task_grades (user_id, task_id, grade, passed) VALUES 
(4, 5, '3', true);
