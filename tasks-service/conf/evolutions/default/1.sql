# --- !Ups

CREATE TABLE tasks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(255) NOT NULL,
    notified BOOLEAN NOT NULL,
    PRIMARY KEY (id)
);


# --- !Downs
DROP TABLE tasks;