-- H2 Database Schema

CREATE TABLE IF NOT EXISTS users (
    id INTEGER AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    class VARCHAR(10),
    role VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expired_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS images ( -- Represents the Exercise and will sended by the frontend.
    id INTEGER AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) UNIQUE,
    image_ref VARCHAR(255) UNIQUE
);

CREATE TABLE IF NOT EXISTS instances ( -- Finish Container Instances Table which can be detected over a user and the Image
    id INTEGER AUTO_INCREMENT PRIMARY KEY,
    container_id VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) UNIQUE,
    image_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    status VARCHAR(50) DEFAULT 'created',
    FOREIGN KEY(image_id) REFERENCES images(id),
    FOREIGN KEY(user_id) REFERENCES users(id)
);



CREATE TABLE IF NOT EXISTS live_environments (
     id INTEGER AUTO_INCREMENT PRIMARY KEY,
     user_id INTEGER UNIQUE NOT NULL,

    -- VNC Verbindungsdaten
    vnc_port INTEGER NOT NULL,
    vnc_host VARCHAR(255) DEFAULT 'localhost',
    vnc_password VARCHAR(255),

    -- Status
    status VARCHAR(50) DEFAULT 'stopped',


    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
    );