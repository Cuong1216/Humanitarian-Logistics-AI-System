CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE disaster_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,
    location VARCHAR(255) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    reported_at TIMESTAMP NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE social_media_posts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    disaster_event_id UUID,
    source VARCHAR(50) NOT NULL,
    author VARCHAR(100),
    content TEXT NOT NULL,
    posted_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_disaster_event 
        FOREIGN KEY (disaster_event_id) 
        REFERENCES disaster_events(id) 
        ON DELETE SET NULL
);

CREATE TABLE analysis_results (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    post_id UUID UNIQUE NOT NULL,
    urgency_score INT NOT NULL,
    intent VARCHAR(100),
    location_extracted VARCHAR(255),
    analyzed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_social_media_post
        FOREIGN KEY (post_id)
        REFERENCES social_media_posts(id)
        ON DELETE CASCADE
);
