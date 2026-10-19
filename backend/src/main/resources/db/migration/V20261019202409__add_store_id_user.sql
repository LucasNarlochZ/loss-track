ALTER TABLE users
ADD COLUMN store_id UUID NULL,
ADD CONSTRAINT fk_users_store
    FOREIGN KEY (store_id)
            REFERENCES stores(id);