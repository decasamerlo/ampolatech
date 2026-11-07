# One instance for all pharmacies, isolated in the code and in the database

All pharmacies share one deployment and one PostgreSQL database. Every row that belongs to a pharmacy carries its `pharmacy_id` from the first version, even while there is only one pharmacy. The data is health data, so isolation is enforced twice, by two mechanisms that fail for unrelated reasons:

- **In the code.** Every repository port method takes a `PharmacyId`, and every query filters on it. The pharmacy is visible in every signature and every query.
- **In the database.** Every pharmacy-owned table has PostgreSQL row-level security with `ENABLE` and `FORCE`, and a policy on `pharmacy_id`. The app connects as a role that owns no tables and has neither `SUPERUSER` nor `BYPASSRLS`; migrations run as the owner. Each transaction sets the pharmacy with a transaction-local `set_config`, taken from the signed-in user rather than from the port argument, so one bug can't defeat both layers. With no pharmacy set, a query fails instead of returning every pharmacy's rows. [ADR 0009](0009-row-level-security-waits-for-a-second-pharmacy.md) defers the policies and the setting, with the read-only role and the tests below that serve them, until a second pharmacy; the roles the app and the migrations connect as stay.

Foreign keys between pharmacy-owned tables include `pharmacy_id`, because referential checks bypass row-level security.

Bypassing is deliberate and visible. A person who needs to read across pharmacies uses a read-only role with `BYPASSRLS`; the app never does. A data migration that must touch every pharmacy's rows lifts `FORCE` inside its own transaction (`NO FORCE`, then `FORCE` again), so the exemption is written in the migration and ends with it.

Tests prove each layer on its own, and every test sets up two pharmacies:

- Repository tests also run under a role that bypasses row-level security, so only the explicit filters protect the data.
- A database test runs SQL with no filter, so only the policies protect the data.
- A schema test checks that every table with a `pharmacy_id` has row-level security, `FORCE` and a policy, and that the app's role is neither superuser nor `BYPASSRLS`.

## Considered options

- **A deployment and database per pharmacy.** The strongest separation, but one more instance to run for every pharmacy. The author prefers to operate one.
- **A filter in every query, alone.** It is the usual approach, and it fails by omission: one query without the filter leaks.
- **A central ORM filter** (Hibernate's `@TenantId`). Implicit, it misses native SQL and plain JDBC, and any code can switch it off.

The survey behind this is [tenant-isolation.md](../research/tenant-isolation.md).
