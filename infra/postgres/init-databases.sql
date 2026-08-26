CREATE USER course_service WITH PASSWORD 'course_service';
CREATE DATABASE course_service OWNER course_service;

CREATE USER ingestion_service WITH PASSWORD 'ingestion_service';
CREATE DATABASE ingestion_service OWNER ingestion_service;

CREATE USER watchlist_service WITH PASSWORD 'watchlist_service';
CREATE DATABASE watchlist_service OWNER watchlist_service;

CREATE USER notification_service WITH PASSWORD 'notification_service';
CREATE DATABASE notification_service OWNER notification_service;
