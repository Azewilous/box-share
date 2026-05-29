ALTER TABLE files
    ADD COLUMN owner_id BIGINT,
    ADD CONSTRAINT fk_owner_id FOREIGN KEY (owner_id) REFERENCES users (id);

UPDATE files f
SET owner_id = u.id
FROM users u
WHERE u.username = f.uploaded_by OR u.email = f.uploaded_by;

ALTER TABLE files
    DROP COLUMN uploaded_by;