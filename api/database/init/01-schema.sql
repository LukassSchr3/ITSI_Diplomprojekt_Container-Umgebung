-- PostgreSQL Database Schema

CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    class VARCHAR(10),
    role VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expired_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS images ( -- Represents the Exercise and will sended by the frontend.
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE,
    image_ref VARCHAR(255) UNIQUE
);

CREATE TABLE IF NOT EXISTS instances ( -- Finish Container Instances Table which can be detected over a user and the Image
    id SERIAL PRIMARY KEY,
    container_id VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) UNIQUE,
    image_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    status VARCHAR(50) DEFAULT 'created',
    FOREIGN KEY(image_id) REFERENCES images(id),
    FOREIGN KEY(user_id) REFERENCES users(id)
);



CREATE TABLE IF NOT EXISTS live_environments (
     id SERIAL PRIMARY KEY,
     user_id INTEGER UNIQUE NOT NULL,

    -- VNC Verbindungsdaten
    vnc_port INTEGER NOT NULL,
    vnc_host VARCHAR(255) DEFAULT 'localhost',
    vnc_password VARCHAR(255),

    -- Status
    status VARCHAR(50) DEFAULT 'stopped',


    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS questions (
    id SERIAL PRIMARY KEY,
    image_id INTEGER NOT NULL,
    frage TEXT NOT NULL,
    antworten JSONB NOT NULL, -- JSON array mit Antworten [{text: "...", richtig: true/false, punkte: 10}, ...]
    bestehgrenze_prozent DECIMAL(5,2) NOT NULL DEFAULT 50.00, -- Bestehgrenze in Prozent (0.00-100.00)
    maximalpunkte INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(image_id) REFERENCES images(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS question_results (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    question_id INTEGER NOT NULL,
    erreichte_punkte INTEGER NOT NULL DEFAULT 0,
    bestanden BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY(question_id) REFERENCES questions(id) ON DELETE CASCADE,
    UNIQUE(user_id, question_id) -- Ein User kann jede Frage nur einmal beantworten
);
