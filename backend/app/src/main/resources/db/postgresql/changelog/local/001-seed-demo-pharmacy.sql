--liquibase formatted sql

--changeset decasamerlo:local-001-seed-demo-pharmacy
-- A demo pharmacy and its user, whom the demo sign-in signs everyone in as.
-- Their ids and the user's roles are the demo sign-in's, which
-- application-local.yml passes in as changelog parameters. The roles come
-- comma-separated, a row each, trimmed as Spring trims each role it binds.
INSERT INTO pharmacy (id, name)
VALUES ('${demo-pharmacy-id}', 'Farmácia Exemplo');

INSERT INTO app_user (id, pharmacy_id, name)
VALUES ('${demo-user-id}', '${demo-pharmacy-id}', 'Joana Exemplo');

INSERT INTO app_user_role (pharmacy_id, user_id, role)
SELECT '${demo-pharmacy-id}', '${demo-user-id}', trim(role)
FROM unnest(string_to_array('${demo-roles}', ',')) AS role;
