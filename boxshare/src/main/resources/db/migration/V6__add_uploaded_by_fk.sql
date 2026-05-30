ALTER TABLE files ADD COLUMN IF NOT EXISTS owner_id BIGINT;

DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_owner_id' AND table_name = 'files'
    ) THEN
        ALTER TABLE files ADD CONSTRAINT fk_owner_id FOREIGN KEY (owner_id) REFERENCES users (id);
    END IF;
END $$;

DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'files' AND column_name = 'uploaded_by') THEN
        UPDATE files f
        SET owner_id = u.id
        FROM users u
        WHERE u.username = f.uploaded_by OR u.email = f.uploaded_by;

        ALTER TABLE files DROP COLUMN uploaded_by;
    END IF;
END $$;
