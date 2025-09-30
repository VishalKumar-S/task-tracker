# --- !Ups

-- Add the owner_id column, allowing it to be null temporarily.
ALTER TABLE tasks ADD COLUMN owner_id BIGINT;

-- Create a placeholder user with a FIXED ID of 1.
-- The `ON DUPLICATE KEY UPDATE` clause makes this script re-runnable without errors (idempotent).
INSERT INTO users (id, username, email, password_hash)
VALUES (1, 'legacy_owner', 'legacy@example.com', '---')
ON DUPLICATE KEY UPDATE id=1;

-- Assign all existing "ownerless" tasks to our new placeholder user with the known ID of 1.
UPDATE tasks SET owner_id = 1 where owner_id IS NULL;

-- Now that all tasks have a valid owner, make the column non-nullable.
ALTER TABLE tasks MODIFY COLUMN owner_id BIGINT NOT NULL;

-- With data integrity confirmed, add the foreign key constraint.
ALTER TABLE tasks ADD CONSTRAINT fk_owner_id FOREIGN KEY (owner_id) REFERENCES users(id);

# --- !Downs
ALTER TABLE tasks DROP FOREIGN KEY fk_owner_id;
ALTER TABLE tasks DROP COLUMN owner_id;

