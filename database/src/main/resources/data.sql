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

-- Instances einfügen
INSERT INTO instances (container_id, name, image_id, user_id, status)
VALUES ('cont_1', 'web-server-1', 2, 2, 'running');

INSERT INTO instances (container_id, name, image_id, user_id, status)
VALUES ('cont_2', 'database-prod', 3, 1, 'running');

INSERT INTO instances (container_id, name, image_id, user_id, status)
VALUES ('cont_3', 'cache-server', 4, 3, 'running');

INSERT INTO instances (container_id, name, image_id, user_id, status)
VALUES ('cont_4', 'app-backend', 5, 4, 'stopped');

INSERT INTO instances (container_id, name, image_id, user_id, status)
VALUES ('cont_5', 'test-environment', 1, 5, 'created');

INSERT INTO instances (container_id, name, image_id, user_id, status)
VALUES ('cont_6', 'python-service', 6, 4, 'running');

INSERT INTO instances (container_id, name, image_id, user_id, status)
VALUES ('cont_7', 'nginx-proxy', 2, 1, 'running');

INSERT INTO instances (container_id, name, image_id, user_id, status)
VALUES ('cont_8', 'dev-database', 3, 4, 'stopped');



INSERT INTO live_environments (user_id, vnc_port, vnc_password, status)
VALUES (1, 5901, 'password123',  'stopped');