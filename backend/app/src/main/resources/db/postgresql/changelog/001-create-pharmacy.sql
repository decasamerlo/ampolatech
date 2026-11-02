--liquibase formatted sql

--changeset decasamerlo:001-create-pharmacy
CREATE TABLE pharmacy (
  id uuid NOT NULL,
  name text NOT NULL,
  CONSTRAINT pharmacy_pkey PRIMARY KEY (id),
  -- Pharmacy refuses a blank name. The class lists what Java's String.isBlank
  -- counts as whitespace, rather than [:space:], whose members vary by locale.
  CONSTRAINT pharmacy_name_check CHECK (
    name ~ '[^\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]'
  )
);

GRANT SELECT ON pharmacy TO ampolatech_app;
