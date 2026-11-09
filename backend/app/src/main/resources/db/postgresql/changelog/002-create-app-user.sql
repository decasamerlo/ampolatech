--liquibase formatted sql

--changeset decasamerlo:002-create-app-user
-- "user" is reserved in PostgreSQL, hence the app_ prefix
-- (docs/conventions/database-naming.md).
CREATE TABLE app_user (
  id uuid NOT NULL,
  pharmacy_id uuid NOT NULL,
  name text NOT NULL,
  CONSTRAINT app_user_pkey PRIMARY KEY (id),
  -- Lets a foreign key reference (pharmacy_id, id), so that a row can point
  -- only at a user of its own pharmacy (ADR 0003).
  CONSTRAINT app_user_pharmacy_id_id_key UNIQUE (pharmacy_id, id),
  CONSTRAINT fk_app_user_pharmacy_id FOREIGN KEY (pharmacy_id) REFERENCES pharmacy (id),
  -- User refuses a blank name. The class lists what Java's String.isBlank
  -- counts as whitespace, rather than [:space:], whose members vary by locale.
  CONSTRAINT app_user_name_check CHECK (
    name ~ '[^\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]'
  )
);

GRANT SELECT ON app_user TO ampolatech_app;
