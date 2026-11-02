--liquibase formatted sql

--changeset decasamerlo:local-001-seed-demo-pharmacy
-- A demo pharmacy and its sales agent, whom the demo sign-in signs everyone
-- in as. Their ids and the agent's role are the demo sign-in's, which
-- application-local.yml passes in as changelog parameters.
INSERT INTO pharmacy (id, name)
VALUES ('${demo-pharmacy-id}', 'Farmácia Exemplo');

INSERT INTO app_user (id, pharmacy_id, name, role)
VALUES ('${demo-user-id}', '${demo-pharmacy-id}', 'Joana Exemplo', '${demo-role}');
