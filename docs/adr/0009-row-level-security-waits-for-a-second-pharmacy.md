# Row-level security waits for a second pharmacy

[ADR 0003](0003-one-instance-for-all-pharmacies.md) isolates each pharmacy's data twice, in the code and in the database. AmpolaTech starts with one pharmacy, and the database half, PostgreSQL's row-level security, keeps one pharmacy's rows from another's, so until a second pharmacy exists it protects nothing. Meanwhile it would ask every query to run in a transaction, every transaction to set the pharmacy before its first statement, and every seed and test that writes as the table owner to set it too. The database half waits for a second pharmacy; the code half stays.

- **What stays.** Every pharmacy-owned row carries its `pharmacy_id`, every repository port method takes the `PharmacyId` and every query filters on it, and foreign keys between pharmacy-owned tables include `pharmacy_id`. Tests that touch data set up two pharmacies. The app connects as a role that owns no tables and can't bypass row-level security, and migrations run as the owner, as `postgres/initdb/create-roles.sh` sets them up.
- **What waits.** The policies, with `ENABLE` and `FORCE`; the transaction-local setting that carries the signed-in user's pharmacy to them; the read-only role with `BYPASSRLS`; and the tests that prove the database layer on its own: the isolation tests repeated under a role that bypasses row-level security, the SQL test with no filter, and the schema test.

Row-level security comes back before a second pharmacy's data goes in. #5, the ticket that would have built it, was closed with what #105 found about it on the pinned versions: where a transaction-begin hook can set the pharmacy, why every query then needs a transaction, and what it costs.

## Considered options

- **Row-level security now**, as ADR 0003 decided. It stops a query that forgets its pharmacy filter from returning another pharmacy's rows, and while one pharmacy exists, there are none to return.
- **Dropping the code half too.** Taking `pharmacy_id` out of the tables and `PharmacyId` out of the ports would save a parameter now, and cost a migration of every table and a rewrite of every port, query and test when a second pharmacy comes. Row-level security adds no column and changes no row, so it can come back over the data as it is.
