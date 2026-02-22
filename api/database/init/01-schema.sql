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

CREATE TABLE IF NOT EXISTS courses ( -- Semester/Kurse als Sammlung von Aufgaben
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tasks ( -- Einzelne Aufgaben/Challenges innerhalb eines Kurses
    id SERIAL PRIMARY KEY,
    course_id INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    points INTEGER NOT NULL DEFAULT 0,
    image_id INTEGER NOT NULL, -- Referenz zum Docker Image
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(course_id) REFERENCES courses(id) ON DELETE CASCADE,
    FOREIGN KEY(image_id) REFERENCES images(id) ON DELETE CASCADE
);

-- Many-to-Many: Ein Kurs kann beliebig viele Schüler haben und umgekehrt
CREATE TABLE IF NOT EXISTS student_courses (
    user_id INTEGER NOT NULL,
    course_id INTEGER NOT NULL,
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP, -- Optional: Zeitbegrenzter Zugriff
    PRIMARY KEY(user_id, course_id), -- Composite Primary Key für Many-to-Many
    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY(course_id) REFERENCES courses(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS instances ( -- Finish Container Instances Table which can be detected over a user and the Image
    id SERIAL PRIMARY KEY,
    container_id VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) UNIQUE,
    image_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    task_id INTEGER, -- Optional: Verknüpfung zur Aufgabe
    status VARCHAR(50) DEFAULT 'created',
    FOREIGN KEY(image_id) REFERENCES images(id),
    FOREIGN KEY(user_id) REFERENCES users(id),
    FOREIGN KEY(task_id) REFERENCES tasks(id) ON DELETE SET NULL
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
    task_id INTEGER NOT NULL, -- Geändert von image_id zu task_id
    frage TEXT NOT NULL,
    antworten JSONB NOT NULL, -- JSON array mit Antworten [{text: "...", richtig: true/false, punkte: 10}, ...]
    bestehgrenze_prozent DECIMAL(5,2) NOT NULL DEFAULT 50.00, -- Bestehgrenze in Prozent (0.00-100.00)
    maximalpunkte INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(task_id) REFERENCES tasks(id) ON DELETE CASCADE
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

CREATE TABLE IF NOT EXISTS task_grades ( -- Benotung von Aufgaben
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    task_id INTEGER NOT NULL,
    grade VARCHAR(10) NOT NULL, -- z.B. "1", "2", "3", "4", "5" oder "A", "B", "C"
    passed BOOLEAN NOT NULL DEFAULT FALSE,
    feedback TEXT, -- Optional: Feedback vom Lehrer
    graded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY(task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    UNIQUE(user_id, task_id) -- Ein User kann pro Task nur eine Benotung haben
);
