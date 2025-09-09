# --- !Ups

ALTER TABLE tasks ADD COLUMN created_at DATETIME;
ALTER TABLE tasks ADD COLUMN updated_at DATETIME;

UPDATE tasks SET created_at = due_date, updated_at = due_date where created_at is NULL;


ALTER TABLE tasks MODIFY COLUMN created_at DATETIME NOT NULL;
ALTER TABLE tasks MODIFY COLUMN updated_at DATETIME NOT NULL;

# --- !Downs

ALTER TABLE tasks DROP COLUMN created_at;
ALTER TABLE tasks DROP COLUMN updated_at;



