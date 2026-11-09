--liquibase formatted sql

--changeset decasamerlo:003-create-app-user-role
-- The roles a user holds, one row each. A user may hold none.
CREATE TABLE app_user_role (
  pharmacy_id uuid NOT NULL,
  user_id uuid NOT NULL,
  role text NOT NULL,
  CONSTRAINT app_user_role_pkey PRIMARY KEY (user_id, role),
  -- References app_user_pharmacy_id_id_key, so that a row can point only at a
  -- user of its own pharmacy (ADR 0003, ADR 0009).
  CONSTRAINT fk_app_user_role_pharmacy_id_user_id FOREIGN KEY (pharmacy_id, user_id)
    REFERENCES app_user (pharmacy_id, id),
  CONSTRAINT app_user_role_role_check CHECK (role IN ('PHARMACIST', 'SALES_AGENT'))
);

GRANT SELECT ON app_user_role TO ampolatech_app;
