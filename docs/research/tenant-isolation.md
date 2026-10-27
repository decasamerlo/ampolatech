# Tenant data isolation in a shared PostgreSQL database

Date: 2026-10-20

**Summary.** The cloud vendors agree on the shape of the pool model (shared tables with a tenant column) and on the bar it must meet. They disagree on the mechanism. AWS states that "isolation enforcement should not be left to service developers" and that for PostgreSQL "RLS is required to maintain tenant data isolation in a pooled model" ([SaaS Lens](https://docs.aws.amazon.com/wellarchitected/latest/saas-lens/isolation-mindset.html), [Prescriptive Guidance](https://docs.aws.amazon.com/prescriptive-guidance/latest/saas-multitenant-managed-postgresql/rls.html)). Microsoft offers row-level security (RLS) as an option and notes that "many multitenant solutions don't use row-level security" because it is complex, so they rely on application code and a tenant identifier ([storage and data](https://learn.microsoft.com/en-us/azure/architecture/guide/multitenant/approaches/storage-data), [tenancy models](https://learn.microsoft.com/en-us/azure/architecture/guide/multitenant/considerations/tenancy-models)). Google publishes no general guide for the pool model. Its Spanner guide makes every query append a tenant predicate and rates that pattern "lowest level of data isolation" ([Spanner](https://docs.cloud.google.com/spanner/docs/implement-multi-tenancy)). OWASP asks for a boundary "traversed by every tenant-owned access path" and calls RLS defense in depth ([OWASP cheat sheet](https://cheatsheetseries.owasp.org/cheatsheets/Multi_Tenant_Security_Cheat_Sheet.html)).

- **(A) Explicit filtering in the application** is what most of the market does in practice. It fails by omission: one query without the predicate leaks. That is broken object-level authorization, which OWASP rates "Widespread" ([API1:2023](https://api-security.owasp.org/editions/2023/en/0xa1-broken-object-level-authorization)).
- **(B) A central ORM filter** is how the Rails, Django and Laravel libraries work, and how Hibernate's `@TenantId` works. It covers only the queries the ORM generates. Native SQL, raw JDBC and every escape hatch fall outside it. Some libraries silently drop the filter when no tenant is set. Hibernate 7.4 refuses to run instead, and has applied the filter to `find()` since 6.6 (verified locally).
- **(C) PostgreSQL RLS** is AWS's recommendation and Supabase's primary mechanism. It covers every statement the application role runs, native SQL included. It does not apply to superusers, `BYPASSRLS` roles, table owners (unless the table is `FORCE`d), views that run with the owner's rights, or referential-integrity checks. The tenant setting must be transaction-local, or it carries over to the next request on a pooled connection (verified locally).
- **Current practice is to layer them** (analysis). The most recent primary sources point the same way. OWASP's cheat sheet revised its RLS and transaction-scoped context guidance on 2026-08-31, and the Hibernate 8.0 beta adds generated RLS policies under `@TenantId` (B + C). For AmpolaTech, the [options at the end](#options-for-ampolatech-analysis) favour **A + C**: `PharmacyId` explicit in every port and query, with RLS as an independent backstop and composite foreign keys that include `pharmacy_id`.

How to read this file:

- Every claim links to the source it was checked against on 2026-10-20: the vendor's architecture docs, the PostgreSQL 18 manual, Hibernate's guides and source, or the framework's own docs. Where only a secondary source exists, the text says so.
- **Verified locally** marks behavior also reproduced on 2026-10-20 with PostgreSQL 18.4 (Docker `postgres:18`), Hibernate ORM 7.4.5.Final, HikariCP 7.0.2 and PostgreSQL JDBC 42.7.13. Spring Boot 4.1.1, the latest GA, manages exactly these library versions ([spring-boot-dependencies 4.1.1](https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.1/spring-boot-dependencies-4.1.1.pom)). The [appendix](#appendix-local-experiments) lists each run.
- Source-code links point at a fixed commit or tag.
- Text marked **analysis** is reasoning from the cited facts, not a sourced claim.

## 1. Cloud vendor guidance on the pool model

### AWS

**The bar.** The SaaS Lens says the pool model "has the potential to complicate the overall isolation story", because "typical networking and IAM constructs cannot be relied upon to create boundaries between tenants". It adds that "you cannot use this as a rationale to relax the isolation requirements of your environment. The shared model increases the chance for cross-tenant access" ([Pool isolation](https://docs.aws.amazon.com/wellarchitected/latest/saas-lens/pool-isolation.html)). Its tenets ([The isolation mindset](https://docs.aws.amazon.com/wellarchitected/latest/saas-lens/isolation-mindset.html)):

- "Isolation is not optional."
- "Authentication and authorization are not equal to isolation." Passing a login screen or an API gateway "does not mean you have achieved isolation".
- "Isolation enforcement should not be left to service developers – While developers are never expected to introduce code that might violate isolation, it's unrealistic to expect that they will never unintentionally cross a tenant boundary. To mitigate this, scoping of access to resources should be controlled through some shared mechanism that is responsible for applying isolation rules (outside the view of developers)."
- "Some high compliance industries may require that every tenant have its own database. In these cases, the shared, policy-based approaches to isolation may not be adequate."

A design principle goes further: "Limit developer awareness of multi-tenant concepts … a developer's experience writing a multi-tenant service should not be all that different from writing a service that has no notion of tenancy" ([General design principles](https://docs.aws.amazon.com/wellarchitected/latest/saas-lens/general-design-principles.html)). This conflicts directly with option A's explicitness. The [options](#options-for-ampolatech-analysis) come back to it.

**PostgreSQL specifically.** AWS Prescriptive Guidance:

- "Row-level security (RLS) is required to maintain tenant data isolation in a pooled model with PostgreSQL. RLS centralizes the enforcement of isolation policies at the database level and removes the burden of maintaining this isolation from software developers" ([RLS recommendations](https://docs.aws.amazon.com/prescriptive-guidance/latest/saas-multitenant-managed-postgresql/rls.html)).
- It compares two ways of identifying the tenant and picks the runtime variable: "The second option is preferred, because the first option requires the creation of a new PostgreSQL user for each tenant." It also says "you should enable RLS on all tables that contain tenant data." Its example policy is `USING (tenant_id = current_setting('app.current_tenant')::UUID)`. The example does not use `FORCE` (same page).
- "Some SaaS customers might not find the logical separation provided by RLS to be sufficient and might ask for additional isolation measures" ([PostgreSQL pool model](https://docs.aws.amazon.com/prescriptive-guidance/latest/saas-multitenant-managed-postgresql/pool.html)).
- The 2020 tenant-isolation whitepaper, now marked "for historical reference only", says the same: "With Aurora PostgreSQL, you cannot use IAM to scope access to data at the row level. Instead, you'll need to use the row level security (RLS) feature of PostgreSQL" ([Pooled storage isolation strategies](https://docs.aws.amazon.com/whitepapers/latest/saas-tenant-isolation-strategies/pooled-storage-isolation-strategies.html)).

**The AWS database blog post** that these pages link to ([Beardsley, 18 May 2020](https://aws.amazon.com/blogs/database/multi-tenant-data-isolation-with-postgresql-row-level-security/)):

- It frames the alternative as isolation "commonly implemented by hoping the correct WHERE clause is implemented in every SQL statement."
- "The table owner bypasses RLS policies unless the table is altered with FORCE ROW LEVEL SECURITY", so "you should have your application connect to the database as a user other than the owner of the database objects."
- "Using session variables may be incompatible with server-side connection pooling such as pgBouncer."
- Its Java sample sets a session-level value on every `getConnection()` by concatenating strings: `sql.execute("SET app.current_tenant = '" + TenantContext.getTenant() + "'")`. **Analysis:** don't copy it. A session-level `SET` survives the transaction (see [section 2](#2-postgresql-row-security)), and string concatenation is an injection path. OWASP's current guidance (below) uses a transaction-local setting.

### Microsoft Azure

- **The shared model relies on code.** "When multiple tenants share a single deployment (a set of infrastructure), you typically rely on your application code and a tenant identifier that's in a database to keep each tenant's data separate." Whatever the model, "be sure to test your solution to verify that one tenant's data isn't accidentally leaked to another" ([Tenancy models](https://learn.microsoft.com/en-us/azure/architecture/guide/multitenant/considerations/tenancy-models)).
- **RLS is optional and costly.** "Row-level security can provide security isolation for specific tenants' data in a shared multitenant database … When you use row-level security, you need to ensure that the user's identity and tenant identity are propagated through the application and into the data store with each query. This approach can be complex to design, implement, test, and maintain. Many multitenant solutions don't use row-level security because of those complexities." The same page lists table-per-tenant as an antipattern: "consider using a single set of multitenant tables with a tenant identifier column" ([Storage and data approaches](https://learn.microsoft.com/en-us/azure/architecture/guide/multitenant/approaches/storage-data), ms.date 2026-08-21).
- **For PostgreSQL:** "Row-level security is useful for enforcing tenant-level isolation when you use shared tables." "Implementing row-level security on a table might affect performance. You might need to create other indexes … use performance testing techniques." Azure Database for PostgreSQL offers `session_variable` and `login_hook` extensions for carrying the tenant into policies ([Azure Database for PostgreSQL in a multitenant solution](https://learn.microsoft.com/en-us/azure/architecture/guide/multitenant/service/postgresql), ms.date 2026-07-07).
- **SQL Server / Azure SQL RLS** lists the multitenant case directly ("A multitenant application can create a policy to enforce a logical separation of each tenant's data rows"). It says "RLS filter predicates are functionally equivalent to appending a `WHERE` clause." Its middle-tier example stores the tenant in `SESSION_CONTEXT` with `@read_only = 1`, which "prevents the value from changing again until the connection is closed (returned to the connection pool)" ([Row-Level Security](https://learn.microsoft.com/en-us/sql/relational-databases/security/row-level-security)). Unlike PostgreSQL, SQL Server applies policies to `dbo` and the table owner (same page).

### Google Cloud

No general Architecture Center guide on pool-model isolation for relational data turned up. The relevant pages are product-level:

- **Spanner** has four patterns. The row pattern (shared tables) has the "lowest level of data isolation" and "no tenant level security". "Each query must append a predicate `AND TENANT = tenant ID`", and "for better data isolation, this column value should be part of the primary key." The guide suggests the pattern for internal applications and free tiers ([Implement multi-tenancy in Spanner](https://docs.cloud.google.com/spanner/docs/implement-multi-tenancy), updated 2026-10-13).
- **Cloud SQL for PostgreSQL:** "Row-level security is useful for multi-tenant applications." It also says "table owners can also bypass row-level security" unless the table is forced, and that `BYPASSRLS` roles bypass it ([Data privacy strategies](https://docs.cloud.google.com/sql/docs/postgres/data-privacy-strategies), updated 2026-10-15).
- **AlloyDB** offers "parameterized secure views", which it describes as "suitable for multi-tenant SaaS applications without requiring separate database roles for each application end user" ([Parameterized secure views](https://docs.cloud.google.com/gemini/data-agents/querydata/alloydb/secure-app-data-parameterized-secure-views)). This is AlloyDB-only.

### OWASP (vendor-neutral)

The [Multi-Tenant Security Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Multi_Tenant_Security_Cheat_Sheet.html) (last revised 2026-10-03; RLS sections clarified on 2026-08-31, per its [commit history](https://github.com/OWASP/CheatSheetSeries/commits/master/cheatsheets/Multi_Tenant_Security_Cheat_Sheet.md)) is the most concrete current guidance found:

- For shared tables, its strategy table reads: "Requires enforceable tenant ownership, policy coverage for classified tenant-owned tables, constrained request roles, and negative-path tests."
- "Enforce tenant ownership at a boundary traversed by every tenant-owned access path." "Database controls such as RLS or schema and credential separation can provide defense in depth."
- For PostgreSQL it forces RLS on table owners, warns that `FORCE` "does not constrain a superuser or a role with the `BYPASSRLS` attribute", and makes omitting `missing_ok` "a deliberate fail-closed choice".
- Tenant context goes in the transaction: "Begin a transaction, set the tenant context with `SET LOCAL`, or call `set_config('app.current_tenant', tenant_id, true)` … Re-establish the tenant context for every transaction … Commit or roll back before returning the connection to the pool."
- Testing should run "through the same role, connection path, and pooling mode used by the application". It should derive the list of tenant tables from the schema and check `relrowsecurity`, `relforcerowsecurity` and `pg_policies`. It should also assert that the request role lacks `rolsuper` and `rolbypassrls`, and "run requests for two tenants over reused connections."

### Where A, B and C stand in this guidance

| Source | A: explicit in code | B: ORM filter | C: database RLS |
|---|---|---|---|
| AWS | Calls it "hoping the correct WHERE clause is implemented in every SQL statement" | Not named; would match its "shared mechanism … outside the view of developers" tenet (analysis) | "Required" for pooled PostgreSQL |
| Azure | The default ("rely on your application code") | Not discussed | Optional; "complex to design, implement, test, and maintain" |
| Google (Spanner) | The row pattern itself ("each query must append a predicate") | Not discussed | Not described for Spanner's row pattern; RLS documented for Cloud SQL |
| OWASP | "Include tenant scope in queries" | Its SQLAlchemy example is a B-style session filter | Defense in depth, with explicit test duties |

**Analysis.** No vendor recommends A alone as best practice. AWS and OWASP want enforcement that doesn't depend on every query being written correctly. Azure acknowledges that most real systems still use only A, for cost reasons.

## 2. PostgreSQL row security

All quotes are from the PostgreSQL 18 manual (current; pages labelled 18.6).

### How policies work

- With RLS enabled, "all normal access to the table for selecting rows or modifying rows must be allowed by a row security policy." "If no policy exists for the table, a default-deny policy is used" ([5.9 Row Security Policies](https://www.postgresql.org/docs/current/ddl-rowsecurity.html)).
- `USING` filters existing rows and `WITH CHECK` validates new ones. Rows that fail `USING` are "silently suppressed", while a `WITH CHECK` failure raises an error. A policy with only `USING` uses it for both ([CREATE POLICY](https://www.postgresql.org/docs/current/sql-createpolicy.html)). Verified locally: an `UPDATE` of another tenant's row reports `UPDATE 0`, and an `INSERT` for another tenant fails with "new row violates row-level security policy" (A7, A8).
- Permissive policies combine with `OR` and restrictive ones with `AND`. "There must be at least one `PERMISSIVE` policy granting access", and "all of the `RESTRICTIVE` policies must pass" (same page).
- A policy expression "will be evaluated for each row prior to any conditions or functions coming from the user's query", except for `leakproof` functions ([5.9](https://www.postgresql.org/docs/current/ddl-rowsecurity.html)).

### Who bypasses RLS

- "Superusers and roles with the `BYPASSRLS` attribute always bypass the row security system when accessing a table. Table owners normally bypass row security as well, though a table owner can choose to be subject to row security with `ALTER TABLE ... FORCE ROW LEVEL SECURITY`" ([5.9](https://www.postgresql.org/docs/current/ddl-rowsecurity.html)). Verified locally: the owner saw both tenants' rows with no tenant set until the table was forced (T1, T2).
- "Only superuser roles or roles with `BYPASSRLS` can specify `BYPASSRLS`." `pg_dump` sets `row_security` off, and "superusers and the owner of the table being dumped always bypass RLS" ([CREATE ROLE](https://www.postgresql.org/docs/current/sql-createrole.html)).
- **Views:** "by default, the row-level security policies of the view owner are applied", unless the view sets `security_invoker` ([CREATE VIEW](https://www.postgresql.org/docs/current/sql-createview.html)). Supabase puts it bluntly: "Views bypass RLS by default because they are usually created with the `postgres` user" ([Supabase RLS](https://supabase.com/docs/guides/database/postgres/row-level-security)).
- **Functions:** Supabase's performance guide uses "a security definer function to bypass RLS on the join table" ([RLS performance](https://supabase.com/docs/guides/database/postgres/row-level-security-performance)). A `SECURITY DEFINER` function owned by the table owner is therefore an opening as well as a tool.
- **Test containers:** the official image's `POSTGRES_USER` "will create the specified user with superuser power" ([postgres image docs](https://github.com/docker-library/docs/blob/bda29b139fc5320c7f5d8a2620743eb943aa92c5/postgres/content.md)). A test connecting with the container's default credentials never exercises RLS.

### What RLS doesn't cover

- "Referential integrity checks, such as unique or primary key constraints and foreign key references, always bypass row security." The manual warns of "covert channel" leaks ([5.9](https://www.postgresql.org/docs/current/ddl-rowsecurity.html)). The CREATE POLICY notes describe probing for a value's existence through a unique-key violation or a foreign-key insert ([CREATE POLICY](https://www.postgresql.org/docs/current/sql-createpolicy.html)). Verified locally: under tenant A, an insert whose single-column foreign key referenced tenant B's patient succeeded (A9). A unique violation revealed that B's id exists (A11). A composite foreign key `(pharmacy_id, patient_id)` rejected the same insert (A10).
- "Operations that apply to the whole table, such as `TRUNCATE` and `REFERENCES`, are not subject to row security" ([5.9](https://www.postgresql.org/docs/current/ddl-rowsecurity.html)).
- "Currently, `COPY FROM` is not supported for tables with row-level security. Use equivalent `INSERT` statements instead" ([COPY](https://www.postgresql.org/docs/current/sql-copy.html)). Verified locally (A13).

### Carrying the tenant into the session

- `current_setting(name)` "throws an error unless *missing_ok* is supplied and is `true` (in which case NULL is returned)". `set_config(name, value, is_local)`: "If *is_local* is `true`, the new value will only apply during the current transaction" ([System administration functions](https://www.postgresql.org/docs/current/functions-admin.html#FUNCTIONS-ADMIN-SET)). `SET` takes its value as a constant or identifier in the statement text ([SET](https://www.postgresql.org/docs/current/sql-set.html)). `set_config` takes it as a function argument, so it can be a bind parameter: H2 below ran it from a `PreparedStatement` with `?`.
- `SET LOCAL` lasts "only till the end of the current transaction, whether committed or not". Issued "outside of a transaction block [it] emits a warning and otherwise has no effect". A plain `SET` committed in a transaction persists "until the end of the session" ([SET](https://www.postgresql.org/docs/current/sql-set.html)). Verified locally (A2, A5).
- PostgreSQL "will accept a setting for any two-part parameter name" ([Customized Options](https://www.postgresql.org/docs/current/runtime-config-custom.html)). Verified locally: the unprivileged application role switched itself to another tenant by running `SET LOCAL` (A6). **Analysis:** a policy keyed on a session setting trusts whoever can run SQL as the application role. It stops a forgotten predicate. It does not stop an attacker who can execute arbitrary SQL, for example through SQL injection. Per-tenant database roles close that gap, at the cost AWS describes.
- Observed but not documented in the pages read: after a transaction that used `SET LOCAL` ends, the setting reads back as `''` rather than `NULL`, and `''::uuid` raises an error (A4). Both outcomes fail closed. Hibernate 8's generated policy wraps the value in `nullif(…, '')`, which turns the empty string into `NULL` ([source](https://github.com/hibernate/hibernate-orm/blob/8.0.0.Beta3/hibernate-core/src/main/java/org/hibernate/dialect/rowsecurity/internal/PostgreSQLRowLevelSecurity.java#L33-L40)).

### Connection pools

- **HikariCP** (the pool Spring Boot 4.1.1 manages, at 7.0.2) resets six properties on return: read-only, auto-commit, isolation, catalog, network timeout and schema ([`PoolBase.resetConnectionState`](https://github.com/brettwooldridge/HikariCP/blob/a4d93f4f85517f90e632b795486d7102e933d7ff/src/main/java/com/zaxxer/hikari/pool/PoolBase.java#L213-L251)). It rolls back a connection closed with uncommitted work ([`ProxyConnection.close`](https://github.com/brettwooldridge/HikariCP/blob/a4d93f4f85517f90e632b795486d7102e933d7ff/src/main/java/com/zaxxer/hikari/pool/ProxyConnection.java#L249-L251)). It does not reset custom settings. Verified locally with a one-connection pool: a session-level `SET` reached the next borrower. A transaction-local `set_config` did not, after either commit or close without commit (H1–H3).
- **PgBouncer** in transaction mode lists `SET/RESET` as "Never" supported, and "a server connection is assigned to a client only during a transaction" ([PgBouncer features](https://www.pgbouncer.org/features.html)). **Analysis:** a transaction-local setting stays on the same server connection for the whole transaction, so it works with transaction pooling. A session `SET` does not.
- **The Rails library acts_as_tenant** documents this exact leak for its RLS hook: "Always handle `nil`, otherwise the setting stays on the database connection and the next request using it runs as the previous tenant" ([README](https://github.com/ErwinM/acts_as_tenant/blob/4fa406f723eda249e3584b65693869ea4e1bec0b/README.md?plain=1#L385-L399)).

### Performance

- Policies that "consider only the current values in the row" are "the simplest and best-performing case" ([5.9](https://www.postgresql.org/docs/current/ddl-rowsecurity.html)).
- Supabase's benchmark on a 100K-row table: indexing the policy column took a query from 171 ms to under 0.1 ms. Adding the same filter explicitly to the query took it from 171 ms to 9 ms ([RLS performance and best practices](https://supabase.com/docs/guides/troubleshooting/rls-performance-and-best-practices-Z5Jjwv)). Its guide says "always add a filter … even though this duplicates the contents of the policy, Postgres can use the filter to construct a better query plan" ([RLS performance](https://supabase.com/docs/guides/database/postgres/row-level-security-performance)).
- Verified locally on 1M rows across 50 tenants: the planner used `pharmacy_id = current_setting('app.current_pharmacy')::uuid` as an index condition on an index leading with `pharmacy_id` (P1).
- Verified locally: non-leakproof user predicates can't use their own index under RLS. `textlike` (`LIKE`) is not leakproof. Under RLS, a prefix `LIKE` stopped using its `text_pattern_ops` index and filtered all of the tenant's rows instead; the owner's plan used the index (L1 versus the owner run). Equality (`texteq`, leakproof) still used it (L2). This follows from the leakproof rule in [CREATE POLICY](https://www.postgresql.org/docs/current/sql-createpolicy.html). **Analysis:** text search inside one pharmacy scans that pharmacy's rows. That is acceptable at pharmacy scale, but it needs measuring.

### Security fixes touching RLS

The PostgreSQL security page lists four RLS CVEs in supported branches ([security](https://www.postgresql.org/support/security/)):

- [CVE-2026-14666](https://www.postgresql.org/support/security/CVE-2026-14666/): cached policies outlive role changes. Fixed in 18.6 on 2026-08-13.
- [CVE-2024-10976](https://www.postgresql.org/support/security/CVE-2024-10976/) and [CVE-2023-2455](https://www.postgresql.org/support/security/CVE-2023-2455/): plans reused across user ID changes.
- [CVE-2023-39418](https://www.postgresql.org/support/security/CVE-2023-39418/): `MERGE` skipping `UPDATE`/`SELECT` policies in 15.

All three plan-reuse CVEs concern cases where "role-specific" policies are used and a query planned under one role runs under another. **Analysis:** a single application role with one setting-based policy sits outside their described scenario. Staying on the latest minor release still matters. The local experiments ran on 18.4, which predates the 18.6 fix; the CVE does not touch the behaviors tested.

## 3. Hibernate and Spring

Hibernate ORM 7.4 is the latest stable series (7.4.13.Final, 2026-10-11). 8.0 is in development (8.0.0.Beta3, 2026-09-25) ([releases](https://hibernate.org/orm/releases/)). Spring Boot 4.1.1 manages 7.4.5.Final ([BOM](https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.1/spring-boot-dependencies-4.1.1.pom)).

### `@TenantId` in 7.4

- **Behavior.** "Within a given session, our data is automatically filtered so that only rows tagged with the tenant id of the current tenant are visible in that session. Native SQL queries are not automatically filtered by tenant id; you'll have to do that part yourself" ([Introduction to Hibernate 7.4, §9.5](https://docs.hibernate.org/orm/7.4/introduction/html_single/#multitenancy)). The field "is automatically populated by Hibernate when an instance is made persistent" ([User Guide 7.4, §24.3.1](https://docs.hibernate.org/orm/7.4/userguide/html_single/#multitenacy-hibernate-TenantId)). The current tenant comes from a `CurrentTenantIdentifierResolver` (same guide).
- **Mechanism.** `@TenantId` registers a filter named `_tenantId` with `applyToLoadByKey = true` and appends `<column> = :tenantId` to the entity ([`TenantIdBinder`](https://github.com/hibernate/hibernate-orm/blob/1a2b6bd457417311eea935e1444b63c9f01b0d55/hibernate-core/src/main/java/org/hibernate/binder/internal/TenantIdBinder.java#L48-L83), [`FilterDefinition` constructor](https://github.com/hibernate/hibernate-orm/blob/1a2b6bd457417311eea935e1444b63c9f01b0d55/hibernate-core/src/main/java/org/hibernate/engine/spi/FilterDefinition.java#L49-L55)).
- **Covered on 7.4.5 (verified locally):**
  - HQL and Criteria selects (E2, E3)
  - `find()` and `getReference()` of another tenant's id (E1, E7)
  - a lazy to-one association to another tenant's row, which raised `ObjectNotFoundException` (E10)
  - HQL bulk `update` and `delete` (E4, E5)
  - `merge()` of a forged copy of another tenant's row, which failed on the primary key instead of overwriting it (E8)
  - no tenant set, which raised an exception (E11)

  The 8.0 docs state the bulk coverage: "Bulk updates and deletes expressed in HQL or Criteria also apply the mapped tenant restrictions" ([MultiTenancy.adoc on main](https://github.com/hibernate/hibernate-orm/blob/24e81f56cf9a136ab2f65d9151752ebae7fbec2c/documentation/src/main/asciidoc/userguide/chapters/multitenancy/MultiTenancy.adoc#L168-L183)). The 7.4 guides don't say it.
- **Not covered:**
  - **Direct SQL.** "Native queries, stored procedures, JDBC callbacks, and user-written SQL are not automatically rewritten to add tenant predicates" (same file). Verified locally: a native `count(*)` returned both tenants' rows (E6).
  - **HQL insert.** "HQL `insert ... values` and `insert ... select` statements do not invoke the entity's `@TenantId` value generator" (same file). Verified locally: the tenant column arrived `NULL` (E12).
  - **Disabling the filter.** Any code holding the session can call `session.disableFilter("_tenantId")`, as Hibernate's own [tests](https://github.com/hibernate/hibernate-orm/blob/1a2b6bd457417311eea935e1444b63c9f01b0d55/hibernate-core/src/test/java/org/hibernate/orm/test/tenantid/TenantIdTest.java#L117-L123) do. Verified locally (E9). A resolver can also declare a "root" tenant through `isRoot` that sees every partition ([MultiTenancy.adoc](https://github.com/hibernate/hibernate-orm/blob/24e81f56cf9a136ab2f65d9151752ebae7fbec2c/documentation/src/main/asciidoc/userguide/chapters/multitenancy/MultiTenancy.adoc#L129-L144)).
  - **Anything that isn't Hibernate:** `JdbcTemplate`, Spring Data JDBC, jOOQ, database functions and views (**analysis**, from the boundary above).
- **History.** Before 6.6, filters, and therefore `@TenantId`, were not applied to `find()`. The issue states that "users are complaining that this almost amounts to some kind of a 'security' hole since filters are used to implement discriminator-based multi-tenancy". It was fixed in 6.6.0.CR1 ([HHH-16830](https://hibernate.atlassian.net/browse/HHH-16830)). An open 2023 report says updates and deletes don't carry the tenant id ([HHH-16889](https://hibernate.atlassian.net/browse/HHH-16889)); the bulk and merge paths above did not reproduce it on 7.4.5.

### `@Filter` in 7.4

`@Filter` is the general form: a named, parameterized native-SQL condition. "By default, a new session comes with every filter disabled", and since 6.5 a filter can be `autoEnabled`. "Filters are not applied to `@ManyToOne` associations, nor to `find()`. This is completely by design" ([Introduction 7.4, §9.1](https://docs.hibernate.org/orm/7.4/introduction/html_single/#filters)). The User Guide adds the opt-in: "Filters apply to entity queries, but not to direct fetching, unless otherwise configured using the `applyToLoadByKey` flag" ([User Guide 7.4, §6.9.3](https://docs.hibernate.org/orm/7.4/userguide/html_single/#pc-filter)). For tenancy, `@TenantId` already configures this, so a hand-made `@Filter` adds nothing.

### Hibernate 8.0: RLS generated from `@TenantId`

The 8.0 betas add an incubating setting, `hibernate.multi_tenant.rls_enabled`, which "defaults to `true` where the dialect supports the required capabilities and the `@TenantId` mapping qualifies" ([MultiTenancy.adoc §Row-level security](https://github.com/hibernate/hibernate-orm/blob/24e81f56cf9a136ab2f65d9151752ebae7fbec2c/documentation/src/main/asciidoc/userguide/chapters/multitenancy/MultiTenancy.adoc#L321-L354); [setting in 8.0.0.Beta3](https://github.com/hibernate/hibernate-orm/blob/8.0.0.Beta3/hibernate-core/src/main/java/org/hibernate/cfg/MultiTenancySettings.java#L96-L106)). On PostgreSQL, Hibernate:

- generates `enable row level security`, `force row level security` and one policy with matching `using` and `with check` clauses
- sets the tenant after each transaction begins with `select set_config('hibernate.tenant_id', ?, true)`, plus a root flag ([`PostgreSQLRowLevelSecurity`](https://github.com/hibernate/hibernate-orm/blob/8.0.0.Beta3/hibernate-core/src/main/java/org/hibernate/dialect/rowsecurity/internal/PostgreSQLRowLevelSecurity.java#L33-L92), [`afterTransactionBegin`](https://github.com/hibernate/hibernate-orm/blob/8.0.0.Beta3/hibernate-core/src/main/java/org/hibernate/internal/AbstractSharedSessionContract.java#L1251-L1283))
- throws "Row-level security enabled, but no tenant identifier specified" when the tenant is missing

The docs warn: "Policies must actually be deployed. If Hibernate does not manage schema creation, arrange equivalent policy deployment through your schema-management process … a bypass role may see data that an ordinary role cannot." This is the framework adopting B + C, with transaction-local context. It is beta and incubating, the RLS doc section exists only on `main`, and Spring Boot 4.1 ships 7.4.

### Spring

No tenancy mechanism of Spring's own turned up in the Spring Boot and Spring Data material read. Spring's own example registers a Hibernate `CurrentTenantIdentifierResolver` through a `HibernatePropertiesCustomizer`. It warns that with partitioned tables "the SQL-based query does not take the tenant into account" ([Spring blog, Schauder, 2022-07-31](https://spring.io/blog/2022/07/31/how-to-integrate-hibernates-multitenant-feature-with-spring-data-jpa-in-a-spring-boot-application/)). Two documented hooks run code at transaction begin, which is where a transaction-local tenant setting belongs:

- `DataSourceTransactionManager.prepareTransactionalConnection`: "Prepare the transactional `Connection` right after transaction begin … override this method accordingly" ([Javadoc, Spring Framework 7.0.10](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/datasource/DataSourceTransactionManager.html)). This covers JDBC and jOOQ.
- `HibernateJpaDialect.beginTransaction`, which is public on a non-final class ([Javadoc](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/orm/jpa/vendor/HibernateJpaDialect.html)). This covers JPA.

Flyway can run as a different user from the application: "Setting either `spring.flyway.url` or `spring.flyway.user` is sufficient to cause Flyway to use its own `DataSource`" ([Spring Boot data initialization](https://docs.spring.io/spring-boot/how-to/data-initialization.html)).

### jOOQ

jOOQ "policies" are "jOOQ's primary way to implement client side row level security by transforming DML statements". They exist in commercial editions only. Their security notes say policies cover only jOOQ expression trees, not plain SQL templates, views or stored functions. Anyone with the JDBC driver or the `Configuration` can get around them. The notes recommend pairing them with the database's own policies, such as PostgreSQL's `POLICY` ([Policies](https://www.jooq.org/doc/latest/manual/sql-building/queryparts/policies/), [Security considerations](https://www.jooq.org/doc/latest/manual/sql-building/queryparts/policies/policies-security/), manual 3.21).

## 4. How frameworks and products do it

| Product or library | Mechanism | What it says about the gaps |
|---|---|---|
| [Supabase](https://supabase.com/docs/guides/database/postgres/row-level-security) | C as the primary boundary; clients reach PostgreSQL through its API | "A table in an exposed schema without RLS is readable and writable by any role with a grant on it." The secret key maps to `service_role`, "which has the `bypassrls` attribute." Views bypass RLS unless `security_invoker`. |
| [Citus](https://docs.citusdata.com/en/stable/use_cases/multi_tenant.html) | Tenant column on every table, used for distribution | "We must ensure all tables have a `company_id` column." "Citus requires that primary and foreign key constraints include the distribution column." RLS on distributed tables is supported "starting in version 11.0", with a role-per-tenant example ([cluster management](https://docs.citusdata.com/en/latest/admin_guide/cluster_management.html)). |
| [acts_as_tenant](https://github.com/ErwinM/acts_as_tenant/blob/4fa406f723eda249e3584b65693869ea4e1bec0b/README.md?plain=1#L198-L205) (Rails) | B via `default_scope` | Not scoped: "any query when no tenant is set, unless `require_tenant` is enabled", `unscoped`, "raw SQL, such as `find_by_sql`, `connection.execute`", and `update_column(s)`. Offers an RLS hook (see [section 2](#connection-pools)). |
| [django-multitenant](https://github.com/citusdata/django-multitenant/blob/72cb58970e707922facd8d5674d10b1027ba63eb/README.md?plain=1#L136-L169) (Citus) | B via managers | "If the current_tenant is not set, then the default/native API without tenant scoping is used." Its PostgreSQL backend creates "composite foreign keys (with tenant_id) at the db layer." |
| [Tenancy for Laravel](https://tenancyforlaravel.com/docs/v3/single-database-tenancy), single-database mode | B via global scopes | "It can't do anything with low level database queries"; `DB` facade calls "won't be scoped"; it costs "larger code complexity than multi-database tenancy". |
| [SQL Server / Azure SQL](https://learn.microsoft.com/en-us/sql/relational-databases/security/row-level-security) | C via `SESSION_CONTEXT` set by the middle tier | Read-only context per pooled connection; policies also bind owners. |
| [AlloyDB](https://docs.cloud.google.com/gemini/data-agents/querydata/alloydb/secure-app-data-parameterized-secure-views) | Parameterized secure views | One shared role and no per-user database roles. |

**Analysis.** Web frameworks default to B. The libraries' own READMEs list raw SQL and "no tenant set" as uncovered, and two of them fail open in that case. Products where clients reach the database directly (Supabase, PostgREST-style APIs) use C, because no application layer exists to do A or B. The data-distribution products (Citus, django-multitenant) also put the tenant into keys and foreign keys, which guards references between tables independently of any filter.

## 5. Known failure modes

### A: explicit filtering in the application

- **The forgotten predicate.** OWASP's top API risk, broken object-level authorization, is "usually implemented at the code level" and rated "Widespread". Its prevention advice includes tests, and "do not deploy changes that make the tests fail" ([API1:2023](https://api-security.owasp.org/editions/2023/en/0xa1-broken-object-level-authorization)). AWS calls never crossing a boundary by accident "unrealistic" ([SaaS Lens](https://docs.aws.amazon.com/wellarchitected/latest/saas-lens/isolation-mindset.html)). Laravel's docs warn "you might still accidentally access another tenant's comments" ([single-database tenancy](https://tenancyforlaravel.com/docs/v3/single-database-tenancy)).
- **Cross-tenant references.** A filter on reads doesn't stop a write that points at another tenant's row through a single-column foreign key. Verified locally (A9). Composite keys stop it (A10), as Citus and django-multitenant do.
- **Paths that aren't queries.** OWASP's cheat sheet lists cache keys, asynchronous jobs, file storage and restores as separate places where tenant scope must be carried ([cheat sheet](https://cheatsheetseries.owasp.org/cheatsheets/Multi_Tenant_Security_Cheat_Sheet.html)). This is true for all three approaches.
- No public primary-source postmortem attributing a leak to a forgotten tenant `WHERE` clause was found. The evidence for this failure mode is OWASP's prevalence rating and the vendors' statements.

### B: a central ORM filter

- **Direct SQL bypasses it.** Every library documents this: Hibernate, Spring's example, acts_as_tenant, Laravel and jOOQ ([section 3](#3-hibernate-and-spring), [section 4](#4-how-frameworks-and-products-do-it)). Verified locally for Hibernate 7.4.5 (E6).
- **Gaps in what the ORM generates.** Hibernate's filter did not apply to `find()` until 6.6 ([HHH-16830](https://hibernate.atlassian.net/browse/HHH-16830)). HQL `insert` still doesn't set the tenant (E12).
- **Fail-open defaults.** django-multitenant drops the scope when no tenant is set. acts_as_tenant does the same unless `require_tenant` is on (section 4). Hibernate 7.4 fails closed (E11).
- **Escape hatches in reach of any code:** `disableFilter("_tenantId")`, `unscoped`, `withoutTenancy()`, a root tenant.

### C: PostgreSQL RLS

- **Missing or insufficient policy.** [CVE-2025-48757](https://nvd.nist.gov/vuln/detail/CVE-2025-48757) reads: "An insufficient database Row-Level Security policy in Lovable through 2025-04-15 allows remote unauthenticated attackers to read or write to arbitrary database tables of generated sites." The vendor disputes it, since each customer configures their own application. The advisory names "missing or insufficient Row Level Security (RLS) policies" as the cause, in apps whose browser code used a "public and unprivileged `anon` key" ([advisory](https://mattpalmer.io/posts/CVE-2025-48757/)). **Analysis:** there, RLS was the only boundary, so one missing policy meant a full read.
- **The wrong role.** Owners without `FORCE`, superusers and `BYPASSRLS` roles see everything (section 2). An application that connects as the migration user has RLS switched off without any error.
- **Owner-context objects:** views without `security_invoker`, and `SECURITY DEFINER` functions (section 2).
- **Tenant setting leaking across pooled connections.** Session `SET` survives the transaction, and HikariCP doesn't reset it (H1). acts_as_tenant warns of exactly this. The same class of bug hit ChatGPT in March 2023, in a cache client rather than a database: a cancelled request left a pooled redis-py connection "corrupted", and "the next response that's dequeued for an unrelated request can receive data left behind in the connection". The same bug "may have caused the unintentional visibility of payment-related information of 1.2% of the ChatGPT Plus subscribers who were active during a specific nine-hour window" ([OpenAI, 2023-03-24](https://openai.com/index/march-20-chatgpt-outage/), read through a [Wayback snapshot](https://web.archive.org/web/20250101055603/https://openai.com/index/march-20-chatgpt-outage/) because the page returns 403 to scripts).
- **Covert channels through constraints** (A9, A11), and **the database trusting the setting** (A6).
- **Operational surprises:** `COPY FROM` refused (A13); non-leakproof predicates losing their index (L1); PostgreSQL's own RLS CVEs (section 2).

## Options for AmpolaTech (analysis)

Everything in this section is analysis, built on the facts above.

[The author's values](../conventions/author-values.md) point at three tests. **Explicit:** can a reader see the tenant in the code? **Correct:** can one mistake leak data? **Principled:** is the boundary something the code can prove?

### The options compared

| Option | What you write | What it stops | What still gets through | How tests prove it |
|---|---|---|---|---|
| **A alone** | A `PharmacyId` parameter on every port method, and `pharmacy_id = ?` in every query | Nothing structural; each query must be correct | One forgotten predicate, a wrong id passed down, cross-tenant foreign keys (unless composite) | Per-method tests with a second pharmacy, which are only as complete as the developer who writes them |
| **B alone** (`@TenantId`) | Mapping plus a resolver backed by a `ThreadLocal` | Forgotten predicates in HQL, Criteria, `find()` and bulk statements | Native SQL, jOOQ, `JdbcTemplate`, `disableFilter`, HQL insert, a misconfigured resolver; tenancy disappears from the port signatures | Hibernate-level tests; nothing covers the SQL Hibernate doesn't generate |
| **C alone** (RLS) | Policies in every migration, two database roles, a transaction-begin hook, `FORCE` | Forgotten predicates in any SQL the application role runs, native included | Wrong role, missing policy on a new table, a session-scoped setting, constraint channels, SQL injection resetting the setting | Database-level tests through the real role and pool, plus a schema coverage check |
| **A + C** | Both of the above | A leak needs a code bug **and** a database misconfiguration at the same time | Paths outside the database (caches, files, jobs) and SQL injection on a query that also lacks its predicate | Each layer tested on its own (below) |
| **B + C** | Hibernate 8's direction, or a hand-written hook today | Like A + C for ORM paths | Tenancy is implicit in code; Hibernate 8's RLS is beta and incubating | As for C |

**A + C fits the stated values best.** The two layers fail for unrelated reasons. A fails through a code omission; C fails through database configuration. Covering both meets the developer-independent enforcement AWS and OWASP ask for, and keeps the tenant visible in every signature and query. The duplicated predicate also helps the planner (Supabase's guidance; P2). B adds a third, implicit mechanism that covers less than C, so it buys little once C exists. B + C becomes worth a second look when Hibernate 8.0 is GA and Spring Boot ships it, if the team wants to give up the explicit predicate in exchange for less code.

### What A + C costs in Spring Boot and PostgreSQL

**Schema conventions (every migration):**
- `pharmacy_id uuid not null` on every tenant-owned table.
- A primary key or unique key on `(pharmacy_id, id)`, and composite foreign keys `(pharmacy_id, <ref>_id) references <ref> (pharmacy_id, id)`. This closes the reference channel that RLS leaves open (A9, A10).
- Indexes leading with `pharmacy_id`.
- Truly global tables (shared reference data) are listed as such and granted read-only.

**Two database roles:**
- An owner role that owns the tables and runs Flyway, via `spring.flyway.user`.
- A runtime role created `NOSUPERUSER NOBYPASSRLS`, with DML grants only.
- Every tenant table gets `ENABLE` and `FORCE ROW LEVEL SECURITY`, plus a policy `USING (pharmacy_id = current_setting('app.pharmacy_id')::uuid) WITH CHECK (…same…)`. Leaving out `missing_ok` makes an unset tenant an error, not an empty result.
- If role-specific permissive policies come later, make the tenant policy `AS RESTRICTIVE` so it is always ANDed in.

**The tenant reaching the session:**
- The first statement of every transaction is `select set_config('app.pharmacy_id', ?, true)`, with the value as a bind parameter. Never a session-level `SET`, and never string concatenation.
- Put it in one transaction-begin hook: a `DataSourceTransactionManager` subclass for JDBC or jOOQ, or a `HibernateJpaDialect` subclass for JPA.
- Database work outside a transaction then fails, by design. Reads run in `@Transactional(readOnly = true)`.
- This works with HikariCP, which rolls back connections returned mid-transaction, and with PgBouncer transaction pooling.

**Two independent tenant sources.** The hook takes the pharmacy from the authenticated principal (Spring Security's context). The port's `PharmacyId` argument comes from the use case. If the hook read the port argument instead, one bug that passes the wrong id would fool both layers at once. With independent sources, a mismatch returns nothing, and the adapter can assert equality and fail loudly.

**Cross-pharmacy work:** platform reports and support tooling run on a separate DataSource and role. That path is explicitly authorized and audited, and never shares the request path (OWASP).

**Tests (Testcontainers):**
- Connect as the runtime role, never the image's superuser.
- Every repository test sets up two pharmacies. Cross-tenant reads, updates and deletes return nothing, an insert for the other pharmacy is rejected, and a cross-tenant foreign key is rejected.
- **Prove C without A:** run raw SQL with no tenant predicate under pharmacy A and see only A's rows. With no tenant set, expect an error.
- **Prove A without C:** run the same repository tests on a connection whose role has `BYPASSRLS`, so only the explicit predicates stand.
- A schema-derived coverage test: every table with a `pharmacy_id` column has `relrowsecurity`, `relforcerowsecurity` and a row in `pg_policies`. Tables without one must be on an explicit global list.
- A role test: the runtime role has neither `rolsuper` nor `rolbypassrls`.
- A pool test: pool size 1, a request as A, then a request as B; B sees nothing of A, and the setting is empty after commit and after rollback.
- An architecture test that every repository port method takes a `PharmacyId`.

**Performance work:** composite indexes, measuring text search inside a pharmacy (L1), and checking plans under the runtime role, not the owner.

### Why it matters under LGPD

Health data is "dado pessoal sensível" (Art. 5, II). Art. 46 requires "medidas de segurança, técnicas e administrativas aptas a proteger os dados pessoais de acessos não autorizados", and its §2 requires them "desde a fase de concepção do produto". An incident notice under Art. 48 §1 must include "a indicação das medidas técnicas e de segurança utilizadas para a proteção dos dados" ([Lei 13.709/2018](https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm)). **Analysis:** a database-enforced boundary with tests that prove each layer is a concrete measure to describe in that notice. A convention that every query includes a filter is weaker.

## Not verified

- No public primary-source postmortem was found that attributes a cross-tenant leak to a forgotten `WHERE` clause (A) or to an ORM filter bypass (B). A secondary-source report of a 2025 cross-tenant incident at a project-management vendor gave no root cause and is not used.
- Google Cloud may have pool-model guidance beyond the Spanner, Cloud SQL and AlloyDB pages; none was found.
- The 7.4 Hibernate guides don't state `@TenantId` coverage for bulk HQL. It was verified by experiment and is stated in the 8.0 docs.
- The `''`-after-`SET LOCAL` behavior is observed (A4), not documented in the pages read.
- PgBouncer's docs have no row for `SET LOCAL`. Its compatibility with transaction pooling is inferred from "assigned to a client only during a transaction".
- HikariCP's README doesn't document what it resets. That claim rests on its source code.
- Hibernate 8.0's RLS support was read in source and docs, not run, and may change before GA.

## Appendix: local experiments

Run on 2026-10-20: PostgreSQL 18.4 in Docker; tables owned by `owner_role`; an application role `app_role NOSUPERUSER NOBYPASSRLS`. Each tenant table had one policy, `USING (pharmacy_id = current_setting('app.current_pharmacy')::uuid)`. The Hibernate run used Hibernate 7.4.5.Final with `@TenantId` on a `uuid` column, with HikariCP 7.0.2 and PostgreSQL JDBC 42.7.13.

| Id | What was done | Result |
|---|---|---|
| T1 | Owner selects, no `FORCE`, no tenant set | Sees both tenants |
| T2 | `FORCE ROW LEVEL SECURITY`, owner selects | Error without a tenant; one row with tenant A |
| A1 | App role selects, no tenant set | `unrecognized configuration parameter` (fails closed) |
| A2 | `SET LOCAL` outside a transaction | `WARNING: SET LOCAL can only be used in transaction blocks`; no value |
| A3 | `SET LOCAL` inside a transaction | Only tenant A's row |
| A4 | Read the setting after that transaction | `''`, not `NULL`; the policy's cast raises `invalid input syntax for type uuid` |
| A5 | `SET` (session) inside a committed transaction | Still in effect after commit |
| A6 | App role sets tenant B itself | Sees tenant B's row |
| A7 | Insert a row for tenant B under tenant A | `new row violates row-level security policy` |
| A8 | Update tenant B's row under tenant A | `UPDATE 0` |
| A9 | Insert with a single-column FK to tenant B's patient | Succeeds |
| A10 | Same insert with a composite FK `(pharmacy_id, patient_id)` | Foreign-key violation |
| A11 | Insert a duplicate of tenant B's unique id under tenant A | Unique violation (existence revealed) |
| A12 | `set_config(…, true)` inside a transaction | Policy applies |
| A13 | `COPY … FROM STDIN` into an RLS table | `COPY FROM not supported with row-level security` |
| P1 | 1M rows, 50 tenants: latest 20 rows under RLS only | Index scan with the policy as `Index Cond` |
| P2 | Same query with an explicit `pharmacy_id` filter | Index scan on the literal, plus a one-time filter |
| L1 | Prefix `LIKE` with a `text_pattern_ops` index, under RLS | Index on `pharmacy_id`, `LIKE` as a filter over 20,000 rows; the owner (no RLS) used the pattern index |
| L2 | Equality on the same column, under RLS | Pattern index used |
| E1 | Hibernate: tenant B, `find()` of tenant A's id | `null` |
| E2 | HQL `select` | Tenant B's rows only |
| E3 | Criteria `select` | Tenant B's rows only |
| E4 | HQL `update Patient set …` with no `where` | 1 row (tenant B's only) |
| E5 | HQL `delete … where id = <A's id>` | 0 rows |
| E6 | Native `select count(*)` | 2 (both tenants) |
| E7 | `getReference(<A's id>)` and access | `ObjectNotFoundException` |
| E8 | `merge()` of a forged entity carrying A's id | Primary-key violation; A's row unchanged |
| E9 | `disableFilter("_tenantId")` | Both tenants visible |
| E10 | Tenant B's prescription with a lazy `@ManyToOne` to A's patient | `ObjectNotFoundException` |
| E11 | No tenant from the resolver | `HibernateException: … no tenant identifier specified` |
| E12 | HQL `insert` without the tenant column | `null value in column "pharmacy_id"` |
| H1 | Hikari, pool size 1: session `SET`, return, borrow again | Previous tenant still set |
| H2 | `set_config(…, true)`, commit, borrow again | `''` |
| H3 | `set_config(…, true)`, close without commit, borrow again | `''` (Hikari rolled back) |
